-- Donk — luoc do CSDL
-- Chay file nay trong MySQL Workbench truoc khi khoi dong server.

CREATE DATABASE IF NOT EXISTS donk
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE donk;

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50) NOT NULL,
    email         VARCHAR(120) NOT NULL,
    display_name  VARCHAR(80)  NULL,
    bio           VARCHAR(200) NULL,
    avatar_url    VARCHAR(500) NULL,
    follower_count INT NOT NULL DEFAULT 0,
    following_count INT NOT NULL DEFAULT 0,
    password_hash VARCHAR(72) NOT NULL,   -- BCrypt luon ra 60 ky tu
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email    (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- videos
--
-- file_name chi luu TEN FILE tren o dia, khong luu duong dan tuyet doi —
-- doi thu muc upload sau nay khong phai sua du lieu.
-- like_count luu san (denormalize) de feed khong phai COUNT cho tung dong.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS videos (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(150) NOT NULL,
    description   VARCHAR(500) NULL,
    file_name     VARCHAR(120) NOT NULL,
    original_name VARCHAR(255) NULL,
    thumbnail_url VARCHAR(500) NULL,
    content_type  VARCHAR(80)  NOT NULL DEFAULT 'video/mp4',
    size_bytes    BIGINT       NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    is_private    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_videos_created      (created_at),
    KEY idx_videos_user_created (user_id, created_at),
    CONSTRAINT fk_videos_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- video_likes
--
-- Ten bang la video_likes vi `like` la tu khoa cua MySQL.
-- uk_likes_video_user chan like trung o tang CSDL, nho do endpoint like
-- thanh idempotent va khong co race read-then-write.
-- idx_likes_user la BAT BUOC: InnoDB can index tren cot tham chieu cua FK,
-- ma uk_likes_video_user bat dau bang video_id nen khong dung duoc cho user_id.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS video_likes (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    video_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_likes_video_user (video_id, user_id),
    KEY idx_likes_user (user_id),
    CONSTRAINT fk_likes_video FOREIGN KEY (video_id)
        REFERENCES videos (id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_user  FOREIGN KEY (user_id)
        REFERENCES users (id)  ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- video_bookmarks
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS video_bookmarks (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    video_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_bookmarks_video_user (video_id, user_id),
    KEY idx_bookmarks_user (user_id),
    CONSTRAINT fk_bookmarks_video FOREIGN KEY (video_id)
        REFERENCES videos (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_user  FOREIGN KEY (user_id)
        REFERENCES users (id)  ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- video_comments (co cot reply_to_id de tra loi binh luan va tag @username)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS video_comments (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    video_id   BIGINT        NOT NULL,
    user_id    BIGINT        NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    reply_to_id BIGINT       NULL,
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_comments_video (video_id, created_at),
    KEY idx_comments_user (user_id),
    CONSTRAINT fk_comments_video FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_comments_reply FOREIGN KEY (reply_to_id) REFERENCES video_comments (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- user_follows
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_follows (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    follower_id  BIGINT      NOT NULL,
    following_id BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_follows (follower_id, following_id),
    KEY idx_follows_following (following_id),
    KEY idx_follows_follower (follower_id),
    CONSTRAINT fk_follows_follower  FOREIGN KEY (follower_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;
