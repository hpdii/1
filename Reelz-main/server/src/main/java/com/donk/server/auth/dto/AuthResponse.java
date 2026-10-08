package com.donk.server.auth.dto;

public record AuthResponse(String token, UserDto user) {
}
