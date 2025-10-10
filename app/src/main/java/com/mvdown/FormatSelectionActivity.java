package com.mvdown;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class FormatSelectionActivity extends AppCompatActivity {
    private TextView tvVideoTitle;
    private TextView tvVideoUrl;
    private RecyclerView rvVideoFormats;
    private RecyclerView rvAudioFormats;
    private String videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_format_selection);

        initializeViews();
        handleIntent();
        fetchFormats();
    }

    private void initializeViews() {
        tvVideoTitle = findViewById(R.id.tvVideoTitle);
        tvVideoUrl = findViewById(R.id.tvVideoUrl);
        rvVideoFormats = findViewById(R.id.rvVideoFormats);
        rvAudioFormats = findViewById(R.id.rvAudioFormats);
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            videoUrl = intent.getStringExtra("url");
            tvVideoUrl.setText(videoUrl);
        }
    }

    private void fetchFormats() {
        // Implementation for fetching available formats
    }
}