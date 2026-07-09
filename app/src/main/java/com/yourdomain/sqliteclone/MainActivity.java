package com.yourdomain.sqliteclone;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.yourdomain.sqliteclone.db.LocalDBEngine;
import com.yourdomain.sqliteclone.db.QueryExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private QueryExecutor queryExecutor;
    private ExecutorService executorService;

    private EditText sqlInput;
    private TextView outputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Database and UI Elements
        LocalDBEngine dbEngine = new LocalDBEngine(this);
        queryExecutor = new QueryExecutor(dbEngine);
        executorService = Executors.newSingleThreadExecutor();

        sqlInput = findViewById(R.id.sqlInput);
        outputText = findViewById(R.id.outputText);
        Button runButton = findViewById(R.id.runButton);

        // Set up the click listener for the Run Button
        runButton.setOnClickListener(v -> {
            String rawSql = sqlInput.getText().toString().trim();
            
            if (rawSql.isEmpty()) {
                outputText.setText("Warning: Please enter a SQL command first.");
                outputText.setTextColor(Color.RED);
                return;
            }

            outputText.setText("Executing...");
            outputText.setTextColor(Color.DKGRAY);
            
            executeCommandInBackground(rawSql);
        });
    }

    private void executeCommandInBackground(String rawSql) {
        // Run the heavy database task on a background thread
        executorService.execute(() -> {
            final String result = queryExecutor.executeWrite(rawSql);

            // Post the result safely back to the Main UI Thread
            runOnUiThread(() -> {
                outputText.setText(result);
                if (result.startsWith("Error")) {
                    outputText.setTextColor(Color.RED);
                } else {
                    outputText.setTextColor(Color.parseColor("#006400")); // Dark Green
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the background thread safely when the app closes
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
