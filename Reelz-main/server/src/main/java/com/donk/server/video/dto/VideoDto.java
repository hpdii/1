package com.donk.server.video.dto;

public record VideoDto(
        Long id,
        String title,
        String description,
        String videoPath,
        String thumbnailUrl,
        Long authorId,
        String authorUsername,
        String authorDisplayName,
        String authorAvatarUrl,
        Integer likeCount,
        Integer commentCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        boolean isPrivate,
        String createdAt) {
}
