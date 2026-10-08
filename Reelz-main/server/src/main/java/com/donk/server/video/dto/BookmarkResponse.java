package com.donk.server.video.dto;

public record BookmarkResponse(
        Long videoId,
        boolean bookmarked) {
}
