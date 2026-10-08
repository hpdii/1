package com.donk.server.video;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    Optional<VideoLike> findByVideoIdAndUserId(Long videoId, Long userId);

    /**
     * Lay like cua mot nguoi trong mot lo video cua feed — dung de dien
     * truong likedByMe cho ca trang ma chi ton mot query.
     */
    List<VideoLike> findByUserIdAndVideoIdIn(Long userId, Collection<Long> videoIds);

    org.springframework.data.domain.Page<VideoLike> findByUserIdOrderByCreatedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("delete from VideoLike vl where vl.videoId = :videoId and vl.userId = :userId")
    int deleteByVideoIdAndUserId(@Param("videoId") Long videoId, @Param("userId") Long userId);

    /** Don sach like khi xoa video (CSDL cung tu cascade, day la lop bao hiem). */
    @Modifying
    @Query("delete from VideoLike vl where vl.videoId = :videoId")
    int deleteByVideoId(@Param("videoId") Long videoId);
}
