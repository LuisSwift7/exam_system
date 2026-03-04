-- 向question表添加stem_id列，用于关联共享题干
ALTER TABLE `question` ADD COLUMN `stem_id` BIGINT(20) COMMENT '共享题干ID' AFTER `update_by`;
