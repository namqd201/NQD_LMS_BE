UPDATE courses SET grade_level = 'Lớp 1' WHERE id = 'acffcddf-ec74-4886-974b-39aee8be1f5e';
UPDATE questions SET grade_level = 'Lớp 1' WHERE grade_level = 'Tiểu học' OR course_id = 'acffcddf-ec74-4886-974b-39aee8be1f5e';
