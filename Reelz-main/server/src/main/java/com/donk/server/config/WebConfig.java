package com.donk.server.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path uploadDir;

    public WebConfig(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void createUploadDir() throws IOException {
        Files.createDirectories(uploadDir);
    }

    /**
     * Phuc vu file video tai /media/** bang static resource handler cua Spring.
     *
     * Day la diem mau chot: ResourceHttpRequestHandler TU CAI DAT HTTP byte-range,
     * nen request co "Range: bytes=0-" nhan 206 Partial Content + Content-Range.
     * Nho do ExoPlayer moi tua duoc. Khong phai viet mot dong code streaming nao.
     *
     * Bay thuong gap: tra ResponseEntity<Resource> tu mot @GetMapping chi cho ra
     * 200 thuong va KHONG co Range — Spring chi ho tro Range cho static resource
     * handler va cho ResourceRegion. Do la ly do ban "tu viet controller stream
     * video" la ban hay hong nhat.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Dung toUri() chu KHONG noi chuoi: tu ghep duong dan Windows cho ra
        // "file:D:\Donk\..." voi dau gach nguoc, am tham khong resolve duoc.
        String location = uploadDir.toUri().toString();

        registry.addResourceHandler("/media/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
