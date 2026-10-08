package com.donk.server.video.dto;

public record LikeResponse(Long videoId, Integer likeCount, boolean likedByMe) {
}
