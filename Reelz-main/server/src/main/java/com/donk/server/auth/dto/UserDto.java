package com.donk.server.auth.dto;

import com.donk.server.user.User;

/** Thong tin nguoi dung tra ra ngoai — KHONG bao gio chua passwordHash. */
public record UserDto(
        Long id,
        String username,
        String email,
        String displayName) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName());
    }
}
