package com.mvdown;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class DownloadsActivity extends AppCompatActivity {
    private RecyclerView rvDownloads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        initializeViews();
        loadDownloads();
    }

    private void initializeViews() {
        rvDownloads = findViewById(R.id.rvDownloads);
    }

    private void loadDownloads() {
        // Implementation for loading downloaded files
    }
}