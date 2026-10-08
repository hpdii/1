package com.donk.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.donk.app.R;
import com.donk.app.feed.FeedActivity;
import com.donk.app.models.AuthResponse;
import com.donk.app.models.RegisterRequest;
import com.donk.app.network.ApiClient;
import com.donk.app.network.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etDisplayName;
    private TextView tvError;
    private ProgressBar progressBar;
    private View btnRegister;
    private TextView tvGoLogin;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etDisplayName = findViewById(R.id.etDisplayName);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());

        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String displayName = etDisplayName.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.err_empty_field));
            return;
        }

        if (username.length() < 3 || username.length() > 50) {
            showError("Tên đăng nhập phải từ 3 đến 50 ký tự");
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            showError("Tên đăng nhập chỉ gồm chữ, số và dấu gạch dưới");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Địa chỉ email không đúng định dạng");
            return;
        }

        if (password.length() < 6) {
            showError("Mật khẩu phải từ 6 ký tự trở lên");
            return;
        }

        setLoading(true);

        RegisterRequest request = new RegisterRequest(
                username,
                email,
                password,
                displayName.isEmpty() ? null : displayName
        );

        ApiClient.getService(this).register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    sessionManager.saveAuth(auth.getToken(), auth.getUser());

                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(RegisterActivity.this, FeedActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
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
        btnRegister.setEnabled(!loading);
        if (loading) {
            tvError.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
