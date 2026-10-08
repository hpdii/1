package com.donk.app.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.donk.app.R;
import com.donk.app.models.VideoDto;

import java.util.ArrayList;
import java.util.List;

public class ProfileVideoAdapter extends RecyclerView.Adapter<ProfileVideoAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(VideoDto video, int position);
    }

    private final List<VideoDto> videos = new ArrayList<>();
    private final OnDeleteClickListener deleteClickListener;

    public ProfileVideoAdapter(OnDeleteClickListener deleteClickListener) {
        this.deleteClickListener = deleteClickListener;
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoDto video = videos.get(position);
        holder.tvTitle.setText(video.getTitle());

        if (video.getDescription() != null && !video.getDescription().isEmpty()) {
            holder.tvDesc.setText(video.getDescription());
            holder.tvDesc.setVisibility(View.VISIBLE);
        } else {
            holder.tvDesc.setVisibility(View.GONE);
        }

        holder.tvLikes.setText(String.valueOf(video.getLikeCount()));

        String date = video.getCreatedAt();
        if (date != null && date.length() >= 10) {
            date = date.substring(0, 10);
        }
        holder.tvDate.setText(date != null ? date : "");

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(video, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvTitle;
        public final TextView tvDesc;
        public final TextView tvLikes;
        public final TextView tvDate;
        public final ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvProfileVideoTitle);
            tvDesc = itemView.findViewById(R.id.tvProfileVideoDesc);
            tvLikes = itemView.findViewById(R.id.tvProfileVideoLikes);
            tvDate = itemView.findViewById(R.id.tvProfileVideoDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteVideo);
        }
    }
}
