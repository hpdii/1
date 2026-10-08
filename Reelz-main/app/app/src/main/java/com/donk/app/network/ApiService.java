package com.donk.app.network;

import com.donk.app.models.AuthResponse;
import com.donk.app.models.BookmarkResponse;
import com.donk.app.models.ChangePasswordRequest;
import com.donk.app.models.LikeResponse;
import com.donk.app.models.LoginRequest;
import com.donk.app.models.PageResponse;
import com.donk.app.models.PingResponse;
import com.donk.app.models.RegisterRequest;
import com.donk.app.models.SearchResponse;
import com.donk.app.models.TrimVideoRequest;
import com.donk.app.models.UpdateProfileRequest;
import com.donk.app.models.UpdateVideoRequest;
import com.donk.app.models.UserDto;
import com.donk.app.models.UserProfileDto;
import com.donk.app.models.VideoDto;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("api/ping")
    Call<PingResponse> ping();

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/change-password")
    Call<java.util.Map<String, String>> changePassword(@Body ChangePasswordRequest request);

    @GET("api/auth/me")
    Call<UserDto> me();

    // Videos
    @GET("api/videos")
    Call<PageResponse<VideoDto>> getFeed(@Query("page") int page, @Query("size") int size);

    @GET("api/videos/following")
    Call<PageResponse<VideoDto>> getFollowingFeed(@Query("page") int page, @Query("size") int size);

    @GET("api/videos/mine")
    Call<PageResponse<VideoDto>> getMyVideos(@Query("page") int page, @Query("size") int size);

    @GET("api/videos/mine/private")
    Call<PageResponse<VideoDto>> getMyPrivateVideos(@Query("page") int page, @Query("size") int size);

    @GET("api/videos/liked")
    Call<PageResponse<VideoDto>> getLikedVideos(@Query("page") int page, @Query("size") int size);

    @GET("api/videos/bookmarked")
    Call<PageResponse<VideoDto>> getBookmarkedVideos(@Query("page") int page, @Query("size") int size);

    @Multipart
    @POST("api/videos")
    Call<VideoDto> uploadVideo(
            @Part MultipartBody.Part file,
            @Part("title") RequestBody title,
            @Part("description") RequestBody description,
            @Query("isPrivate") boolean isPrivate
    );

    @PUT("api/videos/{id}/privacy")
    Call<VideoDto> toggleVideoPrivacy(@Path("id") Long id);

    @PUT("api/videos/{id}")
    Call<VideoDto> updateVideo(@Path("id") Long id, @Body UpdateVideoRequest request);

    @POST("api/videos/{id}/trim")
    Call<VideoDto> trimVideo(@Path("id") Long id, @Body TrimVideoRequest request);

    @POST("api/videos/{id}/like")
    Call<LikeResponse> likeVideo(@Path("id") Long id);

    @DELETE("api/videos/{id}/like")
    Call<LikeResponse> unlikeVideo(@Path("id") Long id);

    @POST("api/videos/{id}/bookmark")
    Call<BookmarkResponse> bookmarkVideo(@Path("id") Long id);

    @DELETE("api/videos/{id}/bookmark")
    Call<BookmarkResponse> unbookmarkVideo(@Path("id") Long id);

    @DELETE("api/videos/{id}")
    Call<Void> deleteVideo(@Path("id") Long id);

    // Comments
    @GET("api/videos/{id}/comments")
    Call<java.util.List<com.donk.app.models.CommentDto>> getComments(@Path("id") Long id);

    @POST("api/videos/{id}/comments")
    Call<com.donk.app.models.CommentDto> addComment(
            @Path("id") Long id,
            @Body com.donk.app.models.CreateCommentRequest request
    );

    @DELETE("api/videos/{id}/comments/{commentId}")
    Call<Void> deleteComment(@Path("id") Long id, @Path("commentId") Long commentId);

    // Profile & Users
    @GET("api/users/{username}")
    Call<UserProfileDto> getProfile(@Path("username") String username);

    @POST("api/users/{username}/follow")
    Call<UserProfileDto> followUser(@Path("username") String username);

    @DELETE("api/users/{username}/follow")
    Call<UserProfileDto> unfollowUser(@Path("username") String username);

    @PUT("api/users/profile")
    Call<UserProfileDto> updateProfile(@Body UpdateProfileRequest request);

    @Multipart
    @POST("api/users/avatar")
    Call<UserProfileDto> uploadAvatar(@Part MultipartBody.Part file);

    @GET("api/users/{username}/following")
    Call<java.util.List<UserProfileDto>> getFollowing(@Path("username") String username);

    @GET("api/users/{username}/followers")
    Call<java.util.List<UserProfileDto>> getFollowers(@Path("username") String username);

    @GET("api/users/{username}/videos")
    Call<PageResponse<VideoDto>> getUserVideos(
            @Path("username") String username,
            @Query("page") int page,
            @Query("size") int size
    );

    // Search
    @GET("api/search")
    Call<SearchResponse> search(@Query("q") String query);
}



