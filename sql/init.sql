CREATE DATABASE IF NOT EXISTS life_share DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE life_share;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    avatar VARCHAR(255),
    bio VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    content_type VARCHAR(20) DEFAULT 'article',
    cover_image VARCHAR(255),
    user_id BIGINT NOT NULL,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (parent_id) REFERENCES comments(id)
);

INSERT INTO users (username, password, nickname, bio) VALUES
('admin', 'admin123', '管理员', '系统管理员账号'),
('zhangsan', '123456', '张三', '热爱生活，分享美好'),
('lisi', '123456', '李四', '旅行达人');

INSERT INTO posts (title, content, content_type, user_id, view_count, like_count) VALUES
('我的第一次旅行', '今天去了西湖，风景真的很美！湖水清澈，远山如黛，让人心旷神怡。', 'article', 2, 100, 25),
('周末美食分享', '自己动手做的红烧排骨，味道超级棒！分享给大家我的独家秘方...', 'article', 3, 50, 18),
('春日赏花', 'http://example.com/image1.jpg,http://example.com/image2.jpg,http://example.com/image3.jpg', 'image', 2, 200, 45),
('学习心得分享', '最近在学习Spring Boot，感觉框架真的很强大，特别是自动配置功能...', 'article', 1, 80, 12);

INSERT INTO comments (content, post_id, user_id) VALUES
('风景确实很美，下次我也要去！', 1, 3),
('红烧排骨看起来很好吃，求教程！', 2, 2),
('照片拍得真漂亮！', 3, 3);
