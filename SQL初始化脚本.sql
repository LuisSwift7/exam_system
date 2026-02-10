CREATE DATABASE IF NOT EXISTS exam_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE exam_system;

CREATE TABLE IF NOT EXISTS sys_user (
  id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  username        VARCHAR(64)   NOT NULL,
  password_hash   VARCHAR(255)  NOT NULL,
  real_name       VARCHAR(64)   NULL,
  role_code       VARCHAR(32)   NOT NULL,
  phone           VARCHAR(32)   NULL,
  email           VARCHAR(128)  NULL,
  status          TINYINT       NULL DEFAULT 1,
  last_login_time TIMESTAMP     NULL DEFAULT NULL,
  created_time    TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
  create_by       BIGINT        NULL,
  updated_time    TIMESTAMP     NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  update_by       BIGINT        NULL,
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role_code (role_code),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sys_user (username, password_hash, real_name, role_code) VALUES ('teacher01', '$2a$10$EixZaY3s7vjRjMRC/.R78O7e5k6bF5W9eQ7e5k6bF5W9eQ7e5k6bF5', '张老师', 'TEACHER');
INSERT IGNORE INTO sys_user (username, password_hash, real_name, role_code) VALUES ('admin', '$2a$10$EixZaY3s7vjRjMRC/.R78O7e5k6bF5W9eQ7e5k6bF5W9eQ7e5k6bF5', '系统管理员', 'ADMIN');

CREATE TABLE IF NOT EXISTS wrong_book (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  question_id BIGINT NOT NULL COMMENT '题目ID',
  exam_id BIGINT COMMENT '来源考试ID',
  wrong_count INT DEFAULT 0 COMMENT '做错次数',
  practice_count INT DEFAULT 0 COMMENT '练习次数',
  practice_correct_count INT DEFAULT 0 COMMENT '练习正确次数',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_student_question (student_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';
