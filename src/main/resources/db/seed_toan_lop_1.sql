-- ========================================================================
-- DATA SEED: KHÓA HỌC "TOÁN 1" CHO NQD-LMS
-- Bám sát chuẩn GDPT: 8 Chương, 34 Bài học, 102 Câu hỏi trắc nghiệm kèm giải thích
-- Database: PostgreSQL (ai_learning_platform)
-- ========================================================================

BEGIN;

-- 1. Ensure Subject "Toán Học" (code: MATH) exists
INSERT INTO subjects (id, name, code, description, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Toán Học',
    'MATH',
    'Nền tảng tư duy toán học, số học, hình học và giải tích từ Tiểu học đến Đại học.',
    'ACTIVE',
    now(),
    now()
)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = 'ACTIVE',
    updated_at = now();

DO $$
DECLARE
    v_subject_id UUID;
    v_course_id UUID;
    v_chapter_id UUID;
    v_lesson_id UUID;
    v_exercise_id UUID;
    v_question_id UUID;
BEGIN
    SELECT id INTO v_subject_id FROM subjects WHERE code = 'MATH' LIMIT 1;

    -- 2. Insert or update Course "Toán 1"
    SELECT id INTO v_course_id FROM courses WHERE code = 'MATH_GRADE_1' LIMIT 1;
    IF v_course_id IS NULL THEN
        v_course_id := gen_random_uuid();
        INSERT INTO courses (
            id, subject_id, name, code, description, grade_level,
            thumbnail_url, status, pricing_type, price, is_private, is_disabled,
            created_at, updated_at
        ) VALUES (
            v_course_id,
            v_subject_id,
            'Toán 1',
            'MATH_GRADE_1',
            'Khóa học Toán 1 chuẩn Chương trình GDPT mới. Cung cấp kiến thức số học 0-100, phép tính cộng trừ, hình phẳng và hình khối, đo độ dài cm, thời gian giờ và lịch.',
            'Lớp 1',
            '/images/courses/toan-1.jpg',
            'ACTIVE',
            'FREE',
            0,
            false,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE courses
        SET name = 'Toán 1',
            description = 'Khóa học Toán 1 chuẩn Chương trình GDPT mới. Cung cấp kiến thức số học 0-100, phép tính cộng trừ, hình phẳng và hình khối, đo độ dài cm, thời gian giờ và lịch.',
            grade_level = 'Lớp 1',
            status = 'ACTIVE',
            pricing_type = 'FREE',
            price = 0,
            is_private = false,
            is_disabled = false,
            updated_at = now()
        WHERE id = v_course_id;
    END IF;

    -- Clean up previous seeded chapters/lessons/exercises for this course to ensure clean idempotency
    -- Or update in-place

    -- ----------------------------------------------------
    -- CHƯƠNG 1: Chương 1: Các số từ 0 đến 10
    -- ----------------------------------------------------
    SELECT id INTO v_chapter_id FROM chapters WHERE course_id = v_course_id AND display_order = 1 LIMIT 1;
    IF v_chapter_id IS NULL THEN
        v_chapter_id := gen_random_uuid();
        INSERT INTO chapters (id, course_id, title, description, display_order, is_sellable, created_at, updated_at)
        VALUES (v_chapter_id, v_course_id, 'Chương 1: Các số từ 0 đến 10', 'Làm quen với môn Toán, nhận biết, đếm, đọc, viết và so sánh các số tự nhiên từ 0 đến 10, nắm vững cấu tạo số tách - gộp.', 1, false, now(), now());
    ELSE
        UPDATE chapters
        SET title = 'Chương 1: Các số từ 0 đến 10',
            description = 'Làm quen với môn Toán, nhận biết, đếm, đọc, viết và so sánh các số tự nhiên từ 0 đến 10, nắm vững cấu tạo số tách - gộp.',
            updated_at = now()
        WHERE id = v_chapter_id;
    END IF;

    -- Bài 1: Tiết học đầu tiên
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 1 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Tiết học đầu tiên',
            'tiet-hoc-dau-tien',
            'Làm quen với lớp học toán, đồ dùng học tập môn Toán và tư thế ngồi học đúng cách.',
            '### 🌟 Chào mừng các em đến với Tiết học Toán đầu tiên!

#### 1. Đồ dùng học Toán thân quen của em
Để học thật tốt môn Toán, chúng mình hãy cùng chuẩn bị các bạn nhỏ này nhé:
- 📖 **Sách giáo khoa Toán 1**: Người bạn mang đến bao điều thú vị và tranh vẽ rực rỡ.
- 📓 **Vở ô li**: Nơi chúng mình viết những chữ số xinh xắn.
- ✏️ **Bút chì & cục tẩy**: Giúp bé viết số tròn trịa và tẩy sạch khi cần sửa.
- 📏 **Thước kẻ**: Dùng để kẻ thẳng hàng và đo độ dài các đồ vật.
- 📦 **Bộ đồ dùng Toán 1**: Chứa các que tính sắc màu 🥢 và các khối hình học vuông, tròn, tam giác.

#### 2. Tư thế ngồi học chuẩn và giữ gìn mắt sáng
- 🪑 **Lưng thẳng**: Không tì ngực vào cạnh bàn.
- 👀 **Khoảng cách**: Mắt cách trang sách, vở từ 25 cm đến 30 cm.
- 💡 **Ánh sáng**: Ngồi học nơi có đủ ánh sáng tự nhiên hoặc đèn bàn sáng rõ.

#### 3. Cùng nhau khám phá thế giới số học
Toán học có ở khắp mọi nơi quanh em:
- Đếm số ngón tay trên một bàn tay: 🖐️ có **5** ngón tay.
- Đếm số bánh xe của chiếc xe đạp: 🚲 có **2** bánh xe.
- Hãy cùng mở sách và bắt đầu chuyến hành trình kỳ thú nào! 🚀',
            1,
            35,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Tiết học đầu tiên',
            slug = 'tiet-hoc-dau-tien',
            summary = 'Làm quen với lớp học toán, đồ dùng học tập môn Toán và tư thế ngồi học đúng cách.',
            content = '### 🌟 Chào mừng các em đến với Tiết học Toán đầu tiên!

#### 1. Đồ dùng học Toán thân quen của em
Để học thật tốt môn Toán, chúng mình hãy cùng chuẩn bị các bạn nhỏ này nhé:
- 📖 **Sách giáo khoa Toán 1**: Người bạn mang đến bao điều thú vị và tranh vẽ rực rỡ.
- 📓 **Vở ô li**: Nơi chúng mình viết những chữ số xinh xắn.
- ✏️ **Bút chì & cục tẩy**: Giúp bé viết số tròn trịa và tẩy sạch khi cần sửa.
- 📏 **Thước kẻ**: Dùng để kẻ thẳng hàng và đo độ dài các đồ vật.
- 📦 **Bộ đồ dùng Toán 1**: Chứa các que tính sắc màu 🥢 và các khối hình học vuông, tròn, tam giác.

#### 2. Tư thế ngồi học chuẩn và giữ gìn mắt sáng
- 🪑 **Lưng thẳng**: Không tì ngực vào cạnh bàn.
- 👀 **Khoảng cách**: Mắt cách trang sách, vở từ 25 cm đến 30 cm.
- 💡 **Ánh sáng**: Ngồi học nơi có đủ ánh sáng tự nhiên hoặc đèn bàn sáng rõ.

#### 3. Cùng nhau khám phá thế giới số học
Toán học có ở khắp mọi nơi quanh em:
- Đếm số ngón tay trên một bàn tay: 🖐️ có **5** ngón tay.
- Đếm số bánh xe của chiếc xe đạp: 🚲 có **2** bánh xe.
- Hãy cùng mở sách và bắt đầu chuyến hành trình kỳ thú nào! 🚀',
            estimated_minutes = 35,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 1
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Tiết học đầu tiên',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Tiết học đầu tiên',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Tiết học đầu tiên',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Đâu là đồ dùng học tập cần thiết để kẻ đường thẳng trong môn Toán?',
        'Thước kẻ giúp chúng mình kẻ những đường thẳng thật đẹp và chính xác đấy các em!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Cục tẩy', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Thước kẻ', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hộp màu sáp', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Cặp sách', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tư thế ngồi học nào sau đây là ĐÚNG để bảo vệ cột sống và mắt?',
        'Ngồi thẳng lưng giúp cột sống phát triển khỏe mạnh và mắt luôn sáng tinh anh!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Nằm ra bàn khi viết bài', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Cúi sát mắt vào trang vở', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Ngồi thẳng lưng, mắt cách vở khoảng 25 - 30 cm', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Tì ngực sát vào cạnh bàn', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Một bàn tay của em có bao nhiêu ngón tay? 🖐️',
        'Bé hãy xòe một bàn tay và đếm cùng cô nhé: 1, 2, 3, 4, 5. Đúng rồi, có 5 ngón tay!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 3 ngón tay', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 4 ngón tay', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 5 ngón tay', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 6 ngón tay', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 2: Bài 1: Các số 0, 1, 2, 3, 4, 5
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 2 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 1: Các số 0, 1, 2, 3, 4, 5',
            'bai-1-cac-so-0-1-2-3-4-5',
            'Đếm, đọc, viết và nhận biết số lượng các nhóm đồ vật từ 0 đến 5.',
            '### 🔢 Các số 0, 1, 2, 3, 4, 5

#### 1. Đếm và nhận biết đồ vật
Hãy cùng đếm các đồ vật xinh xắn nào:
- **Số 0**: Đĩa không có quả táo nào 🍽️ -> **0 quả táo**.
- **Số 1**: Một chú mèo con 🐱 -> Số **1**.
- **Số 2**: Hai chú cún con 🐶🐶 -> Số **2**.
- **Số 3**: Ba bông hoa hồng 🌸🌸🌸 -> Số **3**.
- **Số 4**: Bốn chiếc ô tô đồ chơi 🚗🚗🚗🚗 -> Số **4**.
- **Số 5**: Năm quả bóng bay rực rỡ 🎈🎈🎈🎈🎈 -> Số **5**.

#### 2. Thứ tự các số từ 0 đến 5
- Dãy số tăng dần: **0, 1, 2, 3, 4, 5**
- Dãy số đếm lùi: **5, 4, 3, 2, 1, 0**

#### 3. Bé ghi nhớ:
- **0** là không có gì cả.
- Số đứng liền sau luôn nhiều hơn số đứng liền trước 1 đơn vị.',
            2,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 1: Các số 0, 1, 2, 3, 4, 5',
            slug = 'bai-1-cac-so-0-1-2-3-4-5',
            summary = 'Đếm, đọc, viết và nhận biết số lượng các nhóm đồ vật từ 0 đến 5.',
            content = '### 🔢 Các số 0, 1, 2, 3, 4, 5

#### 1. Đếm và nhận biết đồ vật
Hãy cùng đếm các đồ vật xinh xắn nào:
- **Số 0**: Đĩa không có quả táo nào 🍽️ -> **0 quả táo**.
- **Số 1**: Một chú mèo con 🐱 -> Số **1**.
- **Số 2**: Hai chú cún con 🐶🐶 -> Số **2**.
- **Số 3**: Ba bông hoa hồng 🌸🌸🌸 -> Số **3**.
- **Số 4**: Bốn chiếc ô tô đồ chơi 🚗🚗🚗🚗 -> Số **4**.
- **Số 5**: Năm quả bóng bay rực rỡ 🎈🎈🎈🎈🎈 -> Số **5**.

#### 2. Thứ tự các số từ 0 đến 5
- Dãy số tăng dần: **0, 1, 2, 3, 4, 5**
- Dãy số đếm lùi: **5, 4, 3, 2, 1, 0**

#### 3. Bé ghi nhớ:
- **0** là không có gì cả.
- Số đứng liền sau luôn nhiều hơn số đứng liền trước 1 đơn vị.',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 2
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 1: Các số 0, 1, 2, 3, 4, 5',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 1: Các số 0, 1, 2, 3, 4, 5',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 1: Các số 0, 1, 2, 3, 4, 5',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Bé hãy đếm xem có bao nhiêu quả táo ở đây: 🍎🍎🍎',
        'Chúng mình cùng đếm nhé: một, hai, ba. Vậy có tất cả 3 quả táo!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 2 quả táo', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 3 quả táo', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 4 quả táo', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 5 quả táo', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số nào đứng liền sau số 3 trong dãy số tự nhiên?',
        'Trong dãy số 0, 1, 2, 3, 4, 5, đứng ngay sau số 3 chính là số 4.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Số 2', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Số 4', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Số 5', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Số 1', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Trong một chiếc rổ không có đồ chơi nào, ta dùng số nào để chỉ số lượng đồ chơi trong rổ?',
        'Khi không có đồ vật nào, ta dùng số 0 để biểu thị số lượng.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Số 1', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Số 0', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Số 2', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Số 5', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 3: Bài 2: Các số 6, 7, 8, 9, 10
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 3 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 2: Các số 6, 7, 8, 9, 10',
            'bai-2-cac-so-6-7-8-9-10',
            'Đếm, đọc, viết các số từ 6 đến 10, đặc biệt làm quen với số 10 tròn trĩnh.',
            '### 🔟 Các số 6, 7, 8, 9, 10

#### 1. Đếm tiếp từ 6 đến 10
Khi đã có 5 ngón tay trên một bàn tay, ta mở tiếp các ngón tay ở bàn tay kia:
- **Số 6**: 5 ngón tay thêm 1 ngón tay 🖐️☝️ -> **6**. (Ví dụ: 6 chú chim 🐦🐦🐦🐦🐦🐦)
- **Số 7**: 5 ngón tay thêm 2 ngón tay 🖐️✌️ -> **7**. (Ví dụ: 7 ngôi sao ⭐⭐⭐⭐⭐⭐⭐)
- **Số 8**: 5 ngón tay thêm 3 ngón tay -> **8**. (Ví dụ: 8 cây kem 🍦🍦🍦🍦🍦🍦🍦🍦)
- **Số 9**: 5 ngón tay thêm 4 ngón tay -> **9**. (Ví dụ: 9 chiếc kẹo 🍬🍬🍬🍬🍬🍬🍬🍬🍬)
- **Số 10**: Cả hai bàn tay xòe ra 🖐️🖐️ -> **10 ngón tay**! Số 10 gồm chữ số 1 đứng trước và chữ số 0 đứng sau.

#### 2. Dãy số từ 0 đến 10 hoàn chỉnh
- Đếm xuôi: **0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10**
- Đếm ngược: **10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0**',
            3,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 2: Các số 6, 7, 8, 9, 10',
            slug = 'bai-2-cac-so-6-7-8-9-10',
            summary = 'Đếm, đọc, viết các số từ 6 đến 10, đặc biệt làm quen với số 10 tròn trĩnh.',
            content = '### 🔟 Các số 6, 7, 8, 9, 10

#### 1. Đếm tiếp từ 6 đến 10
Khi đã có 5 ngón tay trên một bàn tay, ta mở tiếp các ngón tay ở bàn tay kia:
- **Số 6**: 5 ngón tay thêm 1 ngón tay 🖐️☝️ -> **6**. (Ví dụ: 6 chú chim 🐦🐦🐦🐦🐦🐦)
- **Số 7**: 5 ngón tay thêm 2 ngón tay 🖐️✌️ -> **7**. (Ví dụ: 7 ngôi sao ⭐⭐⭐⭐⭐⭐⭐)
- **Số 8**: 5 ngón tay thêm 3 ngón tay -> **8**. (Ví dụ: 8 cây kem 🍦🍦🍦🍦🍦🍦🍦🍦)
- **Số 9**: 5 ngón tay thêm 4 ngón tay -> **9**. (Ví dụ: 9 chiếc kẹo 🍬🍬🍬🍬🍬🍬🍬🍬🍬)
- **Số 10**: Cả hai bàn tay xòe ra 🖐️🖐️ -> **10 ngón tay**! Số 10 gồm chữ số 1 đứng trước và chữ số 0 đứng sau.

#### 2. Dãy số từ 0 đến 10 hoàn chỉnh
- Đếm xuôi: **0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10**
- Đếm ngược: **10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0**',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 3
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 2: Các số 6, 7, 8, 9, 10',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 2: Các số 6, 7, 8, 9, 10',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 2: Các số 6, 7, 8, 9, 10',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Bé hãy đếm xem có bao nhiêu ngôi sao: ⭐⭐⭐⭐⭐⭐⭐',
        'Đếm lần lượt từng ngôi sao: 1, 2, 3, 4, 5, 6, 7. Có tất cả 7 ngôi sao!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 6 ngôi sao', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 7 ngôi sao', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 8 ngôi sao', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 9 ngôi sao', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số mười (10) được viết bởi những chữ số nào?',
        'Số 10 được viết bằng chữ số 1 ở hàng chục và chữ số 0 ở hàng đơn vị: số 1 đứng trước, số 0 đứng sau.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Chữ số 0 và chữ số 1', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Chữ số 1 đứng trước, chữ số 0 đứng sau', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hai chữ số 1', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Chỉ có chữ số 1', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số nào lớn nhất trong các số sau: 5, 8, 3, 7?',
        'Khi đếm từ 0 đến 10, số 8 xuất hiện sau cùng trong nhóm nên số 8 là số lớn nhất.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Số 5', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Số 3', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Số 7', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Số 8', true, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 4: Bài 3: Nhiều hơn, ít hơn, bằng nhau
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 4 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 3: Nhiều hơn, ít hơn, bằng nhau',
            'bai-3-nhieu-hon-it-hon-bang-nhau',
            'So sánh số lượng đồ vật của hai nhóm bằng phương pháp ghép cặp một - một.',
            '### ⚖️ Nhiều hơn, ít hơn, bằng nhau

#### 1. Cách so sánh ghép đôi (1 - 1)
Để biết nhóm nào có nhiều hơn hay ít hơn, chúng mình nối từng đồ vật nhóm này với một đồ vật nhóm kia:
- **Nhiều hơn**: Nếu còn thừa đồ vật thì nhóm đó **nhiều hơn**.
  - Ví dụ: Có 4 chú thỏ 🐰🐰🐰🐰 và 3 củ cà rốt 🥕🥕🥕. Mỗi chú thỏ cầm 1 củ cà rốt, còn thừa 1 chú thỏ chưa có cà rốt.
  - Ta nói: *Số chú thỏ nhiều hơn số củ cà rốt*.
- **Ít hơn**: Nếu thiếu đồ vật thì nhóm đó **ít hơn**.
  - Ta nói: *Số củ cà rốt ít hơn số chú thỏ*.
- **Bằng nhau**: Nếu ghép đôi vừa vặn, không thừa không thiếu đồ vật nào.
  - Ví dụ: 3 chú mèo 🐱🐱🐱 và 3 con cá 🐟🐟🐟.
  - Ta nói: *Số chú mèo bằng số con cá*.',
            4,
            35,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 3: Nhiều hơn, ít hơn, bằng nhau',
            slug = 'bai-3-nhieu-hon-it-hon-bang-nhau',
            summary = 'So sánh số lượng đồ vật của hai nhóm bằng phương pháp ghép cặp một - một.',
            content = '### ⚖️ Nhiều hơn, ít hơn, bằng nhau

#### 1. Cách so sánh ghép đôi (1 - 1)
Để biết nhóm nào có nhiều hơn hay ít hơn, chúng mình nối từng đồ vật nhóm này với một đồ vật nhóm kia:
- **Nhiều hơn**: Nếu còn thừa đồ vật thì nhóm đó **nhiều hơn**.
  - Ví dụ: Có 4 chú thỏ 🐰🐰🐰🐰 và 3 củ cà rốt 🥕🥕🥕. Mỗi chú thỏ cầm 1 củ cà rốt, còn thừa 1 chú thỏ chưa có cà rốt.
  - Ta nói: *Số chú thỏ nhiều hơn số củ cà rốt*.
- **Ít hơn**: Nếu thiếu đồ vật thì nhóm đó **ít hơn**.
  - Ta nói: *Số củ cà rốt ít hơn số chú thỏ*.
- **Bằng nhau**: Nếu ghép đôi vừa vặn, không thừa không thiếu đồ vật nào.
  - Ví dụ: 3 chú mèo 🐱🐱🐱 và 3 con cá 🐟🐟🐟.
  - Ta nói: *Số chú mèo bằng số con cá*.',
            estimated_minutes = 35,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 4
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 3: Nhiều hơn, ít hơn, bằng nhau',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 3: Nhiều hơn, ít hơn, bằng nhau',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 3: Nhiều hơn, ít hơn, bằng nhau',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Có 5 bạn học sinh và 4 chiếc ghế. Kết luận nào sau đây là ĐÚNG?',
        'Mỗi bạn ngồi 1 chiếc ghế, có 4 chiếc ghế nên vẫn còn thừa 1 bạn chưa có ghế. Vậy số học sinh nhiều hơn số ghế!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Số học sinh bằng số ghế', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Số học sinh nhiều hơn số ghế', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Số học sinh ít hơn số ghế', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Không so sánh được', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Có 3 con bướm 🦋🦋🦋 và 3 bông hoa 🌸🌸🌸. Ta nói số con bướm và số bông hoa như thế nào?',
        'Mỗi chú bướm đậu trên một bông hoa vừa vặn, không thừa chú bướm hay bông hoa nào, nên hai nhóm bằng nhau.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Nhiều hơn', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Ít hơn', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Bằng nhau', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Khác nhau', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Có 2 chiếc thìa 🥄🥄 và 4 bát súp 🥣🥣🥣🥣. Số chiếc thìa như thế nào so với số bát súp?',
        'Vì chỉ có 2 chiếc thìa mà có tới 4 bát súp nên số chiếc thìa ít hơn số bát súp.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Nhiều hơn', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Ít hơn', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Bằng nhau', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Gấp đôi', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 5: Bài 4: So sánh số
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 5 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 4: So sánh số',
            'bai-4-so-sanh-so',
            'Làm quen với các dấu so sánh: dấu lớn hơn (>), dấu bé hơn (<), dấu bằng (=).',
            '### 🔍 So sánh số: >, <, =

#### 1. Làm quen với các dấu toán học
- **Dấu lớn hơn ( > )**: Miệng cá sấu mở rộng về phía số lớn hơn! 🐊
  - Ví dụ: 5 lớn hơn 3, viết là: **5 > 3**
- **Dấu bé hơn ( < )**: Mũi nhọn quay về phía số bé hơn!
  - Ví dụ: 2 bé hơn 4, viết là: **2 < 4**
- **Dấu bằng ( = )**: Hai nét ngang song song bằng nhau!
  - Ví dụ: 4 bằng 4, viết là: **4 = 4**

#### 2. Mẹo nhớ siêu dễ cho bé
- "Đầu nhọn chỉ số bé, miệng to há về số lớn!"
- Dựa vào dãy số 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10:
  - Số nào đứng trước thì bé hơn.
  - Số nào đứng sau thì lớn hơn.',
            5,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 4: So sánh số',
            slug = 'bai-4-so-sanh-so',
            summary = 'Làm quen với các dấu so sánh: dấu lớn hơn (>), dấu bé hơn (<), dấu bằng (=).',
            content = '### 🔍 So sánh số: >, <, =

#### 1. Làm quen với các dấu toán học
- **Dấu lớn hơn ( > )**: Miệng cá sấu mở rộng về phía số lớn hơn! 🐊
  - Ví dụ: 5 lớn hơn 3, viết là: **5 > 3**
- **Dấu bé hơn ( < )**: Mũi nhọn quay về phía số bé hơn!
  - Ví dụ: 2 bé hơn 4, viết là: **2 < 4**
- **Dấu bằng ( = )**: Hai nét ngang song song bằng nhau!
  - Ví dụ: 4 bằng 4, viết là: **4 = 4**

#### 2. Mẹo nhớ siêu dễ cho bé
- "Đầu nhọn chỉ số bé, miệng to há về số lớn!"
- Dựa vào dãy số 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10:
  - Số nào đứng trước thì bé hơn.
  - Số nào đứng sau thì lớn hơn.',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 5
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 4: So sánh số',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 4: So sánh số',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 4: So sánh số',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Điền dấu thích hợp vào chỗ chấm: 7 ... 4',
        'Số 7 đứng sau số 4 trong dãy số nên 7 lớn hơn 4, ta điền dấu lớn hơn (>).',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. >', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. <', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. =', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. +', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Phép so sánh nào dưới đây là ĐÚNG?',
        'Số 2 đứng trước số 9 nên 2 bé hơn 9 (2 < 9) là đáp án hoàn toàn chính xác.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 8 < 5', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 3 > 6', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 2 < 9', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 10 < 7', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số nào thích hợp điền vào ô trống: [ ? ] = 6',
        'Để dấu bằng (=) đúng thì hai bên phải có giá trị như nhau: 6 = 6.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 5', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 6', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 7', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 0', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 6: Bài 5: Mấy và mấy
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 6 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 5: Mấy và mấy',
            'bai-5-may-va-may',
            'Cấu tạo số: tách một nhóm thành hai phần và gộp hai phần thành một nhóm.',
            '### 🧩 Mấy và mấy (Tách - Gộp số)

#### 1. Thao tác Tách số
- Bé có 4 quả cam 🍊🍊🍊🍊. Bé chia cho mẹ 1 quả 🍊, bé giữ lại 3 quả 🍊🍊🍊.
  - Ta nói: **4 gồm 1 và 3** (hoặc **4 gồm 3 và 1**).
- Bé có thể chia cho mẹ 2 quả 🍊🍊 và bé 2 quả 🍊🍊.
  - Ta nói: **4 gồm 2 và 2**.

#### 2. Thao tác Gộp số
- Tay trái bé cầm 2 viên kẹo 🍬🍬, tay phải bé cầm 3 viên kẹo 🍬🍬🍬. Khi gộp lại vào hai tay, bé có tất cả 5 viên kẹo.
  - Ta nói: **Gộp 2 và 3 được 5**.

#### 3. Ý nghĩa quan trọng:
Học tốt bài "Mấy và mấy" chính là chiếc chìa khóa vàng 🔑 giúp chúng mình tính cộng và trừ cực nhanh ở chương sau!',
            6,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 5: Mấy và mấy',
            slug = 'bai-5-may-va-may',
            summary = 'Cấu tạo số: tách một nhóm thành hai phần và gộp hai phần thành một nhóm.',
            content = '### 🧩 Mấy và mấy (Tách - Gộp số)

#### 1. Thao tác Tách số
- Bé có 4 quả cam 🍊🍊🍊🍊. Bé chia cho mẹ 1 quả 🍊, bé giữ lại 3 quả 🍊🍊🍊.
  - Ta nói: **4 gồm 1 và 3** (hoặc **4 gồm 3 và 1**).
- Bé có thể chia cho mẹ 2 quả 🍊🍊 và bé 2 quả 🍊🍊.
  - Ta nói: **4 gồm 2 và 2**.

#### 2. Thao tác Gộp số
- Tay trái bé cầm 2 viên kẹo 🍬🍬, tay phải bé cầm 3 viên kẹo 🍬🍬🍬. Khi gộp lại vào hai tay, bé có tất cả 5 viên kẹo.
  - Ta nói: **Gộp 2 và 3 được 5**.

#### 3. Ý nghĩa quan trọng:
Học tốt bài "Mấy và mấy" chính là chiếc chìa khóa vàng 🔑 giúp chúng mình tính cộng và trừ cực nhanh ở chương sau!',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 6
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 5: Mấy và mấy',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 5: Mấy và mấy',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 5: Mấy và mấy',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        '5 gồm 2 và mấy?',
        'Có 5 que tính, tách ra 2 que thì còn lại 3 que. Vậy 5 gồm 2 và 3!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 1', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 2', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 3', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 4', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Gộp 4 và 1 được mấy?',
        'Có 4 quả bóng, gộp thêm 1 quả bóng nữa ta được tất cả 5 quả bóng.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 3', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 4', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 5', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 6', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số 6 gồm những cặp số nào sau đây gộp lại?',
        'Gộp 4 và 2 ta đếm: 4... 5, 6. Như vậy 6 gồm 4 và 2.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 3 và 2', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 4 và 2', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 5 và 2', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 1 và 4', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 7: Bài 6: Luyện tập chung
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 7 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 6: Luyện tập chung',
            'bai-6-luyen-tap-chung',
            'Tổng hợp kỹ năng đếm, so sánh và tách gộp các số từ 0 đến 10.',
            '### 🎯 Luyện tập chung Chương 1

#### 1. Nhìn lại kiến thức đã học
Cùng nhau điểm lại những hành trang tuyệt vời của Chương 1 nào:
- 🔢 Các số từ **0 đến 10**: Nhận biết mặt số, đếm xuôi, đếm ngược thành thạo.
- ⚖️ So sánh số lượng: Biết dùng từ **nhiều hơn**, **ít hơn**, **bằng nhau**.
- 🐊 Dấu so sánh: Sử dụng thành thạo dấu lớn **>**, dấu bé **<**, dấu bằng **=**.
- 🧩 Tách và gộp số: Hiểu sâu sắc bản chất số để chuẩn bị học phép tính cộng trừ.

#### 2. Thử thách tài năng nhí
- Đếm nhanh số đồ vật trong phòng ngủ hoặc phòng khách.
- Xếp que tính từ 1 đến 10 thành các hàng đều tăm tắp!',
            7,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 6: Luyện tập chung',
            slug = 'bai-6-luyen-tap-chung',
            summary = 'Tổng hợp kỹ năng đếm, so sánh và tách gộp các số từ 0 đến 10.',
            content = '### 🎯 Luyện tập chung Chương 1

#### 1. Nhìn lại kiến thức đã học
Cùng nhau điểm lại những hành trang tuyệt vời của Chương 1 nào:
- 🔢 Các số từ **0 đến 10**: Nhận biết mặt số, đếm xuôi, đếm ngược thành thạo.
- ⚖️ So sánh số lượng: Biết dùng từ **nhiều hơn**, **ít hơn**, **bằng nhau**.
- 🐊 Dấu so sánh: Sử dụng thành thạo dấu lớn **>**, dấu bé **<**, dấu bằng **=**.
- 🧩 Tách và gộp số: Hiểu sâu sắc bản chất số để chuẩn bị học phép tính cộng trừ.

#### 2. Thử thách tài năng nhí
- Đếm nhanh số đồ vật trong phòng ngủ hoặc phòng khách.
- Xếp que tính từ 1 đến 10 thành các hàng đều tăm tắp!',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 7
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 6: Luyện tập chung',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 6: Luyện tập chung',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 6: Luyện tập chung',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Dãy số nào sau đây được sắp xếp theo thứ tự từ bé đến lớn?',
        'Dãy 0, 2, 5, 8, 10 đi từ số bé đến số lớn dần theo đúng thứ tự đếm.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 1, 4, 3, 7, 9', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 0, 2, 5, 8, 10', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 10, 8, 6, 4, 2', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 3, 5, 2, 7, 8', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Cho ba số: 3, 8, 6. Số bé nhất và số lớn nhất lần lượt là:',
        'Trong ba số 3, 8, 6 thì số 3 nhỏ nhất và số 8 lớn nhất.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Bé nhất là 3, lớn nhất là 6', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Bé nhất là 6, lớn nhất là 8', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Bé nhất là 3, lớn nhất là 8', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Bé nhất là 8, lớn nhất là 3', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Gộp 3 chú gấu 🐻🐻🐻 và 4 chú thỏ 🐰🐰🐰🐰 thì có tất cả bao nhiêu con vật?',
        'Gộp 3 và 4 được 7. Có tất cả 7 con vật đáng yêu!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 6 con vật', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 7 con vật', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 8 con vật', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 9 con vật', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- ----------------------------------------------------
    -- CHƯƠNG 2: Chương 2: Làm quen với một số hình phẳng
    -- ----------------------------------------------------
    SELECT id INTO v_chapter_id FROM chapters WHERE course_id = v_course_id AND display_order = 2 LIMIT 1;
    IF v_chapter_id IS NULL THEN
        v_chapter_id := gen_random_uuid();
        INSERT INTO chapters (id, course_id, title, description, display_order, is_sellable, created_at, updated_at)
        VALUES (v_chapter_id, v_course_id, 'Chương 2: Làm quen với một số hình phẳng', 'Nhận biết và gọi đúng tên các hình phẳng cơ bản: hình vuông, hình tròn, hình tam giác, hình chữ nhật; thực hành xếp hình sáng tạo.', 2, false, now(), now());
    ELSE
        UPDATE chapters
        SET title = 'Chương 2: Làm quen với một số hình phẳng',
            description = 'Nhận biết và gọi đúng tên các hình phẳng cơ bản: hình vuông, hình tròn, hình tam giác, hình chữ nhật; thực hành xếp hình sáng tạo.',
            updated_at = now()
        WHERE id = v_chapter_id;
    END IF;

    -- Bài 8: Bài 7: Hình vuông, hình tròn, hình tam giác, hình chữ nhật
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 8 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 7: Hình vuông, hình tròn, hình tam giác, hình chữ nhật',
            'bai-7-hinh-vuong-hinh-tron-hinh-tam-giac-hinh-chu-nhat',
            'Nhận biết đặc điểm trực quan của 4 hình phẳng cơ bản qua đồ vật quen thuộc.',
            '### 🔴🔺🟦 Làm quen với các hình phẳng cơ bản

#### 1. Nhận biết 4 hình phẳng
- 🔴 **Hình tròn**: Tròn xoe, không có góc, có thể lăn được dễ dàng.
  - Đồ vật quanh em: Chiếc đĩa tròn 🍽️, mặt đồng hồ treo tường ⏰, nắp chai nước.
- 🔺 **Hình tam giác**: Có 3 cạnh và 3 góc nhọn.
  - Đồ vật quanh em: Chiếc thước eke, lá cờ thi đua 🚩, miếng bánh pizza cắt lát 🍕.
- 🟦 **Hình vuông**: Có 4 cạnh dài bằng nhau và 4 góc vuông vức.
  - Đồ vật quanh em: Chiếc bánh chưng ngày Tết, viên gạch hoa lát nền, mặt con xúc xắc 🎲.
- 🟨 **Hình chữ nhật**: Có 4 cạnh (2 cạnh dài bằng nhau, 2 cạnh ngắn bằng nhau).
  - Đồ vật quanh em: Bìa sách giáo khoa 📖, chiếc bảng lớp học, cánh cửa ra vào 🚪.',
            8,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 7: Hình vuông, hình tròn, hình tam giác, hình chữ nhật',
            slug = 'bai-7-hinh-vuong-hinh-tron-hinh-tam-giac-hinh-chu-nhat',
            summary = 'Nhận biết đặc điểm trực quan của 4 hình phẳng cơ bản qua đồ vật quen thuộc.',
            content = '### 🔴🔺🟦 Làm quen với các hình phẳng cơ bản

#### 1. Nhận biết 4 hình phẳng
- 🔴 **Hình tròn**: Tròn xoe, không có góc, có thể lăn được dễ dàng.
  - Đồ vật quanh em: Chiếc đĩa tròn 🍽️, mặt đồng hồ treo tường ⏰, nắp chai nước.
- 🔺 **Hình tam giác**: Có 3 cạnh và 3 góc nhọn.
  - Đồ vật quanh em: Chiếc thước eke, lá cờ thi đua 🚩, miếng bánh pizza cắt lát 🍕.
- 🟦 **Hình vuông**: Có 4 cạnh dài bằng nhau và 4 góc vuông vức.
  - Đồ vật quanh em: Chiếc bánh chưng ngày Tết, viên gạch hoa lát nền, mặt con xúc xắc 🎲.
- 🟨 **Hình chữ nhật**: Có 4 cạnh (2 cạnh dài bằng nhau, 2 cạnh ngắn bằng nhau).
  - Đồ vật quanh em: Bìa sách giáo khoa 📖, chiếc bảng lớp học, cánh cửa ra vào 🚪.',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 8
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 7: Hình vuông, hình tròn, hình tam giác, hình chữ nhật',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 7: Hình vuông, hình tròn, hình tam giác, hình chữ nhật',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 7: Hình vuông, hình tròn, hình tam giác, hình chữ nhật',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Mặt chiếc đồng hồ tròn xoe hoặc nắp chai nước có dạng hình gì?',
        'Mặt đồng hồ tròn xoe và nắp chai có đường viền cong tròn khép kín, chính là hình tròn!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Hình vuông', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Hình tròn', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình tam giác', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình chữ nhật', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Lá cờ thi đua có 3 cạnh nhọn có dạng hình gì? 🚩',
        'Lá cờ có 3 cạnh và 3 đỉnh nhọn chính là hình tam giác.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Hình tròn', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Hình vuông', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình tam giác', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình chữ nhật', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Bìa cuốn sách Toán 1 hoặc cánh cửa phòng học có dạng hình gì?',
        'Bìa sách và cánh cửa có 2 cạnh dài và 2 cạnh ngắn, đó là hình chữ nhật.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Hình tròn', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Hình chữ nhật', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình vuông', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình tam giác', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 9: Bài 8: Thực hành lắp ghép, xếp hình
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 9 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 8: Thực hành lắp ghép, xếp hình',
            'bai-8-thuc-hanh-lap-ghep-xep-hinh',
            'Sử dụng các miếng ghép hình phẳng để sáng tạo nên ngôi nhà, con thuyền, ô tô, cây cối.',
            '### 🏠⛵ Thực hành lắp ghép, xếp hình sáng tạo

#### 1. Ghép các hình đơn giản thành hình mới
- Ghép **2 hình tam giác** giống nhau có thể tạo thành một **hình vuông** hoặc một **hình tam giác lớn hơn**!
- Ghép **1 hình tam giác** (làm mái nhà 🔺) lên trên **1 hình vuông** (làm thân nhà 🟦) ta được một **ngôi nhà ấm cúng** 🏠!

#### 2. Xếp tranh từ bộ đồ dùng Toán
- **Thuyền buồm trên biển ⛵**: Thân thuyền ghép từ hình chữ nhật, cánh buồm ghép từ 2 hình tam giác.
- **Cây xanh tốt 🌲**: Thân cây là hình chữ nhật nâu, tán cây là 3 hình tam giác xếp tầng.
- **Mặt trời tỏa nắng ☀️**: Một hình tròn vàng ở giữa và các que tính xung quanh làm tia nắng.',
            9,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 8: Thực hành lắp ghép, xếp hình',
            slug = 'bai-8-thuc-hanh-lap-ghep-xep-hinh',
            summary = 'Sử dụng các miếng ghép hình phẳng để sáng tạo nên ngôi nhà, con thuyền, ô tô, cây cối.',
            content = '### 🏠⛵ Thực hành lắp ghép, xếp hình sáng tạo

#### 1. Ghép các hình đơn giản thành hình mới
- Ghép **2 hình tam giác** giống nhau có thể tạo thành một **hình vuông** hoặc một **hình tam giác lớn hơn**!
- Ghép **1 hình tam giác** (làm mái nhà 🔺) lên trên **1 hình vuông** (làm thân nhà 🟦) ta được một **ngôi nhà ấm cúng** 🏠!

#### 2. Xếp tranh từ bộ đồ dùng Toán
- **Thuyền buồm trên biển ⛵**: Thân thuyền ghép từ hình chữ nhật, cánh buồm ghép từ 2 hình tam giác.
- **Cây xanh tốt 🌲**: Thân cây là hình chữ nhật nâu, tán cây là 3 hình tam giác xếp tầng.
- **Mặt trời tỏa nắng ☀️**: Một hình tròn vàng ở giữa và các que tính xung quanh làm tia nắng.',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 9
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 8: Thực hành lắp ghép, xếp hình',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 8: Thực hành lắp ghép, xếp hình',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 8: Thực hành lắp ghép, xếp hình',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Để ghép thành mái nhà của một ngôi nhà, em thường dùng hình nào? 🏠',
        'Mái nhà nhọn và dốc sang hai bên thường được xếp bằng một hình tam giác.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Hình tam giác', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Hình tròn', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình chữ nhật', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình thoi', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Em có thể ghép hai hình tam giác vuông bằng nhau để tạo thành hình nào?',
        'Hai hình tam giác vuông cân khi ghép cạnh huyền vào nhau sẽ tạo thành một hình vuông thật đẹp.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Hình tròn', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Hình vuông', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình ngôi sao', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình cầu', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Khi vẽ chiếc xe ô tô tải đồ chơi 🚚, bánh xe thường có dạng hình gì?',
        'Bánh xe phải có dạng hình tròn để có thể lăn tròn và xe chạy bon bon được!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Hình tam giác', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Hình vuông', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình tròn', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình chữ nhật', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 10: Bài 9: Luyện tập chung
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 10 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 9: Luyện tập chung',
            'bai-9-luyen-tap-chung',
            'Đếm số lượng từng loại hình trong một bức tranh và củng cố cách phân biệt các hình phẳng.',
            '### 🎨 Luyện tập chung: Thế giới hình phẳng

#### 1. Kỹ năng quan sát và đếm hình không bị sót
- Khi đếm hình trong tranh, bé hãy đánh dấu nhẹ bằng bút chì:
  - Đếm hết tất cả các **hình tròn** trước.
  - Rồi đếm đến tất cả các **hình tam giác**.
  - Tiếp theo là các **hình vuông**.
  - Cuối cùng là các **hình chữ nhật**.

#### 2. Phân biệt kỹ: Hình vuông và Hình chữ nhật
- **Hình vuông**: 4 cạnh phải bằng nhau như khuôn bánh chưng.
- **Hình chữ nhật**: Có 2 cạnh dài và 2 cạnh ngắn rõ rệt.',
            10,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 9: Luyện tập chung',
            slug = 'bai-9-luyen-tap-chung',
            summary = 'Đếm số lượng từng loại hình trong một bức tranh và củng cố cách phân biệt các hình phẳng.',
            content = '### 🎨 Luyện tập chung: Thế giới hình phẳng

#### 1. Kỹ năng quan sát và đếm hình không bị sót
- Khi đếm hình trong tranh, bé hãy đánh dấu nhẹ bằng bút chì:
  - Đếm hết tất cả các **hình tròn** trước.
  - Rồi đếm đến tất cả các **hình tam giác**.
  - Tiếp theo là các **hình vuông**.
  - Cuối cùng là các **hình chữ nhật**.

#### 2. Phân biệt kỹ: Hình vuông và Hình chữ nhật
- **Hình vuông**: 4 cạnh phải bằng nhau như khuôn bánh chưng.
- **Hình chữ nhật**: Có 2 cạnh dài và 2 cạnh ngắn rõ rệt.',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 10
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 9: Luyện tập chung',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 9: Luyện tập chung',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 9: Luyện tập chung',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Hình nào sau đây KHÔNG CÓ cạnh thẳng và góc nhọn?',
        'Hình tròn là một đường cong kín trơn tru, không hề có góc hay cạnh thẳng.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Hình chữ nhật', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Hình tam giác', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình vuông', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình tròn', true, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Một bức tranh ngôi nhà có 1 mái nhà hình tam giác, 1 thân nhà hình vuông và 2 cửa sổ hình vuông. Hỏi có tất cả bao nhiêu hình vuông?',
        'Có 1 hình vuông thân nhà + 2 hình vuông cửa sổ = 3 hình vuông.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 1 hình vuông', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 2 hình vuông', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 3 hình vuông', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 4 hình vuông', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Hình tam giác có bao nhiêu cạnh?',
        'Hình tam giác (''tam'' có nghĩa là 3) có đúng 3 cạnh và 3 đỉnh.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 2 cạnh', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 3 cạnh', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 4 cạnh', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 5 cạnh', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- ----------------------------------------------------
    -- CHƯƠNG 3: Chương 3: Phép cộng, phép trừ trong phạm vi 10
    -- ----------------------------------------------------
    SELECT id INTO v_chapter_id FROM chapters WHERE course_id = v_course_id AND display_order = 3 LIMIT 1;
    IF v_chapter_id IS NULL THEN
        v_chapter_id := gen_random_uuid();
        INSERT INTO chapters (id, course_id, title, description, display_order, is_sellable, created_at, updated_at)
        VALUES (v_chapter_id, v_course_id, 'Chương 3: Phép cộng, phép trừ trong phạm vi 10', 'Khám phá ý nghĩa phép cộng, phép trừ, làm chủ bảng tính cộng trừ trong phạm vi 10, tính nhẩm nhanh và giải toán theo tranh vẽ.', 3, false, now(), now());
    ELSE
        UPDATE chapters
        SET title = 'Chương 3: Phép cộng, phép trừ trong phạm vi 10',
            description = 'Khám phá ý nghĩa phép cộng, phép trừ, làm chủ bảng tính cộng trừ trong phạm vi 10, tính nhẩm nhanh và giải toán theo tranh vẽ.',
            updated_at = now()
        WHERE id = v_chapter_id;
    END IF;

    -- Bài 11: Bài 10: Phép cộng trong phạm vi 10
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 11 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 10: Phép cộng trong phạm vi 10',
            'bai-10-phep-cong-trong-pham-vi-10',
            'Ý nghĩa của dấu cộng (+), thao tác gộp thêm đồ vật và thực hiện phép cộng đơn giản.',
            '### ➕ Phép cộng trong phạm vi 10

#### 1. Ý nghĩa của dấu cộng (+)
- **Cộng** có nghĩa là **gộp lại**, **thêm vào**, **cùng nhau**.
- Dấu cộng được viết là: **+** (gồm một nét dọc cắt một nét ngang).

#### 2. Ví dụ sinh động
- Trên đĩa có 3 quả táo 🍎🍎🍎. Mẹ cho thêm 2 quả táo nữa 🍎🍎.
  - Ta gộp lại và đếm: 3, thêm 1 là 4, thêm 1 nữa là 5.
  - Ta viết phép tính: **3 + 2 = 5**
  - Đọc là: *Ba cộng hai bằng năm*.
- Có 4 chú vịt đang bơi 🦆🦆🦆🦆, có 1 chú vịt khác nhảy xuống bơi cùng 🦆.
  - Phép tính: **4 + 1 = 5**

#### 3. Bé ghi nhớ:
- Bất kỳ số nào cộng với 0 cũng bằng chính số đó:
  - **4 + 0 = 4**
  - **0 + 5 = 5**',
            11,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 10: Phép cộng trong phạm vi 10',
            slug = 'bai-10-phep-cong-trong-pham-vi-10',
            summary = 'Ý nghĩa của dấu cộng (+), thao tác gộp thêm đồ vật và thực hiện phép cộng đơn giản.',
            content = '### ➕ Phép cộng trong phạm vi 10

#### 1. Ý nghĩa của dấu cộng (+)
- **Cộng** có nghĩa là **gộp lại**, **thêm vào**, **cùng nhau**.
- Dấu cộng được viết là: **+** (gồm một nét dọc cắt một nét ngang).

#### 2. Ví dụ sinh động
- Trên đĩa có 3 quả táo 🍎🍎🍎. Mẹ cho thêm 2 quả táo nữa 🍎🍎.
  - Ta gộp lại và đếm: 3, thêm 1 là 4, thêm 1 nữa là 5.
  - Ta viết phép tính: **3 + 2 = 5**
  - Đọc là: *Ba cộng hai bằng năm*.
- Có 4 chú vịt đang bơi 🦆🦆🦆🦆, có 1 chú vịt khác nhảy xuống bơi cùng 🦆.
  - Phép tính: **4 + 1 = 5**

#### 3. Bé ghi nhớ:
- Bất kỳ số nào cộng với 0 cũng bằng chính số đó:
  - **4 + 0 = 4**
  - **0 + 5 = 5**',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 11
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 10: Phép cộng trong phạm vi 10',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 10: Phép cộng trong phạm vi 10',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 10: Phép cộng trong phạm vi 10',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Bé hãy tính: 3 + 2 = ?',
        'Có 3 que tính, thêm 2 que tính nữa, ta đếm: 3... 4, 5. Vậy 3 + 2 = 5!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 4', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 5', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 6', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 7', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Có 4 chú chim trên cành cây, 2 chú chim khác bay đến đậu cùng. Hỏi có tất cả bao nhiêu chú chim?',
        'Thực hiện phép tính cộng: 4 + 2 = 6 chú chim.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 5 chú chim', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 6 chú chim', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 7 chú chim', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 8 chú chim', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Kết quả của phép tính: 7 + 0 = ?',
        'Một số cộng với 0 luôn bằng chính nó, do đó 7 + 0 = 7.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 0', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 6', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 7', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 8', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 12: Bài 11: Phép trừ trong phạm vi 10
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 12 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 11: Phép trừ trong phạm vi 10',
            'bai-11-phep-tru-trong-pham-vi-10',
            'Ý nghĩa của dấu trừ (-), thao tác bớt đi, tách rời và tính nhẩm phép trừ.',
            '### ➖ Phép trừ trong phạm vi 10

#### 1. Ý nghĩa của dấu trừ (-)
- **Trừ** có nghĩa là **bớt đi**, **cho đi**, **bay mất**, **ăn mất**, **còn lại**.
- Dấu trừ được viết là một nét ngang ngắn: **-**

#### 2. Ví dụ sinh động
- Bé có chùm 5 quả bóng bay 🎈🎈🎈🎈🎈. Không may bị gió thổi bay mất 2 quả 🎈🎈.
  - Hỏi bé còn lại mấy quả bóng bay?
  - Ta bớt đi 2 quả, đếm ngược từ 5: 5 bớt 1 còn 4, bớt 1 nữa còn 3.
  - Ta viết phép tính: **5 - 2 = 3**
  - Đọc là: *Năm trừ hai bằng ba*.
- Có 6 củ cà rốt 🥕🥕🥕🥕🥕🥕, chú thỏ ăn mất 1 củ 🥕.
  - Còn lại: **6 - 1 = 5**

#### 3. Bé ghi nhớ:
- Một số trừ đi 0 vẫn bằng chính nó: **5 - 0 = 5**
- Hai số giống nhau trừ cho nhau thì bằng 0: **5 - 5 = 0**',
            12,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 11: Phép trừ trong phạm vi 10',
            slug = 'bai-11-phep-tru-trong-pham-vi-10',
            summary = 'Ý nghĩa của dấu trừ (-), thao tác bớt đi, tách rời và tính nhẩm phép trừ.',
            content = '### ➖ Phép trừ trong phạm vi 10

#### 1. Ý nghĩa của dấu trừ (-)
- **Trừ** có nghĩa là **bớt đi**, **cho đi**, **bay mất**, **ăn mất**, **còn lại**.
- Dấu trừ được viết là một nét ngang ngắn: **-**

#### 2. Ví dụ sinh động
- Bé có chùm 5 quả bóng bay 🎈🎈🎈🎈🎈. Không may bị gió thổi bay mất 2 quả 🎈🎈.
  - Hỏi bé còn lại mấy quả bóng bay?
  - Ta bớt đi 2 quả, đếm ngược từ 5: 5 bớt 1 còn 4, bớt 1 nữa còn 3.
  - Ta viết phép tính: **5 - 2 = 3**
  - Đọc là: *Năm trừ hai bằng ba*.
- Có 6 củ cà rốt 🥕🥕🥕🥕🥕🥕, chú thỏ ăn mất 1 củ 🥕.
  - Còn lại: **6 - 1 = 5**

#### 3. Bé ghi nhớ:
- Một số trừ đi 0 vẫn bằng chính nó: **5 - 0 = 5**
- Hai số giống nhau trừ cho nhau thì bằng 0: **5 - 5 = 0**',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 12
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 11: Phép trừ trong phạm vi 10',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 11: Phép trừ trong phạm vi 10',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 11: Phép trừ trong phạm vi 10',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính kết quả phép tính: 6 - 2 = ?',
        'Có 6 ngón tay, cụp bớt 2 ngón tay xuống, ta còn lại 4 ngón tay. Vậy 6 - 2 = 4!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 3', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 4', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 5', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 8', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Có 5 chiếc kẹo, bé ăn hết cả 5 chiếc kẹo. Hỏi bé còn lại mấy chiếc kẹo?',
        'Hai số giống nhau trừ đi nhau bằng 0: 5 - 5 = 0 chiếc kẹo.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 0 chiếc kẹo', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 1 chiếc kẹo', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 5 chiếc kẹo', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 10 chiếc kẹo', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Điền số thích hợp vào chỗ chấm: 8 - ... = 5',
        'Vì 8 gồm 5 và 3, nên 8 trừ 3 sẽ bằng 5.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 2', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 3', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 4', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 1', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 13: Bài 12: Bảng cộng, bảng trừ trong phạm vi 10
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 13 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 12: Bảng cộng, bảng trừ trong phạm vi 10',
            'bai-12-bang-cong-bang-tru-trong-pham-vi-10',
            'Mối liên hệ tương quan giữa phép cộng và phép trừ, học thuộc bảng tính cơ bản.',
            '### 📊 Bảng cộng, bảng trừ trong phạm vi 10

#### 1. Mối liên hệ kỳ diệu giữa Cộng và Trừ
Khi biết một phép cộng, ta dễ dàng suy ra hai phép trừ tương ứng:
- Nếu: **3 + 4 = 7**
- Thì:
  - **7 - 3 = 4**
  - **7 - 4 = 3**
- Giống như một gia đình có 3 thành viên vậy: 3, 4 và 7 luôn gắn bó với nhau! 👨‍👩‍👧

#### 2. Các cặp số cộng lại bằng 10 (Bạn thân của 10)
Hãy học thuộc lòng các "cặp đôi bạn thân" để tính siêu nhanh nhé:
- **1 + 9 = 10** (và 9 + 1 = 10)
- **2 + 8 = 10** (và 8 + 2 = 10)
- **3 + 7 = 10** (và 7 + 3 = 10)
- **4 + 6 = 10** (và 6 + 4 = 10)
- **5 + 5 = 10**',
            13,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 12: Bảng cộng, bảng trừ trong phạm vi 10',
            slug = 'bai-12-bang-cong-bang-tru-trong-pham-vi-10',
            summary = 'Mối liên hệ tương quan giữa phép cộng và phép trừ, học thuộc bảng tính cơ bản.',
            content = '### 📊 Bảng cộng, bảng trừ trong phạm vi 10

#### 1. Mối liên hệ kỳ diệu giữa Cộng và Trừ
Khi biết một phép cộng, ta dễ dàng suy ra hai phép trừ tương ứng:
- Nếu: **3 + 4 = 7**
- Thì:
  - **7 - 3 = 4**
  - **7 - 4 = 3**
- Giống như một gia đình có 3 thành viên vậy: 3, 4 và 7 luôn gắn bó với nhau! 👨‍👩‍👧

#### 2. Các cặp số cộng lại bằng 10 (Bạn thân của 10)
Hãy học thuộc lòng các "cặp đôi bạn thân" để tính siêu nhanh nhé:
- **1 + 9 = 10** (và 9 + 1 = 10)
- **2 + 8 = 10** (và 8 + 2 = 10)
- **3 + 7 = 10** (và 7 + 3 = 10)
- **4 + 6 = 10** (và 6 + 4 = 10)
- **5 + 5 = 10**',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 13
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 12: Bảng cộng, bảng trừ trong phạm vi 10',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 12: Bảng cộng, bảng trừ trong phạm vi 10',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 12: Bảng cộng, bảng trừ trong phạm vi 10',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số nào cộng với 6 để bằng 10?',
        '4 và 6 là cặp đôi bạn thân: 4 + 6 = 10!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 3', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 4', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 5', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 2', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Biết 2 + 5 = 7. Hỏi 7 - 5 bằng bao nhiêu?',
        'Từ phép cộng 2 + 5 = 7, ta suy ra ngay 7 - 5 = 2.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 1', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 2', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 3', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 4', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính nhẩm: 10 - 3 = ?',
        'Vì 3 + 7 = 10 nên 10 - 3 = 7.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 6', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 7', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 8', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 9', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 14: Bài 13: Luyện tập chung
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 14 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 13: Luyện tập chung',
            'bai-13-luyen-tap-chung',
            'Củng cố kỹ năng tính nhẩm chuỗi phép tính, giải toán có lời văn trong phạm vi 10.',
            '### 🏅 Luyện tập chung: Cộng trừ phạm vi 10

#### 1. Tính toán chuỗi phép tính (từ trái sang phải)
Khi gặp biểu thức có hai dấu tính, ta làm lần lượt từ trái sang phải:
- Ví dụ: **4 + 2 - 1 = ?**
  - Bước 1: Tính 4 + 2 trước, được 6.
  - Bước 2: Lấy 6 - 1 = 5.
  - Kết quả là: **5**

#### 2. Kỹ năng giải bài toán theo tranh vẽ
- Nhìn tranh và tự đặt câu hỏi: "Lúc đầu có bao nhiêu? Thêm vào hay bớt đi? Cuối cùng còn bao nhiêu?"
- Viết câu trả lời và phép tính tương ứng thật rõ ràng.',
            14,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 13: Luyện tập chung',
            slug = 'bai-13-luyen-tap-chung',
            summary = 'Củng cố kỹ năng tính nhẩm chuỗi phép tính, giải toán có lời văn trong phạm vi 10.',
            content = '### 🏅 Luyện tập chung: Cộng trừ phạm vi 10

#### 1. Tính toán chuỗi phép tính (từ trái sang phải)
Khi gặp biểu thức có hai dấu tính, ta làm lần lượt từ trái sang phải:
- Ví dụ: **4 + 2 - 1 = ?**
  - Bước 1: Tính 4 + 2 trước, được 6.
  - Bước 2: Lấy 6 - 1 = 5.
  - Kết quả là: **5**

#### 2. Kỹ năng giải bài toán theo tranh vẽ
- Nhìn tranh và tự đặt câu hỏi: "Lúc đầu có bao nhiêu? Thêm vào hay bớt đi? Cuối cùng còn bao nhiêu?"
- Viết câu trả lời và phép tính tương ứng thật rõ ràng.',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 14
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 13: Luyện tập chung',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 13: Luyện tập chung',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 13: Luyện tập chung',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính kết quả dãy tính: 3 + 4 - 2 = ?',
        'Thực hiện từ trái sang phải: 3 + 4 = 7; sau đó lấy 7 - 2 = 5.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 5', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 6', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 7', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 4', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Trên cây có 8 quả xoài, rụng mất 3 quả. Hỏi trên cây còn lại mấy quả xoài?',
        'Rụng mất nghĩa là bớt đi, ta lấy 8 - 3 = 5 quả xoài.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 4 quả', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 5 quả', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 6 quả', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 11 quả', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tìm số thích hợp điền vào dấu ?: ? + 4 = 9',
        'Ta lấy 9 - 4 = 5. Kiểm tra lại: 5 + 4 = 9.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 4', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 5', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 6', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 3', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- ----------------------------------------------------
    -- CHƯƠNG 4: Chương 4: Làm quen với một số hình khối
    -- ----------------------------------------------------
    SELECT id INTO v_chapter_id FROM chapters WHERE course_id = v_course_id AND display_order = 4 LIMIT 1;
    IF v_chapter_id IS NULL THEN
        v_chapter_id := gen_random_uuid();
        INSERT INTO chapters (id, course_id, title, description, display_order, is_sellable, created_at, updated_at)
        VALUES (v_chapter_id, v_course_id, 'Chương 4: Làm quen với một số hình khối', 'Nhận biết khối lập phương, khối hộp chữ nhật và xác định vị trí, định hướng trong không gian (trên - dưới, trái - phải, trước - sau, ở giữa).', 4, false, now(), now());
    ELSE
        UPDATE chapters
        SET title = 'Chương 4: Làm quen với một số hình khối',
            description = 'Nhận biết khối lập phương, khối hộp chữ nhật và xác định vị trí, định hướng trong không gian (trên - dưới, trái - phải, trước - sau, ở giữa).',
            updated_at = now()
        WHERE id = v_chapter_id;
    END IF;

    -- Bài 15: Bài 14: Khối lập phương, khối hộp chữ nhật
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 15 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 14: Khối lập phương, khối hộp chữ nhật',
            'bai-14-khoi-lap-phuong-khoi-hop-chu-nhat',
            'Phân biệt khối hình 3 chiều: khối lập phương và khối hộp chữ nhật qua đồ vật thực tế.',
            '### 🎲📦 Khối lập phương và Khối hộp chữ nhật

#### 1. Khối lập phương
- Có 6 mặt đều là các **hình vuông** bằng nhau chằn chặn.
- Đồ vật quen thuộc:
  - Viên xúc xắc chơi cá ngựa 🎲.
  - Khối đồ chơi rubik nhiều màu 🧩.
  - Hộp quà sinh nhật hình vuông vức 🎁.

#### 2. Khối hộp chữ nhật
- Có các mặt là **hình chữ nhật** (hoặc có 2 mặt hình vuông và 4 mặt hình chữ nhật).
- Đồ vật quen thuộc:
  - Hộp sữa tươi uống hàng ngày 🥛.
  - Hộp bút của em ✏️.
  - Viên gạch xây nhà 🧱.
  - Bao diêm, tủ lạnh gia đình 🧊.',
            15,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 14: Khối lập phương, khối hộp chữ nhật',
            slug = 'bai-14-khoi-lap-phuong-khoi-hop-chu-nhat',
            summary = 'Phân biệt khối hình 3 chiều: khối lập phương và khối hộp chữ nhật qua đồ vật thực tế.',
            content = '### 🎲📦 Khối lập phương và Khối hộp chữ nhật

#### 1. Khối lập phương
- Có 6 mặt đều là các **hình vuông** bằng nhau chằn chặn.
- Đồ vật quen thuộc:
  - Viên xúc xắc chơi cá ngựa 🎲.
  - Khối đồ chơi rubik nhiều màu 🧩.
  - Hộp quà sinh nhật hình vuông vức 🎁.

#### 2. Khối hộp chữ nhật
- Có các mặt là **hình chữ nhật** (hoặc có 2 mặt hình vuông và 4 mặt hình chữ nhật).
- Đồ vật quen thuộc:
  - Hộp sữa tươi uống hàng ngày 🥛.
  - Hộp bút của em ✏️.
  - Viên gạch xây nhà 🧱.
  - Bao diêm, tủ lạnh gia đình 🧊.',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 15
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 14: Khối lập phương, khối hộp chữ nhật',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 14: Khối lập phương, khối hộp chữ nhật',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 14: Khối lập phương, khối hộp chữ nhật',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Viên xúc xắc 🎲 trong bộ cờ cá ngựa có dạng hình khối nào?',
        'Viên xúc xắc có các mặt đều là hình vuông bằng nhau, chính là khối lập phương!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Khối hộp chữ nhật', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Khối lập phương', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình vuông', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình tròn', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Hộp sữa tươi giấy Milo hoặc Vinamilk có dạng hình khối nào?',
        'Hộp sữa có các mặt hình chữ nhật, có thể cầm nắm trong không gian, là khối hộp chữ nhật.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Khối lập phương', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Khối cầu', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Khối hộp chữ nhật', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Hình chữ nhật', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Điểm khác biệt cơ bản giữa ''Hình vuông'' và ''Khối lập phương'' là gì?',
        'Hình vuông là hình phẳng vẽ trên trang giấy, còn khối lập phương là vật thể trong không gian có thể cầm nắm được.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Hình vuông lăn được, khối lập phương không lăn được', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Hình vuông là hình phẳng vẽ trên giấy, khối lập phương là khối đồ vật cầm nắm được', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Hình vuông có màu sắc, khối lập phương không có màu', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Cả hai giống hệt nhau', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 16: Bài 15: Vị trí, định hướng trong không gian
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 16 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 15: Vị trí, định hướng trong không gian',
            'bai-15-vi-tri-dinh-huong-trong-khong-gian',
            'Xác định chính xác các cặp vị trí không gian: trên - dưới, trước - sau, phải - trái, ở giữa.',
            '### 🧭 Vị trí và Định hướng trong không gian

#### 1. Các cặp vị trí quan trọng
- **Trên - Dưới**:
  - Quạt trần ở **trên** trần nhà.
  - Đôi dép ở **dưới** sàn nhà.
- **Trước - Sau**:
  - Chiếc bàn ở **trước** mặt em.
  - Chiếc ba lô treo ở **sau** lưng em.
- **Phải - Trái**:
  - Tay **phải** em cầm bút viết bài ✍️.
  - Tay **trái** em giữ trang vở phẳng phiu ✋.
- **Ở giữa**:
  - Vật nằm ngăn cách giữa bên trái và bên phải (hoặc giữa đằng trước và đằng sau).
  - Ví dụ: Bé đứng ở giữa ba và mẹ 👨‍👧‍👩.',
            16,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 15: Vị trí, định hướng trong không gian',
            slug = 'bai-15-vi-tri-dinh-huong-trong-khong-gian',
            summary = 'Xác định chính xác các cặp vị trí không gian: trên - dưới, trước - sau, phải - trái, ở giữa.',
            content = '### 🧭 Vị trí và Định hướng trong không gian

#### 1. Các cặp vị trí quan trọng
- **Trên - Dưới**:
  - Quạt trần ở **trên** trần nhà.
  - Đôi dép ở **dưới** sàn nhà.
- **Trước - Sau**:
  - Chiếc bàn ở **trước** mặt em.
  - Chiếc ba lô treo ở **sau** lưng em.
- **Phải - Trái**:
  - Tay **phải** em cầm bút viết bài ✍️.
  - Tay **trái** em giữ trang vở phẳng phiu ✋.
- **Ở giữa**:
  - Vật nằm ngăn cách giữa bên trái và bên phải (hoặc giữa đằng trước và đằng sau).
  - Ví dụ: Bé đứng ở giữa ba và mẹ 👨‍👧‍👩.',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 16
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 15: Vị trí, định hướng trong không gian',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 15: Vị trí, định hướng trong không gian',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 15: Vị trí, định hướng trong không gian',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Quyển sách đang nằm trên mặt bàn, chiếc cặp sách để dưới sàn nhà. Đồ vật nào ở VỊ TRÍ PHÍA TRÊN?',
        'Quyển sách đặt trên mặt bàn nên nó ở phía trên so với cặp sách để dưới đất.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Chiếc cặp sách', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Quyển sách', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Sàn nhà', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Đôi dép', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Hầu hết các bạn nhỏ dùng bàn tay nào để cầm bút chì viết bài?',
        'Chúng mình thường cầm bút chì bằng tay phải và dùng tay trái để giữ mép vở.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Tay trái', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Tay phải', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Cả hai tay', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Chân phải', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Bé An đứng giữa bạn Bình và bạn Cúc. Ai là người đứng ở giữa?',
        'Theo đề bài, bạn An đứng ở vị trí giữa hai bạn Bình và Cúc.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Bạn Bình', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Bạn Cúc', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Bạn An', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Không có ai', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 17: Bài 16: Luyện tập chung
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 17 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 16: Luyện tập chung',
            'bai-16-luyen-tap-chung',
            'Thực hành xếp chồng các khối hình và xác định vị trí các đồ vật trong phòng học.',
            '### 🏗️ Luyện tập chung: Hình khối và Không gian

#### 1. Xếp chồng các khối hình
- **Khối lập phương** và **khối hộp chữ nhật** có các mặt phẳng nên có thể xếp chồng lên nhau thành tòa tháp cao mà không bị đổ! 🏰
- Khi xếp khối nhỏ lên trên khối to, tòa tháp sẽ vô cùng vững chãi.

#### 2. Nhận biết và định hướng nhanh
- Bé hãy quan sát xung quanh phòng:
  - Tìm nhanh 2 vật có dạng khối hộp chữ nhật.
  - Chỉ tay về phía bên phải, bên trái của mình thật nhanh và chính xác nhé!',
            17,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 16: Luyện tập chung',
            slug = 'bai-16-luyen-tap-chung',
            summary = 'Thực hành xếp chồng các khối hình và xác định vị trí các đồ vật trong phòng học.',
            content = '### 🏗️ Luyện tập chung: Hình khối và Không gian

#### 1. Xếp chồng các khối hình
- **Khối lập phương** và **khối hộp chữ nhật** có các mặt phẳng nên có thể xếp chồng lên nhau thành tòa tháp cao mà không bị đổ! 🏰
- Khi xếp khối nhỏ lên trên khối to, tòa tháp sẽ vô cùng vững chãi.

#### 2. Nhận biết và định hướng nhanh
- Bé hãy quan sát xung quanh phòng:
  - Tìm nhanh 2 vật có dạng khối hộp chữ nhật.
  - Chỉ tay về phía bên phải, bên trái của mình thật nhanh và chính xác nhé!',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 17
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 16: Luyện tập chung',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 16: Luyện tập chung',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 16: Luyện tập chung',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Khối hình nào sau đây CÓ THỂ XẾP CHỒNG lên nhau thành cột mà không bị lăn rơi?',
        'Khối lập phương có các mặt phẳng vững chắc nên có thể xếp chồng lên nhau dễ dàng.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Quả bóng tròn', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Khối lập phương', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Viên bi ve', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Quả trứng', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Xếp 3 khối lập phương chồng lên nhau: khối đỏ ở dưới cùng, khối vàng ở giữa, khối xanh ở trên cùng. Khối nào ở vị trí Ở GIỮA?',
        'Khối vàng nằm giữa khối đỏ (ở dưới) và khối xanh (ở trên).',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Khối đỏ', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Khối xanh', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Khối vàng', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Khối tím', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Có 2 khối lập phương và 3 khối hộp chữ nhật. Hỏi có tất cả bao nhiêu khối hình?',
        'Lấy 2 + 3 = 5 khối hình tất cả.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 4 khối', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 5 khối', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 6 khối', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 7 khối', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- ----------------------------------------------------
    -- CHƯƠNG 5: Chương 5: Các số đến 100
    -- ----------------------------------------------------
    SELECT id INTO v_chapter_id FROM chapters WHERE course_id = v_course_id AND display_order = 5 LIMIT 1;
    IF v_chapter_id IS NULL THEN
        v_chapter_id := gen_random_uuid();
        INSERT INTO chapters (id, course_id, title, description, display_order, is_sellable, created_at, updated_at)
        VALUES (v_chapter_id, v_course_id, 'Chương 5: Các số đến 100', 'Nhận biết cấu tạo số có hai chữ số (chục và đơn vị), so sánh các số đến 100, thành thạo bảng các số từ 1 đến 100.', 5, false, now(), now());
    ELSE
        UPDATE chapters
        SET title = 'Chương 5: Các số đến 100',
            description = 'Nhận biết cấu tạo số có hai chữ số (chục và đơn vị), so sánh các số đến 100, thành thạo bảng các số từ 1 đến 100.',
            updated_at = now()
        WHERE id = v_chapter_id;
    END IF;

    -- Bài 18: Bài 17: Số có hai chữ số
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 18 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 17: Số có hai chữ số',
            'bai-17-so-co-hai-chu-so',
            'Khái niệm chục và đơn vị: 1 chục = 10 đơn vị; đọc và viết các số có hai chữ số từ 11 đến 99.',
            '### 💯 Số có hai chữ số: Chục và Đơn vị

#### 1. Khái niệm Chục và Đơn vị
- Bó 10 que tính lại với nhau 🥢 -> ta được **1 chục que tính**.
- **1 chục = 10 đơn vị**
- **2 chục = 20 đơn vị** (hai mươi)
- **10 chục = 100 đơn vị** (một trăm)

#### 2. Cấu tạo số có hai chữ số
Mỗi số có hai chữ số gồm có:
- **Chữ số hàng chục** đứng ở bên trái.
- **Chữ số hàng đơn vị** đứng ở bên phải.
- Ví dụ: Số **25**
  - Gồm **2 chục** và **5 đơn vị**.
  - Đọc là: *Hai mươi lăm*.
- Ví dụ: Số **40**
  - Gồm **4 chục** và **0 đơn vị**.
  - Đọc là: *Bốn mươi* (hoặc bốn chục).',
            18,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 17: Số có hai chữ số',
            slug = 'bai-17-so-co-hai-chu-so',
            summary = 'Khái niệm chục và đơn vị: 1 chục = 10 đơn vị; đọc và viết các số có hai chữ số từ 11 đến 99.',
            content = '### 💯 Số có hai chữ số: Chục và Đơn vị

#### 1. Khái niệm Chục và Đơn vị
- Bó 10 que tính lại với nhau 🥢 -> ta được **1 chục que tính**.
- **1 chục = 10 đơn vị**
- **2 chục = 20 đơn vị** (hai mươi)
- **10 chục = 100 đơn vị** (một trăm)

#### 2. Cấu tạo số có hai chữ số
Mỗi số có hai chữ số gồm có:
- **Chữ số hàng chục** đứng ở bên trái.
- **Chữ số hàng đơn vị** đứng ở bên phải.
- Ví dụ: Số **25**
  - Gồm **2 chục** và **5 đơn vị**.
  - Đọc là: *Hai mươi lăm*.
- Ví dụ: Số **40**
  - Gồm **4 chục** và **0 đơn vị**.
  - Đọc là: *Bốn mươi* (hoặc bốn chục).',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 18
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 17: Số có hai chữ số',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 17: Số có hai chữ số',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 17: Số có hai chữ số',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        '1 chục que tính bằng bao nhiêu que tính rời?',
        '1 chục bằng đúng 10 đơn vị, tức là 10 que tính rời.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 1 que', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 5 que', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 10 que', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 20 que', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số 38 gồm mấy chục và mấy đơn vị?',
        'Số 38 có chữ số 3 ở hàng chục và chữ số 8 ở hàng đơn vị: gồm 3 chục và 8 đơn vị.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 3 chục và 8 đơn vị', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 8 chục và 3 đơn vị', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 30 chục và 8 đơn vị', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 3 đơn vị và 8 chục', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số ''Bảy mươi tư'' được viết là chữ số nào?',
        'Bảy mươi (7 chục) tư (4 đơn vị), viết là 74.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 47', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 74', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 704', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 70', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 19: Bài 18: So sánh số có hai chữ số
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 19 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 18: So sánh số có hai chữ số',
            'bai-18-so-sanh-so-co-hai-chu-so',
            'Quy tắc so sánh số có hai chữ số: so sánh chữ số hàng chục trước, nếu bằng nhau thì so sánh chữ số hàng đơn vị.',
            '### ⚖️ Cách so sánh hai số có hai chữ số

#### 1. Quy tắc vàng gồm 2 bước:
- **Bước 1: So sánh chữ số hàng chục trước**
  - Số nào có hàng chục lớn hơn thì số đó lớn hơn!
  - *Ví dụ*: So sánh **52** và **39**:
    - Số 52 có 5 chục. Số 39 có 3 chục.
    - Vì 5 > 3 nên **52 > 39** (không cần so sánh hàng đơn vị nữa).
- **Bước 2: Nếu hàng chục bằng nhau, ta so sánh tiếp hàng đơn vị**
  - Số nào có hàng đơn vị lớn hơn thì số đó lớn hơn!
  - *Ví dụ*: So sánh **43** và **47**:
    - Cả hai đều có 4 chục.
    - Hàng đơn vị: 3 < 7 nên **43 < 47**.',
            19,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 18: So sánh số có hai chữ số',
            slug = 'bai-18-so-sanh-so-co-hai-chu-so',
            summary = 'Quy tắc so sánh số có hai chữ số: so sánh chữ số hàng chục trước, nếu bằng nhau thì so sánh chữ số hàng đơn vị.',
            content = '### ⚖️ Cách so sánh hai số có hai chữ số

#### 1. Quy tắc vàng gồm 2 bước:
- **Bước 1: So sánh chữ số hàng chục trước**
  - Số nào có hàng chục lớn hơn thì số đó lớn hơn!
  - *Ví dụ*: So sánh **52** và **39**:
    - Số 52 có 5 chục. Số 39 có 3 chục.
    - Vì 5 > 3 nên **52 > 39** (không cần so sánh hàng đơn vị nữa).
- **Bước 2: Nếu hàng chục bằng nhau, ta so sánh tiếp hàng đơn vị**
  - Số nào có hàng đơn vị lớn hơn thì số đó lớn hơn!
  - *Ví dụ*: So sánh **43** và **47**:
    - Cả hai đều có 4 chục.
    - Hàng đơn vị: 3 < 7 nên **43 < 47**.',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 19
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 18: So sánh số có hai chữ số',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 18: So sánh số có hai chữ số',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 18: So sánh số có hai chữ số',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Điền dấu thích hợp: 65 ... 48',
        'So sánh hàng chục: 6 chục lớn hơn 4 chục (6 > 4), do đó 65 > 48.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. >', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. <', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. =', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. +', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Điền dấu thích hợp: 72 ... 76',
        'Hàng chục đều là 7. So sánh hàng đơn vị: 2 < 6 nên 72 < 76.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. >', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. <', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. =', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. -', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Trong các số: 35, 78, 42, 81, số nào là SỐ LỚN NHẤT?',
        'Số 81 có 8 chục và 1 đơn vị, là số có giá trị lớn nhất trong nhóm.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 35', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 78', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 42', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 81', true, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 20: Bài 19: Bảng các số từ 1 đến 100
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 20 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 19: Bảng các số từ 1 đến 100',
            'bai-19-bang-cac-so-tu-1-den-100',
            'Khám phá quy luật sắp xếp của 100 số tự nhiên trong bảng 10x10, tìm số liền trước, số liền sau.',
            '### 📋 Bảng các số từ 1 đến 100

#### 1. Cấu trúc tuyệt đẹp của bảng số
- Bảng gồm 10 hàng và 10 cột, bắt đầu từ số **1** và kết thúc ở số **100**.
- **Quy luật hàng ngang**: Hai số liền nhau hơn kém nhau **1 đơn vị**.
  - Ví dụ: 21, 22, 23, 24...
- **Quy luật cột dọc**: Hai số liên tiếp trong cùng một cột hơn kém nhau **1 chục (10 đơn vị)**.
  - Ví dụ: 5, 15, 25, 35, 45...

#### 2. Số liền trước và Số liền sau
- Muốn tìm **số liền trước** của một số, ta lấy số đó **bớt đi 1**.
  - *Ví dụ*: Số liền trước của 20 là **19**.
- Muốn tìm **số liền sau** của một số, ta lấy số đó **thêm vào 1**.
  - *Ví dụ*: Số liền sau của 20 là **21**.',
            20,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 19: Bảng các số từ 1 đến 100',
            slug = 'bai-19-bang-cac-so-tu-1-den-100',
            summary = 'Khám phá quy luật sắp xếp của 100 số tự nhiên trong bảng 10x10, tìm số liền trước, số liền sau.',
            content = '### 📋 Bảng các số từ 1 đến 100

#### 1. Cấu trúc tuyệt đẹp của bảng số
- Bảng gồm 10 hàng và 10 cột, bắt đầu từ số **1** và kết thúc ở số **100**.
- **Quy luật hàng ngang**: Hai số liền nhau hơn kém nhau **1 đơn vị**.
  - Ví dụ: 21, 22, 23, 24...
- **Quy luật cột dọc**: Hai số liên tiếp trong cùng một cột hơn kém nhau **1 chục (10 đơn vị)**.
  - Ví dụ: 5, 15, 25, 35, 45...

#### 2. Số liền trước và Số liền sau
- Muốn tìm **số liền trước** của một số, ta lấy số đó **bớt đi 1**.
  - *Ví dụ*: Số liền trước của 20 là **19**.
- Muốn tìm **số liền sau** của một số, ta lấy số đó **thêm vào 1**.
  - *Ví dụ*: Số liền sau của 20 là **21**.',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 20
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 19: Bảng các số từ 1 đến 100',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 19: Bảng các số từ 1 đến 100',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 19: Bảng các số từ 1 đến 100',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số liền trước của số 50 là số nào?',
        'Số liền trước nằm ngay phía trước số 50 khi đếm, đó là số 49 (lấy 50 - 1 = 49).',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 48', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 49', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 51', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 52', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số liền sau của số 79 là số nào?',
        'Số liền sau của 79 khi đếm thêm 1 là số 80 tròn chục.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 78', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 80', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 81', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 70', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số tròn chục lớn nhất có hai chữ số là số nào?',
        'Các số tròn chục có 2 chữ số gồm 10, 20... 90. Lớn nhất chính là 90 (vì 100 có 3 chữ số).',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 10', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 90', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 99', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 100', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 21: Bài 20: Luyện tập chung
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 21 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 20: Luyện tập chung',
            'bai-20-luyen-tap-chung',
            'Sắp xếp dãy số có hai chữ số theo thứ tự tăng dần, giảm dần và giải toán tư duy cấu tạo số.',
            '### 🚀 Luyện tập chung Chương 5

#### 1. Kỹ năng sắp xếp thứ tự các số
- **Từ bé đến lớn (Tăng dần)**: Chọn số bé nhất viết trước, rồi đến các số lớn hơn.
  - Ví dụ: Cho các số 64, 18, 55, 32.
  - Thứ tự từ bé đến lớn: **18, 32, 55, 64**.
- **Từ lớn đến bé (Giảm dần)**: Chọn số lớn nhất viết trước.
  - Thứ tự từ lớn đến bé: **64, 55, 32, 18**.

#### 2. Bí quyết tìm số thông minh
- Đọc kỹ yêu cầu: "Số lớn nhất", "Số bé nhất", hay "Số tròn chục"?
- Gạch chân chữ số hàng chục để so sánh không bao giờ nhầm lẫn!',
            21,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 20: Luyện tập chung',
            slug = 'bai-20-luyen-tap-chung',
            summary = 'Sắp xếp dãy số có hai chữ số theo thứ tự tăng dần, giảm dần và giải toán tư duy cấu tạo số.',
            content = '### 🚀 Luyện tập chung Chương 5

#### 1. Kỹ năng sắp xếp thứ tự các số
- **Từ bé đến lớn (Tăng dần)**: Chọn số bé nhất viết trước, rồi đến các số lớn hơn.
  - Ví dụ: Cho các số 64, 18, 55, 32.
  - Thứ tự từ bé đến lớn: **18, 32, 55, 64**.
- **Từ lớn đến bé (Giảm dần)**: Chọn số lớn nhất viết trước.
  - Thứ tự từ lớn đến bé: **64, 55, 32, 18**.

#### 2. Bí quyết tìm số thông minh
- Đọc kỹ yêu cầu: "Số lớn nhất", "Số bé nhất", hay "Số tròn chục"?
- Gạch chân chữ số hàng chục để so sánh không bao giờ nhầm lẫn!',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 21
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 20: Luyện tập chung',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 20: Luyện tập chung',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 20: Luyện tập chung',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Dãy số nào sau đây được xếp theo thứ tự TỪ LỚN ĐẾN BÉ?',
        'Dãy 89, 75, 43, 21 bắt đầu từ số lớn nhất rồi giảm dần đều xuống số bé nhất.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 12, 34, 56, 78', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 89, 75, 43, 21', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 54, 32, 67, 10', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 19, 45, 23, 90', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số nào có chữ số hàng chục là 5 và chữ số hàng đơn vị là 2?',
        '5 chục đứng trước, 2 đơn vị đứng sau tạo thành số 52.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 25', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 52', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 50', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 22', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Cho ba chữ số: 2, 5, 0. Số tròn chục có hai chữ số ghép được từ các số trên là:',
        'Số tròn chục có hàng đơn vị là 0, ta ghép được hai số: 20 và 50.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 25', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 52', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 20 và 50', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 205', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- ----------------------------------------------------
    -- CHƯƠNG 6: Chương 6: Độ dài và đo độ dài
    -- ----------------------------------------------------
    SELECT id INTO v_chapter_id FROM chapters WHERE course_id = v_course_id AND display_order = 6 LIMIT 1;
    IF v_chapter_id IS NULL THEN
        v_chapter_id := gen_random_uuid();
        INSERT INTO chapters (id, course_id, title, description, display_order, is_sellable, created_at, updated_at)
        VALUES (v_chapter_id, v_course_id, 'Chương 6: Độ dài và đo độ dài', 'Nhận biết dài hơn - ngắn hơn, làm quen đơn vị đo độ dài xăng-ti-mét (cm), thực hành dùng thước kẻ để đo và ước lượng độ dài.', 6, false, now(), now());
    ELSE
        UPDATE chapters
        SET title = 'Chương 6: Độ dài và đo độ dài',
            description = 'Nhận biết dài hơn - ngắn hơn, làm quen đơn vị đo độ dài xăng-ti-mét (cm), thực hành dùng thước kẻ để đo và ước lượng độ dài.',
            updated_at = now()
        WHERE id = v_chapter_id;
    END IF;

    -- Bài 22: Bài 21: Dài hơn, ngắn hơn
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 22 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 21: Dài hơn, ngắn hơn',
            'bai-21-dai-hon-ngan-hon',
            'So sánh trực tiếp chiều dài của hai đồ vật bằng cách đặt một đầu bằng nhau.',
            '### 📏 Dài hơn, ngắn hơn

#### 1. Cách so sánh chiều dài hai đồ vật
Để biết vật nào dài hơn, vật nào ngắn hơn một cách chính xác nhất:
- **Bước 1**: Đặt một đầu của hai vật **thẳng hàng bằng nhau**.
- **Bước 2**: Quan sát đầu còn lại:
  - Vật nào thò ra dài hơn thì vật đó **dài hơn**.
  - Vật nào thụt vào trong thì vật đó **ngắn hơn**.

#### 2. Ví dụ quen thuộc
- Đặt chiếc thước kẻ 30 cm cạnh chiếc bút chì ✏️:
  - Chiếc thước kẻ **dài hơn** chiếc bút chì.
  - Chiếc bút chì **ngắn hơn** chiếc thước kẻ.
- Đặt chiếc bút chì cạnh cục tẩy:
  - Chiếc bút chì **dài hơn** cục tẩy.
  - Cục tẩy **ngắn hơn** chiếc bút chì.',
            22,
            35,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 21: Dài hơn, ngắn hơn',
            slug = 'bai-21-dai-hon-ngan-hon',
            summary = 'So sánh trực tiếp chiều dài của hai đồ vật bằng cách đặt một đầu bằng nhau.',
            content = '### 📏 Dài hơn, ngắn hơn

#### 1. Cách so sánh chiều dài hai đồ vật
Để biết vật nào dài hơn, vật nào ngắn hơn một cách chính xác nhất:
- **Bước 1**: Đặt một đầu của hai vật **thẳng hàng bằng nhau**.
- **Bước 2**: Quan sát đầu còn lại:
  - Vật nào thò ra dài hơn thì vật đó **dài hơn**.
  - Vật nào thụt vào trong thì vật đó **ngắn hơn**.

#### 2. Ví dụ quen thuộc
- Đặt chiếc thước kẻ 30 cm cạnh chiếc bút chì ✏️:
  - Chiếc thước kẻ **dài hơn** chiếc bút chì.
  - Chiếc bút chì **ngắn hơn** chiếc thước kẻ.
- Đặt chiếc bút chì cạnh cục tẩy:
  - Chiếc bút chì **dài hơn** cục tẩy.
  - Cục tẩy **ngắn hơn** chiếc bút chì.',
            estimated_minutes = 35,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 22
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 21: Dài hơn, ngắn hơn',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 21: Dài hơn, ngắn hơn',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 21: Dài hơn, ngắn hơn',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Khi so sánh chiều dài hai chiếc bút chì, ta cần đặt hai chiếc bút như thế nào?',
        'Phải đặt một đầu bằng nhau thì mới quan sát được đầu kia vật nào dài hơn hay ngắn hơn.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Đặt chéo nhau', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Đặt một đầu của hai chiếc bút thẳng hàng bằng nhau', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Đặt cách xa nhau thật nhiều', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Đặt chiếc này đè lên đầu chiếc kia', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Thước kẻ dài hơn bút chì, bút chì dài hơn cục tẩy. Vật nào NGẮN NHẤT trong ba vật?',
        'Cục tẩy ngắn hơn bút chì và ngắn hơn cả thước kẻ nên cục tẩy là vật ngắn nhất.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Thước kẻ', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Bút chì', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Cục tẩy', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Cả ba bằng nhau', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Bé so sánh chiếc đũa ăn cơm và chiếc thìa canh. Nhận xét nào đúng?',
        'Thông thường, chiếc đũa ăn cơm dài hơn chiếc thìa canh.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Chiếc đũa dài hơn chiếc thìa', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Chiếc thìa dài hơn chiếc đũa', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Chiếc đũa ngắn hơn chiếc thìa', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Cả hai ngắn bằng cục tẩy', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 23: Bài 22: Đơn vị đo độ dài
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 23 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 22: Đơn vị đo độ dài',
            'bai-22-don-vi-do-do-dai',
            'Làm quen với đơn vị đo xăng-ti-mét, ký hiệu viết tắt là cm; cách đọc các vạch chia trên thước kẻ.',
            '### 📐 Đơn vị đo độ dài: Xăng-ti-mét (cm)

#### 1. Xăng-ti-mét là gì?
- **Xăng-ti-mét** là một đơn vị đo độ dài chuẩn quốc tế.
- Xăng-ti-mét được viết tắt là: **cm** (chữ c ghép với chữ m).
  - Đọc là: *Xăng-ti-mét*.
  - Ví dụ: 5 cm đọc là *Năm xăng-ti-mét*.

#### 2. Quan sát chiếc thước kẻ học sinh
- Trên thước có các vạch chia đều nhau và có ghi số: 0, 1, 2, 3, 4, 5...
- Khoảng cách từ vạch số **0** đến vạch số **1** dài đúng **1 cm**.
- Khoảng cách từ vạch số **0** đến vạch số **5** dài đúng **5 cm**.',
            23,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 22: Đơn vị đo độ dài',
            slug = 'bai-22-don-vi-do-do-dai',
            summary = 'Làm quen với đơn vị đo xăng-ti-mét, ký hiệu viết tắt là cm; cách đọc các vạch chia trên thước kẻ.',
            content = '### 📐 Đơn vị đo độ dài: Xăng-ti-mét (cm)

#### 1. Xăng-ti-mét là gì?
- **Xăng-ti-mét** là một đơn vị đo độ dài chuẩn quốc tế.
- Xăng-ti-mét được viết tắt là: **cm** (chữ c ghép với chữ m).
  - Đọc là: *Xăng-ti-mét*.
  - Ví dụ: 5 cm đọc là *Năm xăng-ti-mét*.

#### 2. Quan sát chiếc thước kẻ học sinh
- Trên thước có các vạch chia đều nhau và có ghi số: 0, 1, 2, 3, 4, 5...
- Khoảng cách từ vạch số **0** đến vạch số **1** dài đúng **1 cm**.
- Khoảng cách từ vạch số **0** đến vạch số **5** dài đúng **5 cm**.',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 23
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 22: Đơn vị đo độ dài',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 22: Đơn vị đo độ dài',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 22: Đơn vị đo độ dài',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Xăng-ti-mét được viết tắt là gì?',
        'Xăng-ti-mét được viết tắt trong toán học là ''cm''.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. km', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. m', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. cm', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. mm', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        '''8 cm'' được đọc như thế nào?',
        'Số 8 đi liền với đơn vị cm đọc là ''Tám xăng-ti-mét''.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Tám mét', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Tám xăng-ti-mét', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Tám ki-lô-mét', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Tám mi-li-mét', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Khoảng cách từ vạch số 0 đến vạch số 7 trên thước kẻ dài bao nhiêu?',
        'Bắt đầu từ vạch 0 đến vạch số 7 có độ dài đúng bằng 7 cm.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 5 cm', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 6 cm', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 7 cm', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 8 cm', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 24: Bài 23: Thực hành ước lượng và đo độ dài
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 24 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 23: Thực hành ước lượng và đo độ dài',
            'bai-23-thuc-hanh-uoc-luong-va-do-do-dai',
            'Quy trình 3 bước đo độ dài bằng thước kẻ cm và kỹ năng ước lượng bằng mắt.',
            '### 📏 Thực hành đo độ dài chuẩn xác

#### 1. Quy trình 3 bước đo bằng thước kẻ:
- **Bước 1**: Đặt mép thước dọc theo đồ vật cần đo (đoạn thẳng, bút chì...).
- **Bước 2**: Đặt một đầu của đồ vật trùng khít với **vạch số 0** của thước. *(Rất quan trọng, không đặt ở mép ngoài thước nếu vạch 0 lùi vào trong!)*
- **Bước 3**: Nhìn xem đầu kia của đồ vật trùng với vạch số nào, đó chính là độ dài của đồ vật!

#### 2. Kỹ năng ước lượng độ dài
- Ước lượng là đoán nhanh độ dài trước khi đo:
  - Một gang tay của bé dài khoảng **12 cm đến 15 cm**.
  - Chiếc bút chì mới dài khoảng **15 cm đến 18 cm**.
  - Cục tẩy dài khoảng **3 cm đến 4 cm**.',
            24,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 23: Thực hành ước lượng và đo độ dài',
            slug = 'bai-23-thuc-hanh-uoc-luong-va-do-do-dai',
            summary = 'Quy trình 3 bước đo độ dài bằng thước kẻ cm và kỹ năng ước lượng bằng mắt.',
            content = '### 📏 Thực hành đo độ dài chuẩn xác

#### 1. Quy trình 3 bước đo bằng thước kẻ:
- **Bước 1**: Đặt mép thước dọc theo đồ vật cần đo (đoạn thẳng, bút chì...).
- **Bước 2**: Đặt một đầu của đồ vật trùng khít với **vạch số 0** của thước. *(Rất quan trọng, không đặt ở mép ngoài thước nếu vạch 0 lùi vào trong!)*
- **Bước 3**: Nhìn xem đầu kia của đồ vật trùng với vạch số nào, đó chính là độ dài của đồ vật!

#### 2. Kỹ năng ước lượng độ dài
- Ước lượng là đoán nhanh độ dài trước khi đo:
  - Một gang tay của bé dài khoảng **12 cm đến 15 cm**.
  - Chiếc bút chì mới dài khoảng **15 cm đến 18 cm**.
  - Cục tẩy dài khoảng **3 cm đến 4 cm**.',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 24
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 23: Thực hành ước lượng và đo độ dài',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 23: Thực hành ước lượng và đo độ dài',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 23: Thực hành ước lượng và đo độ dài',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Khi đo độ dài một cái bút chì bằng thước kẻ, ta phải đặt một đầu bút chì TRÙNG VỚI VẠCH NÀO?',
        'Luôn luôn đặt một đầu của vật trùng với vạch số 0 để kết quả đo chính xác nhất.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Vạch số 1', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Vạch số 0', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Mép ngoài cùng của thước', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Vạch số 10', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Một đoạn thẳng có một đầu ở vạch số 0, đầu kia ở vạch số 9. Đoạn thẳng đó dài bao nhiêu?',
        'Đầu kia chạm vạch số 9 nên đoạn thẳng dài 9 cm.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 8 cm', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 9 cm', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 10 cm', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 0 cm', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Một que tính dài 10 cm. Bẻ đôi que tính thành 2 nửa bằng nhau. Mỗi nửa que tính dài bao nhiêu?',
        'Vì 5 cm + 5 cm = 10 cm, nên mỗi nửa que tính dài đúng 5 cm.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 4 cm', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 5 cm', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 6 cm', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 10 cm', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 25: Bài 24: Luyện tập chung
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 25 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 24: Luyện tập chung',
            'bai-24-luyen-tap-chung',
            'Thực hiện các phép tính cộng, trừ có kèm đơn vị đo xăng-ti-mét và giải toán thực tiễn.',
            '### 🎯 Luyện tập chung: Phép tính với đơn vị cm

#### 1. Quy tắc cộng trừ có kèm đơn vị cm
Khi thực hiện phép tính có đơn vị đo cm:
- Ta lấy các số cộng hoặc trừ với nhau như bình thường.
- **Bắt buộc ghi thêm chữ "cm"** ở đằng sau kết quả!
- *Ví dụ 1*: **4 cm + 3 cm = 7 cm**
- *Ví dụ 2*: **9 cm - 5 cm = 4 cm**
- *Ví dụ 3*: **10 cm + 20 cm = 30 cm**

#### 2. Chú ý bài thi:
Nếu câu hỏi hỏi "Mảnh giấy dài bao nhiêu xăng-ti-mét?", viết số mà quên chữ "cm" là sẽ bị trừ điểm đấy nhé bé yêu!',
            25,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 24: Luyện tập chung',
            slug = 'bai-24-luyen-tap-chung',
            summary = 'Thực hiện các phép tính cộng, trừ có kèm đơn vị đo xăng-ti-mét và giải toán thực tiễn.',
            content = '### 🎯 Luyện tập chung: Phép tính với đơn vị cm

#### 1. Quy tắc cộng trừ có kèm đơn vị cm
Khi thực hiện phép tính có đơn vị đo cm:
- Ta lấy các số cộng hoặc trừ với nhau như bình thường.
- **Bắt buộc ghi thêm chữ "cm"** ở đằng sau kết quả!
- *Ví dụ 1*: **4 cm + 3 cm = 7 cm**
- *Ví dụ 2*: **9 cm - 5 cm = 4 cm**
- *Ví dụ 3*: **10 cm + 20 cm = 30 cm**

#### 2. Chú ý bài thi:
Nếu câu hỏi hỏi "Mảnh giấy dài bao nhiêu xăng-ti-mét?", viết số mà quên chữ "cm" là sẽ bị trừ điểm đấy nhé bé yêu!',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 25
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 24: Luyện tập chung',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 24: Luyện tập chung',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 24: Luyện tập chung',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính kết quả phép tính: 7 cm + 2 cm = ?',
        'Lấy 7 + 2 = 9 rồi ghi kèm đơn vị cm ở sau: 9 cm.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 9', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 9 cm', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 10 cm', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 8 cm', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Đoạn băng dính dài 18 cm, bé cắt đi 5 cm. Hỏi đoạn băng dính còn lại dài bao nhiêu cm?',
        'Phép tính: 18 cm - 5 cm = 13 cm.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 12 cm', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 13 cm', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 14 cm', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 23 cm', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Điền số thích hợp: 30 cm + ... cm = 50 cm',
        'Vì 30 + 20 = 50 nên số cần điền là 20.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 10', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 20', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 30', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 80', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- ----------------------------------------------------
    -- CHƯƠNG 7: Chương 7: Phép cộng, phép trừ (không nhớ) trong phạm vi 100
    -- ----------------------------------------------------
    SELECT id INTO v_chapter_id FROM chapters WHERE course_id = v_course_id AND display_order = 7 LIMIT 1;
    IF v_chapter_id IS NULL THEN
        v_chapter_id := gen_random_uuid();
        INSERT INTO chapters (id, course_id, title, description, display_order, is_sellable, created_at, updated_at)
        VALUES (v_chapter_id, v_course_id, 'Chương 7: Phép cộng, phép trừ (không nhớ) trong phạm vi 100', 'Nắm vững kỹ năng đặt tính rồi tính, cộng trừ số có hai chữ số với số có một chữ số và số có hai chữ số (không nhớ), giải bài toán có lời văn thực tế.', 7, false, now(), now());
    ELSE
        UPDATE chapters
        SET title = 'Chương 7: Phép cộng, phép trừ (không nhớ) trong phạm vi 100',
            description = 'Nắm vững kỹ năng đặt tính rồi tính, cộng trừ số có hai chữ số với số có một chữ số và số có hai chữ số (không nhớ), giải bài toán có lời văn thực tế.',
            updated_at = now()
        WHERE id = v_chapter_id;
    END IF;

    -- Bài 26: Bài 25: Phép cộng số có hai chữ số với số có một chữ số
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 26 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 25: Phép cộng số có hai chữ số với số có một chữ số',
            'bai-25-phep-cong-so-co-hai-chu-so-voi-so-co-mot-chu-so',
            'Quy tắc đặt tính thẳng cột và tính: cộng hàng đơn vị với hàng đơn vị, hạ hàng chục xuống.',
            '### ➕ Phép cộng số có hai chữ số với số có một chữ số (không nhớ)

#### 1. Ví dụ mẫu: 23 + 4 = ?
Để thực hiện phép tính, chúng mình làm qua 2 bước:
- **Bước 1: Đặt tính thẳng cột**
  - Viết số 23 ở hàng trên.
  - Viết số 4 ở hàng dưới, sao cho **chữ số 4 thẳng cột với chữ số 3** ở hàng đơn vị.
  - Viết dấu **+** ở bên trái và kẻ một đường gạch ngang thay cho dấu bằng.
- **Bước 2: Tính từ phải sang trái (tính hàng đơn vị trước)**
  - Lấy 3 cộng 4 bằng 7, viết 7.
  - Hạ 2 ở hàng chục xuống, viết 2.
  - Vậy: **23 + 4 = 27**

#### 2. Mẹo tính nhẩm nhanh:
Lấy hàng đơn vị cộng với nhau: 3 + 4 = 7, giữ nguyên 2 chục -> được 27!',
            26,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 25: Phép cộng số có hai chữ số với số có một chữ số',
            slug = 'bai-25-phep-cong-so-co-hai-chu-so-voi-so-co-mot-chu-so',
            summary = 'Quy tắc đặt tính thẳng cột và tính: cộng hàng đơn vị với hàng đơn vị, hạ hàng chục xuống.',
            content = '### ➕ Phép cộng số có hai chữ số với số có một chữ số (không nhớ)

#### 1. Ví dụ mẫu: 23 + 4 = ?
Để thực hiện phép tính, chúng mình làm qua 2 bước:
- **Bước 1: Đặt tính thẳng cột**
  - Viết số 23 ở hàng trên.
  - Viết số 4 ở hàng dưới, sao cho **chữ số 4 thẳng cột với chữ số 3** ở hàng đơn vị.
  - Viết dấu **+** ở bên trái và kẻ một đường gạch ngang thay cho dấu bằng.
- **Bước 2: Tính từ phải sang trái (tính hàng đơn vị trước)**
  - Lấy 3 cộng 4 bằng 7, viết 7.
  - Hạ 2 ở hàng chục xuống, viết 2.
  - Vậy: **23 + 4 = 27**

#### 2. Mẹo tính nhẩm nhanh:
Lấy hàng đơn vị cộng với nhau: 3 + 4 = 7, giữ nguyên 2 chục -> được 27!',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 26
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 25: Phép cộng số có hai chữ số với số có một chữ số',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 25: Phép cộng số có hai chữ số với số có một chữ số',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 25: Phép cộng số có hai chữ số với số có một chữ số',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính nhẩm: 42 + 5 = ?',
        'Lấy 2 cộng 5 bằng 7 ở hàng đơn vị, giữ nguyên 4 chục được 47.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 45', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 46', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 47', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 92', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Khi đặt tính phép tính 36 + 2, chữ số 2 phải đặt THẲNG CỘT với chữ số nào?',
        'Chữ số 2 là hàng đơn vị, phải đặt thẳng cột với chữ số 6 ở hàng đơn vị của số 36.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Chữ số 3', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Chữ số 6', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Đặt lệch hẳn ra ngoài', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Ở đâu cũng được', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Lớp 1A có 31 học sinh, có thêm 3 bạn mới chuyển vào. Hỏi lớp 1A hiện có bao nhiêu bạn?',
        'Phép tính: 31 + 3 = 34 bạn học sinh.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 33 bạn', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 34 bạn', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 35 bạn', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 43 bạn', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 27: Bài 26: Phép cộng số có hai chữ số với số có hai chữ số
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 27 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 26: Phép cộng số có hai chữ số với số có hai chữ số',
            'bai-26-phep-cong-so-co-hai-chu-so-voi-so-co-hai-chu-so',
            'Đặt tính rồi tính: hàng chục thẳng cột hàng chục, hàng đơn vị thẳng cột hàng đơn vị.',
            '### ➕ Phép cộng số có hai chữ số với số có hai chữ số (không nhớ)

#### 1. Ví dụ mẫu: 35 + 24 = ?
- **Bước 1: Đặt tính**
  - Viết số 35 ở trên, số 24 ở dưới sao cho các chữ số thẳng cột với nhau:
    - 5 thẳng cột với 4 (hàng đơn vị).
    - 3 thẳng cột với 2 (hàng chục).
  - Viết dấu **+** và kẻ vạch ngang.
- **Bước 2: Tính từ phải qua trái**
  - **Hàng đơn vị**: 5 + 4 = 9, viết 9.
  - **Hàng chục**: 3 + 2 = 5, viết 5.
  - Kết quả: **35 + 24 = 59**

#### 2. Bé ghi nhớ:
"Hàng nào cộng với hàng đó! Đơn vị cộng đơn vị, chục cộng chục!"',
            27,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 26: Phép cộng số có hai chữ số với số có hai chữ số',
            slug = 'bai-26-phep-cong-so-co-hai-chu-so-voi-so-co-hai-chu-so',
            summary = 'Đặt tính rồi tính: hàng chục thẳng cột hàng chục, hàng đơn vị thẳng cột hàng đơn vị.',
            content = '### ➕ Phép cộng số có hai chữ số với số có hai chữ số (không nhớ)

#### 1. Ví dụ mẫu: 35 + 24 = ?
- **Bước 1: Đặt tính**
  - Viết số 35 ở trên, số 24 ở dưới sao cho các chữ số thẳng cột với nhau:
    - 5 thẳng cột với 4 (hàng đơn vị).
    - 3 thẳng cột với 2 (hàng chục).
  - Viết dấu **+** và kẻ vạch ngang.
- **Bước 2: Tính từ phải qua trái**
  - **Hàng đơn vị**: 5 + 4 = 9, viết 9.
  - **Hàng chục**: 3 + 2 = 5, viết 5.
  - Kết quả: **35 + 24 = 59**

#### 2. Bé ghi nhớ:
"Hàng nào cộng với hàng đó! Đơn vị cộng đơn vị, chục cộng chục!"',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 27
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 26: Phép cộng số có hai chữ số với số có hai chữ số',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 26: Phép cộng số có hai chữ số với số có hai chữ số',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 26: Phép cộng số có hai chữ số với số có hai chữ số',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính: 23 + 45 = ?',
        'Hàng đơn vị: 3 + 5 = 8. Hàng chục: 2 + 4 = 6. Kết quả là 68.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 67', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 68', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 58', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 78', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Một cửa hàng buổi sáng bán được 20 chiếc cặp, buổi chiều bán được 15 chiếc cặp. Cả ngày bán được bao nhiêu chiếc cặp?',
        'Phép tính: 20 + 15 = 35 chiếc cặp.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 25 chiếc', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 30 chiếc', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 35 chiếc', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 40 chiếc', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tìm x biết: x - 21 = 34',
        'Muốn tìm số bị trừ x, ta lấy 34 + 21 = 55.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 13', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 55', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 54', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 65', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 28: Bài 27: Phép trừ số có hai chữ số cho số có một chữ số
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 28 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 27: Phép trừ số có hai chữ số cho số có một chữ số',
            'bai-27-phep-tru-so-co-hai-chu-so-cho-so-co-mot-chu-so',
            'Quy tắc đặt tính và tính: trừ hàng đơn vị cho hàng đơn vị, hạ hàng chục xuống.',
            '### ➖ Phép trừ số có hai chữ số cho số có một chữ số (không nhớ)

#### 1. Ví dụ mẫu: 48 - 5 = ?
- **Bước 1: Đặt tính thẳng cột**
  - Viết 48 ở trên, 5 ở dưới thẳng cột với chữ số 8.
  - Viết dấu **-** và kẻ vạch ngang.
- **Bước 2: Tính từ phải qua trái**
  - **Hàng đơn vị**: 8 - 5 = 3, viết 3.
  - **Hàng chục**: Hạ 4 xuống, viết 4.
  - Kết quả: **48 - 5 = 43**

#### 2. Mẹo tính nhẩm:
Lấy hàng đơn vị trừ trước: 8 - 5 = 3, ghép với 4 chục -> ra ngay 43!',
            28,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 27: Phép trừ số có hai chữ số cho số có một chữ số',
            slug = 'bai-27-phep-tru-so-co-hai-chu-so-cho-so-co-mot-chu-so',
            summary = 'Quy tắc đặt tính và tính: trừ hàng đơn vị cho hàng đơn vị, hạ hàng chục xuống.',
            content = '### ➖ Phép trừ số có hai chữ số cho số có một chữ số (không nhớ)

#### 1. Ví dụ mẫu: 48 - 5 = ?
- **Bước 1: Đặt tính thẳng cột**
  - Viết 48 ở trên, 5 ở dưới thẳng cột với chữ số 8.
  - Viết dấu **-** và kẻ vạch ngang.
- **Bước 2: Tính từ phải qua trái**
  - **Hàng đơn vị**: 8 - 5 = 3, viết 3.
  - **Hàng chục**: Hạ 4 xuống, viết 4.
  - Kết quả: **48 - 5 = 43**

#### 2. Mẹo tính nhẩm:
Lấy hàng đơn vị trừ trước: 8 - 5 = 3, ghép với 4 chục -> ra ngay 43!',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 28
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 27: Phép trừ số có hai chữ số cho số có một chữ số',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 27: Phép trừ số có hai chữ số cho số có một chữ số',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 27: Phép trừ số có hai chữ số cho số có một chữ số',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính nhẩm: 67 - 4 = ?',
        'Hàng đơn vị: 7 - 4 = 3, giữ nguyên 6 chục được 63.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 61', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 62', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 63', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 64', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Một cây ăn quả có 39 quả cam, mẹ hái xuống 7 quả. Hỏi trên cây còn lại mấy quả cam?',
        'Lấy 39 - 7 = 32 quả cam.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 31 quả', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 32 quả', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 33 quả', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 34 quả', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Điền số thích hợp vào chỗ chấm: 86 - ... = 81',
        'Lấy 86 - 81 = 5.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 4', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 5', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 6', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 7', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 29: Bài 28: Phép trừ số có hai chữ số cho số có hai chữ số
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 29 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 28: Phép trừ số có hai chữ số cho số có hai chữ số',
            'bai-28-phep-tru-so-co-hai-chu-so-cho-so-co-hai-chu-so',
            'Đặt tính thẳng cột: hàng chục trừ hàng chục, hàng đơn vị trừ hàng đơn vị.',
            '### ➖ Phép trừ số có hai chữ số cho số có hai chữ số (không nhớ)

#### 1. Ví dụ mẫu: 57 - 23 = ?
- **Bước 1: Đặt tính thẳng hàng**
  - Số 57 ở trên, số 23 ở dưới (7 thẳng 3, 5 thẳng 2).
  - Viết dấu **-** và kẻ gạch ngang.
- **Bước 2: Trừ từ phải sang trái**
  - **Hàng đơn vị**: 7 - 3 = 4, viết 4.
  - **Hàng chục**: 5 - 2 = 3, viết 3.
  - Kết quả: **57 - 23 = 34**

#### 2. Trường hợp hai số chục bằng nhau:
- Ví dụ: 68 - 62 = 6 (hàng chục 6 - 6 = 0, ta chỉ viết 6, không cần viết 06).',
            29,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 28: Phép trừ số có hai chữ số cho số có hai chữ số',
            slug = 'bai-28-phep-tru-so-co-hai-chu-so-cho-so-co-hai-chu-so',
            summary = 'Đặt tính thẳng cột: hàng chục trừ hàng chục, hàng đơn vị trừ hàng đơn vị.',
            content = '### ➖ Phép trừ số có hai chữ số cho số có hai chữ số (không nhớ)

#### 1. Ví dụ mẫu: 57 - 23 = ?
- **Bước 1: Đặt tính thẳng hàng**
  - Số 57 ở trên, số 23 ở dưới (7 thẳng 3, 5 thẳng 2).
  - Viết dấu **-** và kẻ gạch ngang.
- **Bước 2: Trừ từ phải sang trái**
  - **Hàng đơn vị**: 7 - 3 = 4, viết 4.
  - **Hàng chục**: 5 - 2 = 3, viết 3.
  - Kết quả: **57 - 23 = 34**

#### 2. Trường hợp hai số chục bằng nhau:
- Ví dụ: 68 - 62 = 6 (hàng chục 6 - 6 = 0, ta chỉ viết 6, không cần viết 06).',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 29
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 28: Phép trừ số có hai chữ số cho số có hai chữ số',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 28: Phép trừ số có hai chữ số cho số có hai chữ số',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 28: Phép trừ số có hai chữ số cho số có hai chữ số',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính kết quả phép tính: 78 - 34 = ?',
        'Hàng đơn vị: 8 - 4 = 4. Hàng chục: 7 - 3 = 4. Kết quả là 44.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 42', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 44', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 54', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 46', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Bác nông dân có 65 quả trứng gà, bác đã bán đi 25 quả. Hỏi bác còn lại bao nhiêu quả trứng gà?',
        'Phép tính: 65 - 25 = 40 quả trứng gà.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 35 quả', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 40 quả', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 45 quả', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 50 quả', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Kết quả của phép trừ: 49 - 41 = ?',
        '49 - 41 = 8 (hàng chục bằng nhau nên hiệu là số có 1 chữ số).',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 08', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 8', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 18', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 90', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 30: Bài 29: Luyện tập chung
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 30 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 29: Luyện tập chung',
            'bai-29-luyen-tap-chung',
            'Tổng hợp các kỹ năng tính toán trong phạm vi 100 và giải toán có lời văn một bước tính.',
            '### 🏅 Luyện tập chung Chương 7

#### 1. Bốn bước giải bài toán có lời văn điểm 10:
- **Bước 1**: Đọc kỹ đề bài 2 - 3 lần. Gạch chân từ khóa: *có tất cả / mua thêm* (làm phép **+**); *bớt đi / bán đi / biếu / cho / bay mất* (làm phép **-**).
- **Bước 2**: Viết câu lời giải chuẩn chỉnh.
- **Bước 3**: Viết phép tính kèm tên đơn vị trong dấu ngoặc đơn (ví dụ: *quả cam, học sinh, chiếc kẹo*).
- **Bước 4**: Ghi Đáp số rõ ràng.',
            30,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 29: Luyện tập chung',
            slug = 'bai-29-luyen-tap-chung',
            summary = 'Tổng hợp các kỹ năng tính toán trong phạm vi 100 và giải toán có lời văn một bước tính.',
            content = '### 🏅 Luyện tập chung Chương 7

#### 1. Bốn bước giải bài toán có lời văn điểm 10:
- **Bước 1**: Đọc kỹ đề bài 2 - 3 lần. Gạch chân từ khóa: *có tất cả / mua thêm* (làm phép **+**); *bớt đi / bán đi / biếu / cho / bay mất* (làm phép **-**).
- **Bước 2**: Viết câu lời giải chuẩn chỉnh.
- **Bước 3**: Viết phép tính kèm tên đơn vị trong dấu ngoặc đơn (ví dụ: *quả cam, học sinh, chiếc kẹo*).
- **Bước 4**: Ghi Đáp số rõ ràng.',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 30
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 29: Luyện tập chung',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 29: Luyện tập chung',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 29: Luyện tập chung',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Dãy tính: 20 + 30 - 10 có kết quả bằng bao nhiêu?',
        'Tính từ trái sang phải: 20 + 30 = 50, sau đó 50 - 10 = 40.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 30', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 40', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 50', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 60', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Đàn vịt có 54 con dưới ao và 23 con trên bờ. Hỏi đàn vịt có tất cả bao nhiêu con?',
        'Lấy 54 + 23 = 77 con vịt.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 71 con', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 75 con', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 77 con', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 87 con', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Phép tính nào dưới đây có kết quả BẰNG 50?',
        '80 - 30 = 50, chính xác bằng 50.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 25 + 35', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 80 - 30', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 90 - 30', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 15 + 25', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- ----------------------------------------------------
    -- CHƯƠNG 8: Chương 8: Thời gian, giờ và lịch
    -- ----------------------------------------------------
    SELECT id INTO v_chapter_id FROM chapters WHERE course_id = v_course_id AND display_order = 8 LIMIT 1;
    IF v_chapter_id IS NULL THEN
        v_chapter_id := gen_random_uuid();
        INSERT INTO chapters (id, course_id, title, description, display_order, is_sellable, created_at, updated_at)
        VALUES (v_chapter_id, v_course_id, 'Chương 8: Thời gian, giờ và lịch', 'Nhận biết các bộ phận của đồng hồ, xem giờ đúng, thuộc 7 ngày trong tuần, thực hành xem lịch và sắp xếp thời gian biểu sinh hoạt.', 8, false, now(), now());
    ELSE
        UPDATE chapters
        SET title = 'Chương 8: Thời gian, giờ và lịch',
            description = 'Nhận biết các bộ phận của đồng hồ, xem giờ đúng, thuộc 7 ngày trong tuần, thực hành xem lịch và sắp xếp thời gian biểu sinh hoạt.',
            updated_at = now()
        WHERE id = v_chapter_id;
    END IF;

    -- Bài 31: Bài 30: Xem giờ đúng trên đồng hồ
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 31 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 30: Xem giờ đúng trên đồng hồ',
            'bai-30-xem-gio-dung-tren-dong-ho',
            'Nhận biết kim ngắn chỉ giờ, kim dài chỉ phút; nhận biết giờ đúng khi kim dài chỉ số 12.',
            '### ⏰ Xem giờ đúng trên mặt đồng hồ

#### 1. Cấu tạo mặt đồng hồ tròn
- Trên mặt đồng hồ có **12 số** từ 1 đến 12 được xếp tròn đều.
- **Kim ngắn (kim giờ)**: Chạy chậm chạp, chỉ số giờ.
- **Kim dài (kim phút)**: Chạy nhanh hơn, chỉ số phút.

#### 2. Quy tắc vàng xem GIỜ ĐÚNG:
- Khi **kim dài chỉ thẳng vào số 12**: Đó là một giờ đúng!
- Kim ngắn chỉ vào số nào thì đồng hồ chỉ bấy nhiêu giờ.
  - Kim ngắn chỉ số **7**, kim dài chỉ số **12** -> **7 giờ đúng** (giờ bé thức dậy đi học 🎒).
  - Kim ngắn chỉ số **8**, kim dài chỉ số **12** -> **8 giờ đúng** (giờ vào tiết học đầu tiên).
  - Kim ngắn chỉ số **11**, kim dài chỉ số **12** -> **11 giờ đúng** (giờ ăn trưa 🥣).
  - Kim ngắn chỉ số **9**, kim dài chỉ số **12** (buổi tối) -> **9 giờ tối** (giờ bé đi ngủ 😴).',
            31,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 30: Xem giờ đúng trên đồng hồ',
            slug = 'bai-30-xem-gio-dung-tren-dong-ho',
            summary = 'Nhận biết kim ngắn chỉ giờ, kim dài chỉ phút; nhận biết giờ đúng khi kim dài chỉ số 12.',
            content = '### ⏰ Xem giờ đúng trên mặt đồng hồ

#### 1. Cấu tạo mặt đồng hồ tròn
- Trên mặt đồng hồ có **12 số** từ 1 đến 12 được xếp tròn đều.
- **Kim ngắn (kim giờ)**: Chạy chậm chạp, chỉ số giờ.
- **Kim dài (kim phút)**: Chạy nhanh hơn, chỉ số phút.

#### 2. Quy tắc vàng xem GIỜ ĐÚNG:
- Khi **kim dài chỉ thẳng vào số 12**: Đó là một giờ đúng!
- Kim ngắn chỉ vào số nào thì đồng hồ chỉ bấy nhiêu giờ.
  - Kim ngắn chỉ số **7**, kim dài chỉ số **12** -> **7 giờ đúng** (giờ bé thức dậy đi học 🎒).
  - Kim ngắn chỉ số **8**, kim dài chỉ số **12** -> **8 giờ đúng** (giờ vào tiết học đầu tiên).
  - Kim ngắn chỉ số **11**, kim dài chỉ số **12** -> **11 giờ đúng** (giờ ăn trưa 🥣).
  - Kim ngắn chỉ số **9**, kim dài chỉ số **12** (buổi tối) -> **9 giờ tối** (giờ bé đi ngủ 😴).',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 31
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 30: Xem giờ đúng trên đồng hồ',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 30: Xem giờ đúng trên đồng hồ',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 30: Xem giờ đúng trên đồng hồ',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Khi kim dài chỉ số 12 và kim ngắn chỉ số 4, đồng hồ đang chỉ mấy giờ?',
        'Kim dài chỉ số 12 là giờ đúng, kim ngắn chỉ số 4 nên đồng hồ đang chỉ 4 giờ.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 12 giờ', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 4 giờ', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 16 giờ', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 8 giờ', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Trong hai chiếc kim đồng hồ, kim nào dùng để chỉ GIỜ?',
        'Kim ngắn (mập hơn và ngắn hơn) chính là kim chỉ giờ.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Kim dài', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Kim ngắn', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Cả hai kim', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Kim mảnh nhất', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Bé An đi ngủ lúc 9 giờ tối. Lúc đó kim ngắn và kim dài chỉ số mấy?',
        '9 giờ đúng thì kim ngắn chỉ số 9 và kim dài chỉ số 12.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Kim ngắn chỉ số 12, kim dài chỉ số 9', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Kim ngắn chỉ số 9, kim dài chỉ số 12', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Cả hai kim chỉ số 9', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Cả hai kim chỉ số 12', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 32: Bài 31: Các ngày trong tuần
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 32 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 31: Các ngày trong tuần',
            'bai-31-cac-ngay-trong-tuan',
            'Một tuần lễ có 7 ngày; thuộc thứ tự các ngày trong tuần và phân biệt ngày đi học, ngày nghỉ cuối tuần.',
            '### 📅 Các ngày trong tuần lễ

#### 1. Một tuần lễ có bao nhiêu ngày?
- Một tuần lễ có đúng **7 ngày**.
- Thứ tự 7 ngày lần lượt là:
  1. **Thứ Hai**: Ngày đầu tuần em đi học hát Quốc ca chào cờ 🚩.
  2. **Thứ Ba**: Ngày học tập chăm ngoan.
  3. **Thứ Tư**: Giữa tuần vui vẻ.
  4. **Thứ Năm**: Tiếp tục rèn luyện chăm chỉ.
  5. **Thứ Sáu**: Ngày học cuối cùng trong tuần ở trường.
  6. **Thứ Bảy**: Ngày nghỉ cuối tuần, em giúp đỡ bố mẹ 🧹.
  7. **Chủ Nhật**: Ngày nghỉ ngơi, vui chơi cùng gia đình 🎡.

#### 2. Bé ghi nhớ:
- Các ngày đi học: Từ **Thứ Hai** đến **Thứ Sáu** (5 ngày).
- Hai ngày nghỉ cuối tuần: **Thứ Bảy** và **Chủ Nhật** (2 ngày).',
            32,
            40,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 31: Các ngày trong tuần',
            slug = 'bai-31-cac-ngay-trong-tuan',
            summary = 'Một tuần lễ có 7 ngày; thuộc thứ tự các ngày trong tuần và phân biệt ngày đi học, ngày nghỉ cuối tuần.',
            content = '### 📅 Các ngày trong tuần lễ

#### 1. Một tuần lễ có bao nhiêu ngày?
- Một tuần lễ có đúng **7 ngày**.
- Thứ tự 7 ngày lần lượt là:
  1. **Thứ Hai**: Ngày đầu tuần em đi học hát Quốc ca chào cờ 🚩.
  2. **Thứ Ba**: Ngày học tập chăm ngoan.
  3. **Thứ Tư**: Giữa tuần vui vẻ.
  4. **Thứ Năm**: Tiếp tục rèn luyện chăm chỉ.
  5. **Thứ Sáu**: Ngày học cuối cùng trong tuần ở trường.
  6. **Thứ Bảy**: Ngày nghỉ cuối tuần, em giúp đỡ bố mẹ 🧹.
  7. **Chủ Nhật**: Ngày nghỉ ngơi, vui chơi cùng gia đình 🎡.

#### 2. Bé ghi nhớ:
- Các ngày đi học: Từ **Thứ Hai** đến **Thứ Sáu** (5 ngày).
- Hai ngày nghỉ cuối tuần: **Thứ Bảy** và **Chủ Nhật** (2 ngày).',
            estimated_minutes = 40,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 32
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 31: Các ngày trong tuần',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 31: Các ngày trong tuần',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 31: Các ngày trong tuần',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Một tuần lễ có bao nhiêu ngày?',
        'Một tuần lễ luôn có 7 ngày từ Thứ Hai đến Chủ Nhật.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 5 ngày', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 6 ngày', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 7 ngày', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 10 ngày', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Nếu hôm nay là Thứ Ba thì ngày mai sẽ là thứ mấy?',
        'Ngay sau ngày Thứ Ba trong tuần lễ chính là ngày Thứ Tư.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Thứ Hai', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Thứ Tư', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Thứ Năm', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Chủ Nhật', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Hai ngày nghỉ cuối tuần của học sinh là hai ngày nào?',
        'Thứ Bảy và Chủ Nhật là hai ngày nghỉ ngơi cuối tuần sau những ngày học chăm chỉ.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Thứ Năm và Thứ Sáu', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Thứ Hai và Thứ Ba', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Thứ Bảy và Chủ Nhật', true, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Thứ Sáu và Thứ Bảy', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 33: Bài 32: Thực hành xem lịch và giờ
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 33 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 32: Thực hành xem lịch và giờ',
            'bai-32-thuc-hanh-xem-lich-va-gio',
            'Kỹ năng xem tờ lịch bóc hàng ngày (ngày dương lịch, thứ trong tuần) và lập thời gian biểu khoa học.',
            '### 🗓️ Thực hành xem lịch và lập thời gian biểu

#### 1. Xem tờ lịch ngày
- Trên một tờ lịch treo tường thường ghi rõ:
  - **Chữ to nhất ở giữa**: Chỉ ngày trong tháng (ví dụ: ngày 15).
  - **Chữ ở phía trên hoặc dưới**: Chỉ thứ trong tuần (ví dụ: Thứ Sáu).
  - **Tên tháng và năm**: Tháng 9 năm 2026.

#### 2. Thời gian biểu mẫu của bạn học sinh gương mẫu:
- ⏰ **6 giờ sáng**: Thức dậy, đánh răng, tập thể dục buổi sáng.
- ⏰ **7 giờ sáng**: Đến trường sẵn sàng vào lớp.
- ⏰ **11 giờ trưa**: Ăn trưa và ngủ trưa ngon giấc.
- ⏰ **4 giờ chiều (16 giờ)**: Tan trường về nhà với bố mẹ.
- ⏰ **7 giờ tối (19 giờ)**: Ôn bài và làm bài tập toán cùng NQD LMS.
- ⏰ **9 giờ tối (21 giờ)**: Lên giường đi ngủ để ngày mai dậy sớm khỏe khoắn!',
            33,
            45,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 32: Thực hành xem lịch và giờ',
            slug = 'bai-32-thuc-hanh-xem-lich-va-gio',
            summary = 'Kỹ năng xem tờ lịch bóc hàng ngày (ngày dương lịch, thứ trong tuần) và lập thời gian biểu khoa học.',
            content = '### 🗓️ Thực hành xem lịch và lập thời gian biểu

#### 1. Xem tờ lịch ngày
- Trên một tờ lịch treo tường thường ghi rõ:
  - **Chữ to nhất ở giữa**: Chỉ ngày trong tháng (ví dụ: ngày 15).
  - **Chữ ở phía trên hoặc dưới**: Chỉ thứ trong tuần (ví dụ: Thứ Sáu).
  - **Tên tháng và năm**: Tháng 9 năm 2026.

#### 2. Thời gian biểu mẫu của bạn học sinh gương mẫu:
- ⏰ **6 giờ sáng**: Thức dậy, đánh răng, tập thể dục buổi sáng.
- ⏰ **7 giờ sáng**: Đến trường sẵn sàng vào lớp.
- ⏰ **11 giờ trưa**: Ăn trưa và ngủ trưa ngon giấc.
- ⏰ **4 giờ chiều (16 giờ)**: Tan trường về nhà với bố mẹ.
- ⏰ **7 giờ tối (19 giờ)**: Ôn bài và làm bài tập toán cùng NQD LMS.
- ⏰ **9 giờ tối (21 giờ)**: Lên giường đi ngủ để ngày mai dậy sớm khỏe khoắn!',
            estimated_minutes = 45,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 33
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 32: Thực hành xem lịch và giờ',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 32: Thực hành xem lịch và giờ',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 32: Thực hành xem lịch và giờ',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tờ lịch ghi: ''Thứ Năm, Ngày 20''. Vậy ngày hôm trước tờ lịch đó là ngày mấy, thứ mấy?',
        'Ngày hôm trước của ngày 20 là ngày 19, trước Thứ Năm là Thứ Tư.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Thứ Tư, Ngày 19', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Thứ Sáu, Ngày 21', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Thứ Tư, Ngày 21', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Thứ Sáu, Ngày 19', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Hoạt động nào sau đây phù hợp với thời điểm 7 giờ sáng của bé?',
        '7 giờ sáng là lúc các bạn nhỏ ăn sáng xong và chuẩn bị vào lớp học tập.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Đi ngủ ban đêm', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Chuẩn bị đến trường học bài', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Ăn bữa cơm tối', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Đi xem phim khuya', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Kim ngắn chỉ số 6, kim dài chỉ số 12 lúc bình minh thức giấc. Đó là mấy giờ?',
        'Bình minh buổi sáng, kim ngắn chỉ số 6 là 6 giờ sáng.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 12 giờ trưa', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 6 giờ sáng', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 6 giờ tối', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 12 giờ đêm', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Bài 34: Bài 33: Luyện tập chung
    SELECT id INTO v_lesson_id FROM lessons WHERE chapter_id = v_chapter_id AND display_order = 34 LIMIT 1;
    IF v_lesson_id IS NULL THEN
        v_lesson_id := gen_random_uuid();
        INSERT INTO lessons (
            id, chapter_id, title, slug, summary, content,
            display_order, estimated_minutes, status, is_preview, is_sellable,
            created_at, updated_at
        ) VALUES (
            v_lesson_id,
            v_chapter_id,
            'Bài 33: Luyện tập chung',
            'bai-33-luyen-tap-chung',
            'Ôn tập tổng kết toàn diện toàn bộ chương trình Toán 1: số học, hình học, đo lường, thời gian.',
            '### 🎓 Tổng kết Chương trình Toán 1: Bé là Hiệp sĩ Toán học!

#### 1. Chúc mừng các em đã hoàn thành xuất sắc chặng đường Toán 1!
Chúng mình đã cùng nhau học được biết bao nhiêu điều kỳ diệu:
1. 🔢 **Số học**: Biết đếm, đọc, viết, so sánh các số từ **0 đến 100**.
2. ➕➖ **Phép tính**: Thành thạo bảng cộng, trừ phạm vi 10 và cộng trừ không nhớ phạm vi 100.
3. 📐 **Hình học**: Gọi tên chuẩn xác các hình phẳng (*vuông, tròn, tam giác, chữ nhật*) và khối hình (*lập phương, hộp chữ nhật*).
4. 📏 **Đo lường**: Biết dùng thước kẻ đo độ dài chính xác từng **xăng-ti-mét (cm)**.
5. ⏰ **Thời gian**: Tự tin nhìn đồng hồ xem giờ đúng và đọc lịch 7 ngày trong tuần.

#### 2. Lời dặn dò thân thương
Toán học là người bạn thân thiết sẽ cùng các em lớn khôn từng ngày. Hãy luôn tự tin, chăm chỉ và yêu thích khám phá các con số nhé! 🌟❤️',
            34,
            50,
            'PUBLISHED',
            true,
            false,
            now(),
            now()
        );
    ELSE
        UPDATE lessons
        SET title = 'Bài 33: Luyện tập chung',
            slug = 'bai-33-luyen-tap-chung',
            summary = 'Ôn tập tổng kết toàn diện toàn bộ chương trình Toán 1: số học, hình học, đo lường, thời gian.',
            content = '### 🎓 Tổng kết Chương trình Toán 1: Bé là Hiệp sĩ Toán học!

#### 1. Chúc mừng các em đã hoàn thành xuất sắc chặng đường Toán 1!
Chúng mình đã cùng nhau học được biết bao nhiêu điều kỳ diệu:
1. 🔢 **Số học**: Biết đếm, đọc, viết, so sánh các số từ **0 đến 100**.
2. ➕➖ **Phép tính**: Thành thạo bảng cộng, trừ phạm vi 10 và cộng trừ không nhớ phạm vi 100.
3. 📐 **Hình học**: Gọi tên chuẩn xác các hình phẳng (*vuông, tròn, tam giác, chữ nhật*) và khối hình (*lập phương, hộp chữ nhật*).
4. 📏 **Đo lường**: Biết dùng thước kẻ đo độ dài chính xác từng **xăng-ti-mét (cm)**.
5. ⏰ **Thời gian**: Tự tin nhìn đồng hồ xem giờ đúng và đọc lịch 7 ngày trong tuần.

#### 2. Lời dặn dò thân thương
Toán học là người bạn thân thiết sẽ cùng các em lớn khôn từng ngày. Hãy luôn tự tin, chăm chỉ và yêu thích khám phá các con số nhé! 🌟❤️',
            estimated_minutes = 50,
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_lesson_id;
    END IF;

    -- Practice Exercise for Lesson 34
    SELECT id INTO v_exercise_id FROM exercises WHERE lesson_id = v_lesson_id LIMIT 1;
    IF v_exercise_id IS NULL THEN
        v_exercise_id := gen_random_uuid();
        INSERT INTO exercises (
            id, lesson_id, title, description, instructions,
            type, time_limit_minutes, passing_score, status,
            show_explanation_immediately, allow_retry, created_at, updated_at
        ) VALUES (
            v_exercise_id,
            v_lesson_id,
            'Bài tập củng cố: Bài 33: Luyện tập chung',
            '3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: Bài 33: Luyện tập chung',
            'Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!',
            'PRACTICE',
            10,
            2.00,
            'PUBLISHED',
            true,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE exercises
        SET title = 'Bài tập củng cố: Bài 33: Luyện tập chung',
            status = 'PUBLISHED',
            updated_at = now()
        WHERE id = v_exercise_id;
    END IF;

    -- Question 1
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Số lớn nhất có hai chữ số khác nhau là số nào?',
        'Số 99 có hai chữ số giống nhau, do đó số lớn nhất có hai chữ số KHÁC NHAU là số 98.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 99', false, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 98', true, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 90', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 100', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 1, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 2
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Tính kết quả: 45 cm + 20 cm - 15 cm = ?',
        'Lấy 45 cm + 20 cm = 65 cm; sau đó lấy 65 cm - 15 cm = 50 cm.',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. 50 cm', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. 55 cm', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. 65 cm', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. 40 cm', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 2, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    -- Question 3
    v_question_id := gen_random_uuid();
    INSERT INTO questions (
        id, subject_id, course_id, lesson_id, grade_level,
        question_type, difficulty, content, explanation, default_marks,
        source, status, created_at, updated_at
    ) VALUES (
        v_question_id,
        v_subject_id,
        v_course_id,
        v_lesson_id,
        'Lớp 1',
        'MULTIPLE_CHOICE',
        'EASY',
        'Nếu ngày mai là Thứ Bảy thì hôm qua là thứ mấy?',
        'Ngày mai là Thứ Bảy thì hôm nay là Thứ Sáu. Vậy hôm qua chính là Thứ Năm!',
        1.00,
        'MANUAL',
        'APPROVED',
        now(),
        now()
    );

    -- Question Options
    INSERT INTO question_options (id, question_id, option_key, option_text, is_correct, display_order, created_at)
    VALUES
        (gen_random_uuid(), v_question_id, 'A', 'A. Thứ Năm', true, 1, now()),
        (gen_random_uuid(), v_question_id, 'B', 'B. Thứ Sáu', false, 2, now()),
        (gen_random_uuid(), v_question_id, 'C', 'C. Chủ Nhật', false, 3, now()),
        (gen_random_uuid(), v_question_id, 'D', 'D. Thứ Tư', false, 4, now());

    -- Link Question to Exercise
    INSERT INTO exercise_questions (exercise_id, question_id, display_order, marks)
    VALUES (v_exercise_id, v_question_id, 3, 1.00)
    ON CONFLICT (exercise_id, display_order) DO UPDATE
    SET question_id = EXCLUDED.question_id,
        marks = 1.00;

    RAISE NOTICE 'Seed Toan 1 successfully finished!';
END $$;

COMMIT;
