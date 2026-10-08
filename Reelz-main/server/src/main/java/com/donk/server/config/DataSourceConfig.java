package com.donk.server.config;

import java.net.URI;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties, Environment env) {
        String url = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        // Tu dong loai bo ky tu xuong dong (\r, \n) va khoang trang do copy/paste tren giao dien web
        if (url != null) {
            url = url.replaceAll("[\\r\\n\\s]+", "");
            if (!url.contains("allowPublicKeyRetrieval=true")) {
                url += (url.contains("?") ? "&" : "?") + "allowPublicKeyRetrieval=true";
            }
        }
        if (username != null) {
            username = username.replaceAll("[\\r\\n\\s]+", "");
        }
        if (password != null) {
            password = password.replaceAll("[\\r\\n\\s]+", "");
        }

        // Ho tro doc truc tiep tu MYSQL_URL hoac MYSQLURL cua Railway/Render
        String mysqlUrl = env.getProperty("MYSQL_URL");
        if (mysqlUrl == null || mysqlUrl.isBlank()) {
            mysqlUrl = env.getProperty("MYSQLURL");
        }

        boolean fromRailwayUrl = false;
        if (mysqlUrl != null && !mysqlUrl.isBlank()) {
            mysqlUrl = mysqlUrl.trim();
            if (mysqlUrl.startsWith("mysql://")) {
                try {
                    URI uri = new URI(mysqlUrl.replace("mysql://", "http://"));
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        if (parts.length > 1) {
                            password = parts[1];
                        }
                    }
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                    String path = uri.getPath(); // /railway
                    url = "jdbc:mysql://" + host + ":" + port + path
                            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8";
                    fromRailwayUrl = true;
                } catch (Exception ignored) {
                }
            }
        }

        System.out.println("=== REELZ DATASOURCE INIT ===");
        System.out.println("Source: " + (fromRailwayUrl ? "Railway MYSQL_URL" : "Custom/Default Properties"));
        System.out.println("Connecting to JDBC URL: " + url);
        System.out.println("Database Username: " + username);
        System.out.println("Password set: " + (password != null && !password.isEmpty()));

        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(url)
                .username(username)
                .password(password)
                .build();
    }
}
