package com.donk.server.video.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record TrimVideoRequest(
        @DecimalMin(value = "0.0", message = "Diem bat dau khong hop le") Double startSeconds,
        @DecimalMin(value = "0.0", message = "Diem ket thuc khong hop le") Double endSeconds,
        @DecimalMin(value = "1.0", message = "Do dai toi thieu 1 giay") @DecimalMax(value = "600.0", message = "Do dai toi da 10 phut") Double maxDurationSeconds) {
}
