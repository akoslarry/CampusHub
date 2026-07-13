package com.example.campustask.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.example.campustask.data.CampusTaskDbHelper;

/**
 * 成员C负责的ContentProvider组件。
 * 暴露任务数据（tasks表），供外部应用或桌面小组件查询待办事项。
 * URI: content://com.example.campustask.task/tasks
 * 使用当前登录用户的个人业务数据库。
 */
public class TaskProvider extends ContentProvider {
    public static final String AUTHORITY = "com.example.campustask.task";
    public static final String TABLE_TASKS = "tasks";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_TASKS);

    private static final int CODE_TASKS = 1;
    private static final int CODE_TASK_ID = 2;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, TABLE_TASKS, CODE_TASKS);
        URI_MATCHER.addURI(AUTHORITY, TABLE_TASKS + "/#", CODE_TASK_ID);
    }

    private static final String PREFS_NAME = "campus_hub_auth";
    private static final String PREF_USERNAME = "username";

    private CampusTaskDbHelper dbHelper;

    private void ensureDbHelper() {
        if (dbHelper == null) {
            Context context = getContext();
            String username = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(PREF_USERNAME, "guest");
            dbHelper = new CampusTaskDbHelper(context, username);
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
        Cursor cursor;
        switch (URI_MATCHER.match(uri)) {
            case CODE_TASKS:
                cursor = db.query(TABLE_TASKS, projection, selection, selectionArgs, null, null,
                        sortOrder == null ? "due_at ASC" : sortOrder);
                break;
            case CODE_TASK_ID:
                cursor = db.query(TABLE_TASKS, projection, "id=?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))}, null, null, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case CODE_TASKS:
                return "vnd.android.cursor.dir/vnd.campustask.task";
            case CODE_TASK_ID:
                return "vnd.android.cursor.item/vnd.campustask.task";
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        ensureDbHelper();
        if (URI_MATCHER.match(uri) != CODE_TASKS) {
            throw new IllegalArgumentException("Insert not supported for URI: " + uri);
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(TABLE_TASKS, null, values);
        if (id > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        ensureDbHelper();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows;
        switch (URI_MATCHER.match(uri)) {
            case CODE_TASKS:
                rows = db.delete(TABLE_TASKS, selection, selectionArgs);
                break;
            case CODE_TASK_ID:
                rows = db.delete(TABLE_TASKS, "id=?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
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
        int rows;
        switch (URI_MATCHER.match(uri)) {
            case CODE_TASKS:
                rows = db.update(TABLE_TASKS, values, selection, selectionArgs);
                break;
            case CODE_TASK_ID:
                rows = db.update(TABLE_TASKS, values, "id=?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (rows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }
}
