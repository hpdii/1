package com.donk.app.feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donk.app.R;
import com.donk.app.models.CommentDto;
import com.donk.app.models.CreateCommentRequest;
import com.donk.app.network.ApiClient;
import com.donk.app.network.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentsBottomSheet extends BottomSheetDialogFragment {

    public interface OnCommentCountChangedListener {
        void onCommentCountChanged(int newCount);
    }

    private static final String ARG_VIDEO_ID = "video_id";
    private static final String ARG_COMMENT_COUNT = "comment_count";

    private Long videoId;
    private Long videoOwnerId;
    private int commentCount;
    private OnCommentCountChangedListener countChangedListener;

    private TextView txtCommentTitle;
    private ImageView btnCommentClose;
    private RecyclerView commentRecyclerView;
    private View commentsListContainer;
    private ProgressBar progressBarComments;
    private TextView txtEmptyComment;
    private TextView txtCommentError;
    private LinearLayout layoutReplyHint;
    private TextView txtReplyHint;
    private ImageView btnCancelReply;
    private EditText edtComment;
    private View btnSendComment;

    private CommentAdapter adapter;

    // Nguoi dang duoc tra loi (null = binh luan moi cap 1)
    private CommentDto replyTarget = null;

    private static final String ARG_VIDEO_OWNER_ID = "video_owner_id";

    public static CommentsBottomSheet newInstance(Long videoId, int commentCount) {
        return newInstance(videoId, commentCount, null);
    }

    public static CommentsBottomSheet newInstance(Long videoId, int commentCount, Long videoOwnerId) {
        CommentsBottomSheet sheet = new CommentsBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_VIDEO_ID, videoId);
        args.putInt(ARG_COMMENT_COUNT, commentCount);
        if (videoOwnerId != null) {
            args.putLong(ARG_VIDEO_OWNER_ID, videoOwnerId);
        }
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnCommentCountChangedListener(OnCommentCountChangedListener listener) {
        this.countChangedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoId = getArguments().getLong(ARG_VIDEO_ID);
            commentCount = getArguments().getInt(ARG_COMMENT_COUNT);
            if (getArguments().containsKey(ARG_VIDEO_OWNER_ID)) {
                videoOwnerId = getArguments().getLong(ARG_VIDEO_OWNER_ID);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_comments, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!(getDialog() instanceof BottomSheetDialog)) return;

        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;

        View content = getView();
        if (content == null) return;

        int halfHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.5f);
        ViewGroup.LayoutParams sheetParams = bottomSheet.getLayoutParams();
        sheetParams.height = halfHeight;
        bottomSheet.setLayoutParams(sheetParams);
        ViewGroup.LayoutParams contentParams = content.getLayoutParams();
        contentParams.height = halfHeight;
        content.setLayoutParams(contentParams);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setFitToContents(true);
        behavior.setPeekHeight(halfHeight, true);
        bottomSheet.post(() -> {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            int headerHeight = viewHeight(R.id.txtCommentTitle) + viewHeight(R.id.commentInputBar) + dp(80);
            int listHeight = Math.max(dp(100), halfHeight - headerHeight);
            ViewGroup.LayoutParams listParams = commentsListContainer.getLayoutParams();
            listParams.height = listHeight;
            commentsListContainer.setLayoutParams(listParams);
        });
    }

    private int viewHeight(int viewId) {
        View view = getView() != null ? getView().findViewById(viewId) : null;
        return view != null ? view.getMeasuredHeight() : 0;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtCommentTitle = view.findViewById(R.id.txtCommentTitle);
        btnCommentClose = view.findViewById(R.id.btnCommentClose);
        commentRecyclerView = view.findViewById(R.id.commentRecyclerView);
        commentsListContainer = view.findViewById(R.id.commentsListContainer);
        progressBarComments = view.findViewById(R.id.progressBarComments);
        txtEmptyComment = view.findViewById(R.id.txtEmptyComment);
        txtCommentError = view.findViewById(R.id.txtCommentError);
        layoutReplyHint = view.findViewById(R.id.layoutReplyHint);
        txtReplyHint = view.findViewById(R.id.txtReplyHint);
        btnCancelReply = view.findViewById(R.id.btnCancelReply);
        edtComment = view.findViewById(R.id.edtComment);
        btnSendComment = view.findViewById(R.id.btnSendComment);

        updateTitle(commentCount);
        btnCommentClose.setOnClickListener(v -> dismiss());
        btnCancelReply.setOnClickListener(v -> cancelReply());

        SessionManager session = SessionManager.getInstance(requireContext());
        adapter = new CommentAdapter(session.getUserId(), videoOwnerId, new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onDeleteComment(CommentDto comment, int position) {
                deleteComment(comment.getId(), position);
            }

            @Override
            public void onReplyComment(CommentDto comment) {
                startReply(comment);
            }
        });

        commentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        commentRecyclerView.setHasFixedSize(false);
        commentRecyclerView.setNestedScrollingEnabled(true);
        commentRecyclerView.setVisibility(View.VISIBLE);
        commentRecyclerView.setAdapter(adapter);

        btnSendComment.setOnClickListener(v -> postComment());

        loadComments();
    }

    private void startReply(CommentDto comment) {
        replyTarget = comment;
        layoutReplyHint.setVisibility(View.VISIBLE);
        String name = comment.getDisplayName() != null && !comment.getDisplayName().isEmpty()
                ? comment.getDisplayName() : comment.getUsername();
        txtReplyHint.setText("Đang trả lời " + name);
        edtComment.requestFocus();
        edtComment.setHint("Trả lời " + name + "...");
    }

    private void cancelReply() {
        replyTarget = null;
        layoutReplyHint.setVisibility(View.GONE);
        edtComment.setHint("Thêm bình luận...");
    }

    private void updateTitle(int count) {
        this.commentCount = count;
        if (txtCommentTitle != null) {
            txtCommentTitle.setText("Bình luận (" + count + ")");
        }
        if (countChangedListener != null) {
            countChangedListener.onCommentCountChanged(count);
        }
    }

    private void loadComments() {
        progressBarComments.setVisibility(View.VISIBLE);
        txtEmptyComment.setVisibility(View.GONE);
        txtCommentError.setVisibility(View.GONE);

        ApiClient.getService(requireContext()).getComments(videoId).enqueue(new Callback<List<CommentDto>>() {
            @Override
            public void onResponse(Call<List<CommentDto>> call, Response<List<CommentDto>> response) {
                if (!isAdded() || getView() == null) return;
                progressBarComments.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<CommentDto> list = response.body();
                    adapter.setComments(list);
                    updateTitle(list.size());
                    txtEmptyComment.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    txtCommentError.setVisibility(View.GONE);
                } else {
                    showCommentError("Không tải được bình luận: " + ApiClient.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<CommentDto>> call, Throwable t) {
                if (!isAdded() || getView() == null) return;
                progressBarComments.setVisibility(View.GONE);
                showCommentError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void showCommentError(String message) {
        txtEmptyComment.setVisibility(View.GONE);
        txtCommentError.setText(message);
        txtCommentError.setVisibility(View.VISIBLE);
    }

    private void postComment() {
        String content = edtComment.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        Long replyToId = replyTarget != null ? replyTarget.getId() : null;
        if (replyTarget != null && replyTarget.getReplyToId() != null) {
            replyToId = replyTarget.getReplyToId();
        }

        edtComment.setText("");
        btnSendComment.setEnabled(false);

        ApiClient.getService(requireContext()).addComment(videoId, new CreateCommentRequest(content, replyToId))
                .enqueue(new Callback<CommentDto>() {
                    @Override
                    public void onResponse(Call<CommentDto> call, Response<CommentDto> response) {
                        btnSendComment.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.addComment(response.body());
                            updateTitle(adapter.getItemCount());
                            txtEmptyComment.setVisibility(View.GONE);
                            commentRecyclerView.post(() -> commentRecyclerView.smoothScrollToPosition(adapter.getItemCount() - 1));
                            cancelReply();
                        } else {
                            Toast.makeText(requireContext(), "Lỗi gửi bình luận: " + ApiClient.parseError(response), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentDto> call, Throwable t) {
                        btnSendComment.setEnabled(true);
                        Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteComment(Long commentId, int position) {
        if (position < 0 || position >= adapter.getItemCount()) return;
        ApiClient.getService(requireContext()).deleteComment(videoId, commentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    adapter.removeAt(position);
                    updateTitle(adapter.getItemCount());
                    txtEmptyComment.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }
}
