package com.donk.server.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Binh luan khong duoc de trong")
        @Size(max = 1000, message = "Binh luan toi da 1000 ky tu")
        String content,

        Long replyToId) {
}
