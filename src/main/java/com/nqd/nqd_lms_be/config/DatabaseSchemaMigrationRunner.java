package com.nqd.nqd_lms_be.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private static volatile boolean migrated = false;

    @Override
    public void run(String... args) {
        migrateDatabaseSchema();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public synchronized void migrateDatabaseSchema() {
        if (migrated) {
            return;
        }
        log.info("Checking and applying database schema migrations...");
        try {
            // 1. Alter courses table for Phase 3 Monetization
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS pricing_type VARCHAR(20) DEFAULT 'FREE'");
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS price NUMERIC(15, 2) DEFAULT 0.00");
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS sale_price NUMERIC(15, 2)");
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'VND'");
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS published_at TIMESTAMP WITHOUT TIME ZONE");
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS reject_reason TEXT");
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS average_rating DOUBLE PRECISION DEFAULT 0.0");
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS enrollment_count INTEGER DEFAULT 0");

            // 2. Alter lessons table
            jdbcTemplate.execute("ALTER TABLE lessons ADD COLUMN IF NOT EXISTS is_preview BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_url TEXT");

            // 2b. Alter lesson_progress table
            jdbcTemplate.execute("ALTER TABLE lesson_progress ADD COLUMN IF NOT EXISTS is_unlocked_by_admin BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE lesson_progress SET is_unlocked_by_admin = false WHERE is_unlocked_by_admin IS NULL");
            jdbcTemplate.execute("ALTER TABLE lesson_progress ADD COLUMN IF NOT EXISTS video_watched BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE lesson_progress SET video_watched = false WHERE video_watched IS NULL");

            // 3. Alter lesson_resources table
            jdbcTemplate.execute("ALTER TABLE lesson_resources ADD COLUMN IF NOT EXISTS is_preview BOOLEAN DEFAULT FALSE");

            // 4. Create course_reviews table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS course_reviews (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    rating INTEGER NOT NULL,
                    comment TEXT,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    CONSTRAINT uk_course_review_user UNIQUE (course_id, user_id)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_course_reviews_course ON course_reviews(course_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_course_reviews_user ON course_reviews(user_id)");

            // 5. Create user_usage_records table for Phase 2 Quota Control
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_usage_records (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    feature_key VARCHAR(64) NOT NULL,
                    period_key VARCHAR(64) NOT NULL,
                    used_count BIGINT NOT NULL DEFAULT 0,
                    last_used_at TIMESTAMP WITHOUT TIME ZONE,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    CONSTRAINT uk_user_feature_period UNIQUE (user_id, feature_key, period_key)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_usage_lookup ON user_usage_records(user_id, feature_key, period_key)");

            // 6. Alter membership_plans table if needed
            jdbcTemplate.execute("ALTER TABLE membership_plans ADD COLUMN IF NOT EXISTS user_type VARCHAR(20) DEFAULT 'STUDENT'");
            jdbcTemplate.execute("ALTER TABLE membership_plans ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE membership_plans ADD COLUMN IF NOT EXISTS exam_limit_per_week INTEGER");
            jdbcTemplate.execute("ALTER TABLE membership_plans ADD COLUMN IF NOT EXISTS ai_question_limit_per_day INTEGER");
            jdbcTemplate.execute("ALTER TABLE membership_plans ADD COLUMN IF NOT EXISTS max_classes_limit INTEGER");
            jdbcTemplate.execute("ALTER TABLE membership_plans ADD COLUMN IF NOT EXISTS max_questions_limit INTEGER");

            // 7. Phase 4 - Finance & Revenue Sharing tables
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS teacher_earnings (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    order_id UUID NOT NULL REFERENCES orders(id),
                    order_item_id UUID NOT NULL REFERENCES order_items(id),
                    course_id UUID NOT NULL REFERENCES courses(id),
                    teacher_id UUID NOT NULL REFERENCES users(id),
                    gross_amount NUMERIC(15, 2) NOT NULL,
                    platform_fee_rate NUMERIC(7, 4) NOT NULL,
                    platform_fee NUMERIC(15, 2) NOT NULL,
                    teacher_share_rate NUMERIC(7, 4) NOT NULL,
                    teacher_amount NUMERIC(15, 2) NOT NULL,
                    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    available_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                    reversed_at TIMESTAMP WITHOUT TIME ZONE,
                    reversal_reason TEXT,
                    version BIGINT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    CONSTRAINT uk_teacher_earning_order_item UNIQUE (order_item_id)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_earnings_teacher ON teacher_earnings(teacher_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_earnings_status ON teacher_earnings(status)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_earnings_available_at ON teacher_earnings(available_at)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_earnings_order ON teacher_earnings(order_id)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS teacher_bank_accounts (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    bank_name VARCHAR(255) NOT NULL,
                    bank_code VARCHAR(50) NOT NULL,
                    account_number VARCHAR(100) NOT NULL,
                    account_holder_name VARCHAR(255) NOT NULL,
                    is_default BOOLEAN NOT NULL DEFAULT TRUE,
                    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_bank_accounts_teacher ON teacher_bank_accounts(teacher_id)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS teacher_withdrawals (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    withdrawal_code VARCHAR(64) NOT NULL UNIQUE,
                    teacher_id UUID NOT NULL REFERENCES users(id),
                    amount NUMERIC(15, 2) NOT NULL,
                    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
                    bank_name VARCHAR(255) NOT NULL,
                    bank_code VARCHAR(50) NOT NULL,
                    account_number VARCHAR(100) NOT NULL,
                    account_holder_name VARCHAR(255) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    idempotency_key VARCHAR(128) UNIQUE,
                    reference_code VARCHAR(128),
                    rejection_reason TEXT,
                    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                    processed_at TIMESTAMP WITHOUT TIME ZONE,
                    processed_by VARCHAR(255),
                    version BIGINT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_withdrawals_code ON teacher_withdrawals(withdrawal_code)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_withdrawals_teacher ON teacher_withdrawals(teacher_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_withdrawals_status ON teacher_withdrawals(status)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_withdrawals_idempotency ON teacher_withdrawals(idempotency_key)");

            // 8. Fix legacy status check constraints that predate the current enum values
            jdbcTemplate.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check");
            jdbcTemplate.execute("ALTER TABLE payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_status_check");
            jdbcTemplate.execute("ALTER TABLE subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check");
            jdbcTemplate.execute("ALTER TABLE course_enrollments DROP CONSTRAINT IF EXISTS course_enrollments_status_check");

            // 9. Phase 2 - Certificates table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS certificates (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    certificate_code VARCHAR(64) NOT NULL UNIQUE,
                    enrollment_id UUID NOT NULL REFERENCES course_enrollments(id),
                    course_id UUID NOT NULL REFERENCES courses(id),
                    student_id UUID NOT NULL REFERENCES users(id),
                    issued_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    expiry_date TIMESTAMP WITHOUT TIME ZONE,
                    is_revoked BOOLEAN DEFAULT FALSE,
                    revocation_reason TEXT,
                    final_grade DOUBLE PRECISION,
                    metadata_json TEXT,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    CONSTRAINT uk_certificate_course_student UNIQUE (course_id, student_id)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_certificates_code ON certificates(certificate_code)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_certificates_student ON certificates(student_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_certificates_course ON certificates(course_id)");

            // 10. Phase 3 - Discussion Threads, Posts, Reactions & Course Announcements
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS discussion_threads (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
                    lesson_id UUID REFERENCES lessons(id) ON DELETE SET NULL,
                    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    title VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    is_pinned BOOLEAN DEFAULT FALSE,
                    is_locked BOOLEAN DEFAULT FALSE,
                    status VARCHAR(32) DEFAULT 'OPEN',
                    post_count INTEGER DEFAULT 0,
                    view_count INTEGER DEFAULT 0,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_discussion_threads_course ON discussion_threads(course_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_discussion_threads_lesson ON discussion_threads(lesson_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_discussion_threads_author ON discussion_threads(author_id)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS discussion_posts (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    thread_id UUID NOT NULL REFERENCES discussion_threads(id) ON DELETE CASCADE,
                    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    content TEXT NOT NULL,
                    is_answer BOOLEAN DEFAULT FALSE,
                    upvote_count INTEGER DEFAULT 0,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_discussion_posts_thread ON discussion_posts(thread_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_discussion_posts_author ON discussion_posts(author_id)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS post_reactions (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    post_id UUID NOT NULL REFERENCES discussion_posts(id) ON DELETE CASCADE,
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    type VARCHAR(32) NOT NULL DEFAULT 'UPVOTE',
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    CONSTRAINT uk_post_user_reaction UNIQUE (post_id, user_id, type)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_post_reactions_post ON post_reactions(post_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_post_reactions_user ON post_reactions(user_id)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS thread_reactions (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    thread_id UUID NOT NULL REFERENCES discussion_threads(id) ON DELETE CASCADE,
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    type VARCHAR(32) NOT NULL DEFAULT 'LIKE',
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    CONSTRAINT uk_thread_user_reaction UNIQUE (thread_id, user_id)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_thread_reactions_thread ON thread_reactions(thread_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_thread_reactions_user ON thread_reactions(user_id)");

            try {
                jdbcTemplate.execute("UPDATE post_reactions SET type = 'LIKE' WHERE type = 'UPVOTE'");
            } catch (Exception ignored) {}

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS course_announcements (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
                    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    title VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    posted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_course_announcements_course ON course_announcements(course_id)");

            // 11. Phase 4 - Exercise configs and Exercise Attempts & Answers
            jdbcTemplate.execute("ALTER TABLE exercises ADD COLUMN IF NOT EXISTS max_attempts INTEGER");
            jdbcTemplate.execute("ALTER TABLE exercises ADD COLUMN IF NOT EXISTS show_explanation_immediately BOOLEAN DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE exercises ADD COLUMN IF NOT EXISTS allow_retry BOOLEAN DEFAULT TRUE");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS exercise_attempts (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    exercise_id UUID NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
                    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    attempt_number INTEGER NOT NULL DEFAULT 1,
                    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
                    started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    submitted_at TIMESTAMP WITHOUT TIME ZONE,
                    total_score NUMERIC(7, 2),
                    max_score NUMERIC(7, 2),
                    percentage NUMERIC(5, 2),
                    passed BOOLEAN,
                    correct_count INTEGER,
                    total_questions INTEGER,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    CONSTRAINT uk_exercise_attempt_number UNIQUE (exercise_id, student_id, attempt_number)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exercise_attempts_exercise ON exercise_attempts(exercise_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exercise_attempts_student ON exercise_attempts(student_id)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS exercise_attempt_answers (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    attempt_id UUID NOT NULL REFERENCES exercise_attempts(id) ON DELETE CASCADE,
                    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
                    selected_option_id UUID REFERENCES question_options(id) ON DELETE SET NULL,
                    answer_text TEXT,
                    is_correct BOOLEAN,
                    marks_awarded NUMERIC(7, 2),
                    max_marks NUMERIC(7, 2) NOT NULL DEFAULT 1.00,
                    ai_explanation TEXT,
                    answered_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    CONSTRAINT uk_exercise_attempt_question UNIQUE (attempt_id, question_id)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exercise_attempt_answers_attempt ON exercise_attempt_answers(attempt_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exercise_attempt_answers_question ON exercise_attempt_answers(question_id)");

            // 12. Phase 5 - Shared Exam Library (visibility & origin_exam_id)
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) DEFAULT 'PRIVATE'");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS origin_exam_id UUID REFERENCES exams(id) ON DELETE SET NULL");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exams_visibility ON exams(visibility)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exams_origin_exam ON exams(origin_exam_id)");

            // 13. Phase 6 - Proctoring & Anti-Cheat System
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS max_violation_count INTEGER DEFAULT 5");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS enable_proctoring BOOLEAN DEFAULT FALSE");

            jdbcTemplate.execute("ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS violation_count INTEGER DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS is_flagged BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS flag_reason TEXT");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS exam_attempt_events (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    attempt_id UUID NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
                    event_type VARCHAR(30) NOT NULL,
                    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    metadata TEXT,
                    is_violation BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exam_attempt_events_attempt ON exam_attempt_events(attempt_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exam_attempt_events_occurred ON exam_attempt_events(occurred_at)");

            // 8. User Onboarding & Teacher Applications
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_onboarded BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("""
                UPDATE users SET is_onboarded = TRUE 
                WHERE id IN (
                    SELECT ur.user_id FROM user_roles ur 
                    JOIN roles r ON ur.role_id = r.id 
                    WHERE r.name IN ('TEACHER', 'ADMIN', 'ROLE_TEACHER', 'ROLE_ADMIN')
                )
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS teacher_applications (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    applicant_type VARCHAR(50) NOT NULL,
                    full_name VARCHAR(255) NOT NULL,
                    phone_number VARCHAR(50) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    institution_name VARCHAR(255) NOT NULL,
                    major_or_subject VARCHAR(255) NOT NULL,
                    bio TEXT,
                    document_urls TEXT,
                    id_card_front_url TEXT,
                    id_card_back_url TEXT,
                    sample_video_url VARCHAR(500),
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    reject_reason TEXT,
                    reviewed_by UUID REFERENCES users(id),
                    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_app_user ON teacher_applications(user_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_teacher_app_status ON teacher_applications(status)");

            // 9. Dedicated Knowledge Base Schema (Admin managed GDPT standard curriculum)
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_curriculums (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    grade_level VARCHAR(50) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    description TEXT,
                    education_tier VARCHAR(50) DEFAULT 'Tiểu học',
                    thumbnail_url VARCHAR(500),
                    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                    display_order INTEGER DEFAULT 1,
                    is_published BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_k_curriculums_sub_grade ON knowledge_curriculums(subject_id, grade_level)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_chapters (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    curriculum_id UUID NOT NULL REFERENCES knowledge_curriculums(id) ON DELETE CASCADE,
                    chapter_order INTEGER NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    description TEXT,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_k_chapters_curriculum ON knowledge_chapters(curriculum_id)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_lessons (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    chapter_id UUID NOT NULL REFERENCES knowledge_chapters(id) ON DELETE CASCADE,
                    lesson_order INTEGER NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    slug VARCHAR(255),
                    summary TEXT,
                    theory_markdown TEXT,
                    estimated_minutes INTEGER DEFAULT 40,
                    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255)
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_k_lessons_chapter ON knowledge_lessons(chapter_id)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_questions (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    lesson_id UUID NOT NULL REFERENCES knowledge_lessons(id) ON DELETE CASCADE,
                    question_order INTEGER NOT NULL,
                    question_text TEXT NOT NULL,
                    options_json TEXT NOT NULL,
                    correct_answer VARCHAR(10) NOT NULL,
                    explanation TEXT,
                    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    is_active BOOLEAN DEFAULT TRUE,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at TIMESTAMP WITHOUT TIME ZONE,
                    deleted_by VARCHAR(255),
                    created_by_user VARCHAR(255),
                    updated_by VARCHAR(255)
                )
            """);
            // 10. Classroom Meeting & Feature columns
            jdbcTemplate.execute("ALTER TABLE classrooms ADD COLUMN IF NOT EXISTS lark_meeting_url TEXT");
            jdbcTemplate.execute("ALTER TABLE classrooms ADD COLUMN IF NOT EXISTS meeting_id VARCHAR(100)");
            jdbcTemplate.execute("ALTER TABLE classrooms ADD COLUMN IF NOT EXISTS passcode VARCHAR(100)");
            jdbcTemplate.execute("ALTER TABLE classrooms ADD COLUMN IF NOT EXISTS meeting_note TEXT");
            jdbcTemplate.execute("ALTER TABLE classrooms ADD COLUMN IF NOT EXISTS is_live_now BOOLEAN NOT NULL DEFAULT FALSE");

            // 11. Classroom Materials (Lesson syllabus columns)
            jdbcTemplate.execute("ALTER TABLE classroom_materials ADD COLUMN IF NOT EXISTS chapter_title VARCHAR(255) DEFAULT 'Chủ đề chung'");
            jdbcTemplate.execute("ALTER TABLE classroom_materials ADD COLUMN IF NOT EXISTS lesson_order INTEGER DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE classroom_materials ADD COLUMN IF NOT EXISTS content TEXT");
            jdbcTemplate.execute("ALTER TABLE classroom_materials ADD COLUMN IF NOT EXISTS video_url TEXT");
            jdbcTemplate.execute("ALTER TABLE classroom_materials ADD COLUMN IF NOT EXISTS attachment_name VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE classroom_materials ALTER COLUMN file_url DROP NOT NULL");

            // 12. English Listening Audio & Script columns for Questions, Exams, and AI Generated Questions
            jdbcTemplate.execute("ALTER TABLE questions ADD COLUMN IF NOT EXISTS audio_url TEXT");
            jdbcTemplate.execute("ALTER TABLE questions ADD COLUMN IF NOT EXISTS audio_script TEXT");

            jdbcTemplate.execute("ALTER TABLE ai_generated_questions ADD COLUMN IF NOT EXISTS audio_url TEXT");
            jdbcTemplate.execute("ALTER TABLE ai_generated_questions ADD COLUMN IF NOT EXISTS audio_script TEXT");

            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS audio_url TEXT");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS audio_script TEXT");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS max_listening_plays INTEGER DEFAULT 2");

            log.info("Database schema migrations verified and applied successfully.");
        } catch (Exception e) {
            log.error("Error during database schema migration: {}", e.getMessage(), e);
        }
    }
}
