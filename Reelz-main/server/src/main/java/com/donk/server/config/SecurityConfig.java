package com.donk.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.donk.server.auth.JsonAuthEntryPoint;
import com.donk.server.auth.JwtAuthFilter;
import com.donk.server.auth.JwtService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final JsonAuthEntryPoint jsonAuthEntryPoint;

    public SecurityConfig(JwtService jwtService, JsonAuthEntryPoint jsonAuthEntryPoint) {
        this.jwtService = jwtService;
        this.jsonAuthEntryPoint = jsonAuthEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API stateless dung token, khong dung cookie nen CSRF khong co y nghia.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)

                // Tat het co che dang nhap mac dinh cua Spring Security. Neu khong
                // tat, no se tu bat form dang nhap HTML va tra ve trang HTML thay
                // vi JSON.
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/ping").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/videos").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/videos/*/comments").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/search").permitAll()
                        // Video de cong khai: ExoPlayer kho gan header Authorization
                        // vao request media. Ten file la UUID nen khong doan duoc.
                        .requestMatchers("/media/**").permitAll()
                        // Thieu dong nay thi MOI loi deu bien thanh 401 va khong bao
                        // gio nhin thay thong bao loi that.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())

                .exceptionHandling(ex -> ex.authenticationEntryPoint(jsonAuthEntryPoint))

                .addFilterBefore(new JwtAuthFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
