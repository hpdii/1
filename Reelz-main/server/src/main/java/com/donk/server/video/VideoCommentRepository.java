package com.donk.server.video;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    List<VideoComment> findByVideoIdOrderByCreatedAtAsc(Long videoId);

    List<VideoComment> findByReplyToId(Long replyToId);

    int countByVideoId(Long videoId);

    void deleteByVideoId(Long videoId);
}
