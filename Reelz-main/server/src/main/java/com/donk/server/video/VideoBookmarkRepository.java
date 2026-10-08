package com.donk.server.video;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoBookmarkRepository extends JpaRepository<VideoBookmark, Long> {

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    int deleteByVideoIdAndUserId(Long videoId, Long userId);

    List<VideoBookmark> findByUserIdAndVideoIdIn(Long userId, Collection<Long> videoIds);

    Page<VideoBookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    void deleteByVideoId(Long videoId);
}
