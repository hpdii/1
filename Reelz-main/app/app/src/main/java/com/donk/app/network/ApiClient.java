package com.donk.app.network;

import android.content.Context;

import com.donk.app.constants.AppConstants;
import com.donk.app.models.ApiError;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static ApiService apiService;
    private static final Gson gson = new Gson();

    public static synchronized ApiService getService(Context context) {
        if (apiService == null) {
            SessionManager sessionManager = SessionManager.getInstance(context);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(sessionManager))
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(AppConstants.BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    public static String getVideoUrl(String relativePath) {
        if (relativePath == null) {
            return "";
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return AppConstants.BASE_URL + relativePath;
    }

    public static String parseError(Response<?> response) {
        if (response == null || response.errorBody() == null) {
            return "Lỗi không xác định";
        }
        try (ResponseBody body = response.errorBody()) {
            ApiError apiError = gson.fromJson(body.string(), ApiError.class);
            if (apiError != null && apiError.getMessage() != null && !apiError.getMessage().trim().isEmpty()) {
                return apiError.getMessage();
            }
        } catch (Exception ignored) {
        }
        return "Lỗi phản hồi từ máy chủ (mã " + response.code() + ")";
    }
}
