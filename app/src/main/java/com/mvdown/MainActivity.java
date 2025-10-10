package com.mvdown;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {
    private EditText etUrl;
    private Button btnFetchFormats;
    private ImageButton btnDownloads, btnSettings;
    private CardView progressCard;
    private TextView tvProgressTitle, tvProgressDetails;
    private String apiUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        handleIntent(getIntent());
    }

    private void initializeViews() {
        etUrl = findViewById(R.id.etUrl);
        btnFetchFormats = findViewById(R.id.btnFetchFormats);
        btnDownloads = findViewById(R.id.btnDownloads);
        btnSettings = findViewById(R.id.btnSettings);
        progressCard = findViewById(R.id.progressCard);
        tvProgressTitle = findViewById(R.id.tvProgressTitle);
        tvProgressDetails = findViewById(R.id.tvProgressDetails);
    }

    private void setupClickListeners() {
        btnFetchFormats.setOnClickListener(v -> handleFetchFormats());
        btnDownloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadsActivity.class)));
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    private void handleFetchFormats() {
        String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, FormatSelectionActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }

    private void showSettingsDialog() {
        // Show settings dialog implementation
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                etUrl.setText(sharedText);
            }
        }
    }
}