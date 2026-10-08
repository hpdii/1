package com.donk.server.video;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.donk.server.auth.CurrentUser;
import com.donk.server.common.ApiException;
import com.donk.server.video.dto.LikeResponse;
import com.donk.server.video.dto.PageResponse;
import com.donk.server.video.dto.VideoDto;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public PageResponse<VideoDto> feed(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return videoService.feed(CurrentUser.id(), page, size);
    }

    @GetMapping("/following")
    public PageResponse<VideoDto> followingFeed(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return videoService.followingFeed(requireUser(), page, size);
    }

    /**
     * Trang ca nhan. Duong dan la /mine chu KHONG phai /{id} — neu dat
     * @GetMapping("/{id}") thi "mine" se dung {id} va Spring tra 400 khi co
     * chuyen chuoi "mine" thanh Long.
     */
    @GetMapping("/mine")
    public PageResponse<VideoDto> myVideos(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Long userId = requireUser();
        return videoService.videosOf(userId, userId, page, size);
    }

    @GetMapping("/mine/private")
    public PageResponse<VideoDto> myPrivateVideos(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        Long userId = requireUser();
        return videoService.myPrivateVideos(userId, page, size);
    }

    @PostMapping
    public ResponseEntity<VideoDto> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam("title") String title,
                                           @RequestParam(value = "description", required = false)
                                           String description,
                                           @RequestParam(value = "isPrivate", defaultValue = "false")
                                           boolean isPrivate) {
        VideoDto created = videoService.upload(requireUser(), file, title, description, isPrivate);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/privacy")
    public VideoDto togglePrivacy(@PathVariable Long id) {
        return videoService.togglePrivacy(id, requireUser());
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public VideoDto updateVideo(
            @PathVariable Long id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.donk.server.video.dto.UpdateVideoRequest request) {
        return videoService.updateVideo(id, requireUser(), request.title(), request.description(), request.isPrivate());
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/trim")
    public VideoDto trimVideo(
            @PathVariable Long id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.donk.server.video.dto.TrimVideoRequest request) {
        return videoService.trimVideo(id, requireUser(), request.startSeconds(), request.endSeconds());
    }

    @PostMapping("/{id}/like")
    public LikeResponse like(@PathVariable Long id) {
        return videoService.like(id, requireUser());
    }

    @DeleteMapping("/{id}/like")
    public LikeResponse unlike(@PathVariable Long id) {
        return videoService.unlike(id, requireUser());
    }

    @PostMapping("/{id}/bookmark")
    public com.donk.server.video.dto.BookmarkResponse bookmark(@PathVariable Long id) {
        return videoService.bookmark(id, requireUser());
    }

    @DeleteMapping("/{id}/bookmark")
    public com.donk.server.video.dto.BookmarkResponse unbookmark(@PathVariable Long id) {
        return videoService.unbookmark(id, requireUser());
    }

    @GetMapping("/liked")
    public PageResponse<VideoDto> myLikedVideos(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return videoService.likedVideos(requireUser(), page, size);
    }

    @GetMapping("/bookmarked")
    public PageResponse<VideoDto> myBookmarkedVideos(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return videoService.bookmarkedVideos(requireUser(), page, size);
    }

    // --- Comments ---
    @GetMapping("/{id}/comments")
    public List<com.donk.server.video.dto.CommentDto> getComments(@PathVariable Long id) {
        return videoService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<com.donk.server.video.dto.CommentDto> addComment(
            @PathVariable Long id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.donk.server.video.dto.CreateCommentRequest request) {
        com.donk.server.video.dto.CommentDto created = videoService.addComment(id, requireUser(), request.content(), request.replyToId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        videoService.deleteComment(commentId, requireUser());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        videoService.delete(id, requireUser());

        return ResponseEntity.noContent().build();
    }

    private Long requireUser() {
        Long userId = CurrentUser.id();

        if (userId == null) {
            throw ApiException.unauthorized("Chua dang nhap");
        }

        return userId;
    }
}

