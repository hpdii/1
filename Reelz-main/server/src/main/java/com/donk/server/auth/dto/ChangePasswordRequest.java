package com.donk.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Thieu mat khau hien tai") String currentPassword,
        @NotBlank(message = "Thieu mat khau moi") @Size(min = 6, max = 72, message = "Mat khau moi tu 6 den 72 ky tu") String newPassword) {
}
