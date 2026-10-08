package com.donk.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Loai tru UserDetailsServiceAutoConfiguration: neu khong, Spring Boot tao mot
 * tai khoan "user" mac dinh va in mat khau ngau nhien ra log, rat de gay hieu
 * nham la phai dung tai khoan do moi goi duoc API. Chung ta xac thuc bang JWT.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DonkServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DonkServerApplication.class, args);
    }
}
