package com.yourdomain.sqliteclone;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.yourdomain.sqliteclone.db.LocalDBEngine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LocalDBEngine dbEngine;
    private ExecutorService executorService;
    private MultiAutoCompleteTextView sqlInput;
    private TextView outputText;
    private TableLayout tableLayout;
    
    private static final int EXPORT_REQUEST_CODE = 101;
    private static final int IMPORT_REQUEST_CODE = 102;

    // The dictionary for your auto-complete engine
    private static final String[] SQL_KEYWORDS = new String[]{
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "CREATE", 
            "TABLE", "UPDATE", "SET", "DELETE", "DROP", "INTEGER", "TEXT", 
            "PRIMARY KEY", "FOREIGN KEY", "INNER JOIN", "LEFT JOIN", "ORDER BY"
    };

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

        // Setup the predictive auto-complete behavior
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, SQL_KEYWORDS);
        sqlInput.setAdapter(adapter);
        sqlInput.setTokenizer(new MultiAutoCompleteTextView.SpaceTokenizer());

        runButton.setOnClickListener(v -> {
            String rawSql = sqlInput.getText().toString().trim();
            if (rawSql.isEmpty()) return;

            outputText.setText("Executing...");
            outputText.setTextColor(Color.LTGRAY);
            tableLayout.removeAllViews(); 

            if (rawSql.toUpperCase().startsWith("SELECT")) {
                executeReadInBackground(rawSql);
            } else {
                executeWriteInBackground(rawSql);
            }
        });
    }

    // --- NEW: WIRING UP THE 3-DOT MENU ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_import) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, IMPORT_REQUEST_CODE);
            return true;
        } else if (id == R.id.action_export) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_TITLE, "mysql_backup.sql");
            startActivityForResult(intent, EXPORT_REQUEST_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // -------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == EXPORT_REQUEST_CODE) {
                outputText.setText("Exporting database...");
                exportDatabaseToUri(data.getData());
            } else if (requestCode == IMPORT_REQUEST_CODE) {
                outputText.setText("Importing database...");
                importDatabaseFromUri(data.getData());
            }
        }
    }

    private void importDatabaseFromUri(Uri uri) {
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) return;
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sqlBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("--") || line.trim().isEmpty()) continue;
                    sqlBuilder.append(line).append(" ");
                }
                reader.close();
                String[] sqlCommands = sqlBuilder.toString().split(";");
                SQLiteDatabase db = dbEngine.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (String command : sqlCommands) {
                        if (!command.trim().isEmpty()) {
                            db.execSQL(command);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                runOnUiThread(() -> {
                    outputText.setText("Success: Database imported!");
                    outputText.setTextColor(Color.GREEN);
                });
            } catch (Exception e) {
                String error = "Import failed: " + e.getMessage();
                runOnUiThread(() -> {
                    outputText.setText(error);
                    outputText.setTextColor(Color.parseColor("#FF5555"));
                });
            }
        });
    }

    private void exportDatabaseToUri(Uri uri) {
        executorService.execute(() -> {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream == null) return;
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                SQLiteDatabase db = dbEngine.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT IN ('android_metadata', 'sqlite_sequence')", null);
                while (cursor.moveToNext()) {
                    String tableName = cursor.getString(0);
                    writer.write("-- Backup for table: " + tableName + "\n");
                    Cursor createCursor = db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='" + tableName + "'", null);
                    if (createCursor.moveToFirst()) {
                        writer.write(createCursor.getString(0) + ";\n\n");
                    }
                    createCursor.close();
                    Cursor dataCursor = db.rawQuery("SELECT * FROM " + tableName, null);
                    String[] columns = dataCursor.getColumnNames();
                    while (dataCursor.moveToNext()) {
                        StringBuilder insert = new StringBuilder("INSERT INTO " + tableName + " VALUES (");
                        for (int i = 0; i < columns.length; i++) {
                            String val = dataCursor.getString(i);
                            if (val == null) {
                                insert.append("NULL");
                            } else {
                                insert.append("'").append(val.replace("'", "''")).append("'");
                            }
                            if (i < columns.length - 1) insert.append(", ");
                        }
                        insert.append(");\n");
                        writer.write(insert.toString());
                    }
                    dataCursor.close();
                    writer.write("\n\n");
                }
                cursor.close();
                writer.flush();
                writer.close();
                runOnUiThread(() -> {
                    outputText.setText("Success: Database exported successfully!");
                    outputText.setTextColor(Color.GREEN);
                });
            } catch (Exception e) {
                String error = "Export failed: " + e.getMessage();
                runOnUiThread(() -> {
                    outputText.setText(error);
                    outputText.setTextColor(Color.parseColor("#FF5555"));
                });
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
            }
            final String finalResultMsg = resultMsg;
            runOnUiThread(() -> {
                outputText.setText(finalResultMsg);
                outputText.setTextColor(finalResultMsg.startsWith("Error") ? Color.parseColor("#FF5555") : Color.GREEN);
            });
        });
    }

    private void executeReadInBackground(String rawSql) {
        executorService.execute(() -> {
            SQLiteDatabase db = dbEngine.getReadableDatabase();
            try {
                Cursor cursor = db.rawQuery(rawSql, null);
                runOnUiThread(() -> {
                    outputText.setText("Query returned " + cursor.getCount() + " rows.");
                    outputText.setTextColor(Color.GREEN);
                    buildTable(cursor);
                });
            } catch (Exception e) {
                String errorMsg = "Error: " + e.getMessage();
                runOnUiThread(() -> {
                    outputText.setText(errorMsg);
                    outputText.setTextColor(Color.parseColor("#FF5555"));
                });
            }
        });
    }

    private void buildTable(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) return;
        String[] columnNames = cursor.getColumnNames();
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#44000000")); // Darker semi-transparent for header
        for (String colName : columnNames) {
            headerRow.addView(createCell(colName, Color.WHITE, true));
        }
        tableLayout.addView(headerRow);
        cursor.moveToFirst();
        boolean alternate = false;
        do {
            TableRow dataRow = new TableRow(this);
            dataRow.setBackgroundColor(alternate ? Color.parseColor("#1AFFFFFF") : Color.TRANSPARENT);
            for (int i = 0; i < columnNames.length; i++) {
                String val = cursor.getString(i);
                dataRow.addView(createCell(val != null ? val : "NULL", Color.WHITE, false));
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
