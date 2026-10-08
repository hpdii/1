package com.donk.app.feed;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.donk.app.R;
import com.donk.app.models.CommentDto;
import com.donk.app.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentVH> {

    public interface OnCommentActionListener {
        void onDeleteComment(CommentDto comment, int position);

        void onReplyComment(CommentDto comment);
    }

    private final List<CommentDto> comments = new ArrayList<>();
    private final List<CommentDto> displayComments = new ArrayList<>();
    private final Long currentUserId;
    private final Long videoOwnerId;
    private final OnCommentActionListener listener;

    public CommentAdapter(Long currentUserId, OnCommentActionListener listener) {
        this(currentUserId, null, listener);
    }

    public CommentAdapter(Long currentUserId, Long videoOwnerId, OnCommentActionListener listener) {
        this.currentUserId = currentUserId;
        this.videoOwnerId = videoOwnerId;
        this.listener = listener;
    }

    public void setComments(List<CommentDto> newComments) {
        comments.clear();
        if (newComments != null) {
            comments.addAll(newComments);
        }
        rebuildDisplayComments();
        notifyDataSetChanged();
    }

    public void addComment(CommentDto comment) {
        if (comment != null) {
            comments.add(comment);
            rebuildDisplayComments();
            notifyDataSetChanged();
        }
    }

    public void removeAt(int position) {
        if (position >= 0 && position < displayComments.size()) {
            CommentDto removed = displayComments.get(position);
            comments.remove(removed);
            rebuildDisplayComments();
            notifyDataSetChanged();
        }
    }

    private void rebuildDisplayComments() {
        displayComments.clear();
        List<CommentDto> roots = new ArrayList<>();
        List<CommentDto> replies = new ArrayList<>();
        for (CommentDto comment : comments) {
            if (comment.getReplyToId() == null) roots.add(comment);
            else replies.add(comment);
        }

        for (CommentDto root : roots) {
            displayComments.add(root);
            appendReplies(root, replies, new ArrayList<>());
        }

        // Keep replies visible when their original comment is missing from the response.
        for (CommentDto reply : replies) {
            if (!containsComment(displayComments, reply)) displayComments.add(reply);
        }
    }

    private void appendReplies(CommentDto parent, List<CommentDto> replies, List<Long> visitedIds) {
        if (parent.getId() == null || visitedIds.contains(parent.getId())) return;
        visitedIds.add(parent.getId());
        for (CommentDto reply : replies) {
            if (parent.getId().equals(reply.getReplyToId()) && !containsComment(displayComments, reply)) {
                displayComments.add(reply);
                appendReplies(reply, replies, visitedIds);
            }
        }
    }

    private boolean containsComment(List<CommentDto> list, CommentDto target) {
        for (CommentDto item : list) {
            if (item.getId() != null && item.getId().equals(target.getId())) return true;
        }
        return false;
    }

    private int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
    }

    @NonNull
    @Override
    public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentVH holder, int position) {
        CommentDto comment = displayComments.get(position);
        boolean isReply = comment.getReplyToId() != null;
        ViewGroup.MarginLayoutParams itemParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        itemParams.leftMargin = isReply ? dp(holder.itemView, 44) : 0;
        holder.itemView.setLayoutParams(itemParams);
        String replyTarget = comment.getReplyToUsername();
        if (isReply && replyTarget != null && !replyTarget.isEmpty()) {
            holder.tvReplyContext.setVisibility(View.VISIBLE);
            holder.tvReplyContext.setText("Đang trả lời @" + replyTarget);
        } else {
            holder.tvReplyContext.setVisibility(View.GONE);
        }

        String author = comment.getDisplayName() != null && !comment.getDisplayName().isEmpty()
                ? comment.getDisplayName() : comment.getUsername();
        holder.tvAuthor.setText(author != null ? author : "user");

        // Noi dung: tag @username mau xanh neu la tra loi
        String content = comment.getContent() != null ? comment.getContent() : "";
        if (content.startsWith("@") && content.contains(" ")) {
            int space = content.indexOf(' ');
            String tag = content.substring(0, space);
            SpannableString spannable = new SpannableString(content);
            spannable.setSpan(new ForegroundColorSpan(0xFF000000), 0, tag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.tvContent.setText(spannable);
        } else {
            holder.tvContent.setText(content);
        }

        String date = comment.getCreatedAt();
        if (date != null && date.length() >= 10) {
            date = date.substring(0, 10);
        }
        holder.tvDate.setText(date != null ? date : "");

        String username = comment.getUsername() != null ? comment.getUsername() : "";
        char initial = username.isEmpty() ? 'U' : Character.toUpperCase(username.charAt(0));
        holder.tvAvatarChar.setText(String.valueOf(initial));

        if (comment.getAvatarUrl() != null && !comment.getAvatarUrl().isEmpty()) {
            String fullAvatarUrl = ApiClient.getVideoUrl(comment.getAvatarUrl());
            Glide.with(holder.itemView.getContext())
                    .load(fullAvatarUrl)
                    .circleCrop()
                    .into(holder.ivAvatar);
            holder.tvAvatarChar.setVisibility(View.GONE);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bg_button_primary);
            holder.tvAvatarChar.setVisibility(View.VISIBLE);
        }

        // Nhan giu len binh luan de tra loi
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onReplyComment(comment);
                return true;
            }
            return false;
        });
        holder.btnReply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReplyComment(comment);
            }
        });

        boolean canDelete = currentUserId != null
                && (currentUserId.equals(comment.getUserId())
                        || (videoOwnerId != null && currentUserId.equals(videoOwnerId)));
        if (canDelete) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteComment(comment, holder.getBindingAdapterPosition());
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return displayComments.size();
    }

    public static class CommentVH extends RecyclerView.ViewHolder {
        public final ImageView ivAvatar;
        public final TextView tvAvatarChar;
        public final TextView tvAuthor;
        public final TextView tvDate;
        public final TextView tvContent;
        public final TextView tvReplyContext;
        public final TextView btnReply;
        public final ImageView btnDelete;

        public CommentVH(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivCommentAvatar);
            tvAvatarChar = itemView.findViewById(R.id.tvCommentAvatarChar);
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvDate = itemView.findViewById(R.id.tvCommentDate);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvReplyContext = itemView.findViewById(R.id.tvCommentReplyContext);
            btnReply = itemView.findViewById(R.id.btnReplyComment);
            btnDelete = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}
