package com.example.campustask.model;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 自研轻量级 xlsx 解析器（不依赖 Apache POI）。
 * xlsx 本质是 zip 压缩包，内含 XML 文件：
 * - xl/sharedStrings.xml：共享字符串表（可能为空）
 * - xl/worksheets/sheet1.xml：工作表数据
 * <p>
 * 支持两种字符串存储方式：
 * - 共享字符串（t="s"）：值在 <v> 标签中，内容为 sharedStrings 索引
 * - 内联字符串（t="inlineStr"）：值在 <is><t> 标签中，直接存储文本
 */
public final class XlsxScheduleParser {

    private XlsxScheduleParser() {
    }

    public static List<List<String>> parse(InputStream input) {
        List<String> sharedStrings = new ArrayList<>();
        String sheetXml = null;

        try (ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("xl/sharedStrings.xml")) {
                    sharedStrings = parseSharedStrings(zis);
                } else if (name.equals("xl/worksheets/sheet1.xml")) {
                    sheetXml = readStream(zis);
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("无法读取 xlsx 文件: " + e.getMessage());
        }

        if (sheetXml == null) {
            throw new IllegalArgumentException("xlsx 文件中未找到工作表");
        }

        return parseSheet(sheetXml, sharedStrings);
    }

    private static List<String> parseSharedStrings(InputStream input) {
        List<String> strings = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(input, "UTF-8");
            int event = parser.getEventType();
            StringBuilder current = new StringBuilder();
            boolean inSi = false;
            boolean inT = false;
            while (event != XmlPullParser.END_DOCUMENT) {
                String tag = parser.getName();
                if (event == XmlPullParser.START_TAG) {
                    if ("si".equals(tag)) {
                        current.setLength(0);
                        inSi = true;
                    } else if ("t".equals(tag) && inSi) {
                        inT = true;
                    }
                } else if (event == XmlPullParser.TEXT && inT) {
                    current.append(parser.getText());
                } else if (event == XmlPullParser.END_TAG) {
                    if ("t".equals(tag)) {
                        inT = false;
                    } else if ("si".equals(tag)) {
                        strings.add(current.toString());
                        inSi = false;
                    }
                }
                event = parser.next();
            }
        } catch (Exception e) {
            // sharedStrings 解析失败时返回空列表
        }
        return strings;
    }

    private static String readStream(InputStream input) {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[4096];
        int len;
        try {
            while ((len = input.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len, "UTF-8"));
            }
        } catch (Exception e) {
            // ignore
        }
        return sb.toString();
    }

    private static List<List<String>> parseSheet(String xml, List<String> sharedStrings) {
        List<List<String>> rows = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new java.io.StringReader(xml));
            int event = parser.getEventType();
            List<String> currentRow = null;
            Map<Integer, String> cellMap = null;
            int maxCol = 0;
            boolean inRow = false;
            boolean inCell = false;
            // 内联字符串状态：<is> 内的 <t> 标签
            boolean inInlineString = false;
            boolean inInlineT = false;
            // 共享字符串/数字状态：<v> 标签
            boolean inValue = false;
            String cellType = null;
            String cellRef = null;
            StringBuilder currentValue = new StringBuilder();

            while (event != XmlPullParser.END_DOCUMENT) {
                String tag = parser.getName();
                if (event == XmlPullParser.START_TAG) {
                    if ("row".equals(tag)) {
                        currentRow = new ArrayList<>();
                        cellMap = new HashMap<>();
                        maxCol = 0;
                        inRow = true;
                    } else if ("c".equals(tag) && inRow) {
                        inCell = true;
                        cellType = parser.getAttributeValue(null, "t");
                        cellRef = parser.getAttributeValue(null, "r");
                        currentValue.setLength(0);
                    } else if ("v".equals(tag) && inCell) {
                        // 共享字符串索引或数字值
                        inValue = true;
                    } else if ("is".equals(tag) && inCell) {
                        // 内联字符串开始 <is>
                        inInlineString = true;
                    } else if ("t".equals(tag) && inInlineString) {
                        // 内联字符串的文本内容 <is><t>...</t></is>
                        inInlineT = true;
                    }
                } else if (event == XmlPullParser.TEXT) {
                    if (inValue) {
                        currentValue.append(parser.getText());
                    } else if (inInlineT) {
                        currentValue.append(parser.getText());
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    if ("v".equals(tag)) {
                        inValue = false;
                    } else if ("t".equals(tag) && inInlineT) {
                        inInlineT = false;
                    } else if ("is".equals(tag)) {
                        inInlineString = false;
                    } else if ("c".equals(tag)) {
                        String value = resolveCellValue(cellType, currentValue.toString(), sharedStrings);
                        int colIndex = parseColumnIndex(cellRef);
                        cellMap.put(colIndex, value);
                        if (colIndex > maxCol) {
                            maxCol = colIndex;
                        }
                        inCell = false;
                        cellType = null;
                        cellRef = null;
                        inValue = false;
                        inInlineString = false;
                        inInlineT = false;
                    } else if ("row".equals(tag)) {
                        for (int c = 0; c <= maxCol; c++) {
                            String v = cellMap.get(c);
                            currentRow.add(v == null ? "" : v);
                        }
                        rows.add(currentRow);
                        inRow = false;
                    }
                }
                event = parser.next();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("解析工作表失败: " + e.getMessage());
        }
        return rows;
    }

    private static String resolveCellValue(String cellType, String rawValue, List<String> sharedStrings) {
        if (rawValue == null || rawValue.isEmpty()) {
            return "";
        }
        if ("s".equals(cellType)) {
            // 共享字符串类型，值为索引
            try {
                int index = Integer.parseInt(rawValue);
                if (index >= 0 && index < sharedStrings.size()) {
                    return sharedStrings.get(index);
                }
            } catch (NumberFormatException e) {
                return rawValue;
            }
            return "";
        }
        // inlineStr 类型或数字类型，直接返回文本值
        return rawValue;
    }

    private static int parseColumnIndex(String cellRef) {
        if (cellRef == null || cellRef.isEmpty()) {
            return 0;
        }
        int col = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char c = cellRef.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                col = col * 26 + (c - 'A' + 1);
            } else {
                break;
            }
        }
        return col - 1;
    }
}
