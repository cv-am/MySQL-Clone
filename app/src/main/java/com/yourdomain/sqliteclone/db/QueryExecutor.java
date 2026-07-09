package com.yourdomain.sqliteclone.db;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class QueryExecutor {
    
    private LocalDBEngine dbEngine;

    public QueryExecutor(LocalDBEngine engine) {
        this.dbEngine = engine;
    }

    /**
     * Executes commands that alter data or schema (INSERT, UPDATE, DELETE, CREATE, DROP).
     * We will handle SELECT statements later since they require returning a Cursor.
     */
    public String executeWrite(String sqlQuery) {
        SQLiteDatabase db = dbEngine.getWritableDatabase();
        try {
            db.execSQL(sqlQuery);
            return "Success: Command executed without errors.";
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            db.close();
        }
    }
}
