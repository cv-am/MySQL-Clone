package com.yourdomain.sqliteclone;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.yourdomain.sqliteclone.db.LocalDBEngine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LocalDBEngine dbEngine;
    private ExecutorService executorService;

    private EditText sqlInput;
    private TextView outputText;
    private TableLayout tableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbEngine = new LocalDBEngine(this);
        executorService = Executors.newSingleThreadExecutor();

        sqlInput = findViewById(R.id.sqlInput);
        outputText = findViewById(R.id.outputText);
        tableLayout = findViewById(R.id.tableLayout);
        Button runButton = findViewById(R.id.runButton);

        runButton.setOnClickListener(v -> {
            String rawSql = sqlInput.getText().toString().trim();
            if (rawSql.isEmpty()) return;

            outputText.setText("Executing...");
            outputText.setTextColor(Color.DKGRAY);
            tableLayout.removeAllViews(); // Clear previous table

            // Route the query based on what the user typed
            if (rawSql.toUpperCase().startsWith("SELECT")) {
                executeReadInBackground(rawSql);
            } else {
                executeWriteInBackground(rawSql);
            }
        });
    }

    private void executeWriteInBackground(String rawSql) {
        executorService.execute(() -> {
            SQLiteDatabase db = dbEngine.getWritableDatabase();
            String resultMsg;
            try {
                db.execSQL(rawSql);
                resultMsg = "Success: Command executed without errors.";
            } catch (SQLException e) {
                resultMsg = "Error: " + e.getMessage();
            } finally {
                db.close();
            }

            String finalMsg = resultMsg;
            runOnUiThread(() -> {
                outputText.setText(finalMsg);
                outputText.setTextColor(finalMsg.startsWith("Error") ? Color.RED : Color.parseColor("#006400"));
            });
        });
    }

    private void executeReadInBackground(String rawSql) {
        executorService.execute(() -> {
            SQLiteDatabase db = dbEngine.getReadableDatabase();
            try {
                // rawQuery is specifically for SELECT statements
                Cursor cursor = db.rawQuery(rawSql, null);
                
                runOnUiThread(() -> {
                    outputText.setText("Query returned " + cursor.getCount() + " rows.");
                    outputText.setTextColor(Color.parseColor("#006400"));
                    buildTable(cursor);
                });
            } catch (Exception e) {
                String errorMsg = "Error: " + e.getMessage();
                runOnUiThread(() -> {
                    outputText.setText(errorMsg);
                    outputText.setTextColor(Color.RED);
                });
            }
        });
    }

    // Dynamically builds the rows and columns based on the Cursor
    private void buildTable(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) return;

        // 1. Build Header Row
        String[] columnNames = cursor.getColumnNames();
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.DKGRAY);

        for (String colName : columnNames) {
            TextView tv = createCell(colName, Color.WHITE, true);
            headerRow.addView(tv);
        }
        tableLayout.addView(headerRow);

        // 2. Build Data Rows
        cursor.moveToFirst();
        boolean alternate = false;
        do {
            TableRow dataRow = new TableRow(this);
            dataRow.setBackgroundColor(alternate ? Color.parseColor("#F0F0F0") : Color.WHITE);

            for (int i = 0; i < columnNames.length; i++) {
                String val = cursor.getString(i);
                TextView tv = createCell(val != null ? val : "NULL", Color.BLACK, false);
                dataRow.addView(tv);
            }
            tableLayout.addView(dataRow);
            alternate = !alternate;

        } while (cursor.moveToNext());

        cursor.close();
    }

    private TextView createCell(String text, int color, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setPadding(32, 16, 32, 16);
        tv.setGravity(Gravity.CENTER);
        if (isHeader) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }
}
