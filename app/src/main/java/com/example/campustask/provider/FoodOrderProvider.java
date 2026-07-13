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
 * 成员B负责的ContentProvider组件。
 * 暴露外卖订单数据（food_orders表），供外部应用查询订单状态。
 * URI: content://com.example.campustask.foodorder/orders
 * 使用当前登录用户的个人业务数据库。
 */
public class FoodOrderProvider extends ContentProvider {
    public static final String AUTHORITY = "com.example.campustask.foodorder";
    public static final String TABLE_ORDERS = "food_orders";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_ORDERS);

    private static final int CODE_ORDERS = 1;
    private static final int CODE_ORDER_ID = 2;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, TABLE_ORDERS, CODE_ORDERS);
        URI_MATCHER.addURI(AUTHORITY, TABLE_ORDERS + "/#", CODE_ORDER_ID);
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
            case CODE_ORDERS:
                cursor = db.query(TABLE_ORDERS, projection, selection, selectionArgs, null, null,
                        sortOrder == null ? "created_at DESC" : sortOrder);
                break;
            case CODE_ORDER_ID:
                cursor = db.query(TABLE_ORDERS, projection, "id=?",
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
            case CODE_ORDERS:
                return "vnd.android.cursor.dir/vnd.campustask.foodorder";
            case CODE_ORDER_ID:
                return "vnd.android.cursor.item/vnd.campustask.foodorder";
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        ensureDbHelper();
        if (URI_MATCHER.match(uri) != CODE_ORDERS) {
            throw new IllegalArgumentException("Insert not supported for URI: " + uri);
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(TABLE_ORDERS, null, values);
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
            case CODE_ORDERS:
                rows = db.delete(TABLE_ORDERS, selection, selectionArgs);
                break;
            case CODE_ORDER_ID:
                rows = db.delete(TABLE_ORDERS, "id=?",
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
            case CODE_ORDERS:
                rows = db.update(TABLE_ORDERS, values, selection, selectionArgs);
                break;
            case CODE_ORDER_ID:
                rows = db.update(TABLE_ORDERS, values, "id=?",
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
