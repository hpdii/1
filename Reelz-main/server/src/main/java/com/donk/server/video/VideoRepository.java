package com.donk.server.video;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long> {

    /** Feed chung: chi hien video cong khai. */
    Page<Video> findByIsPrivateFalseOrderByCreatedAtDesc(Pageable pageable);

    /** Feed da follow: chi video cong khai cua nhung nguoi minh dang follow. */
    Page<Video> findByUserIdInAndIsPrivateFalseOrderByCreatedAtDesc(Collection<Long> userIds, Pageable pageable);

    /** Trang ca nhan cua nguoi khac: chi video cong khai. */
    Page<Video> findByUserIdAndIsPrivateFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Video bi an (chua rieng tu) cua chinh minh. */
    Page<Video> findByUserIdAndIsPrivateTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Video> findByIdIn(Collection<Long> ids);

    int countByUserId(Long userId);

    int countByUserIdAndIsPrivateFalse(Long userId);

    @Query("select coalesce(sum(v.likeCount), 0) from Video v where v.userId = :userId")
    int sumLikesByUserId(@Param("userId") Long userId);

    @Query("select v from Video v where v.isPrivate = false and (lower(v.title) like lower(concat('%', :q, '%')) or lower(v.description) like lower(concat('%', :q, '%'))) order by v.createdAt desc")
    List<Video> searchVideos(@Param("q") String query);
}
