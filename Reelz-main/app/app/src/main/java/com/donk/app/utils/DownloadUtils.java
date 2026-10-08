package com.donk.app.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

public final class DownloadUtils {

    private DownloadUtils() {
    }

    public static void downloadVideo(Context context, String videoUrl, String title) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(context, "Đường dẫn video không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(context, "Không thể khởi tạo DownloadManager", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = Uri.parse(videoUrl);
            String fileName = "Reelz_" + System.currentTimeMillis() + ".mp4";

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(title != null && !title.isEmpty() ? title : "Video Reelz");
            request.setDescription("Đang lưu video về thiết bị...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            downloadManager.enqueue(request);
            Toast.makeText(context, "Đang lưu video về máy! Kiểm tra thanh thông báo hoặc thư mục Tải về (Downloads).", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi khi lưu video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
