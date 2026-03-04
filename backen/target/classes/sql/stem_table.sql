-- 创建stem表，用于存储资料分析题的共享题干
CREATE TABLE IF NOT EXISTS `stem` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content` TEXT COMMENT '题干内容',
  `content_image_url` VARCHAR(255) COMMENT '题干图片URL',
  `category` VARCHAR(50) COMMENT '分类，如资料分析',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` BIGINT(20) COMMENT '创建者',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` BIGINT(20) COMMENT '更新者',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享题干表';
