package com.donk.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.donk.app.R;
import com.donk.app.feed.FeedActivity;
import com.donk.app.models.AuthResponse;
import com.donk.app.models.LoginRequest;
import com.donk.app.network.ApiClient;
import com.donk.app.network.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private TextView tvError;
    private ProgressBar progressBar;
    private View btnLogin;
    private TextView tvGoRegister;
    private TextView tvSkipLogin;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, FeedActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoRegister = findViewById(R.id.tvGoRegister);
        tvSkipLogin = findViewById(R.id.tvSkipLogin);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvSkipLogin.setOnClickListener(v -> {
            // Che do khach: khong can dang nhap, chi xem video
            startActivity(new Intent(LoginActivity.this, FeedActivity.class));
        });
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.err_empty_field));
            return;
        }

        setLoading(true);

        LoginRequest request = new LoginRequest(username, password);
        ApiClient.getService(this).login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    sessionManager.saveAuth(auth.getToken(), auth.getUser());

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(LoginActivity.this, FeedActivity.class));
                    finish();
                } else {
                    showError(ApiClient.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                showError(getString(R.string.err_network));
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        if (loading) {
            tvError.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
