package com.donk.server.auth;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donk.server.auth.dto.AuthResponse;
import com.donk.server.auth.dto.LoginRequest;
import com.donk.server.auth.dto.RegisterRequest;
import com.donk.server.auth.dto.UserDto;
import com.donk.server.common.ApiException;
import com.donk.server.user.User;
import com.donk.server.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw ApiException.conflict("Ten dang nhap da co nguoi dung");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("Email da duoc dang ky");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(resolveDisplayName(request.displayName(), request.username()));
        user.setCreatedAt(LocalDateTime.now());

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Hai nguoi dang ky cung luc: rang buoc UNIQUE o CSDL moi la thu
            // chan that su, khong phai hai lan kiem tra exists ben tren.
            throw ApiException.conflict("Ten dang nhap hoac email da co nguoi dung");
        }

        return new AuthResponse(jwtService.generate(user.getId()), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String key = request.usernameOrEmail();

        User user = userRepository.findByUsernameOrEmail(key, key)
                // Cung mot thong bao cho "khong co user" va "sai mat khau" —
                // khong de lo tai khoan nao co that.
                .orElseThrow(() -> ApiException.unauthorized("Sai ten dang nhap hoac mat khau"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Sai ten dang nhap hoac mat khau");
        }

        return new AuthResponse(jwtService.generate(user.getId()), UserDto.from(user));
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Tai khoan khong ton tai"));

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.unauthorized("Mat khau hien tai khong dung");
        }

        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 72) {
            throw ApiException.badRequest("Mat khau moi tu 6 den 72 ky tu");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Mat khau moi phai khac mat khau hien tai");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String resolveDisplayName(String displayName, String username) {
        if (displayName == null || displayName.isBlank()) {
            return username;
        }

        return displayName.trim();
    }
}

