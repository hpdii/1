package com.donk.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "khong duoc de trong")
        String usernameOrEmail,

        @NotBlank(message = "khong duoc de trong")
        String password) {
}
