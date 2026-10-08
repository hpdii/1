package com.donk.server.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 80, message = "Ten hien thi toi da 80 ky tu")
        String displayName,

        @Size(max = 200, message = "Tieu su toi da 200 ky tu")
        String bio) {
}
