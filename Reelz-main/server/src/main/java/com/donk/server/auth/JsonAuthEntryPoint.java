package com.donk.server.auth;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.donk.server.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tra 401 dang JSON khi thieu hoac sai token.
 *
 * Mac dinh cua Spring Security tra body rong hoac trang HTML — Gson ben client
 * se vo ngay khi parse. Day la cai bay so mot cua Spring Security.
 */
@Component
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError body = new ApiError(
                LocalDateTime.now().toString(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Thieu token hoac token khong hop le",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
