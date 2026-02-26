-- 创建班级表
CREATE TABLE `class` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '班级名称',
  `code` varchar(50) NOT NULL COMMENT '班级代码',
  `teacher_id` bigint(20) NOT NULL COMMENT '教师ID',
  `status` int(11) DEFAULT 1 COMMENT '状态：1正常，0禁用',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 创建班级学生关联表
CREATE TABLE `class_student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(20) NOT NULL COMMENT '班级ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID',
  `status` int(11) DEFAULT 0 COMMENT '状态：0待审核，1已加入，2已拒绝',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_by` bigint(20) DEFAULT NULL COMMENT '审批人',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_student` (`class_id`,`student_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生关联表';

-- 修改考试表，添加班级ID字段
ALTER TABLE `exam` ADD COLUMN `class_id` bigint(20) DEFAULT NULL COMMENT '班级ID' AFTER `update_by`;
ALTER TABLE `exam` ADD KEY `idx_class_id` (`class_id`);