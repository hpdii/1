package com.donk.app.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donk.app.R;
import com.donk.app.feed.FeedActivity;
import com.donk.app.models.SearchResponse;
import com.donk.app.models.UserProfileDto;
import com.donk.app.models.VideoDto;
import com.donk.app.network.ApiClient;
import com.donk.app.profile.ProfileActivity;
import com.donk.app.profile.VideoGridAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private ImageView btnSearchBack;
    private EditText etSearchInput;
    private ImageView btnClearSearch;
    private View tabSearchUsers;
    private TextView tvTabSearchUsersText;
    private View indicatorSearchUsers;
    private View tabSearchVideos;
    private TextView tvTabSearchVideosText;
    private View indicatorSearchVideos;
    private ProgressBar progressBarSearch;
    private TextView tvSearchEmpty;
    private RecyclerView rvSearchUsers;
    private RecyclerView rvSearchVideos;

    private UserSearchAdapter userAdapter;
    private VideoGridAdapter videoAdapter;

    private int currentTab = 0; // 0 = Users, 1 = Videos
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final List<UserProfileDto> cachedUsers = new ArrayList<>();
    private final List<VideoDto> cachedVideos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupRecyclerViews();
        setupListeners();
        autoFocusKeyboard();
    }

    private void initViews() {
        btnSearchBack = findViewById(R.id.btnSearchBack);
        etSearchInput = findViewById(R.id.etSearchInput);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tabSearchUsers = findViewById(R.id.tabSearchUsers);
        tvTabSearchUsersText = findViewById(R.id.tvTabSearchUsersText);
        indicatorSearchUsers = findViewById(R.id.indicatorSearchUsers);
        tabSearchVideos = findViewById(R.id.tabSearchVideos);
        tvTabSearchVideosText = findViewById(R.id.tvTabSearchVideosText);
        indicatorSearchVideos = findViewById(R.id.indicatorSearchVideos);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        tvSearchEmpty = findViewById(R.id.tvSearchEmpty);
        rvSearchUsers = findViewById(R.id.rvSearchUsers);
        rvSearchVideos = findViewById(R.id.rvSearchVideos);
    }

    private void autoFocusKeyboard() {
        etSearchInput.requestFocus();
        etSearchInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 150);
    }

    private void setupRecyclerViews() {
        // Users list
        rvSearchUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserSearchAdapter(user -> {
            Intent intent = new Intent(SearchActivity.this, ProfileActivity.class);
            intent.putExtra("username", user.getUsername());
            startActivity(intent);
        });
        rvSearchUsers.setAdapter(userAdapter);

        // Videos 3-column grid
        rvSearchVideos.setLayoutManager(new GridLayoutManager(this, 3));
        videoAdapter = new VideoGridAdapter(new VideoGridAdapter.OnVideoGridClickListener() {
            @Override
            public void onVideoClick(VideoDto video, int position) {
                Intent intent = new Intent(SearchActivity.this, FeedActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }

            @Override
            public void onVideoLongClick(VideoDto video, int position) {
            }
        });
        rvSearchVideos.setAdapter(videoAdapter);
    }

    private void setupListeners() {
        btnSearchBack.setOnClickListener(v -> finish());

        btnClearSearch.setOnClickListener(v -> {
            etSearchInput.setText("");
            cachedUsers.clear();
            cachedVideos.clear();
            userAdapter.setUsers(null);
            videoAdapter.setVideos(null);
            tvSearchEmpty.setVisibility(View.VISIBLE);
            tvSearchEmpty.setText("Nhập từ khóa để bắt đầu tìm kiếm");
        });

        tabSearchUsers.setOnClickListener(v -> selectTab(0));
        tabSearchVideos.setOnClickListener(v -> selectTab(1));

        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchInput.getText().toString().trim());
                return true;
            }
            return false;
        });

        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> performSearch(s.toString().trim());
                searchHandler.postDelayed(searchRunnable, 350); // 350ms debounce
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void selectTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;

        if (tab == 0) {
            tvTabSearchUsersText.setTextColor(getColor(R.color.color_text_white));
            tvTabSearchUsersText.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorSearchUsers.setVisibility(View.VISIBLE);

            tvTabSearchVideosText.setTextColor(getColor(R.color.color_text_secondary));
            tvTabSearchVideosText.setTypeface(null, android.graphics.Typeface.NORMAL);
            indicatorSearchVideos.setVisibility(View.INVISIBLE);

            rvSearchUsers.setVisibility(cachedUsers.isEmpty() ? View.GONE : View.VISIBLE);
            rvSearchVideos.setVisibility(View.GONE);
            updateEmptyState(cachedUsers.isEmpty());
        } else {
            tvTabSearchVideosText.setTextColor(getColor(R.color.color_text_white));
            tvTabSearchVideosText.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorSearchVideos.setVisibility(View.VISIBLE);

            tvTabSearchUsersText.setTextColor(getColor(R.color.color_text_secondary));
            tvTabSearchUsersText.setTypeface(null, android.graphics.Typeface.NORMAL);
            indicatorSearchUsers.setVisibility(View.INVISIBLE);

            rvSearchUsers.setVisibility(View.GONE);
            rvSearchVideos.setVisibility(cachedVideos.isEmpty() ? View.GONE : View.VISIBLE);
            updateEmptyState(cachedVideos.isEmpty());
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            cachedUsers.clear();
            cachedVideos.clear();
            userAdapter.setUsers(null);
            videoAdapter.setVideos(null);
            tvSearchEmpty.setVisibility(View.VISIBLE);
            tvSearchEmpty.setText("Nhập từ khóa để bắt đầu tìm kiếm");
            return;
        }

        progressBarSearch.setVisibility(View.VISIBLE);
        tvSearchEmpty.setVisibility(View.GONE);

        ApiClient.getService(this).search(query).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                progressBarSearch.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    SearchResponse res = response.body();

                    cachedUsers.clear();
                    cachedUsers.addAll(res.getUsers());
                    userAdapter.setUsers(cachedUsers);

                    cachedVideos.clear();
                    cachedVideos.addAll(res.getVideos());
                    videoAdapter.setVideos(cachedVideos);

                    if (currentTab == 0) {
                        rvSearchUsers.setVisibility(cachedUsers.isEmpty() ? View.GONE : View.VISIBLE);
                        rvSearchVideos.setVisibility(View.GONE);
                        updateEmptyState(cachedUsers.isEmpty());
                    } else {
                        rvSearchUsers.setVisibility(View.GONE);
                        rvSearchVideos.setVisibility(cachedVideos.isEmpty() ? View.GONE : View.VISIBLE);
                        updateEmptyState(cachedVideos.isEmpty());
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "Lỗi tìm kiếm: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                progressBarSearch.setVisibility(View.GONE);
                Toast.makeText(SearchActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            tvSearchEmpty.setVisibility(View.VISIBLE);
            tvSearchEmpty.setText(currentTab == 0 ? "Không tìm thấy người dùng nào" : "Không tìm thấy video nào");
        } else {
            tvSearchEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchHandler.removeCallbacksAndMessages(null);
    }
}
