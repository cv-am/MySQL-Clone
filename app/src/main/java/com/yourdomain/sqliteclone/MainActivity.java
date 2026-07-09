package com.yourdomain.sqliteclone;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                        
                                // Links this Java file to the UI layout below
                                        setContentView(R.layout.activity_main);

                                                // A simple test to ensure Java is interacting with the screen
                                                        TextView statusText = findViewById(R.id.statusText);
                                                                statusText.setText("MySQL Clone Engine Initialized Successfully!");
                                                                    }
                                                                    }
                                                                    