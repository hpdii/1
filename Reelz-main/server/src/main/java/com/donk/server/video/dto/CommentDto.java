package com.donk.server.video.dto;

public record CommentDto(
        Long id,
        Long videoId,
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        String content,
        String replyToUsername,
        Long replyToId,
        String createdAt) {
}
