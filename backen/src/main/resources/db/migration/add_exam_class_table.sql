-- 试卷班级关联表
CREATE TABLE IF NOT EXISTS `exam_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exam_id` bigint NOT NULL COMMENT '试卷ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_class` (`exam_id`, `class_id`),
  KEY `idx_exam_id` (`exam_id`),
  KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷班级关联表';
