-- 创建系统日志表
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作',
  `method` VARCHAR(255) DEFAULT NULL COMMENT '方法',
  `params` TEXT DEFAULT NULL COMMENT '参数',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `duration` INT(11) DEFAULT NULL COMMENT '耗时(ms)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_username` (`username`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';