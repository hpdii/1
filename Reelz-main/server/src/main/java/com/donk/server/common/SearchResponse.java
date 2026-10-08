package com.donk.server.common;

import java.util.List;

import com.donk.server.user.dto.UserProfileDto;
import com.donk.server.video.dto.VideoDto;

public record SearchResponse(
        List<UserProfileDto> users,
        List<VideoDto> videos) {
}
