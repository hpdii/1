package com.donk.server.user;

import jakarta.validation.Valid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.donk.server.auth.CurrentUser;
import com.donk.server.common.ApiException;
import com.donk.server.storage.CloudinaryService;
import com.donk.server.user.dto.UpdateProfileRequest;
import com.donk.server.user.dto.UserProfileDto;
import com.donk.server.video.VideoRepository;
import com.donk.server.video.VideoService;
import com.donk.server.video.dto.PageResponse;
import com.donk.server.video.dto.VideoDto;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final VideoRepository videoRepository;
    private final VideoService videoService;
    private final CloudinaryService cloudinaryService;
    private final Path uploadDir;

    public UserController(UserRepository userRepository,
                          UserFollowRepository userFollowRepository,
                          VideoRepository videoRepository,
                          VideoService videoService,
                          CloudinaryService cloudinaryService,
                          @Value("${app.upload.dir}") String uploadDir) {
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.videoRepository = videoRepository;
        this.videoService = videoService;
        this.cloudinaryService = cloudinaryService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Lay thong tin ho so cua mot nguoi (bao gom bio, so follow, tong like, avatar). */
    @GetMapping("/{username}")
    public UserProfileDto getProfile(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay nguoi dung"));

        return toProfileDto(user, CurrentUser.id());
    }

    /** Danh sach nhung nguoi ma nguoi dung dang theo doi. */
    @GetMapping("/{username}/following")
    public List<UserProfileDto> getFollowing(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay nguoi dung"));

        List<Long> followingIds = userFollowRepository.findFollowingIdsByFollowerId(user.getId());
        if (followingIds.isEmpty()) {
            return List.of();
        }

        Long currentUserId = CurrentUser.id();
        List<User> users = userRepository.findAllById(followingIds);
        return users.stream().map(u -> toProfileDto(u, currentUserId)).toList();
    }

    /** Danh sach nhung nguoi dang theo doi nguoi dung nay. */
    @GetMapping("/{username}/followers")
    public List<UserProfileDto> getFollowers(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay nguoi dung"));

        List<Long> followerIds = userFollowRepository.findFollowerIdsByFollowingId(user.getId());
        if (followerIds.isEmpty()) {
            return List.of();
        }

        Long currentUserId = CurrentUser.id();
        List<User> users = userRepository.findAllById(followerIds);
        return users.stream().map(u -> toProfileDto(u, currentUserId)).toList();
    }

    private UserProfileDto toProfileDto(User user, Long currentUserId) {
        boolean isFollowing = currentUserId != null &&
                userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getId());

        int videoCount = videoRepository.countByUserIdAndIsPrivateFalse(user.getId());
        int followerCount = userFollowRepository.countByFollowingId(user.getId());
        int followingCount = userFollowRepository.countByFollowerId(user.getId());
        int totalLikes = videoRepository.sumLikesByUserId(user.getId());

        String avatar = user.getAvatarUrl();
        if (avatar != null && !avatar.startsWith("http")) {
            avatar = "media/" + avatar;
        }

        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                avatar,
                videoCount,
                followerCount,
                followingCount,
                totalLikes,
                isFollowing
        );
    }

    /** Upload anh Avatar len Cloudinary hoac luu server. */
    @PostMapping("/avatar")
    @Transactional
    public UserProfileDto uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long currentUserId = CurrentUser.id();
        if (currentUserId == null) {
            throw ApiException.unauthorized("Chua dang nhap");
        }

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Chua chon file anh");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay nguoi dung"));

        String avatarUrl = null;
        if (cloudinaryService.isConfigured()) {
            try {
                avatarUrl = cloudinaryService.uploadImage(file);
            } catch (Exception e) {
                System.out.println("[REELZ] Cloudinary upload avatar that bai, ve luu local: " + e);
            }
        }

        if (avatarUrl == null) {
            // Luu local
            String ext = "jpg";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.') + 1);
            }
            String storedName = "avatar_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            try {
                Files.createDirectories(uploadDir);
                file.transferTo(uploadDir.resolve(storedName));
                avatarUrl = storedName;
            } catch (IOException e) {
                throw ApiException.badRequest("Khong the luu file anh avatar: " + e.getMessage());
            }
        }

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return getProfile(user.getUsername());
    }

    /** Theo doi (Follow) mot nguoi dung. */
    @PostMapping("/{username}/follow")
    @Transactional
    public UserProfileDto follow(@PathVariable String username) {
        Long currentUserId = CurrentUser.id();
        if (currentUserId == null) {
            throw ApiException.unauthorized("Chua dang nhap");
        }

        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay nguoi dung"));

        if (target.getId().equals(currentUserId)) {
            throw ApiException.badRequest("Khong the tu theo doi chinh minh");
        }

        if (!userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, target.getId())) {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(currentUserId);
            follow.setFollowingId(target.getId());
            follow.setCreatedAt(LocalDateTime.now());
            userFollowRepository.save(follow);

            target.setFollowerCount(target.getFollowerCount() + 1);
            userRepository.save(target);

            userRepository.findById(currentUserId).ifPresent(u -> {
                u.setFollowingCount(u.getFollowingCount() + 1);
                userRepository.save(u);
            });
        }

        return getProfile(username);
    }

    /** Bo theo doi (Unfollow) mot nguoi dung. */
    @DeleteMapping("/{username}/follow")
    @Transactional
    public UserProfileDto unfollow(@PathVariable String username) {
        Long currentUserId = CurrentUser.id();
        if (currentUserId == null) {
            throw ApiException.unauthorized("Chua dang nhap");
        }

        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay nguoi dung"));

        int deleted = userFollowRepository.deleteByFollowerIdAndFollowingId(currentUserId, target.getId());
        if (deleted > 0) {
            if (target.getFollowerCount() > 0) {
                target.setFollowerCount(target.getFollowerCount() - 1);
                userRepository.save(target);
            }

            userRepository.findById(currentUserId).ifPresent(u -> {
                if (u.getFollowingCount() > 0) {
                    u.setFollowingCount(u.getFollowingCount() - 1);
                    userRepository.save(u);
                }
            });
        }

        return getProfile(username);
    }

    /** Cap nhat tieu su (bio) va ten hien thi cua chinh minh. */
    @PutMapping("/profile")
    public UserProfileDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long currentUserId = CurrentUser.id();
        if (currentUserId == null) {
            throw ApiException.unauthorized("Chua dang nhap");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay nguoi dung"));

        // Note: username KHONG cho doi. Ten dang nhap la thong tin dang nhap,
        // doi thong dung bi chiem ten va phai kiem tra lai mat khau.

        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().trim());
        }

        user = userRepository.save(user);
        return getProfile(user.getUsername());
    }

    /** Video cong khai cua mot nguoi, tim theo ten dang nhap. */
    @GetMapping("/{username}/videos")
    public PageResponse<VideoDto> videosOf(@PathVariable String username,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay nguoi dung"));

        return videoService.videosOf(author.getId(), CurrentUser.id(), page, size);
    }
}
