package com.example.campustask.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.example.campustask.data.CommunityDbHelper;

/**
 * 成员D负责的ContentProvider组件。
 * 暴露社区公共数据（forum_posts、marketplace_items、market_bids表），
 * URI:
 *   content://com.example.campustask.community/posts
 *   content://com.example.campustask.community/items
 *   content://com.example.campustask.community/bids
 * 使用campus_community.db公共社区数据库。
 */
public class CommunityProvider extends ContentProvider {
    public static final String AUTHORITY = "com.example.campustask.community";
    public static final String PATH_POSTS = "forum_posts";
    public static final String PATH_ITEMS = "marketplace_items";
    public static final String PATH_BIDS = "market_bids";

    public static final Uri POSTS_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_POSTS);
    public static final Uri ITEMS_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_ITEMS);
    public static final Uri BIDS_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_BIDS);

    private static final int CODE_POSTS = 1;
    private static final int CODE_POST_ID = 2;
    private static final int CODE_ITEMS = 3;
    private static final int CODE_ITEM_ID = 4;
    private static final int CODE_BIDS = 5;
    private static final int CODE_BID_ID = 6;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, PATH_POSTS, CODE_POSTS);
        URI_MATCHER.addURI(AUTHORITY, PATH_POSTS + "/#", CODE_POST_ID);
        URI_MATCHER.addURI(AUTHORITY, PATH_ITEMS, CODE_ITEMS);
        URI_MATCHER.addURI(AUTHORITY, PATH_ITEMS + "/#", CODE_ITEM_ID);
        URI_MATCHER.addURI(AUTHORITY, PATH_BIDS, CODE_BIDS);
        URI_MATCHER.addURI(AUTHORITY, PATH_BIDS + "/#", CODE_BID_ID);
    }

    private CommunityDbHelper dbHelper;

    private void ensureDbHelper() {
        if (dbHelper == null) {
            dbHelper = new CommunityDbHelper(getContext());
        }
    }

    private String tableForCode(int code) {
        switch (code) {
            case CODE_POSTS:
            case CODE_POST_ID:
                return PATH_POSTS;
            case CODE_ITEMS:
            case CODE_ITEM_ID:
                return PATH_ITEMS;
            case CODE_BIDS:
            case CODE_BID_ID:
                return PATH_BIDS;
            default:
                return null;
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        ensureDbHelper();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int match = URI_MATCHER.match(uri);
        String table = tableForCode(match);
        if (table == null) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        Cursor cursor;
        if (match == CODE_POSTS || match == CODE_ITEMS || match == CODE_BIDS) {
            String defaultSort = PATH_POSTS.equals(table) || PATH_ITEMS.equals(table)
                    ? "created_at DESC" : "price DESC, created_at DESC";
            cursor = db.query(table, projection, selection, selectionArgs, null, null,
                    sortOrder == null ? defaultSort : sortOrder);
        } else {
            cursor = db.query(table, projection, "id=?",
                    new String[]{String.valueOf(ContentUris.parseId(uri))}, null, null, null);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int match = URI_MATCHER.match(uri);
        String table = tableForCode(match);
        if (table == null) {
            return null;
        }
        boolean isDir = match == CODE_POSTS || match == CODE_ITEMS || match == CODE_BIDS;
        String typePrefix = isDir ? "vnd.android.cursor.dir" : "vnd.android.cursor.item";
        return typePrefix + "/vnd.campustask." + table;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        ensureDbHelper();
        int match = URI_MATCHER.match(uri);
        String table = tableForCode(match);
        if (table == null || match == CODE_POST_ID || match == CODE_ITEM_ID || match == CODE_BID_ID) {
            throw new IllegalArgumentException("Insert not supported for URI: " + uri);
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(table, null, values);
        if (id > 0) {
            Uri newUri = ContentUris.withAppendedId(uri, id);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        ensureDbHelper();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        String table = tableForCode(match);
        if (table == null) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int rows;
        if (match == CODE_POSTS || match == CODE_ITEMS || match == CODE_BIDS) {
            rows = db.delete(table, selection, selectionArgs);
        } else {
            rows = db.delete(table, "id=?",
                    new String[]{String.valueOf(ContentUris.parseId(uri))});
        }
        if (rows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        ensureDbHelper();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        String table = tableForCode(match);
        if (table == null) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int rows;
        if (match == CODE_POSTS || match == CODE_ITEMS || match == CODE_BIDS) {
            rows = db.update(table, values, selection, selectionArgs);
        } else {
            rows = db.update(table, values, "id=?",
                    new String[]{String.valueOf(ContentUris.parseId(uri))});
        }
        if (rows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }
}
