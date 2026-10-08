package com.donk.server.user.dto;

public record UserProfileDto(
        Long id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        int videoCount,
        int followerCount,
        int followingCount,
        int totalLikes,
        boolean isFollowing) {
}
