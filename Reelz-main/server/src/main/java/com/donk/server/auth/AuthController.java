package com.donk.server.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donk.server.auth.dto.AuthResponse;
import com.donk.server.auth.dto.ChangePasswordRequest;
import com.donk.server.auth.dto.LoginRequest;
import com.donk.server.auth.dto.RegisterRequest;
import com.donk.server.auth.dto.UserDto;
import com.donk.server.common.ApiException;
import com.donk.server.user.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /** Dang ky xong tra token luon, do client phai goi dang nhap lai. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<java.util.Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = CurrentUser.id();
        if (userId == null) {
            throw ApiException.unauthorized("Chua dang nhap");
        }
        authService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(java.util.Map.of("message", "Doi mat khau thanh cong"));
    }

    /** Dung de kiem tra token con hieu luc khong, va de test bang curl. */
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        Long userId = CurrentUser.id();

        if (userId == null) {
            throw ApiException.unauthorized("Chua dang nhap");
        }

        return userRepository.findById(userId)
                .map(UserDto::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> ApiException.unauthorized("Tai khoan khong ton tai"));
    }
}

