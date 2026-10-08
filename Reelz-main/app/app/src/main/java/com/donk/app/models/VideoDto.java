package com.donk.app.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class VideoDto implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("videoPath")
    private String videoPath;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName(value = "authorId", alternate = {"userId"})
    private Long authorId;

    @SerializedName(value = "authorUsername", alternate = {"username"})
    private String authorUsername;

    @SerializedName(value = "authorDisplayName", alternate = {"displayName"})
    private String authorDisplayName;

    @SerializedName("authorAvatarUrl")
    private String authorAvatarUrl;

    @SerializedName("likeCount")
    private int likeCount;

    @SerializedName("commentCount")
    private int commentCount;

    @SerializedName(value = "likedByMe", alternate = {"liked"})
    private boolean likedByMe;

    @SerializedName(value = "bookmarkedByMe", alternate = {"bookmarked"})
    private boolean bookmarkedByMe;

    @SerializedName("isPrivate")
    private boolean isPrivate;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Long getUserId() {
        return authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getUsername() {
        return authorUsername;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }

    public String getDisplayName() {
        return authorDisplayName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }

    public void setLikedByMe(boolean likedByMe) {
        this.likedByMe = likedByMe;
    }

    public boolean isLiked() {
        return likedByMe;
    }

    public void setLiked(boolean liked) {
        this.likedByMe = liked;
    }

    public boolean isBookmarked() {
        return bookmarkedByMe;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarkedByMe = bookmarked;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
