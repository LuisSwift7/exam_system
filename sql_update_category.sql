-- 2026-02-25 Add category column to question table
ALTER TABLE question ADD COLUMN category VARCHAR(50) DEFAULT '其他' COMMENT '题目分类：言语理解、资料分析、判断推理、数量关系、常识判断';
