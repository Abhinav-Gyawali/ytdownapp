package com.mvdown;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class SplashActivity extends AppCompatActivity {
    private LottieAnimationView animationView;
    private TextView tvStatus;
    private Button btnRetry;
    private OkHttpClient client;
    private String apiUrl;
    private static final int SPLASH_TIMEOUT = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initializeViews();
        setupClickListeners();
        client = new OkHttpClient();
        
        // Get API URL from SharedPreferences or use default
        apiUrl = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("api_url", "http://192.168.1.100:8000");
        
        checkServerStatus();
    }

    private void initializeViews() {
        animationView = findViewById(R.id.animationView);
        tvStatus = findViewById(R.id.tvStatus);
        btnRetry = findViewById(R.id.btnRetry);
    }

    private void setupClickListeners() {
        btnRetry.setOnClickListener(v -> {
            btnRetry.setVisibility(View.GONE);
            tvStatus.setText("Checking server status...");
            checkServerStatus();
        });
    }

    private void checkServerStatus() {
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showError("Cannot connect to server");
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }, SPLASH_TIMEOUT);
                } else {
                    showError("Server error: " + response.code());
                }
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            tvStatus.setText(message);
            btnRetry.setVisibility(View.VISIBLE);
        });
    }
}