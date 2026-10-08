package com.donk.app.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.donk.app.R;
import com.donk.app.auth.LoginActivity;
import com.donk.app.feed.FeedActivity;
import com.donk.app.models.PageResponse;
import com.donk.app.models.ChangePasswordRequest;
import com.donk.app.models.UpdateProfileRequest;
import com.donk.app.models.UpdateVideoRequest;
import com.donk.app.models.UserProfileDto;
import com.donk.app.models.VideoDto;
import com.donk.app.network.ApiClient;
import com.donk.app.network.SessionManager;
import com.donk.app.search.SearchActivity;
import com.donk.app.search.UserSearchAdapter;
import com.donk.app.upload.UploadActivity;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageView btnProfileBack;
    private TextView tvHeaderTitle;
    private ImageView btnProfileSearch;
    private View btnLogout;
    private View layoutProfileAvatar;
    private ImageView ivProfileAvatar;
    private TextView tvAvatarChar;
    private TextView tvProfileDisplayName;
    private TextView tvProfileUsername;
    private View layoutStatFollowing;
    private TextView tvStatFollowing;
    private View layoutStatFollowers;
    private TextView tvStatFollowers;
    private TextView tvStatLikes;
    private TextView tvBio;
    private View btnEditBio;
    private Button btnFollowAction;

    private LinearLayout layoutTabs;
    private View tabUploaded;
    private TextView tvTabUploadedLabel;
    private View indicatorTabUploaded;

    private View tabPrivate;
    private TextView tvTabPrivateLabel;
    private View indicatorTabPrivate;

    private View tabLiked;
    private TextView tvTabLikedLabel;
    private View indicatorTabLiked;

    private View tabBookmarked;
    private TextView tvTabBookmarkedLabel;
    private View indicatorTabBookmarked;

    private ProgressBar progressBarProfile;
    private RecyclerView rvMyVideos;
    private TextView tvEmptyMyVideos;
    private View bottomNavProfile;
    private View navHome;
    private View navUpload;
    private View navProfile;

    private SessionManager sessionManager;
    private VideoGridAdapter adapter;

    private String targetUsername;
    private boolean isOwnProfile = true;
    private UserProfileDto userProfile;
    private int currentTab = 0; // 0 = Public, 1 = Private, 2 = Liked, 3 = Bookmarked

    private final androidx.activity.result.ActivityResultLauncher<String> pickAvatarLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadAvatar(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        targetUsername = getIntent().getStringExtra("username");
        if (targetUsername != null && !targetUsername.isEmpty() && !targetUsername.equals(sessionManager.getUsername())) {
            isOwnProfile = false;
        } else {
            isOwnProfile = true;
            targetUsername = sessionManager.getUsername();
        }

        setContentView(R.layout.activity_profile);

        initViews();
        setupUIForProfileType();
        setupRecyclerView();
        setupTabs();
        setupBottomNav();

        loadProfileInfo();
        loadTabContent();
    }

    private void initViews() {
        btnProfileBack = findViewById(R.id.btnProfileBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnProfileSearch = findViewById(R.id.btnProfileSearch);
        btnLogout = findViewById(R.id.btnLogout);
        layoutProfileAvatar = findViewById(R.id.layoutProfileAvatar);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvAvatarChar = findViewById(R.id.tvAvatarChar);
        tvProfileDisplayName = findViewById(R.id.tvProfileDisplayName);
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        layoutStatFollowing = findViewById(R.id.layoutStatFollowing);
        tvStatFollowing = findViewById(R.id.tvStatFollowing);
        layoutStatFollowers = findViewById(R.id.layoutStatFollowers);
        tvStatFollowers = findViewById(R.id.tvStatFollowers);
        tvStatLikes = findViewById(R.id.tvStatLikes);
        tvBio = findViewById(R.id.tvBio);
        btnEditBio = findViewById(R.id.btnEditBio);
        btnFollowAction = findViewById(R.id.btnFollowAction);

        if (layoutStatFollowing != null) {
            layoutStatFollowing.setOnClickListener(v -> showFollowListDialog(true));
        }
        if (layoutStatFollowers != null) {
            layoutStatFollowers.setOnClickListener(v -> showFollowListDialog(false));
        }

        layoutTabs = findViewById(R.id.layoutTabs);
        tabUploaded = findViewById(R.id.tabUploaded);
        tvTabUploadedLabel = findViewById(R.id.tvTabUploadedLabel);
        indicatorTabUploaded = findViewById(R.id.indicatorTabUploaded);

        tabPrivate = findViewById(R.id.tabPrivate);
        tvTabPrivateLabel = findViewById(R.id.tvTabPrivateLabel);
        indicatorTabPrivate = findViewById(R.id.indicatorTabPrivate);

        tabLiked = findViewById(R.id.tabLiked);
        tvTabLikedLabel = findViewById(R.id.tvTabLikedLabel);
        indicatorTabLiked = findViewById(R.id.indicatorTabLiked);

        tabBookmarked = findViewById(R.id.tabBookmarked);
        tvTabBookmarkedLabel = findViewById(R.id.tvTabBookmarkedLabel);
        indicatorTabBookmarked = findViewById(R.id.indicatorTabBookmarked);

        progressBarProfile = findViewById(R.id.progressBarProfile);
        rvMyVideos = findViewById(R.id.rvMyVideos);
        tvEmptyMyVideos = findViewById(R.id.tvEmptyMyVideos);
        bottomNavProfile = findViewById(R.id.bottomNavProfile);
        navHome = findViewById(R.id.navHome);
        navUpload = findViewById(R.id.navUpload);
        navProfile = findViewById(R.id.navProfile);
    }

    private void setupUIForProfileType() {
        btnProfileSearch.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, SearchActivity.class)));

        if (!isOwnProfile) {
            btnProfileBack.setVisibility(View.VISIBLE);
            btnProfileBack.setOnClickListener(v -> finish());
            btnLogout.setVisibility(View.GONE);
            btnEditBio.setVisibility(View.GONE);
            btnFollowAction.setVisibility(View.VISIBLE);
            layoutTabs.setVisibility(View.GONE);
            bottomNavProfile.setVisibility(View.GONE);
            tvHeaderTitle.setText("@" + targetUsername);

            btnFollowAction.setOnClickListener(v -> toggleFollow());
        } else {
            btnProfileBack.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
            btnLogout.setOnClickListener(v -> showAccountMenu());
            btnEditBio.setVisibility(View.VISIBLE);
            btnEditBio.setOnClickListener(v -> showEditBioDialog());
            layoutProfileAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
            btnFollowAction.setVisibility(View.GONE);
            layoutTabs.setVisibility(View.VISIBLE);
            bottomNavProfile.setVisibility(View.VISIBLE);
            tvHeaderTitle.setText("Hồ sơ của tôi");
        }
    }

    private void setupRecyclerView() {
        // 3 columns grid like Facebook Reels
        rvMyVideos.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new VideoGridAdapter(new VideoGridAdapter.OnVideoGridClickListener() {
            @Override
            public void onVideoClick(VideoDto video, int position) {
                // View video
                Intent intent = new Intent(ProfileActivity.this, FeedActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }

            @Override
            public void onVideoLongClick(VideoDto video, int position) {
                if (isOwnProfile && (currentTab == 0 || currentTab == 1)) {
                    showVideoManageDialog(video, position);
                }
            }
        });
        rvMyVideos.setAdapter(adapter);
    }

    private void setupTabs() {
        tabUploaded.setOnClickListener(v -> selectTab(0));
        tabPrivate.setOnClickListener(v -> selectTab(1));
        tabLiked.setOnClickListener(v -> selectTab(2));
        tabBookmarked.setOnClickListener(v -> selectTab(3));
    }

    private void selectTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;

        resetTabIndicators();
        if (tab == 0) {
            tvTabUploadedLabel.setTextColor(getColor(R.color.color_text_white));
            tvTabUploadedLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorTabUploaded.setVisibility(View.VISIBLE);
        } else if (tab == 1) {
            tvTabPrivateLabel.setTextColor(getColor(R.color.color_text_white));
            tvTabPrivateLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorTabPrivate.setVisibility(View.VISIBLE);
        } else if (tab == 2) {
            tvTabLikedLabel.setTextColor(getColor(R.color.color_text_white));
            tvTabLikedLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorTabLiked.setVisibility(View.VISIBLE);
        } else if (tab == 3) {
            tvTabBookmarkedLabel.setTextColor(getColor(R.color.color_text_white));
            tvTabBookmarkedLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorTabBookmarked.setVisibility(View.VISIBLE);
        }

        loadTabContent();
    }

    private void resetTabIndicators() {
        tvTabUploadedLabel.setTextColor(getColor(R.color.color_text_secondary));
        tvTabUploadedLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        indicatorTabUploaded.setVisibility(View.INVISIBLE);

        tvTabPrivateLabel.setTextColor(getColor(R.color.color_text_secondary));
        tvTabPrivateLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        indicatorTabPrivate.setVisibility(View.INVISIBLE);

        tvTabLikedLabel.setTextColor(getColor(R.color.color_text_secondary));
        tvTabLikedLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        indicatorTabLiked.setVisibility(View.INVISIBLE);

        tvTabBookmarkedLabel.setTextColor(getColor(R.color.color_text_secondary));
        tvTabBookmarkedLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        indicatorTabBookmarked.setVisibility(View.INVISIBLE);
    }

    private void setupBottomNav() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FeedActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        navUpload.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, UploadActivity.class));
        });
    }

    private void loadProfileInfo() {
        String usernameToQuery = targetUsername != null ? targetUsername : sessionManager.getUsername();
        if (usernameToQuery == null) return;

        char firstChar = usernameToQuery.isEmpty() ? 'U' : Character.toUpperCase(usernameToQuery.charAt(0));
        tvAvatarChar.setText(String.valueOf(firstChar));

        ApiClient.getService(this).getProfile(usernameToQuery).enqueue(new Callback<UserProfileDto>() {
            @Override
            public void onResponse(Call<UserProfileDto> call, Response<UserProfileDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userProfile = response.body();
                    tvProfileDisplayName.setText(userProfile.getDisplayName() != null ? userProfile.getDisplayName() : userProfile.getUsername());
                    tvProfileUsername.setText("@" + userProfile.getUsername());

                    tvStatFollowing.setText(formatCount(userProfile.getFollowingCount()));
                    tvStatFollowers.setText(formatCount(userProfile.getFollowerCount()));
                    tvStatLikes.setText(formatCount(userProfile.getTotalLikes()));

                    if (userProfile.getBio() != null && !userProfile.getBio().trim().isEmpty()) {
                        tvBio.setText(userProfile.getBio());
                        tvBio.setTextColor(getColor(R.color.color_text_white));
                    } else {
                        tvBio.setText(isOwnProfile ? "Chạm \"Chỉnh sửa thông tin cá nhân\" để viết vài câu giới thiệu..." : "Chưa có tiểu sử");
                        tvBio.setTextColor(getColor(R.color.color_text_tertiary));
                    }

                    if (userProfile.getAvatarUrl() != null && !userProfile.getAvatarUrl().isEmpty()) {
                        String fullAvatar = ApiClient.getVideoUrl(userProfile.getAvatarUrl());
                        com.bumptech.glide.Glide.with(ProfileActivity.this)
                                .load(fullAvatar)
                                .circleCrop()
                                .into(ivProfileAvatar);
                        tvAvatarChar.setVisibility(View.GONE);
                    } else {
                        ivProfileAvatar.setImageResource(R.drawable.bg_button_primary);
                        tvAvatarChar.setVisibility(View.VISIBLE);
                    }

                    if (!isOwnProfile) {
                        updateFollowButtonState(userProfile.isFollowing());
                    }
                }
            }

            @Override
            public void onFailure(Call<UserProfileDto> call, Throwable t) {
            }
        });
    }

    private void uploadAvatar(android.net.Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            byte[] bytes = is.readAllBytes();
            is.close();

            okhttp3.RequestBody reqFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), bytes);
            okhttp3.MultipartBody.Part body = okhttp3.MultipartBody.Part.createFormData("file", "avatar.jpg", reqFile);

            Toast.makeText(this, "Đang tải ảnh đại diện lên...", Toast.LENGTH_SHORT).show();
            ApiClient.getService(this).uploadAvatar(body).enqueue(new Callback<UserProfileDto>() {
                @Override
                public void onResponse(Call<UserProfileDto> call, Response<UserProfileDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(ProfileActivity.this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                        loadProfileInfo();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Lỗi: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UserProfileDto> call, Throwable t) {
                    Toast.makeText(ProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Không thể đọc file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFollowButtonState(boolean isFollowing) {
        if (isFollowing) {
            btnFollowAction.setText("Đang theo dõi");
            btnFollowAction.setBackgroundResource(R.drawable.bg_button_light);
            btnFollowAction.setTextColor(getColor(R.color.color_primary));
        } else {
            btnFollowAction.setText("+ Theo dõi");
            btnFollowAction.setBackgroundResource(R.drawable.bg_button_primary);
            btnFollowAction.setTextColor(getColor(R.color.color_text_white));
        }
    }

    private void toggleFollow() {
        if (userProfile == null) return;
        boolean currentlyFollowing = userProfile.isFollowing();

        Call<UserProfileDto> call = currentlyFollowing
                ? ApiClient.getService(this).unfollowUser(targetUsername)
                : ApiClient.getService(this).followUser(targetUsername);

        call.enqueue(new Callback<UserProfileDto>() {
            @Override
            public void onResponse(Call<UserProfileDto> call, Response<UserProfileDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userProfile = response.body();
                    updateFollowButtonState(userProfile.isFollowing());
                    tvStatFollowers.setText(formatCount(userProfile.getFollowerCount()));
                    Toast.makeText(ProfileActivity.this, userProfile.isFollowing() ? "Đã theo dõi" : "Đã hủy theo dõi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileDto> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTabContent() {
        progressBarProfile.setVisibility(View.VISIBLE);
        Call<PageResponse<VideoDto>> call;

        if (!isOwnProfile) {
            // Other user's profile: only public videos
            call = ApiClient.getService(this).getUserVideos(targetUsername, 0, 50);
        } else {
            if (currentTab == 1) {
                // Private / Hidden videos
                call = ApiClient.getService(this).getMyPrivateVideos(0, 50);
            } else if (currentTab == 2) {
                // Liked videos (only self can see)
                call = ApiClient.getService(this).getLikedVideos(0, 50);
            } else if (currentTab == 3) {
                // Bookmarked / Saved videos (only self can see)
                call = ApiClient.getService(this).getBookmarkedVideos(0, 50);
            } else {
                // Own public videos
                call = ApiClient.getService(this).getMyVideos(0, 50);
            }
        }

        call.enqueue(new Callback<PageResponse<VideoDto>>() {
            @Override
            public void onResponse(Call<PageResponse<VideoDto>> call, Response<PageResponse<VideoDto>> response) {
                progressBarProfile.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<VideoDto> page = response.body();
                    List<VideoDto> items = page.getItems();
                    adapter.setVideos(items);

                    if (items.isEmpty()) {
                        tvEmptyMyVideos.setVisibility(View.VISIBLE);
                        rvMyVideos.setVisibility(View.GONE);
                        if (currentTab == 1) {
                            tvEmptyMyVideos.setText("Không có video nào bị ẩn");
                        } else if (currentTab == 2) {
                            tvEmptyMyVideos.setText("Bạn chưa thích video nào");
                        } else if (currentTab == 3) {
                            tvEmptyMyVideos.setText("Bạn chưa lưu video nào");
                        } else {
                            tvEmptyMyVideos.setText(isOwnProfile ? "Bạn chưa đăng video nào" : "Người dùng này chưa có video nào");
                        }
                    } else {
                        tvEmptyMyVideos.setVisibility(View.GONE);
                        rvMyVideos.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Lỗi tải video: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<VideoDto>> call, Throwable t) {
                progressBarProfile.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVideoManageDialog(VideoDto video, int position) {
        String toggleText = "Sửa video";
        CharSequence[] options = {toggleText, "Xóa video"};

        new AlertDialog.Builder(this)
                .setTitle(video.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditVideoDialog(video, position);
                    } else if (which == 1) {
                        confirmDeleteVideo(video, position);
                    }
                })
                .show();
    }

    private void toggleVideoPrivacy(VideoDto video, int position) {
        ApiClient.getService(this).toggleVideoPrivacy(video.getId()).enqueue(new Callback<VideoDto>() {
            @Override
            public void onResponse(Call<VideoDto> call, Response<VideoDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    VideoDto updated = response.body();
                    Toast.makeText(ProfileActivity.this, updated.isPrivate() ? "Đã ẩn video" : "Đã đặt công khai", Toast.LENGTH_SHORT).show();
                    adapter.removeAt(position);
                } else {
                    Toast.makeText(ProfileActivity.this, "Lỗi: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<VideoDto> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditVideoDialog(VideoDto video, int position) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText etEditTitle = new EditText(this);
        etEditTitle.setHint("Tiêu đề video");
        etEditTitle.setText(video.getTitle() != null ? video.getTitle() : "");
        layout.addView(etEditTitle);

        EditText etEditDescription = new EditText(this);
        etEditDescription.setHint("Mô tả video");
        etEditDescription.setText(video.getDescription() != null ? video.getDescription() : "");
        etEditDescription.setMinLines(2);
        layout.addView(etEditDescription);

        androidx.appcompat.widget.SwitchCompat switchEditPrivate = new androidx.appcompat.widget.SwitchCompat(this);
        switchEditPrivate.setText("Chỉ mình tôi (riêng tư)");
        switchEditPrivate.setChecked(video.isPrivate());
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        switchParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        layout.addView(switchEditPrivate, switchParams);

        new AlertDialog.Builder(this)
                .setTitle("Sửa video")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newTitle = etEditTitle.getText().toString().trim();
                    String newDescription = etEditDescription.getText().toString().trim();
                    if (newTitle.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập tiêu đề cho video", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ApiClient.getService(this)
                            .updateVideo(video.getId(), new UpdateVideoRequest(newTitle, newDescription, switchEditPrivate.isChecked()))
                            .enqueue(new Callback<VideoDto>() {
                                @Override
                                public void onResponse(Call<VideoDto> call, Response<VideoDto> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        loadTabContent();
                                        Toast.makeText(ProfileActivity.this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Lỗi: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<VideoDto> call, Throwable t) {
                                    Toast.makeText(ProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteVideo(VideoDto video, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa video")
                .setMessage("Bạn có chắc chắn muốn xóa video \"" + video.getTitle() + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteVideo(video.getId(), position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteVideo(Long videoId, int position) {
        ApiClient.getService(this).deleteVideo(videoId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    adapter.removeAt(position);
                    Toast.makeText(ProfileActivity.this, "Đã xóa video", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "Lỗi xóa video: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditBioDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView lblDisplayName = new TextView(this);
        lblDisplayName.setText("Tên hiển thị");
        lblDisplayName.setTextColor(0xFFFFFFFF);
        lblDisplayName.setTextSize(13);
        layout.addView(lblDisplayName);

        EditText etDisplayName = new EditText(this);
        etDisplayName.setHint("Nhập tên hiển thị (tối đa 80 ký tự)...");
        String currentName = userProfile != null && userProfile.getDisplayName() != null
                ? userProfile.getDisplayName()
                : (tvProfileDisplayName.getText() != null ? tvProfileDisplayName.getText().toString() : "");
        etDisplayName.setText(currentName);
        etDisplayName.setMaxLines(1);
        etDisplayName.setSingleLine(true);
        layout.addView(etDisplayName);

        TextView lblBio = new TextView(this);
        lblBio.setText("Tiểu sử");
        lblBio.setTextColor(0xFFFFFFFF);
        lblBio.setTextSize(13);
        LinearLayout.LayoutParams bioLabelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bioLabelParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        layout.addView(lblBio, bioLabelParams);

        final EditText input = new EditText(this);
        input.setHint("Nhập tiểu sử ngắn (tối đa 200 ký tự)...");
        String currentBio = userProfile != null && userProfile.getBio() != null ? userProfile.getBio() : "";
        input.setText(currentBio);
        input.setLines(3);
        input.setMaxLines(5);
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa thông tin cá nhân")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = etDisplayName.getText().toString().trim();
                    String newBio = input.getText().toString().trim();
                    updateProfileOnServer(newName.isEmpty() ? null : newName, newBio);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateProfileOnServer(String newDisplayName, String newBio) {
        UpdateProfileRequest request = new UpdateProfileRequest(newDisplayName, newBio);
        ApiClient.getService(this).updateProfile(request).enqueue(new Callback<UserProfileDto>() {
            @Override
            public void onResponse(Call<UserProfileDto> call, Response<UserProfileDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (isOwnProfile && newDisplayName != null && !newDisplayName.isEmpty()) {
                        sessionManager.setDisplayName(newDisplayName);
                    }
                    Toast.makeText(ProfileActivity.this, "Đã cập nhật thông tin cá nhân!", Toast.LENGTH_SHORT).show();
                    loadProfileInfo();
                } else {
                    Toast.makeText(ProfileActivity.this, "Lỗi: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileDto> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFollowListDialog(boolean isFollowing) {
        String queryUser = targetUsername != null ? targetUsername : sessionManager.getUsername();
        if (queryUser == null || queryUser.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_user_list, null);
        dialog.setContentView(sheetView);

        TextView txtTitle = sheetView.findViewById(R.id.txtUserListTitle);
        ImageView btnClose = sheetView.findViewById(R.id.btnUserListClose);
        RecyclerView rvUsers = sheetView.findViewById(R.id.rvUserList);
        ProgressBar progressBar = sheetView.findViewById(R.id.progressBarUserList);
        TextView txtEmpty = sheetView.findViewById(R.id.txtEmptyUserList);

        txtTitle.setText(isFollowing ? "Đang theo dõi" : "Người theo dõi");
        btnClose.setOnClickListener(v -> dialog.dismiss());

        UserSearchAdapter userAdapter = new UserSearchAdapter(user -> {
            dialog.dismiss();
            if (user.getUsername() != null && !user.getUsername().equals(queryUser)) {
                Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
                intent.putExtra("username", user.getUsername());
                startActivity(intent);
            }
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);

        progressBar.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);

        Call<List<UserProfileDto>> call = isFollowing
                ? ApiClient.getService(this).getFollowing(queryUser)
                : ApiClient.getService(this).getFollowers(queryUser);

        call.enqueue(new Callback<List<UserProfileDto>>() {
            @Override
            public void onResponse(Call<List<UserProfileDto>> call, Response<List<UserProfileDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<UserProfileDto> list = response.body();
                    userAdapter.setUsers(list);
                    txtTitle.setText((isFollowing ? "Đang theo dõi (" : "Người theo dõi (") + list.size() + ")");
                    if (list.isEmpty()) {
                        txtEmpty.setText(isFollowing ? "Chưa theo dõi người dùng nào" : "Chưa có người theo dõi nào");
                        txtEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    txtEmpty.setText("Không thể tải danh sách");
                    txtEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<UserProfileDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                txtEmpty.setText("Lỗi kết nối: " + t.getMessage());
                txtEmpty.setVisibility(View.VISIBLE);
            }
        });

        dialog.show();
    }
    private void showAccountMenu() {
        CharSequence[] options = {"Đổi mật khẩu", "Đăng xuất"};
        new AlertDialog.Builder(this)
                .setTitle("Cài đặt tài khoản")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showChangePasswordDialog();
                    } else {
                        confirmLogout();
                    }
                })
                .show();
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText etCurrent = new EditText(this);
        etCurrent.setHint("Mật khẩu hiện tại");
        etCurrent.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrent);

        EditText etNew = new EditText(this);
        etNew.setHint("Mật khẩu mới (từ 6 ký tự)");
        etNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNew);

        EditText etConfirm = new EditText(this);
        etConfirm.setHint("Nhập lại mật khẩu mới");
        etConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirm);

        new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(layout)
                .setPositiveButton("Đổi", (dialog, which) -> {
                    String current = etCurrent.getText().toString();
                    String newPass = etNew.getText().toString();
                    String confirm = etConfirm.getText().toString();
                    if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Mật khẩu mới phải từ 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirm)) {
                        Toast.makeText(this, "Mật khẩu nhập lại chưa khớp", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ApiClient.getService(this).changePassword(new ChangePasswordRequest(current, newPass))
                            .enqueue(new Callback<java.util.Map<String, String>>() {
                                @Override
                                public void onResponse(Call<java.util.Map<String, String>> call, Response<java.util.Map<String, String>> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(ProfileActivity.this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Lỗi: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<java.util.Map<String, String>> call, Throwable t) {
                                    Toast.makeText(ProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất khỏi tài khoản này?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    sessionManager.clear();
                    Toast.makeText(ProfileActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                    goToLogin();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String formatCount(int count) {
        if (count >= 1000000) {
            return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", count / 1000.0);
        } else {
            return String.valueOf(count);
        }
    }
}










