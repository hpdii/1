package com.donk.server.video;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.donk.server.common.ApiException;

/**
 * Noi duy nhat trong he thong ghi/doc file video.
 *
 * Gom vao mot lop rieng la co y: khi deploy len cloud, chi doi phan ruot cua lop
 * nay sang S3/R2, khong phai sua controller hay CSDL.
 */
@Service
public class VideoStorageService {

    /** Chi cho phep cac dinh dang nay — khong lay phan mo rong tu ten file client gui. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "mov", "webm", "mkv", "m4v");

    private final Path uploadDir;

    public VideoStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Luu file va tra ve TEN FILE (khong phai duong dan) de ghi vao CSDL. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Chua chon file video");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        Path target = uploadDir.resolve(storedName).normalize();

        // Chan path traversal: du ten file da sinh bang UUID, van kiem tra lai cho chac.
        if (!target.startsWith(uploadDir)) {
            throw ApiException.badRequest("Ten file khong hop le");
        }

        try {
            Files.createDirectories(uploadDir);
            file.transferTo(target);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Khong luu duoc file: " + e.getMessage());
        }

        return storedName;
    }

    public Path load(String storedName) {
        return uploadDir.resolve(storedName).normalize();
    }

    /** Cat doan video bang ffmpeg va tra ve TEN FILE moi. */
    public String trim(Path source, double startSeconds, double endSeconds) {
        if (source == null || !java.nio.file.Files.exists(source)) {
            throw ApiException.badRequest("Khong tim thay file video goc");
        }

        String extension = resolveExtension(source.getFileName().toString());
        String trimmedName = UUID.randomUUID().toString().replace("-", "") + "-trim." + extension;
        Path target = uploadDir.resolve(trimmedName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw ApiException.badRequest("Ten file khong hop le");
        }

        java.util.List<String> command = java.util.List.of(
                "ffmpeg", "-y",
                "-ss", String.valueOf(startSeconds),
                "-i", source.toAbsolutePath().toString(),
                "-t", String.valueOf(endSeconds - startSeconds),
                "-c", "copy",
                target.toAbsolutePath().toString());

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw ApiException.badRequest("Khong cat duoc video: " + output);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Khong cat duoc video: " + e.getMessage());
        }

        try {
            if (!java.nio.file.Files.exists(target) || java.nio.file.Files.size(target) == 0) {
                throw ApiException.badRequest("Khong cat duoc video");
            }
        } catch (java.io.IOException e) {
            throw ApiException.badRequest("Khong cat duoc video");
        }

        return trimmedName;
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(uploadDir.resolve(storedName).normalize());
        } catch (IOException ignored) {
            // Xoa file loi khong nen lam hong thao tac xoa ban ghi.
        }
    }

    /**
     * Lay phan mo rong tu ten file goc, nhung chi chap nhan gia tri nam trong
     * danh sach cho phep. Khong bao gio dung ten file do client gui de dat ten.
     */
    private String resolveExtension(String originalName) {
        if (originalName == null) {
            return "mp4";
        }

        int dot = originalName.lastIndexOf('.');

        if (dot < 0 || dot == originalName.length() - 1) {
            return "mp4";
        }

        String extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);

        return ALLOWED_EXTENSIONS.contains(extension) ? extension : "mp4";
    }
}

