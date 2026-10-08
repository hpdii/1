package com.donk.app.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.donk.app.R;
import com.donk.app.models.VideoDto;
import com.donk.app.network.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VideoGridAdapter extends RecyclerView.Adapter<VideoGridAdapter.GridVH> {

    public interface OnVideoGridClickListener {
        void onVideoClick(VideoDto video, int position);

        void onVideoLongClick(VideoDto video, int position);
    }

    private final List<VideoDto> videos = new ArrayList<>();
    private final OnVideoGridClickListener listener;

    public VideoGridAdapter(OnVideoGridClickListener listener) {
        this.listener = listener;
    }

    public void setVideos(List<VideoDto> newVideos) {
        videos.clear();
        if (newVideos != null) {
            videos.addAll(newVideos);
        }
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < videos.size()) {
            videos.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public GridVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_grid, parent, false);
        return new GridVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridVH holder, int position) {
        VideoDto video = videos.get(position);

        holder.tvLikes.setText(formatCount(video.getLikeCount()));
        holder.ivLock.setVisibility(video.isPrivate() ? View.VISIBLE : View.GONE);

        String videoUrl = ApiClient.getVideoUrl(video.getVideoPath());
        Glide.with(holder.itemView.getContext())
                .asBitmap()
                .load(videoUrl)
                .placeholder(R.drawable.bg_thumbnail_placeholder)
                .error(R.drawable.bg_thumbnail_placeholder)
                .centerCrop()
                .into(holder.ivThumbnail);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(video, holder.getBindingAdapterPosition());
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onVideoLongClick(video, holder.getBindingAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
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

    public static class GridVH extends RecyclerView.ViewHolder {
        public final ImageView ivThumbnail;
        public final TextView tvLikes;
        public final ImageView ivLock;

        public GridVH(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivGridThumbnail);
            tvLikes = itemView.findViewById(R.id.tvGridLikes);
            ivLock = itemView.findViewById(R.id.ivGridLock);
        }
    }
}
