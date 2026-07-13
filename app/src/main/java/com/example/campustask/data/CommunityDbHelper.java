package com.example.campustask.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CommunityDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "campus_community.db";
    private static final int DB_VERSION = 4;

    public CommunityDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        db.execSQL("INSERT INTO forum_sections(name, description, created_at) VALUES " +
                "('求助', '遇到问题在这里寻求帮助', " + System.currentTimeMillis() + ")," +
                "('咨询', '学业、生活方面的咨询交流', " + System.currentTimeMillis() + ")," +
                "('吐槽', '发泄情绪、吐槽校园生活', " + System.currentTimeMillis() + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);
        if (oldVersion < 2) {
            addMarketplaceSaleColumns(db);
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS classroom_reservations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "room TEXT NOT NULL, " +
                    "time_slot TEXT NOT NULL, " +
                    "username TEXT NOT NULL DEFAULT '', " +
                    "created_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS forum_sections (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "description TEXT NOT NULL DEFAULT '', " +
                    "created_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS forum_posts_v2 (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "section_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "content TEXT NOT NULL, " +
                    "author TEXT NOT NULL DEFAULT '', " +
                    "attachment TEXT NOT NULL DEFAULT '', " +
                    "created_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS forum_comments_v2 (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "post_id INTEGER NOT NULL, " +
                    "content TEXT NOT NULL, " +
                    "author TEXT NOT NULL DEFAULT '', " +
                    "created_at INTEGER NOT NULL)");
        }
        if (oldVersion < 4) {
            addColumn(db, "marketplace_items", "image_uri", "TEXT NOT NULL DEFAULT ''");
        }
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS forum_posts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "author TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS forum_comments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "post_id INTEGER NOT NULL, " +
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
                "image_uri TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'on_sale', " +
                "sold_price INTEGER NOT NULL DEFAULT 0, " +
                "buyer TEXT NOT NULL DEFAULT '', " +
                "sold_at INTEGER NOT NULL DEFAULT 0, " +
                "platform_fee INTEGER NOT NULL DEFAULT 0, " +
                "seller_income INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS marketplace_comments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id INTEGER NOT NULL, " +
                "content TEXT NOT NULL, " +
                "author TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS market_bids (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id INTEGER NOT NULL, " +
                "price INTEGER NOT NULL, " +
                "bidder TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS classroom_reservations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "room TEXT NOT NULL, " +
                "time_slot TEXT NOT NULL, " +
                "username TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS forum_sections (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "description TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS forum_posts_v2 (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "section_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "author TEXT NOT NULL DEFAULT '', " +
                "attachment TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS forum_comments_v2 (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "post_id INTEGER NOT NULL, " +
                "content TEXT NOT NULL, " +
                "author TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
    }

    private void addMarketplaceSaleColumns(SQLiteDatabase db) {
        addColumn(db, "marketplace_items", "status", "TEXT NOT NULL DEFAULT 'on_sale'");
        addColumn(db, "marketplace_items", "sold_price", "INTEGER NOT NULL DEFAULT 0");
        addColumn(db, "marketplace_items", "buyer", "TEXT NOT NULL DEFAULT ''");
        addColumn(db, "marketplace_items", "sold_at", "INTEGER NOT NULL DEFAULT 0");
        addColumn(db, "marketplace_items", "platform_fee", "INTEGER NOT NULL DEFAULT 0");
        addColumn(db, "marketplace_items", "seller_income", "INTEGER NOT NULL DEFAULT 0");
    }

    private void addColumn(SQLiteDatabase db, String table, String column, String definition) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (Exception ignored) {
            // Existing databases may already contain the column after a previous upgrade attempt.
        }
    }
}
