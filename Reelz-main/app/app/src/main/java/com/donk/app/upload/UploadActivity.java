package com.donk.app.upload;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.donk.app.R;
import com.donk.app.database.BackgroundExecutor;
import com.donk.app.models.VideoDto;
import com.donk.app.network.ApiClient;
import com.donk.app.network.ContentUriRequestBody;

import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView btnQuickPost;
    private View layoutPickPrompt;
    private CardView cardPreview;
    private ImageView ivThumbnail;
    private View btnChangeVideo;
    private TextView tvPreviewFileName;
    private TextView tvPreviewMeta;
    private TextView tvTitleCount;
    private EditText etTitle;
    private TextView tvDescCount;
    private EditText etDescription;
    private androidx.appcompat.widget.SwitchCompat switchPrivate;
    private TextView tvError;
    private LinearLayout layoutProgress;
    private TextView tvProgressStatus;
    private View btnUpload;

    private Uri selectedVideoUri;
    private String selectedFileName = "video.mp4";
    private long selectedFileSize = 0;
    private String selectedMimeType = "video/mp4";

    private final ActivityResultLauncher<String> pickVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    onVideoSelected(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnQuickPost = findViewById(R.id.btnQuickPost);
        layoutPickPrompt = findViewById(R.id.layoutPickPrompt);
        cardPreview = findViewById(R.id.cardPreview);
        ivThumbnail = findViewById(R.id.ivThumbnail);
        btnChangeVideo = findViewById(R.id.btnChangeVideo);
        tvPreviewFileName = findViewById(R.id.tvPreviewFileName);
        tvPreviewMeta = findViewById(R.id.tvPreviewMeta);
        tvTitleCount = findViewById(R.id.tvTitleCount);
        etTitle = findViewById(R.id.etTitle);
        tvDescCount = findViewById(R.id.tvDescCount);
        etDescription = findViewById(R.id.etDescription);
        switchPrivate = findViewById(R.id.switchPrivate);
        tvError = findViewById(R.id.tvError);
        layoutProgress = findViewById(R.id.layoutProgress);
        tvProgressStatus = findViewById(R.id.tvProgressStatus);
        btnUpload = findViewById(R.id.btnUpload);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        layoutPickPrompt.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        btnChangeVideo.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        btnUpload.setOnClickListener(v -> attemptUpload());
        btnQuickPost.setOnClickListener(v -> attemptUpload());

        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvTitleCount.setText(s.length() + "/150");
                if (s.length() > 0) {
                    tvError.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvDescCount.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void onVideoSelected(Uri uri) {
        this.selectedVideoUri = uri;
        String mime = getContentResolver().getType(uri);
        if (mime != null) {
            this.selectedMimeType = mime;
        }

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                if (nameIndex != -1) {
                    selectedFileName = cursor.getString(nameIndex);
                }
                if (sizeIndex != -1) {
                    selectedFileSize = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
        }

        layoutPickPrompt.setVisibility(View.GONE);
        cardPreview.setVisibility(View.VISIBLE);
        tvPreviewFileName.setText(selectedFileName);
        tvError.setVisibility(View.GONE);

        BackgroundExecutor.runQuery(() -> {
            Bitmap thumb = null;
            long durationMs = 0;
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, uri);
                thumb = retriever.getFrameAtTime(1000000);
                String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (dur != null) {
                    durationMs = Long.parseLong(dur);
                }
                retriever.release();
            } catch (Exception ignored) {
            }
            return new VideoMeta(thumb, durationMs);
        }, meta -> {
            if (meta != null) {
                if (meta.thumbnail != null) {
                    ivThumbnail.setImageBitmap(meta.thumbnail);
                }
                double sizeMb = selectedFileSize / (1024.0 * 1024.0);
                long seconds = (meta.durationMs / 1000) % 60;
                long minutes = (meta.durationMs / (1000 * 60)) % 60;
                String durStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                tvPreviewMeta.setText(String.format(Locale.getDefault(), "▶ %s • %.1f MB", durStr, sizeMb));
            }
        });
    }

    private void attemptUpload() {
        if (selectedVideoUri == null) {
            showError("Vui lòng chọn một video từ thiết bị của bạn");
            return;
        }

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            showError("Vui lòng nhập tiêu đề cho video");
            etTitle.requestFocus();
            return;
        }

        setUploading(true);

        ContentUriRequestBody fileRequestBody = new ContentUriRequestBody(
                getContentResolver(),
                selectedVideoUri,
                selectedMimeType,
                selectedFileSize
        );

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", selectedFileName, fileRequestBody);
        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody descPart = RequestBody.create(MediaType.parse("text/plain"), description);

        double sizeMb = selectedFileSize / (1024.0 * 1024.0);
        tvProgressStatus.setText(String.format(Locale.getDefault(), "Đang tải video lên server (%.1f MB)...", sizeMb));

        boolean isPrivate = switchPrivate != null && switchPrivate.isChecked();

        ApiClient.getService(this).uploadVideo(filePart, titlePart, descPart, isPrivate).enqueue(new Callback<VideoDto>() {
            @Override
            public void onResponse(Call<VideoDto> call, Response<VideoDto> response) {
                setUploading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(UploadActivity.this, "Đăng video thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    showError(ApiClient.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<VideoDto> call, Throwable t) {
                setUploading(false);
                showError("Lỗi kết nối khi tải video lên: " + t.getMessage());
            }
        });
    }

    private void setUploading(boolean uploading) {
        layoutProgress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        btnUpload.setEnabled(!uploading);
        btnQuickPost.setEnabled(!uploading);
        cardPreview.setEnabled(!uploading);
        etTitle.setEnabled(!uploading);
        etDescription.setEnabled(!uploading);

        if (uploading) {
            tvError.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private static class VideoMeta {
        final Bitmap thumbnail;
        final long durationMs;

        VideoMeta(Bitmap thumbnail, long durationMs) {
            this.thumbnail = thumbnail;
            this.durationMs = durationMs;
        }
    }
}
