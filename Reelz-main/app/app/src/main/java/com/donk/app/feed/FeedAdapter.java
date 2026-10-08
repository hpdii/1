package com.donk.app.feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.donk.app.R;
import com.donk.app.models.VideoDto;
import com.donk.app.network.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    public interface OnFeedActionListener {
        void onLikeClick(VideoDto video, int position);

        void onCommentClick(VideoDto video, int position);

        void onBookmarkClick(VideoDto video, int position);

        void onShareClick(VideoDto video);

        void onMoreOptionsClick(VideoDto video, int position, FeedViewHolder holder);

        void onAuthorClick(VideoDto video);

        void onVideoTap(FeedViewHolder holder);

        void onSeek(long targetPositionMs);
    }

    private final List<VideoDto> videos = new ArrayList<>();
    private final ExoPlayer player;
    private final OnFeedActionListener actionListener;

    public FeedAdapter(ExoPlayer player, OnFeedActionListener actionListener) {
        this.player = player;
        this.actionListener = actionListener;
    }

    public void setVideos(List<VideoDto> newVideos) {
        videos.clear();
        if (newVideos != null) {
            videos.addAll(newVideos);
        }
        notifyDataSetChanged();
    }

    public VideoDto getVideoAt(int position) {
        if (position >= 0 && position < videos.size()) {
            return videos.get(position);
        }
        return null;
    }

    public void updateVideo(int position, VideoDto video) {
        if (position >= 0 && position < videos.size()) {
            videos.set(position, video);
            notifyItemChanged(position);
        }
    }

    public void updateLikeState(int position, VideoDto video) {
        if (position < 0 || position >= videos.size()) return;
        videos.set(position, video);
        notifyItemChanged(position, "like_state");
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("like_state")) {
            VideoDto video = videos.get(position);
            holder.tvLikeCount.setText(holder.formatCount(video.getLikeCount()));
            holder.updateLikeIcon(video.isLiked());
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        holder.bind(videos.get(position), actionListener);
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull FeedViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.playerView.setPlayer(player);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull FeedViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.playerView.setPlayer(null);
    }

    public static class FeedViewHolder extends RecyclerView.ViewHolder {

        public final PlayerView playerView;
        public final ImageView ivVideoThumbnail;
        public final View layoutAuthor;
        public final ImageView ivAuthorAvatar;
        public final TextView tvAuthorAvatarChar;
        public final View layoutBottomInfo;
        public final TextView tvAuthor;
        public final TextView tvTitle;
        public final TextView tvDescription;

        public final View btnLike;
        public final ImageView ivLike;
        public final TextView tvLikeCount;

        public final View btnComment;
        public final ImageView ivComment;
        public final TextView tvCommentCount;

        public final View btnBookmark;
        public final ImageView ivBookmark;
        public final TextView tvBookmarkLabel;

        public final View btnShare;
        public final View btnMoreOptions;

        public final ImageView ivPlayIndicator;
        public final ProgressBar progressBarBuffering;
        public final View vGradientShadow;
        public final View layoutScrubber;
        public final SeekBar seekBarVideo;
        public final TextView tvCurrentTime;
        public final TextView tvTotalTime;

        public boolean isUserTrackingSeekBar = false;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.playerView);
            ivVideoThumbnail = itemView.findViewById(R.id.ivVideoThumbnail);
            layoutAuthor = itemView.findViewById(R.id.layoutAuthor);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            tvAuthorAvatarChar = itemView.findViewById(R.id.tvAuthorAvatarChar);
            layoutBottomInfo = itemView.findViewById(R.id.layoutBottomInfo);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);

            btnLike = itemView.findViewById(R.id.btnLike);
            ivLike = itemView.findViewById(R.id.ivLike);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);

            btnComment = itemView.findViewById(R.id.btnComment);
            ivComment = itemView.findViewById(R.id.ivComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);

            btnBookmark = itemView.findViewById(R.id.btnBookmark);
            ivBookmark = itemView.findViewById(R.id.ivBookmark);
            tvBookmarkLabel = itemView.findViewById(R.id.tvBookmarkLabel);

            btnShare = itemView.findViewById(R.id.btnShare);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);

            ivPlayIndicator = itemView.findViewById(R.id.ivPlayIndicator);
            progressBarBuffering = itemView.findViewById(R.id.progressBarBuffering);
            vGradientShadow = itemView.findViewById(R.id.vGradientShadow);
            layoutScrubber = itemView.findViewById(R.id.layoutScrubber);
            seekBarVideo = itemView.findViewById(R.id.seekBarVideo);
            tvCurrentTime = itemView.findViewById(R.id.tvCurrentTime);
            tvTotalTime = itemView.findViewById(R.id.tvTotalTime);
        }

        public void bind(VideoDto video, OnFeedActionListener listener) {
            showThumbnail(video);
            String author = video.getDisplayName() != null && !video.getDisplayName().isEmpty()
                    ? video.getDisplayName() : video.getUsername();
            tvAuthor.setText("@" + (author != null ? author : "user"));

            tvTitle.setText(video.getTitle());

            if (video.getDescription() != null && !video.getDescription().isEmpty()) {
                tvDescription.setText(video.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Author Avatar
            String username = video.getUsername() != null ? video.getUsername() : "";
            char initial = username.isEmpty() ? 'U' : Character.toUpperCase(username.charAt(0));
            tvAuthorAvatarChar.setText(String.valueOf(initial));

            if (video.getAuthorAvatarUrl() != null && !video.getAuthorAvatarUrl().isEmpty()) {
                String fullAvatarUrl = ApiClient.getVideoUrl(video.getAuthorAvatarUrl());
                Glide.with(itemView.getContext())
                        .load(fullAvatarUrl)
                        .circleCrop()
                        .into(ivAuthorAvatar);
                tvAuthorAvatarChar.setVisibility(View.GONE);
            } else {
                ivAuthorAvatar.setImageResource(R.drawable.bg_button_primary);
                tvAuthorAvatarChar.setVisibility(View.VISIBLE);
            }

            // Like state
            tvLikeCount.setText(formatCount(video.getLikeCount()));
            updateLikeIcon(video.isLiked());

            // Comment state
            tvCommentCount.setText(formatCount(video.getCommentCount()));

            // Bookmark state
            if (video.isBookmarked()) {
                ivBookmark.setImageResource(R.drawable.ic_bookmark); // Gold #FFD700
                tvBookmarkLabel.setTextColor(0xFFFFD700);
            } else {
                ivBookmark.setImageResource(R.drawable.ic_bookmark_border);
                tvBookmarkLabel.setTextColor(0xFFFFFFFF);
            }

            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(video, getBindingAdapterPosition());
                }
            });

            btnComment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(video, getBindingAdapterPosition());
                }
            });

            btnBookmark.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookmarkClick(video, getBindingAdapterPosition());
                }
            });

            btnShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(video);
                }
            });

            btnMoreOptions.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMoreOptionsClick(video, getBindingAdapterPosition(), this);
                }
            });

            layoutAuthor.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAuthorClick(video);
                }
            });

            playerView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVideoTap(this);
                }
            });

            seekBarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isUserTrackingSeekBar = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isUserTrackingSeekBar = false;
                    if (listener != null) {
                        listener.onSeek(seekBar.getProgress());
                    }
                }
            });
        }

        public void setOrientationUI(boolean isLandscape) {
            if (vGradientShadow != null) {
                vGradientShadow.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
            }

            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            if (layoutScrubber.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams sp = (android.widget.FrameLayout.LayoutParams) layoutScrubber.getLayoutParams();
                sp.bottomMargin = (int) (isLandscape ? (8 * density) : (60 * density));
                layoutScrubber.setLayoutParams(sp);
            }
            if (layoutBottomInfo.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams ip = (android.widget.FrameLayout.LayoutParams) layoutBottomInfo.getLayoutParams();
                ip.bottomMargin = (int) (isLandscape ? (28 * density) : (80 * density));
                layoutBottomInfo.setLayoutParams(ip);
            }
        }

        public void updateProgress(long currentMs, long durationMs) {
            if (isUserTrackingSeekBar || durationMs <= 0) {
                return;
            }
            int progress = (int) ((currentMs * 1000) / durationMs);
            seekBarVideo.setProgress(progress);
            tvCurrentTime.setText(formatTime(currentMs));
            tvTotalTime.setText(formatTime(durationMs));
        }

        private String formatTime(long ms) {
            long totalSeconds = ms / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }

        public void updateLikeIcon(boolean isLiked) {
            ivLike.setImageResource(isLiked ? R.drawable.ic_favorite : R.drawable.ic_favorite_border_white);
        }

        public void showThumbnail(VideoDto video) {
            String thumbnail = video.getThumbnailUrl();
            if (thumbnail == null || thumbnail.trim().isEmpty()) {
                Glide.with(itemView.getContext()).clear(ivVideoThumbnail);
                ivVideoThumbnail.setVisibility(View.GONE);
                return;
            }
            ivVideoThumbnail.setVisibility(View.VISIBLE);
            Glide.with(itemView.getContext())
                    .load(thumbnail)
                    .centerCrop()
                    .into(ivVideoThumbnail);
        }

        public void hideThumbnail() {
            Glide.with(itemView.getContext()).clear(ivVideoThumbnail);
            ivVideoThumbnail.setVisibility(View.GONE);
        }

        public String formatCount(int count) {
            if (count >= 1000000) {
                return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
            } else if (count >= 1000) {
                return String.format(Locale.getDefault(), "%.1fK", count / 1000.0);
            } else {
                return String.valueOf(count);
            }
        }
    }
}
