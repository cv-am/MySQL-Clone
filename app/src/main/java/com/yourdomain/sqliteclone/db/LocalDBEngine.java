package com.yourdomain.sqliteclone.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalDBEngine extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MySQLCloneWorkspace.db";
    private static final int DATABASE_VERSION = 1;

    public LocalDBEngine(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // We leave this intentionally blank. 
        // In a normal app, tables are created here. 
        // In our clone, the user will type "CREATE TABLE" manually.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Required method for Android SQLite, left blank for now.
    }
}
