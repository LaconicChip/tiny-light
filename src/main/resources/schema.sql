CREATE TABLE IF NOT EXISTS tiny_light (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    content VARCHAR(500) NOT NULL,
    mood VARCHAR(20),
    light_date DATE NOT NULL,
    created_at DATETIME,
    UNIQUE KEY uk_user_date (user_id, light_date)   -- 一人一天只能一颗
);