-- 通知表
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
  `type` VARCHAR(50) NOT NULL COMMENT '通知类型：exam_published, class_apply, exam_graded, class_approved, system',
  `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `related_id` BIGINT COMMENT '关联ID（如考试ID、班级ID等）',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 通知类型说明：
-- exam_published: 考试发布通知
-- class_apply: 学生申请加入班级通知
-- exam_graded: 考试批改完成通知
-- class_approved: 班级申请批准通知
-- system: 系统通知