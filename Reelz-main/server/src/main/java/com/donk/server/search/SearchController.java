package com.donk.server.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donk.server.auth.CurrentUser;
import com.donk.server.common.SearchResponse;
import com.donk.server.user.User;
import com.donk.server.user.UserFollowRepository;
import com.donk.server.user.UserRepository;
import com.donk.server.user.dto.UserProfileDto;
import com.donk.server.video.VideoRepository;
import com.donk.server.video.VideoService;
import com.donk.server.video.dto.VideoDto;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final VideoRepository videoRepository;
    private final VideoService videoService;

    public SearchController(UserRepository userRepository,
                            UserFollowRepository userFollowRepository,
                            VideoRepository videoRepository,
                            VideoService videoService) {
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.videoRepository = videoRepository;
        this.videoService = videoService;
    }

    @GetMapping
    public SearchResponse search(@RequestParam(value = "q", defaultValue = "") String query) {
        String q = query.trim();
        if (q.isEmpty()) {
            return new SearchResponse(List.of(), List.of());
        }

        Long currentUserId = CurrentUser.id();

        // 1. Tim nguoi dung
        List<User> matchedUsers = userRepository.findTop20ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(q, q);
        List<UserProfileDto> userDtos = new ArrayList<>();
        for (User user : matchedUsers) {
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

            userDtos.add(new UserProfileDto(
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
            ));
        }

        // 2. Tim video cong khai
        List<VideoDto> videoDtos = videoService.searchVideos(q, currentUserId);

        return new SearchResponse(userDtos, videoDtos);
    }
}
