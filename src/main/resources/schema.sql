-- Users 테이블 생성
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL,
                                     email VARCHAR(100) NOT NULL,
                                     age INTEGER,
                                     department VARCHAR(50),
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     active BOOLEAN DEFAULT TRUE
);