ALTER TABLE wrong_book
ADD COLUMN note TEXT NULL COMMENT '学生错题笔记' AFTER practice_correct_count;
