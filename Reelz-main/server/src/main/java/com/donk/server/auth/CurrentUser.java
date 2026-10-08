package com.donk.server.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Lay userId cua nguoi dang goi tu SecurityContext.
 *
 * JwtAuthFilter dat userId vao principal, nen o day chi viec doc ra. Controller
 * khong phai dung tới SecurityContextHolder truc tiep.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            return null;
        }

        return userId;
    }
}
