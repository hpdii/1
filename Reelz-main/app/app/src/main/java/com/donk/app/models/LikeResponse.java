package com.donk.app.models;

public class LikeResponse {
    private Long videoId;
    private int likeCount;
    private boolean likedByMe;

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public boolean isLiked() {
        return likedByMe;
    }

    public void setLiked(boolean liked) {
        this.likedByMe = liked;
    }
}
