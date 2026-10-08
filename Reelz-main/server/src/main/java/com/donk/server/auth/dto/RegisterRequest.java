package com.donk.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "khong duoc de trong")
        @Size(min = 3, max = 50, message = "phai tu 3 den 50 ky tu")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$",
                message = "chi duoc dung chu, so va dau gach duoi")
        String username,

        @NotBlank(message = "khong duoc de trong")
        @Email(message = "khong dung dinh dang email")
        @Size(max = 120, message = "toi da 120 ky tu")
        String email,

        @NotBlank(message = "khong duoc de trong")
        @Size(min = 6, max = 100, message = "phai tu 6 ky tu tro len")
        String password,

        @Size(max = 80, message = "toi da 80 ky tu")
        String displayName) {
}
