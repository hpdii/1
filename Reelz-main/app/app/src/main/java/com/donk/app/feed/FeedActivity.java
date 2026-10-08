package com.donk.app.feed;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.donk.app.R;
import com.donk.app.auth.LoginActivity;
import com.donk.app.models.BookmarkResponse;
import com.donk.app.models.LikeResponse;
import com.donk.app.models.PageResponse;
import com.donk.app.models.UpdateVideoRequest;
import com.donk.app.models.VideoDto;
import com.donk.app.network.ApiClient;
import com.donk.app.network.SessionManager;
import com.donk.app.profile.ProfileActivity;
import com.donk.app.upload.UploadActivity;
import com.donk.app.utils.DownloadUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedActivity extends AppCompatActivity implements FeedAdapter.OnFeedActionListener {

    private RecyclerView recyclerViewFeed;
    private View layoutEmpty;
    private View btnEmptyUpload;
    private View btnReloadFeed;
    private View layoutTopBarFeed;
    private TextView tabFeedFollowing;
    private TextView tabFeedReels;
    private View btnFeedSearch;
    private View bottomNav;
    private View navHome;
    private View navUpload;
    private View navProfile;

    private ExoPlayer player;
    private FeedAdapter adapter;
    private LinearLayoutManager layoutManager;
    private PagerSnapHelper snapHelper;

    private int currentPlayingPos = -1;
    private int currentFeedMode = 1; // 0 = Đã follow, 1 = Reels (Khám phá)
    private final java.util.Map<Long, Float> videoSpeeds = new java.util.HashMap<>();
    private SessionManager sessionManager;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying() && currentPlayingPos >= 0) {
                RecyclerView.ViewHolder vh = recyclerViewFeed.findViewHolderForAdapterPosition(currentPlayingPos);
                if (vh instanceof FeedAdapter.FeedViewHolder) {
                    ((FeedAdapter.FeedViewHolder) vh).updateProgress(player.getCurrentPosition(), player.getDuration());
                }
            }
            progressHandler.postDelayed(this, 300);
        }
    };

    private final ActivityResultLauncher<Intent> uploadLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadFeed();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);

        setContentView(R.layout.activity_feed);

        // Edge-to-edge transparent navigation & status bars
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        initViews();
        setupPlayer();
        setupRecyclerView();
        setupBottomNav();

        loadFeed();
    }

    private void initViews() {
        recyclerViewFeed = findViewById(R.id.recyclerViewFeed);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnEmptyUpload = findViewById(R.id.btnEmptyUpload);
        btnReloadFeed = findViewById(R.id.btnReloadFeed);
        layoutTopBarFeed = findViewById(R.id.layoutTopBarFeed);
        tabFeedFollowing = findViewById(R.id.tabFeedFollowing);
        tabFeedReels = findViewById(R.id.tabFeedReels);
        btnFeedSearch = findViewById(R.id.btnFeedSearch);
        bottomNav = findViewById(R.id.bottomNav);
        navHome = findViewById(R.id.navHome);
        navUpload = findViewById(R.id.navUpload);
        navProfile = findViewById(R.id.navProfile);
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean isLandscape = (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE);
        updateSystemBars(isLandscape);
        if (currentPlayingPos >= 0) {
            RecyclerView.ViewHolder vh = recyclerViewFeed.findViewHolderForAdapterPosition(currentPlayingPos);
            if (vh instanceof FeedAdapter.FeedViewHolder) {
                ((FeedAdapter.FeedViewHolder) vh).setOrientationUI(isLandscape);
            }
        }
    }

    private void updateSystemBars(boolean isLandscape) {
        androidx.core.view.WindowInsetsControllerCompat controller =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            if (isLandscape) {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            }
        }
        if (bottomNav != null) {
            bottomNav.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        }
        if (layoutTopBarFeed != null) {
            layoutTopBarFeed.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return;
        }
        super.onBackPressed();
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updateBufferingState(playbackState == Player.STATE_BUFFERING);
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("DonkFeed", "ExoPlayer error: " + error.getMessage(), error);
                Toast.makeText(FeedActivity.this, "Lỗi phát video: " + error.getErrorCodeName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewFeed.setLayoutManager(layoutManager);

        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerViewFeed);

        adapter = new FeedAdapter(player, this);
        recyclerViewFeed.setAdapter(adapter);

        recyclerViewFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    playVideoAtCurrentSnap();
                }
            }
        });
    }

    private void setupBottomNav() {
        tabFeedFollowing.setOnClickListener(v -> switchFeedMode(0));
        tabFeedReels.setOnClickListener(v -> switchFeedMode(1));

        navHome.setOnClickListener(v -> {
            loadFeed();
            if (adapter.getItemCount() > 0) {
                recyclerViewFeed.smoothScrollToPosition(0);
                playVideoAt(0);
            }
        });

        btnReloadFeed.setOnClickListener(v -> {
            Toast.makeText(this, "Đang tải lại danh sách video...", Toast.LENGTH_SHORT).show();
            loadFeed();
        });

        btnFeedSearch.setOnClickListener(v -> {
            if (!requireLogin()) return;
            startActivity(new Intent(FeedActivity.this, com.donk.app.search.SearchActivity.class));
        });

        navUpload.setOnClickListener(v -> {
            if (!requireLogin()) return;
            uploadLauncher.launch(new Intent(FeedActivity.this, UploadActivity.class));
        });

        btnEmptyUpload.setOnClickListener(v -> {
            if (!requireLogin()) return;
            uploadLauncher.launch(new Intent(FeedActivity.this, UploadActivity.class));
        });

        navProfile.setOnClickListener(v -> {
            if (!requireLogin()) return;
            startActivity(new Intent(FeedActivity.this, ProfileActivity.class));
        });
    }

    private void switchFeedMode(int mode) {
        if (currentFeedMode == mode) return;
        currentFeedMode = mode;

        if (mode == 0) {
            tabFeedFollowing.setTextColor(getColor(R.color.color_text_white));
            tabFeedFollowing.setTextSize(18);
            tabFeedFollowing.setTypeface(null, android.graphics.Typeface.BOLD);

            tabFeedReels.setTextColor(getColor(R.color.color_text_tertiary));
            tabFeedReels.setTextSize(17);
            tabFeedReels.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tabFeedReels.setTextColor(getColor(R.color.color_text_white));
            tabFeedReels.setTextSize(18);
            tabFeedReels.setTypeface(null, android.graphics.Typeface.BOLD);

            tabFeedFollowing.setTextColor(getColor(R.color.color_text_tertiary));
            tabFeedFollowing.setTextSize(17);
            tabFeedFollowing.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        if (player.isPlaying()) {
            player.stop();
        }
        currentPlayingPos = -1;
        loadFeed();
    }

    private void loadFeed() {
        Call<PageResponse<VideoDto>> call = (currentFeedMode == 0)
                ? ApiClient.getService(this).getFollowingFeed(0, 20)
                : ApiClient.getService(this).getFeed(0, 20);

        call.enqueue(new Callback<PageResponse<VideoDto>>() {
            @Override
            public void onResponse(Call<PageResponse<VideoDto>> call, Response<PageResponse<VideoDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<VideoDto> page = response.body();
                    List<VideoDto> items = page.getItems();
                    adapter.setVideos(items);

                    if (items == null || items.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerViewFeed.setVisibility(View.GONE);
                        if (player.isPlaying()) {
                            player.stop();
                        }
                        currentPlayingPos = -1;
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerViewFeed.setVisibility(View.VISIBLE);
                        recyclerViewFeed.post(() -> playVideoAt(0));
                    }
                } else {
                    String err = ApiClient.parseError(response);
                    Toast.makeText(FeedActivity.this, "Không thể tải video: " + err, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<VideoDto>> call, Throwable t) {
                Toast.makeText(FeedActivity.this, "Lỗi kết nối tới server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playVideoAtCurrentSnap() {
        int pos = 0;
        View snapView = snapHelper.findSnapView(layoutManager);
        if (snapView != null) {
            int snappedPos = layoutManager.getPosition(snapView);
            if (snappedPos != RecyclerView.NO_POSITION) {
                pos = snappedPos;
            }
        }
        playVideoAt(pos);
    }

    private void playVideoAt(int pos) {
        if (pos < 0 || pos >= adapter.getItemCount()) {
            return;
        }

        if (pos == currentPlayingPos && player.isPlaying()) {
            return;
        }

        currentPlayingPos = pos;
        VideoDto video = adapter.getVideoAt(pos);
        if (video != null && video.getVideoPath() != null) {
            String fullUrl = ApiClient.getVideoUrl(video.getVideoPath());
            Log.d("DonkFeed", "Playing video at pos " + pos + ": " + fullUrl);

            MediaItem mediaItem = MediaItem.fromUri(fullUrl);
            player.setMediaItem(mediaItem);
            player.setPlaybackSpeed(getVideoSpeed(video));
            player.prepare();
            player.play();

            RecyclerView.ViewHolder vh = recyclerViewFeed.findViewHolderForAdapterPosition(pos);
            if (vh instanceof FeedAdapter.FeedViewHolder) {
                FeedAdapter.FeedViewHolder feedVh = (FeedAdapter.FeedViewHolder) vh;
                feedVh.playerView.setPlayer(player);
                feedVh.hideThumbnail();
                feedVh.ivPlayIndicator.setVisibility(View.GONE);
                boolean isLandscape = (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE);
                feedVh.setOrientationUI(isLandscape);
            }
        }
    }

    private void updateBufferingState(boolean buffering) {
        if (currentPlayingPos >= 0) {
            RecyclerView.ViewHolder vh = recyclerViewFeed.findViewHolderForAdapterPosition(currentPlayingPos);
            if (vh instanceof FeedAdapter.FeedViewHolder) {
                ((FeedAdapter.FeedViewHolder) vh).progressBarBuffering.setVisibility(buffering ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onLikeClick(VideoDto video, int position) {
        if (!requireLogin()) return;
        boolean currentlyLiked = video.isLiked();
        int newCount = currentlyLiked ? Math.max(0, video.getLikeCount() - 1) : video.getLikeCount() + 1;
        video.setLiked(!currentlyLiked);
        video.setLikeCount(newCount);
        RecyclerView.ViewHolder currentHolder = recyclerViewFeed.findViewHolderForAdapterPosition(position);
        if (currentHolder instanceof FeedAdapter.FeedViewHolder) {
            ((FeedAdapter.FeedViewHolder) currentHolder).updateLikeIcon(video.isLiked());
            ((FeedAdapter.FeedViewHolder) currentHolder).tvLikeCount.setText(String.valueOf(newCount));
        }
        adapter.updateLikeState(position, video);

        Call<LikeResponse> call = currentlyLiked
                ? ApiClient.getService(this).unlikeVideo(video.getId())
                : ApiClient.getService(this).likeVideo(video.getId());

        call.enqueue(new Callback<LikeResponse>() {
            @Override
            public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LikeResponse res = response.body();
                    video.setLiked(res.isLiked());
                    video.setLikeCount(res.getLikeCount());
                    adapter.updateLikeState(position, video);
                }
            }

            @Override
            public void onFailure(Call<LikeResponse> call, Throwable t) {
                video.setLiked(currentlyLiked);
                video.setLikeCount(currentlyLiked ? newCount + 1 : newCount - 1);
                adapter.updateVideo(position, video);
                Toast.makeText(FeedActivity.this, "Lỗi cập nhật lượt like", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCommentClick(VideoDto video, int position) {
        if (!requireLogin()) return;
        CommentsBottomSheet sheet = CommentsBottomSheet.newInstance(video.getId(), video.getCommentCount(), video.getUserId());
        sheet.setOnCommentCountChangedListener(newCount -> {
            video.setCommentCount(newCount);
            adapter.updateVideo(position, video);
        });
        sheet.show(getSupportFragmentManager(), "CommentsBottomSheet");
    }

    @Override
    public void onBookmarkClick(VideoDto video, int position) {
        if (!requireLogin()) return;
        boolean currentlyBookmarked = video.isBookmarked();
        video.setBookmarked(!currentlyBookmarked);
        adapter.updateVideo(position, video);

        Call<BookmarkResponse> call = currentlyBookmarked
                ? ApiClient.getService(this).unbookmarkVideo(video.getId())
                : ApiClient.getService(this).bookmarkVideo(video.getId());

        call.enqueue(new Callback<BookmarkResponse>() {
            @Override
            public void onResponse(Call<BookmarkResponse> call, Response<BookmarkResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BookmarkResponse res = response.body();
                    video.setBookmarked(res.isBookmarked());
                    adapter.updateVideo(position, video);
                    Toast.makeText(FeedActivity.this, res.isBookmarked() ? "Đã lưu video" : "Đã bỏ lưu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BookmarkResponse> call, Throwable t) {
                video.setBookmarked(currentlyBookmarked);
                adapter.updateVideo(position, video);
                Toast.makeText(FeedActivity.this, "Lỗi cập nhật lưu video", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onShareClick(VideoDto video) {
        String videoUrl = ApiClient.getVideoUrl(video.getVideoPath());
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, video.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, video.getTitle() + "\nXem ngay trên Reelz:" + videoUrl);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ video qua"));
    }

    @Override
    public void onMoreOptionsClick(VideoDto video, int position, FeedAdapter.FeedViewHolder holder) {
        List<String> options = new ArrayList<>();
        options.add("Tốc độ phát video này (hiện tại " + getVideoSpeed(video) + "x)");
        options.add("Chia sẻ video");
        options.add("Tải video về máy");

        boolean isMyVideo = sessionManager.getUserId() != null && sessionManager.getUserId().equals(video.getUserId());
        if (isMyVideo) {
            options.add("Sửa video");
            options.add("Xóa video của tôi");
        }

        CharSequence[] items = options.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
                .setTitle(video.getTitle())
                .setItems(items, (dialog, which) -> {
                    String selected = items[which].toString();
                    if (selected.startsWith("Tốc độ phát")) {
                        showSpeedPickerDialog(video);
                    } else if (selected.startsWith("Chia sẻ")) {
                        onShareClick(video);
                    } else if (selected.startsWith("Tải video")) {
                        DownloadUtils.downloadVideo(this, ApiClient.getVideoUrl(video.getVideoPath()), video.getTitle());
                    } else if (selected.startsWith("Sửa video")) {
                        showEditVideoDialog(video, position);
                    } else if (selected.contains("Xóa video")) {
                        confirmDeleteVideo(video, position);
                    }
                })
                .show();
    }

    private float getVideoSpeed(VideoDto video) {
        if (video == null || video.getId() == null) return 1.0f;
        Float speed = videoSpeeds.get(video.getId());
        return speed != null ? speed : 1.0f;
    }

    private void showSpeedPickerDialog(VideoDto video) {
        CharSequence[] speeds = {"0.75x", "1.0x (Mặc định)", "1.25x", "1.5x", "2.0x"};
        float[] speedValues = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

        new AlertDialog.Builder(this)
                .setTitle("Tốc độ của video này")
                .setItems(speeds, (dialog, which) -> {
                    float chosen = speedValues[which];
                    if (video.getId() != null) {
                        videoSpeeds.put(video.getId(), chosen);
                    }
                    if (player != null && currentPlayingPos == getVideoPosition(video)) {
                        player.setPlaybackSpeed(chosen);
                    }
                    Toast.makeText(this, "Video này phát ở tốc độ: " + chosen + "x", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private int getVideoPosition(VideoDto video) {
        if (video == null || video.getId() == null) return -1;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            VideoDto item = adapter.getVideoAt(i);
            if (item != null && video.getId().equals(item.getId())) return i;
        }
        return -1;
    }

    private void toggleVideoPrivacy(VideoDto video, int position) {
        ApiClient.getService(this).toggleVideoPrivacy(video.getId()).enqueue(new Callback<VideoDto>() {
            @Override
            public void onResponse(Call<VideoDto> call, Response<VideoDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    VideoDto updated = response.body();
                    video.setPrivate(updated.isPrivate());
                    adapter.updateVideo(position, video);
                    Toast.makeText(FeedActivity.this, updated.isPrivate() ? "Đã ẩn video" : "Đã đặt công khai", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FeedActivity.this, "Lỗi: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<VideoDto> call, Throwable t) {
                Toast.makeText(FeedActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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
                                        VideoDto updated = response.body();
                                        video.setTitle(updated.getTitle());
                                        video.setDescription(updated.getDescription());
                                        video.setPrivate(updated.isPrivate());
                                        adapter.updateVideo(position, video);
                                        Toast.makeText(FeedActivity.this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(FeedActivity.this, "Lỗi: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<VideoDto> call, Throwable t) {
                                    Toast.makeText(FeedActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteVideo(VideoDto video, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa video")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn video này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    ApiClient.getService(this).deleteVideo(video.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(FeedActivity.this, "Đã xóa video", Toast.LENGTH_SHORT).show();
                                loadFeed();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onAuthorClick(VideoDto video) {
        if (video.getUsername() != null) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("username", video.getUsername());
            startActivity(intent);
        }
    }

    @Override
    public void onSeek(long progress) {
        if (player != null && player.getDuration() > 0) {
            long targetMs = (player.getDuration() * progress) / 1000;
            player.seekTo(targetMs);
        }
    }

    @Override
    public void onVideoTap(FeedAdapter.FeedViewHolder holder) {
        if (player.isPlaying()) {
            player.pause();
            holder.ivPlayIndicator.setVisibility(View.VISIBLE);
        } else {
            player.play();
            holder.ivPlayIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isLandscape = (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE);
        updateSystemBars(isLandscape);
        progressHandler.post(progressRunnable);
        loadFeed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentPlayingPos >= 0 && adapter.getItemCount() > 0) {
            player.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        progressHandler.removeCallbacks(progressRunnable);
        player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        player.release();
    }

    private boolean requireLogin() {
        if (sessionManager.isLoggedIn()) {
            return true;
        }
        startActivity(new Intent(this, LoginActivity.class));
        return false;
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}








