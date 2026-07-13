package com.example.campustask.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AuthDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "campus_auth.db";
    private static final int DB_VERSION = 2;

    public AuthDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'user')");
        db.execSQL("INSERT INTO users(username, password, role) VALUES ('admin', 'admin123', 'admin')");
        db.execSQL("INSERT INTO users(username, password, role) VALUES ('wyj', '926495', 'admin')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'user'");
            } catch (Exception ignored) {
            }
            db.execSQL("INSERT OR IGNORE INTO users(username, password, role) VALUES ('admin', 'admin123', 'admin')");
            db.execSQL("INSERT OR IGNORE INTO users(username, password, role) VALUES ('wyj', '926495', 'admin')");
        }
    }
}
