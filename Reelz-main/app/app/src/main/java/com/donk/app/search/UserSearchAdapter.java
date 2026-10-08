package com.donk.app.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.donk.app.R;
import com.donk.app.models.UserProfileDto;
import com.donk.app.network.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserVH> {

    public interface OnUserClickListener {
        void onUserClick(UserProfileDto user);
    }

    private final List<UserProfileDto> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public UserSearchAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserProfileDto> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_user, parent, false);
        return new UserVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserVH holder, int position) {
        UserProfileDto user = users.get(position);

        String username = user.getUsername() != null ? user.getUsername() : "";
        char initial = username.isEmpty() ? 'U' : Character.toUpperCase(username.charAt(0));
        holder.tvAvatarChar.setText(String.valueOf(initial));

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            String fullAvatarUrl = ApiClient.getVideoUrl(user.getAvatarUrl());
            Glide.with(holder.itemView.getContext())
                    .load(fullAvatarUrl)
                    .circleCrop()
                    .into(holder.ivAvatar);
            holder.tvAvatarChar.setVisibility(View.GONE);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bg_button_primary);
            holder.tvAvatarChar.setVisibility(View.VISIBLE);
        }

        holder.tvDisplayName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName() : username);
        holder.tvUsername.setText("@" + username);

        if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
            holder.tvBio.setText(user.getBio());
            holder.tvBio.setVisibility(View.VISIBLE);
        } else {
            holder.tvBio.setVisibility(View.GONE);
        }

        holder.tvFollowerCount.setText(String.format(Locale.getDefault(), "%d followers", user.getFollowerCount()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class UserVH extends RecyclerView.ViewHolder {
        public final ImageView ivAvatar;
        public final TextView tvAvatarChar;
        public final TextView tvDisplayName;
        public final TextView tvUsername;
        public final TextView tvBio;
        public final TextView tvFollowerCount;

        public UserVH(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivSearchAvatar);
            tvAvatarChar = itemView.findViewById(R.id.tvSearchAvatarChar);
            tvDisplayName = itemView.findViewById(R.id.tvSearchDisplayName);
            tvUsername = itemView.findViewById(R.id.tvSearchUsername);
            tvBio = itemView.findViewById(R.id.tvSearchBio);
            tvFollowerCount = itemView.findViewById(R.id.tvSearchFollowerCount);
        }
    }
}
