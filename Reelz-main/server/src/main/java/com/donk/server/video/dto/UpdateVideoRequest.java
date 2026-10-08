package com.donk.server.video.dto;

import jakarta.validation.constraints.Size;

public record UpdateVideoRequest(
        @Size(max = 150, message = "Tieu de toi da 150 ky tu") String title,
        @Size(max = 500, message = "Mo ta toi da 500 ky tu") String description,
        Boolean isPrivate) {
}
