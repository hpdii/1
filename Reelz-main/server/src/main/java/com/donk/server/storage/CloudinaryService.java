package com.donk.server.storage;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean isConfigured;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {

        if (cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank()) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName.trim(),
                    "api_key", apiKey.trim(),
                    "api_secret", apiSecret.trim()));
            this.isConfigured = true;
        } else {
            this.cloudinary = null;
            this.isConfigured = false;
        }
    }

    public boolean isConfigured() {
        return isConfigured;
    }

    public CloudinaryVideoUpload uploadVideo(File file) throws IOException {
        if (!isConfigured || cloudinary == null) {
            return null;
        }

        // PHAI dung HashMap. Map.of() tra ve map BAT BIEN va SDK Cloudinary tu
        // put() them chunk_size vao chinh map nay -> UnsupportedOperationException.
        // Loi do bi nuot im lam video roi ve luu o dia tam cua Railway va mat
        // sau moi lan deploy (nguyen nhan "khong tai duoc video").
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("resource_type", "video");

        Map<?, ?> uploadResult = cloudinary.uploader().uploadLarge(file, params);
        String videoUrl = (String) uploadResult.get("secure_url");
        String thumbnailUrl = null;
        if (videoUrl != null) {
            thumbnailUrl = videoUrl.replace("/upload/", "/upload/so_1,w_720,h_1280,c_fill,f_jpg/");
        }
        return new CloudinaryVideoUpload(videoUrl, thumbnailUrl);
    }

    public record CloudinaryVideoUpload(String videoUrl, String thumbnailUrl) {
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (!isConfigured || cloudinary == null) {
            return null;
        }
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), Map.of(
                "resource_type", "image"
        ));
        return (String) uploadResult.get("secure_url");
    }
}
