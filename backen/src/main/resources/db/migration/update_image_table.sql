-- 2026-03-05 Update image table structure
ALTER TABLE image 
CHANGE file_name name VARCHAR(255) NOT NULL, 
CHANGE file_path path VARCHAR(255) NOT NULL, 
CHANGE content_type type VARCHAR(100) NOT NULL, 
CHANGE file_size size BIGINT NOT NULL, 
DROP COLUMN original_name, 
DROP COLUMN create_by, 
ADD COLUMN exam_record_id BIGINT;