package com.donk.server.video;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.donk.server.common.ApiException;
import com.donk.server.storage.CloudinaryService;
import com.donk.server.user.User;
import com.donk.server.user.UserRepository;
import com.donk.server.video.dto.BookmarkResponse;
import com.donk.server.video.dto.CommentDto;
import com.donk.server.video.dto.LikeResponse;
import com.donk.server.video.dto.PageResponse;
import com.donk.server.video.dto.VideoDto;

@Service
public class VideoService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final VideoRepository videoRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final VideoBookmarkRepository videoBookmarkRepository;
    private final VideoCommentRepository videoCommentRepository;
    private final com.donk.server.user.UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final VideoStorageService storageService;
    private final CloudinaryService cloudinaryService;

    public VideoService(VideoRepository videoRepository,
                        VideoLikeRepository videoLikeRepository,
                        VideoBookmarkRepository videoBookmarkRepository,
                        VideoCommentRepository videoCommentRepository,
                        com.donk.server.user.UserFollowRepository userFollowRepository,
                        UserRepository userRepository,
                        VideoStorageService storageService,
                        CloudinaryService cloudinaryService) {
        this.videoRepository = videoRepository;
        this.videoLikeRepository = videoLikeRepository;
        this.videoBookmarkRepository = videoBookmarkRepository;
        this.videoCommentRepository = videoCommentRepository;
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoDto> feed(Long currentUserId, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<Video> videos = videoRepository.findByIsPrivateFalseOrderByCreatedAtDesc(pageable);

        return toPageResponse(videos, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoDto> followingFeed(Long currentUserId, int page, int size) {
        if (currentUserId == null) {
            return new PageResponse<>(List.of(), 0, size, 0, false);
        }

        List<Long> followingIds = userFollowRepository.findFollowingIdsByFollowerId(currentUserId);
        if (followingIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0, size, 0, false);
        }

        Pageable pageable = toPageable(page, size);
        Page<Video> videos = videoRepository.findByUserIdInAndIsPrivateFalseOrderByCreatedAtDesc(followingIds, pageable);

        return toPageResponse(videos, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoDto> videosOf(Long authorId, Long currentUserId, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<Video> videos;
        if (authorId.equals(currentUserId)) {
            videos = videoRepository.findByUserIdAndIsPrivateFalseOrderByCreatedAtDesc(authorId, pageable);
        } else {
            videos = videoRepository.findByUserIdAndIsPrivateFalseOrderByCreatedAtDesc(authorId, pageable);
        }

        return toPageResponse(videos, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoDto> myPrivateVideos(Long currentUserId, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<Video> videos = videoRepository.findByUserIdAndIsPrivateTrueOrderByCreatedAtDesc(currentUserId, pageable);

        return toPageResponse(videos, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoDto> likedVideos(Long userId, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<VideoLike> likes = videoLikeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<Long> videoIds = likes.getContent().stream().map(VideoLike::getVideoId).toList();
        List<Video> videos = videoRepository.findAllById(videoIds);

        Map<Long, Video> map = new HashMap<>();
        for (Video v : videos) {
            map.put(v.getId(), v);
        }
        List<Video> ordered = videoIds.stream().map(map::get).filter(Objects::nonNull).toList();

        return buildPageResponse(ordered, likes.getNumber(), likes.getSize(), likes.getTotalElements(), likes.hasNext(), userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoDto> bookmarkedVideos(Long userId, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<VideoBookmark> bookmarks = videoBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<Long> videoIds = bookmarks.getContent().stream().map(VideoBookmark::getVideoId).toList();
        List<Video> videos = videoRepository.findAllById(videoIds);

        Map<Long, Video> map = new HashMap<>();
        for (Video v : videos) {
            map.put(v.getId(), v);
        }
        List<Video> ordered = videoIds.stream().map(map::get).filter(Objects::nonNull).toList();

        return buildPageResponse(ordered, bookmarks.getNumber(), bookmarks.getSize(), bookmarks.getTotalElements(), bookmarks.hasNext(), userId);
    }

    @Transactional(readOnly = true)
    public List<VideoDto> searchVideos(String query, Long currentUserId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Video> matches = videoRepository.searchVideos(query.trim());
        Set<Long> likedIds = likedVideoIds(matches, currentUserId);
        Set<Long> bookmarkedIds = bookmarkedVideoIds(matches, currentUserId);
        Map<Long, User> authors = authorMap(matches);

        return matches.stream()
                .map(v -> toDto(v, authors, likedIds, bookmarkedIds, null))
                .toList();
    }

    @Transactional
    public VideoDto upload(Long userId, MultipartFile file, String title, String description, boolean isPrivate) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("Thieu tieu de video");
        }

        if (title.length() > 150) {
            throw ApiException.badRequest("Tieu de toi da 150 ky tu");
        }

        if (description != null && description.length() > 500) {
            throw ApiException.badRequest("Mo ta toi da 500 ky tu");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("video/")) {
            throw ApiException.badRequest("File gui len khong phai video");
        }

        String storedName = storageService.store(file);
        String thumbnailUrl = null;

        // Neu Cloudinary duoc cau hinh, upload len Cloudinary de co CDN link.
        // NEU THAT BAI phai bao loi, khong duoc re ve luu local: o Railway dia
        // chi la o tam, xoa sau moi lan deploy -> video mat danh sach ma khong bao.
        if (cloudinaryService.isConfigured()) {
            try {
                File localFile = storageService.load(storedName).toFile();
                CloudinaryService.CloudinaryVideoUpload uploaded = cloudinaryService.uploadVideo(localFile);
                if (uploaded != null && uploaded.videoUrl() != null) {
                    storedName = uploaded.videoUrl();
                    thumbnailUrl = uploaded.thumbnailUrl();
                } else {
                    throw new IllegalStateException("Cloudinary khong tra ve URL");
                }
            } catch (Exception e) {
                storageService.delete(storedName);
                System.out.println("[REELZ] Cloudinary upload video that bai: " + e);
                throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "Khong tai len duoc Cloudinary, hay thu lai sau");
            }
        }

        Video video = new Video();
        video.setUserId(userId);
        video.setTitle(title.trim());
        video.setDescription(description == null ? null : description.trim());
        video.setFileName(storedName);
        video.setThumbnailUrl(thumbnailUrl);
        video.setOriginalName(file.getOriginalFilename());
        video.setContentType(contentType);
        video.setSizeBytes(file.getSize());
        video.setLikeCount(0);
        video.setCommentCount(0);
        video.setPrivate(isPrivate);
        video.setCreatedAt(LocalDateTime.now());

        video = videoRepository.save(video);

        return toDto(video, Map.of(), Set.of(), Set.of(), currentAuthorMap(userId));
    }

    @Transactional
    public VideoDto updateVideo(Long videoId, Long userId, String title, String description, Boolean isPrivate) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay video"));

        if (!video.getUserId().equals(userId)) {
            throw ApiException.forbidden("Ban khong phai chu so huu video");
        }

        if (title != null) {
            String cleanTitle = title.trim();
            if (cleanTitle.isEmpty()) {
                throw ApiException.badRequest("Thieu tieu de video");
            }
            if (cleanTitle.length() > 150) {
                throw ApiException.badRequest("Tieu de toi da 150 ky tu");
            }
            video.setTitle(cleanTitle);
        }

        if (description != null) {
            String cleanDescription = description.trim();
            if (cleanDescription.length() > 500) {
                throw ApiException.badRequest("Mo ta toi da 500 ky tu");
            }
            video.setDescription(cleanDescription.isEmpty() ? null : cleanDescription);
        }

        if (isPrivate != null) {
            video.setPrivate(isPrivate);
        }

        video = videoRepository.save(video);
        return toDto(video, Map.of(), Set.of(), Set.of(), currentAuthorMap(userId));
    }

    @Transactional
    public VideoDto trimVideo(Long videoId, Long userId, Double startSeconds, Double endSeconds) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay video"));

        if (!video.getUserId().equals(userId)) {
            throw ApiException.forbidden("Ban khong phai chu so huu video");
        }

        if (video.getFileName().startsWith("http")) {
            throw ApiException.badRequest("Video nay khong ho tro cat tren server");
        }

        double start = startSeconds != null ? startSeconds : 0;
        double end = endSeconds != null ? endSeconds : 0;
        if (start < 0 || end <= 0 || end <= start || (end - start) < 1 || (end - start) > 600) {
            throw ApiException.badRequest("Doan cat khong hop le (toi thieu 1 giay, toi da 10 phut)");
        }

        java.nio.file.Path source = storageService.load(video.getFileName());
        String trimmedName = storageService.trim(source, start, end);
        String oldName = video.getFileName();
        video.setFileName(trimmedName);
        video.setThumbnailUrl(null);
        video = videoRepository.save(video);

        if (oldName != null && !oldName.equals(trimmedName)) {
            storageService.delete(oldName);
        }

        return toDto(video, Map.of(), Set.of(), Set.of(), currentAuthorMap(userId));
    }

    @Transactional
    public VideoDto togglePrivacy(Long videoId, Long userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay video"));

        if (!video.getUserId().equals(userId)) {
            throw ApiException.forbidden("Ban khong phai chu so huu video");
        }

        video.setPrivate(!video.isPrivate());
        video = videoRepository.save(video);

        return toDto(video, Map.of(), Set.of(), Set.of(), currentAuthorMap(userId));
    }

    @Transactional
    public LikeResponse like(Long videoId, Long userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay video"));

        if (!videoLikeRepository.existsByVideoIdAndUserId(videoId, userId)) {
            VideoLike like = new VideoLike();
            like.setVideoId(videoId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            videoLikeRepository.save(like);

            video.setLikeCount(video.getLikeCount() + 1);
            videoRepository.save(video);
        }

        return new LikeResponse(videoId, video.getLikeCount(), true);
    }

    @Transactional
    public LikeResponse unlike(Long videoId, Long userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay video"));

        int deleted = videoLikeRepository.deleteByVideoIdAndUserId(videoId, userId);

        if (deleted > 0 && video.getLikeCount() > 0) {
            video.setLikeCount(video.getLikeCount() - 1);
            videoRepository.save(video);
        }

        return new LikeResponse(videoId, video.getLikeCount(), false);
    }

    @Transactional
    public BookmarkResponse bookmark(Long videoId, Long userId) {
        if (!videoRepository.existsById(videoId)) {
            throw ApiException.notFound("Khong tim thay video");
        }

        if (!videoBookmarkRepository.existsByVideoIdAndUserId(videoId, userId)) {
            VideoBookmark bookmark = new VideoBookmark();
            bookmark.setVideoId(videoId);
            bookmark.setUserId(userId);
            bookmark.setCreatedAt(LocalDateTime.now());
            videoBookmarkRepository.save(bookmark);
        }

        return new BookmarkResponse(videoId, true);
    }

    @Transactional
    public BookmarkResponse unbookmark(Long videoId, Long userId) {
        if (!videoRepository.existsById(videoId)) {
            throw ApiException.notFound("Khong tim thay video");
        }

        videoBookmarkRepository.deleteByVideoIdAndUserId(videoId, userId);
        return new BookmarkResponse(videoId, false);
    }

    // ------------------------------------------------------------------
    // Comments
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(Long videoId) {
        if (!videoRepository.existsById(videoId)) {
            throw ApiException.notFound("Khong tim thay video");
        }

        List<VideoComment> comments = videoCommentRepository.findByVideoIdOrderByCreatedAtAsc(videoId);
        if (comments.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = comments.stream().map(VideoComment::getUserId).distinct().toList();
        Map<Long, User> userMap = new HashMap<>();
        for (User u : userRepository.findAllById(userIds)) {
            userMap.put(u.getId(), u);
        }

        List<CommentDto> dtos = new ArrayList<>();
        for (VideoComment c : comments) {
            User u = userMap.get(c.getUserId());
            String username = u != null ? u.getUsername() : "user";
            String displayName = u != null ? u.getDisplayName() : username;
            String avatarUrl = u != null ? u.getAvatarUrl() : null;

            String replyToUsername = null;
            if (c.getReplyToId() != null) {
                for (VideoComment parent : comments) {
                    if (parent.getId().equals(c.getReplyToId())) {
                        User parentUser = userMap.get(parent.getUserId());
                        replyToUsername = parentUser != null ? parentUser.getUsername() : null;
                        break;
                    }
                }
            }

            dtos.add(new CommentDto(
                    c.getId(),
                    c.getVideoId(),
                    c.getUserId(),
                    username,
                    displayName,
                    avatarUrl,
                    c.getContent(),
                    replyToUsername,
                    c.getReplyToId(),
                    c.getCreatedAt().format(ISO)
            ));
        }

        return dtos;
    }

    @Transactional
    public CommentDto addComment(Long videoId, Long userId, String content) {
        return addComment(videoId, userId, content, null);
    }

    @Transactional
    public CommentDto addComment(Long videoId, Long userId, String content, Long replyToId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay video"));

        if (content == null || content.isBlank()) {
            throw ApiException.badRequest("Noi dung binh luan khong duoc de trong");
        }

        String finalContent = content.trim();
        String replyToUsername = null;

        if (replyToId != null) {
            VideoComment parent = videoCommentRepository.findById(replyToId)
                    .orElseThrow(() -> ApiException.notFound("Khong tim thay binh luan goc"));

            if (!parent.getVideoId().equals(videoId)) {
                throw ApiException.badRequest("Binh luan goc khong thuoc video nay");
            }

            String tag = null;
            if (!finalContent.startsWith("@")) {
                User parentUser = userRepository.findById(parent.getUserId()).orElse(null);
                if (parentUser != null && parentUser.getUsername() != null) {
                    tag = "@" + parentUser.getUsername();
                    finalContent = tag + " " + finalContent;
                    replyToUsername = parentUser.getUsername();
                }
            } else {
                replyToUsername = finalContent.split("\\s+")[0].substring(1);
            }
        }

        VideoComment comment = new VideoComment();
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setContent(finalContent);
        comment.setReplyToId(replyToId);
        comment.setCreatedAt(LocalDateTime.now());
        comment = videoCommentRepository.save(comment);

        video.setCommentCount(video.getCommentCount() + 1);
        videoRepository.save(video);

        User user = userRepository.findById(userId).orElse(null);
        String username = user != null ? user.getUsername() : "user";
        String displayName = user != null ? user.getDisplayName() : username;
        String avatarUrl = user != null ? user.getAvatarUrl() : null;

        return new CommentDto(
                comment.getId(),
                comment.getVideoId(),
                comment.getUserId(),
                username,
                displayName,
                avatarUrl,
                comment.getContent(),
                replyToUsername,
                comment.getReplyToId(),
                comment.getCreatedAt().format(ISO)
        );
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        VideoComment comment = videoCommentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay binh luan"));

        Video video = videoRepository.findById(comment.getVideoId())
                .orElseThrow(() -> ApiException.notFound("Khong tim thay video"));

        boolean isCommentOwner = comment.getUserId().equals(userId);
        boolean isVideoOwner = video.getUserId().equals(userId);
        if (!isCommentOwner && !isVideoOwner) {
            throw ApiException.forbidden("Ban khong co quyen xoa binh luan nay");
        }

        int removedCount = 1 + deleteReplies(comment.getId());

        int nextCount = Math.max(0, video.getCommentCount() - removedCount);
        video.setCommentCount(nextCount);
        videoRepository.save(video);

        videoCommentRepository.delete(comment);
    }

    private int deleteReplies(Long parentCommentId) {
        List<VideoComment> replies = videoCommentRepository.findByReplyToId(parentCommentId);
        int removed = 0;
        for (VideoComment reply : replies) {
            removed += 1 + deleteReplies(reply.getId());
            videoCommentRepository.delete(reply);
        }
        return removed;
    }

    @Transactional
    public void delete(Long videoId, Long userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay video"));

        if (!video.getUserId().equals(userId)) {
            throw ApiException.notFound("Khong tim thay video");
        }

        videoCommentRepository.deleteByVideoId(videoId);
        videoBookmarkRepository.deleteByVideoId(videoId);
        videoLikeRepository.deleteByVideoId(videoId);
        videoRepository.delete(video);

        if (!video.getFileName().startsWith("http")) {
            storageService.delete(video.getFileName());
        }
    }

    // ------------------------------------------------------------------
    // DTO helpers
    // ------------------------------------------------------------------

    private PageResponse<VideoDto> toPageResponse(Page<Video> videos, Long currentUserId) {
        return buildPageResponse(videos.getContent(), videos.getNumber(), videos.getSize(), videos.getTotalElements(), videos.hasNext(), currentUserId);
    }

    private PageResponse<VideoDto> buildPageResponse(List<Video> content, int page, int size, long total, boolean hasNext, Long currentUserId) {
        Set<Long> likedIds = likedVideoIds(content, currentUserId);
        Set<Long> bookmarkedIds = bookmarkedVideoIds(content, currentUserId);
        Map<Long, User> authors = authorMap(content);

        List<VideoDto> items = content.stream()
                .map(v -> toDto(v, authors, likedIds, bookmarkedIds, null))
                .toList();

        return new PageResponse<>(items, page, size, total, hasNext);
    }

    private Set<Long> likedVideoIds(List<Video> videos, Long currentUserId) {
        if (currentUserId == null || videos.isEmpty()) {
            return Set.of();
        }

        List<Long> videoIds = videos.stream().map(Video::getId).toList();
        Set<Long> result = new HashSet<>();

        for (VideoLike like : videoLikeRepository.findByUserIdAndVideoIdIn(currentUserId, videoIds)) {
            result.add(like.getVideoId());
        }

        return result;
    }

    private Set<Long> bookmarkedVideoIds(List<Video> videos, Long currentUserId) {
        if (currentUserId == null || videos.isEmpty()) {
            return Set.of();
        }

        List<Long> videoIds = videos.stream().map(Video::getId).toList();
        Set<Long> result = new HashSet<>();

        for (VideoBookmark bookmark : videoBookmarkRepository.findByUserIdAndVideoIdIn(currentUserId, videoIds)) {
            result.add(bookmark.getVideoId());
        }

        return result;
    }

    private Map<Long, User> authorMap(List<Video> videos) {
        if (videos.isEmpty()) {
            return Map.of();
        }

        List<Long> authorIds = videos.stream().map(Video::getUserId).distinct().toList();
        Map<Long, User> map = new HashMap<>();

        for (User user : userRepository.findAllById(authorIds)) {
            map.put(user.getId(), user);
        }

        return map;
    }

    private Map<Long, User> currentAuthorMap(Long userId) {
        return userRepository.findById(userId)
                .map(u -> Map.of(userId, u))
                .orElse(Map.of());
    }

    private VideoDto toDto(Video video,
                           Map<Long, User> authors,
                           Set<Long> likedIds,
                           Set<Long> bookmarkedIds,
                           Map<Long, User> fallbackAuthors) {
        Map<Long, User> authorSource = authors.isEmpty() ? fallbackAuthors : authors;
        User author = authorSource == null ? null : authorSource.get(video.getUserId());

        String username = author == null ? "unknown" : author.getUsername();
        String displayName = author == null ? "unknown" : author.getDisplayName();
        String avatarUrl = author != null ? author.getAvatarUrl() : null;

        String path = video.getFileName().startsWith("http")
                ? video.getFileName()
                : "media/" + video.getFileName();

        return new VideoDto(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                path,
                video.getThumbnailUrl(),
                video.getUserId(),
                username,
                displayName,
                avatarUrl,
                video.getLikeCount(),
                video.getCommentCount(),
                likedIds.contains(video.getId()),
                bookmarkedIds.contains(video.getId()),
                video.isPrivate(),
                video.getCreatedAt().format(ISO));
    }

    private Pageable toPageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        return PageRequest.of(safePage, safeSize);
    }
}

