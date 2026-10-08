package com.donk.server.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Modifying
    @Query("delete from UserFollow uf where uf.followerId = :followerId and uf.followingId = :followingId")
    int deleteByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    int countByFollowingId(Long followingId);

    int countByFollowerId(Long followerId);

    @Query("select uf.followingId from UserFollow uf where uf.followerId = :followerId")
    java.util.List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);

    @Query("select uf.followerId from UserFollow uf where uf.followingId = :followingId")
    java.util.List<Long> findFollowerIdsByFollowingId(@Param("followingId") Long followingId);
}
