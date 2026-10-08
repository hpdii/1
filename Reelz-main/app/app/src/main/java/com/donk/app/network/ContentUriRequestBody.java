package com.donk.app.network;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Stream file truc tiep tu ContentResolver qua OkHttp RequestBody,
 * khong copy file ra cache o dia de tiet kiem bo nho va thoi gian.
 */
public class ContentUriRequestBody extends RequestBody {

    private final ContentResolver contentResolver;
    private final Uri uri;
    private final String contentType;
    private final long contentLength;

    public ContentUriRequestBody(ContentResolver contentResolver, Uri uri, String contentType, long contentLength) {
        this.contentResolver = contentResolver;
        this.uri = uri;
        this.contentType = contentType != null ? contentType : "video/mp4";
        this.contentLength = contentLength;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        InputStream inputStream = contentResolver.openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Khong the mo file tu URI: " + uri);
        }
        try (Source source = Okio.source(inputStream)) {
            sink.writeAll(source);
        }
    }
}
