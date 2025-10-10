// MainActivity.java
package com.example.mediadownloader;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText etUrl;
    private Button btnFetchFormats;
    private ImageButton btnDownloads, btnSettings;
    private CardView progressCard;
    private TextView tvProgressTitle, tvProgressDetails;
    private ProgressBar progressBar;
    private RecyclerView rvRecentDownloads;
    
    private ApiService apiService;
    private SharedPreferences sharedPreferences;
    private RecentDownloadsAdapter recentAdapter;
    private List<DownloadItem> recentDownloads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupPreferences();
        setupRecyclerView();
        setupListeners();
        handleSharedIntent();
    }

    private void initViews() {
        etUrl = findViewById(R.id.etUrl);
        btnFetchFormats = findViewById(R.id.btnFetchFormats);
        btnDownloads = findViewById(R.id.btnDownloads);
        btnSettings = findViewById(R.id.btnSettings);
        progressCard = findViewById(R.id.progressCard);
        tvProgressTitle = findViewById(R.id.tvProgressTitle);
        tvProgressDetails = findViewById(R.id.tvProgressDetails);
        progressBar = findViewById(R.id.progressBar);
        rvRecentDownloads = findViewById(R.id.rvRecentDownloads);
    }

    private void setupPreferences() {
        sharedPreferences = getSharedPreferences("MediaDownloader", MODE_PRIVATE);
        String apiUrl = sharedPreferences.getString("api_url", "http://192.168.1.100:8000");
        apiService = new ApiService(apiUrl);
    }

    private void setupRecyclerView() {
        recentDownloads = new ArrayList<>();
        recentAdapter = new RecentDownloadsAdapter(recentDownloads, this);
        rvRecentDownloads.setLayoutManager(new LinearLayoutManager(this));
        rvRecentDownloads.setAdapter(recentAdapter);
        
        loadRecentDownloads();
    }

    private void setupListeners() {
        btnFetchFormats.setOnClickListener(v -> fetchFormats());
        
        btnDownloads.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DownloadsActivity.class);
            startActivity(intent);
        });
        
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    private void handleSharedIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    etUrl.setText(sharedText);
                    Toast.makeText(this, "URL received from share", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void fetchFormats() {
        String url = etUrl.getText().toString().trim();
        
        if (url.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress("Processing your request...", "Fetching available formats...");

        apiService.fetchFormats(url, new ApiService.FormatCallback() {
            @Override
            public void onSuccess(FormatResponse response) {
                runOnUiThread(() -> {
                    hideProgress();
                    Intent intent = new Intent(MainActivity.this, FormatSelectionActivity.class);
                    intent.putExtra("format_response", response);
                    intent.putExtra("url", url);
                    startActivity(intent);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showProgress(String title, String details) {
        progressCard.setVisibility(View.VISIBLE);
        tvProgressTitle.setText(title);
        tvProgressDetails.setText(details);
        progressBar.setIndeterminate(true);
        btnFetchFormats.setEnabled(false);
    }

    private void hideProgress() {
        progressCard.setVisibility(View.GONE);
        btnFetchFormats.setEnabled(true);
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        
        EditText etApiUrl = view.findViewById(R.id.etApiUrl);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        
        String currentUrl = sharedPreferences.getString("api_url", "http://192.168.1.100:8000");
        etApiUrl.setText(currentUrl);
        
        AlertDialog dialog = builder.setView(view).create();
        
        btnSave.setOnClickListener(v -> {
            String newUrl = etApiUrl.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                sharedPreferences.edit().putString("api_url", newUrl).apply();
                apiService = new ApiService(newUrl);
                Toast.makeText(this, "API URL saved", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void loadRecentDownloads() {
        // Load from local database or shared preferences
        // For now, we'll fetch from server
        apiService.getFiles(new ApiService.FilesCallback() {
            @Override
            public void onSuccess(List<DownloadItem> items) {
                runOnUiThread(() -> {
                    recentDownloads.clear();
                    // Show only first 5 items
                    int count = Math.min(items.size(), 5);
                    for (int i = 0; i < count; i++) {
                        recentDownloads.add(items.get(i));
                    }
                    recentAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String error) {
                // Silent fail for recent downloads
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentDownloads();
    }
}

// FormatResponse.java
package com.example.mediadownloader;

import java.io.Serializable;
import java.util.List;

public class FormatResponse implements Serializable {
    private String title;
    private String url;
    private List<Format> video_formats;
    private List<Format> audio_formats;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Format> getVideoFormats() {
        return video_formats;
    }

    public void setVideoFormats(List<Format> video_formats) {
        this.video_formats = video_formats;
    }

    public List<Format> getAudioFormats() {
        return audio_formats;
    }

    public void setAudioFormats(List<Format> audio_formats) {
        this.audio_formats = audio_formats;
    }

    public static class Format implements Serializable {
        private String format_id;
        private String ext;
        private String resolution;
        private Double abr;
        private Long filesize;

        public String getFormatId() {
            return format_id;
        }

        public void setFormatId(String format_id) {
            this.format_id = format_id;
        }

        public String getExt() {
            return ext;
        }

        public void setExt(String ext) {
            this.ext = ext;
        }

        public String getResolution() {
            return resolution;
        }