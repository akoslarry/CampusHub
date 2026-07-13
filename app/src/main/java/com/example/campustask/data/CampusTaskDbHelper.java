package com.example.campustask.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.campustask.model.AccountDatabaseRules;

public class CampusTaskDbHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 7;

    public CampusTaskDbHelper(Context context, String username) {
        super(context, AccountDatabaseRules.businessDatabaseName(username), null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE courses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "teacher TEXT NOT NULL DEFAULT '', " +
                "location TEXT NOT NULL DEFAULT '', " +
                "color INTEGER NOT NULL, " +
                "weekday INTEGER NOT NULL DEFAULT 1, " +
                "start_section INTEGER NOT NULL DEFAULT 1, " +
                "end_section INTEGER NOT NULL DEFAULT 2, " +
                "start_week INTEGER NOT NULL DEFAULT 1, " +
                "end_week INTEGER NOT NULL DEFAULT 16)");
        db.execSQL("CREATE TABLE tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "course_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "description TEXT NOT NULL DEFAULT '', " +
                "due_at INTEGER NOT NULL, " +
                "remind_at INTEGER NOT NULL, " +
                "repeat_minutes INTEGER NOT NULL DEFAULT 0, " +
                "priority INTEGER NOT NULL, " +
                "completed INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE merchants (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "notice TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE dishes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "merchant_id INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "price INTEGER NOT NULL, " +
                "FOREIGN KEY(merchant_id) REFERENCES merchants(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE food_orders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "merchant_name TEXT NOT NULL, " +
                "items_summary TEXT NOT NULL, " +
                "total_price INTEGER NOT NULL, " +
                "status TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("INSERT INTO merchants(name, notice) VALUES " +
                "('一食堂轻食', '支持教学楼自取，约 20 分钟出餐')," +
                "('咖啡角', '咖啡、茶饮和简餐')," +
                "('二食堂面馆', '热汤面和盖饭窗口')," +
                "('水果铺', '宿舍区水果酸奶')");
        db.execSQL("INSERT INTO dishes(merchant_id, name, price) VALUES " +
                "(1, '鸡胸肉沙拉', 18)," +
                "(1, '牛肉饭', 22)," +
                "(2, '拿铁', 15)," +
                "(2, '三明治套餐', 20)," +
                "(3, '番茄牛腩面', 19)," +
                "(3, '鸡腿盖饭', 21)," +
                "(4, '酸奶水果杯', 16)," +
                "(4, '香蕉草莓盒', 14)");
        createCommunityTables(db);
        createWalletTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            createCommunityTables(db);
        }
        if (oldVersion < 6) {
            createWalletTables(db);
        }
        if (oldVersion < 7) {
            addTaskRepeatColumn(db);
        }
    }

    private void createCommunityTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_services (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "description TEXT NOT NULL DEFAULT '', " +
                "category TEXT NOT NULL DEFAULT '', " +
                "icon_text TEXT NOT NULL DEFAULT '', " +
                "color INTEGER NOT NULL, " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS forum_posts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "author TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS marketplace_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "price INTEGER NOT NULL, " +
                "description TEXT NOT NULL DEFAULT '', " +
                "contact TEXT NOT NULL DEFAULT '', " +
                "seller TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
    }

    private void createWalletTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS wallet_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "amount INTEGER NOT NULL, " +
                "description TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
    }

    private void addTaskRepeatColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE tasks ADD COLUMN repeat_minutes INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // Existing databases may already contain this column.
        }
    }
}
