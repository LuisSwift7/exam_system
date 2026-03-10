-- 创建讲评表
CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exam_id` BIGINT(20) NOT NULL COMMENT '关联试卷ID',
  `teacher_id` BIGINT(20) NOT NULL COMMENT '教师ID',
  `title` VARCHAR(200) NOT NULL COMMENT '讲评标题',
  `content` TEXT DEFAULT NULL COMMENT '讲评内容',
  `status` TINYINT(4) DEFAULT 0 COMMENT '状态：0-未发布，1-已发布',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_exam_id` (`exam_id`),
  INDEX `idx_teacher_id` (`teacher_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讲评表';