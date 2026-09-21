-- AI Learning & Assessment Platform
-- PostgreSQL schema
-- Target: Spring Boot 4.1.0 / JDK 17 / PostgreSQL
-- Design: production-oriented MVP, modular-monolith ready

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- COMMON: updated_at trigger
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 1. USERS & AUTHENTICATION
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL,
    avatar_url      TEXT,
    phone_number    VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMPTZ
);

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role_id     UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE oauth_accounts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    provider          VARCHAR(50) NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    provider_email    VARCHAR(255),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id)
);

-- ============================================================
-- 2. EDUCATION STRUCTURE
-- ============================================================

CREATE TABLE subjects (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE courses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id    UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    name          VARCHAR(255) NOT NULL,
    code          VARCHAR(50) NOT NULL UNIQUE,
    description   TEXT,
    grade_level   VARCHAR(50),
    thumbnail_url TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    created_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE course_teachers (
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE RESTRICT,
    teacher_id  UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (course_id, teacher_id)
);

CREATE TABLE course_enrollments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID NOT NULL REFERENCES courses(id) ON DELETE RESTRICT,
    student_id   UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status       VARCHAR(20) NOT NULL DEFAULT 'ENROLLED'
                 CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED')),
    enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    UNIQUE (course_id, student_id),
    CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR status <> 'COMPLETED'
    )
);

CREATE TABLE chapters (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id     UUID NOT NULL REFERENCES courses(id) ON DELETE RESTRICT,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    display_order INT NOT NULL CHECK (display_order > 0),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (course_id, display_order)
);

CREATE TABLE lessons (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id        UUID NOT NULL REFERENCES chapters(id) ON DELETE RESTRICT,
    title             VARCHAR(255) NOT NULL,
    slug              VARCHAR(255),
    summary           TEXT,
    content           TEXT,
    display_order     INT NOT NULL CHECK (display_order > 0),
    estimated_minutes INT CHECK (estimated_minutes IS NULL OR estimated_minutes > 0),
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                      CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (chapter_id, display_order)
);

CREATE UNIQUE INDEX uq_lessons_slug
    ON lessons(slug)
    WHERE slug IS NOT NULL;

CREATE TABLE lesson_resources (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id     UUID NOT NULL REFERENCES lessons(id) ON DELETE RESTRICT,
    resource_type VARCHAR(30) NOT NULL
                  CHECK (resource_type IN ('IMAGE', 'VIDEO', 'PDF', 'DOCUMENT', 'LINK', 'OTHER')),
    title         VARCHAR(255) NOT NULL,
    url           TEXT NOT NULL,
    metadata      JSONB,
    display_order INT NOT NULL DEFAULT 1 CHECK (display_order > 0),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (lesson_id, display_order)
);

-- ============================================================
-- 3. QUESTION BANK
-- ============================================================

CREATE TABLE questions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id     UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    course_id      UUID REFERENCES courses(id) ON DELETE SET NULL,
    lesson_id      UUID REFERENCES lessons(id) ON DELETE SET NULL,
    question_type  VARCHAR(30) NOT NULL
                   CHECK (question_type IN (
                       'MULTIPLE_CHOICE',
                       'TRUE_FALSE',
                       'SHORT_ANSWER',
                       'FILL_IN_THE_BLANK',
                       'ESSAY'
                   )),
    difficulty     VARCHAR(20) NOT NULL
                   CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    content        TEXT NOT NULL,
    explanation    TEXT,
    default_marks  NUMERIC(7,2) NOT NULL DEFAULT 1.00
                   CHECK (default_marks > 0),
    source         VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
                   CHECK (source IN ('MANUAL', 'AI_GENERATED', 'IMPORTED')),
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN (
                       'DRAFT',
                       'REVIEW',
                       'APPROVED',
                       'REJECTED',
                       'ARCHIVED'
                   )),
    created_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE question_options (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id   UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    option_key    VARCHAR(10) NOT NULL,
    option_text   TEXT NOT NULL,
    is_correct    BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL CHECK (display_order > 0),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (question_id, option_key),
    UNIQUE (question_id, display_order),
    UNIQUE (question_id, id)
);

CREATE TABLE question_tags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE question_tag_relations (
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    tag_id      UUID NOT NULL REFERENCES question_tags(id) ON DELETE RESTRICT,
    PRIMARY KEY (question_id, tag_id)
);

-- ============================================================
-- 4. GRADING RUBRICS
-- ============================================================

CREATE TABLE grading_rubrics (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    total_points NUMERIC(7,2) NOT NULL CHECK (total_points > 0),
    created_by   UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE grading_rubric_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_id    UUID NOT NULL REFERENCES grading_rubrics(id) ON DELETE RESTRICT,
    criterion    VARCHAR(255) NOT NULL,
    description  TEXT,
    max_points   NUMERIC(7,2) NOT NULL CHECK (max_points > 0),
    display_order INT NOT NULL CHECK (display_order > 0),
    UNIQUE (rubric_id, display_order)
);

-- ============================================================
-- 5. EXERCISES
-- ============================================================

CREATE TABLE exercises (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id           UUID NOT NULL REFERENCES lessons(id) ON DELETE RESTRICT,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    instructions        TEXT,
    type                VARCHAR(20) NOT NULL DEFAULT 'PRACTICE'
                        CHECK (type IN ('PRACTICE', 'HOMEWORK', 'QUIZ')),
    time_limit_minutes  INT
                        CHECK (time_limit_minutes IS NULL OR time_limit_minutes > 0),
    passing_score       NUMERIC(7,2)
                        CHECK (passing_score IS NULL OR passing_score >= 0),
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE exercise_questions (
    exercise_id   UUID NOT NULL REFERENCES exercises(id) ON DELETE RESTRICT,
    question_id   UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    display_order INT NOT NULL CHECK (display_order > 0),
    marks         NUMERIC(7,2) NOT NULL CHECK (marks > 0),
    PRIMARY KEY (exercise_id, question_id),
    UNIQUE (exercise_id, display_order)
);

-- ============================================================
-- 6. EXAMS & BLUEPRINTS
-- ============================================================

CREATE TABLE exam_templates (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id         UUID REFERENCES courses(id) ON DELETE RESTRICT,
    subject_id        UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    duration_minutes  INT NOT NULL CHECK (duration_minutes > 0),
    total_marks       NUMERIC(7,2) NOT NULL CHECK (total_marks > 0),
    passing_marks     NUMERIC(7,2) NOT NULL CHECK (passing_marks >= 0),
    blueprint_config  JSONB,
    created_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (passing_marks <= total_marks)
);

CREATE TABLE exams (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id         UUID REFERENCES courses(id) ON DELETE RESTRICT,
    subject_id        UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    template_id       UUID REFERENCES exam_templates(id) ON DELETE SET NULL,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    instructions      TEXT,
    duration_minutes  INT NOT NULL CHECK (duration_minutes > 0),
    total_marks       NUMERIC(7,2) NOT NULL CHECK (total_marks > 0),
    passing_marks     NUMERIC(7,2) NOT NULL CHECK (passing_marks >= 0),
    max_attempts      INT NOT NULL DEFAULT 1 CHECK (max_attempts > 0),
    shuffle_questions BOOLEAN NOT NULL DEFAULT FALSE,
    shuffle_options   BOOLEAN NOT NULL DEFAULT FALSE,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                      CHECK (status IN ('DRAFT', 'REVIEW', 'PUBLISHED', 'CLOSED', 'ARCHIVED')),
    start_at          TIMESTAMPTZ,
    end_at            TIMESTAMPTZ,
    created_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (passing_marks <= total_marks),
    CHECK (end_at IS NULL OR start_at IS NULL OR end_at > start_at)
);

CREATE TABLE exam_blueprints (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id           UUID NOT NULL UNIQUE REFERENCES exams(id) ON DELETE RESTRICT,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    total_questions   INT NOT NULL CHECK (total_questions > 0),
    total_marks       NUMERIC(7,2) NOT NULL CHECK (total_marks > 0),
    configuration     JSONB,
    created_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE exam_blueprint_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blueprint_id        UUID NOT NULL REFERENCES exam_blueprints(id) ON DELETE RESTRICT,
    tag_id              UUID REFERENCES question_tags(id) ON DELETE SET NULL,
    question_type       VARCHAR(30) NOT NULL
                        CHECK (question_type IN (
                            'MULTIPLE_CHOICE',
                            'TRUE_FALSE',
                            'SHORT_ANSWER',
                            'FILL_IN_THE_BLANK',
                            'ESSAY'
                        )),
    difficulty          VARCHAR(20) NOT NULL
                        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    question_count      INT NOT NULL CHECK (question_count > 0),
    marks_per_question  NUMERIC(7,2) NOT NULL CHECK (marks_per_question > 0),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE exam_questions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id       UUID NOT NULL REFERENCES exams(id) ON DELETE RESTRICT,
    question_id   UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    display_order INT NOT NULL CHECK (display_order > 0),
    marks         NUMERIC(7,2) NOT NULL CHECK (marks > 0),
    PRIMARY KEY (exam_id, question_id),
    UNIQUE (exam_id, display_order),
    UNIQUE (id, exam_id)
);

-- ============================================================
-- 7. EXAM VERSIONS / MULTIPLE CODES
-- ============================================================

CREATE TABLE exam_versions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id     UUID NOT NULL REFERENCES exams(id) ON DELETE RESTRICT,
    version_code VARCHAR(20) NOT NULL,
    version_name VARCHAR(100),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (exam_id, version_code),
    UNIQUE (id, exam_id)
);

CREATE TABLE exam_version_questions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_version_id   UUID NOT NULL REFERENCES exam_versions(id) ON DELETE RESTRICT,
    exam_question_id  UUID NOT NULL REFERENCES exam_questions(id) ON DELETE RESTRICT,
    display_order     INT NOT NULL CHECK (display_order > 0),
    UNIQUE (exam_version_id, exam_question_id),
    UNIQUE (exam_version_id, display_order)
);

CREATE TABLE exam_version_options (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_version_question_id UUID NOT NULL REFERENCES exam_version_questions(id) ON DELETE RESTRICT,
    question_option_id      UUID NOT NULL REFERENCES question_options(id) ON DELETE RESTRICT,
    display_order            INT NOT NULL CHECK (display_order > 0),
    UNIQUE (exam_version_question_id, question_option_id),
    UNIQUE (exam_version_question_id, display_order)
);

-- ============================================================
-- 8. EXAM ATTEMPTS & ANSWERS
-- ============================================================

CREATE TABLE exam_attempts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id           UUID NOT NULL REFERENCES exams(id) ON DELETE RESTRICT,
    exam_version_id   UUID REFERENCES exam_versions(id) ON DELETE RESTRICT,
    student_id        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    attempt_number    INT NOT NULL DEFAULT 1 CHECK (attempt_number > 0),
    status            VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                      CHECK (status IN (
                          'IN_PROGRESS',
                          'SUBMITTED',
                          'GRADING',
                          'GRADED',
                          'REVIEWED',
                          'CANCELLED'
                      )),
    started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at      TIMESTAMPTZ,
    graded_at         TIMESTAMPTZ,
    total_score       NUMERIC(7,2)
                      CHECK (total_score IS NULL OR total_score >= 0),
    percentage        NUMERIC(5,2)
                      CHECK (percentage IS NULL OR (percentage >= 0 AND percentage <= 100)),
    passed            BOOLEAN,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (exam_id, student_id, attempt_number),
    CHECK (
        (status = 'IN_PROGRESS' AND submitted_at IS NULL)
        OR status <> 'IN_PROGRESS'
    )
);

CREATE TABLE exam_answers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id          UUID NOT NULL REFERENCES exam_attempts(id) ON DELETE RESTRICT,
    question_id         UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    selected_option_id  UUID,
    answer_text         TEXT,
    is_correct          BOOLEAN,
    max_marks           NUMERIC(7,2) NOT NULL CHECK (max_marks > 0),
    marks_awarded       NUMERIC(7,2)
                        CHECK (marks_awarded IS NULL OR
                               (marks_awarded >= 0 AND marks_awarded <= max_marks)),
    grading_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (grading_status IN (
                            'PENDING',
                            'AUTO_GRADED',
                            'AI_GRADED',
                            'TEACHER_REVIEWED'
                        )),
    grading_method      VARCHAR(20)
                        CHECK (grading_method IS NULL OR
                               grading_method IN ('AUTO', 'AI', 'TEACHER')),
    ai_feedback         TEXT,
    teacher_feedback    TEXT,
    graded_by           UUID REFERENCES users(id) ON DELETE SET NULL,
    answered_at         TIMESTAMPTZ,
    graded_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (attempt_id, question_id),
    FOREIGN KEY (question_id, selected_option_id)
        REFERENCES question_options(question_id, id)
);

-- ============================================================
-- 9. AI GRADING
-- ============================================================

CREATE TABLE ai_grading_runs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_answer_id      UUID NOT NULL REFERENCES exam_answers(id) ON DELETE RESTRICT,
    provider             VARCHAR(50) NOT NULL,
    model                VARCHAR(100) NOT NULL,
    prompt_version       VARCHAR(50),
    score                NUMERIC(7,2)
                         CHECK (score IS NULL OR score >= 0),
    confidence_score     NUMERIC(5,4)
                         CHECK (confidence_score IS NULL OR
                                (confidence_score >= 0 AND confidence_score <= 1)),
    feedback             TEXT,
    rubric_result        JSONB,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    error_message        TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at         TIMESTAMPTZ
);

-- ============================================================
-- 10. STUDENT PROGRESS
-- ============================================================

CREATE TABLE lesson_progress (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    lesson_id         UUID NOT NULL REFERENCES lessons(id) ON DELETE RESTRICT,
    status            VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                      CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    progress_percent  NUMERIC(5,2) NOT NULL DEFAULT 0
                      CHECK (progress_percent >= 0 AND progress_percent <= 100),
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    last_accessed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, lesson_id),
    CHECK (
        (status = 'COMPLETED' AND progress_percent = 100 AND completed_at IS NOT NULL)
        OR status = 'IN_PROGRESS'
    )
);

-- ============================================================
-- 11. AI GENERATION
-- ============================================================

CREATE TABLE ai_generation_jobs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    generation_type   VARCHAR(30) NOT NULL
                      CHECK (generation_type IN (
                          'QUESTION',
                          'EXERCISE',
                          'EXAM',
                          'EXPLANATION',
                          'GRADING',
                          'TUTOR'
                      )),
    provider          VARCHAR(50),
    model             VARCHAR(100),
    prompt_version    VARCHAR(50),
    request_parameters JSONB,
    result_metadata   JSONB,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN (
                          'PENDING',
                          'PROCESSING',
                          'SUCCESS',
                          'FAILED',
                          'CANCELLED'
                      )),
    error_message     TEXT,
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_generated_questions (
    generation_job_id UUID NOT NULL REFERENCES ai_generation_jobs(id) ON DELETE RESTRICT,
    question_id       UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    validation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                      CHECK (validation_status IN (
                          'PENDING',
                          'PASSED',
                          'FAILED',
                          'REVIEW_REQUIRED'
                      )),
    validation_score  NUMERIC(5,2)
                      CHECK (validation_score IS NULL OR
                             (validation_score >= 0 AND validation_score <= 100)),
    reviewer_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at       TIMESTAMPTZ,
    PRIMARY KEY (generation_job_id, question_id)
);

CREATE TABLE question_validations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id      UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    validation_type  VARCHAR(20) NOT NULL
                     CHECK (validation_type IN ('AI', 'RULE_ENGINE', 'TEACHER')),
    status           VARCHAR(20) NOT NULL
                     CHECK (status IN ('PASSED', 'FAILED', 'REVIEW_REQUIRED')),
    score            NUMERIC(5,2)
                     CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
    feedback         TEXT,
    metadata         JSONB,
    validated_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 12. DOCUMENT GENERATION
-- ============================================================

CREATE TABLE generated_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID NOT NULL REFERENCES exams(id) ON DELETE RESTRICT,
    exam_version_id UUID REFERENCES exam_versions(id) ON DELETE RESTRICT,
    document_type   VARCHAR(30) NOT NULL
                    CHECK (document_type IN (
                        'STUDENT_EXAM',
                        'TEACHER_EXAM',
                        'ANSWER_KEY',
                        'EXPLANATION'
                    )),
    format          VARCHAR(10) NOT NULL
                    CHECK (format IN ('PDF', 'DOCX')),
    version         INT NOT NULL DEFAULT 1 CHECK (version > 0),
    file_name       VARCHAR(255) NOT NULL,
    file_url        TEXT,
    storage_key     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'GENERATING'
                    CHECK (status IN ('GENERATING', 'READY', 'FAILED')),
    generated_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 13. NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    type        VARCHAR(50) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at     TIMESTAMPTZ,
    CHECK (
        (is_read = TRUE AND read_at IS NOT NULL)
        OR (is_read = FALSE AND read_at IS NULL)
    )
);

-- ============================================================
-- 14. AUDIT LOG
-- ============================================================

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   UUID,
    details     JSONB,
    ip_address  INET,
    user_agent  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 15. KAFKA TRANSACTIONAL OUTBOX
-- ============================================================

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count     INT NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);

-- ============================================================
-- 16. INDEXES
-- ============================================================

-- Auth
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Courses / learning
CREATE INDEX idx_courses_subject_id ON courses(subject_id);
CREATE INDEX idx_courses_status ON courses(status);
CREATE INDEX idx_courses_created_by ON courses(created_by);
CREATE INDEX idx_course_teachers_teacher_id ON course_teachers(teacher_id);
CREATE INDEX idx_course_enrollments_student_id ON course_enrollments(student_id);
CREATE INDEX idx_course_enrollments_status ON course_enrollments(status);

CREATE INDEX idx_chapters_course_id ON chapters(course_id);
CREATE INDEX idx_lessons_chapter_id ON lessons(chapter_id);
CREATE INDEX idx_lessons_status ON lessons(status);
CREATE INDEX idx_lesson_resources_lesson_id ON lesson_resources(lesson_id);

-- Questions
CREATE INDEX idx_questions_subject_id ON questions(subject_id);
CREATE INDEX idx_questions_course_id ON questions(course_id);
CREATE INDEX idx_questions_lesson_id ON questions(lesson_id);
CREATE INDEX idx_questions_type_difficulty ON questions(question_type, difficulty);
CREATE INDEX idx_questions_status ON questions(status);
CREATE INDEX idx_questions_created_by ON questions(created_by);
CREATE INDEX idx_question_options_question_id ON question_options(question_id);
CREATE INDEX idx_question_tag_relations_tag_id ON question_tag_relations(tag_id);

-- Exercises
CREATE INDEX idx_exercises_lesson_id ON exercises(lesson_id);
CREATE INDEX idx_exercises_status ON exercises(status);
CREATE INDEX idx_exercise_questions_question_id ON exercise_questions(question_id);

-- Exams
CREATE INDEX idx_exam_templates_course_id ON exam_templates(course_id);
CREATE INDEX idx_exam_templates_subject_id ON exam_templates(subject_id);
CREATE INDEX idx_exams_course_id ON exams(course_id);
CREATE INDEX idx_exams_subject_id ON exams(subject_id);
CREATE INDEX idx_exams_status ON exams(status);
CREATE INDEX idx_exams_created_by ON exams(created_by);
CREATE INDEX idx_exam_blueprint_items_tag_id ON exam_blueprint_items(tag_id);
CREATE INDEX idx_exam_questions_question_id ON exam_questions(question_id);
CREATE INDEX idx_exam_versions_exam_id ON exam_versions(exam_id);
CREATE INDEX idx_exam_version_questions_exam_version_id ON exam_version_questions(exam_version_id);
CREATE INDEX idx_exam_version_questions_exam_question_id ON exam_version_questions(exam_question_id);
CREATE INDEX idx_exam_version_options_question_id ON exam_version_options(question_option_id);

-- Attempts / grading
CREATE INDEX idx_exam_attempts_student_id ON exam_attempts(student_id);
CREATE INDEX idx_exam_attempts_exam_id ON exam_attempts(exam_id);
CREATE INDEX idx_exam_attempts_status ON exam_attempts(status);
CREATE INDEX idx_exam_answers_attempt_id ON exam_answers(attempt_id);
CREATE INDEX idx_exam_answers_question_id ON exam_answers(question_id);
CREATE INDEX idx_exam_answers_grading_status ON exam_answers(grading_status);
CREATE INDEX idx_ai_grading_runs_answer_id ON ai_grading_runs(exam_answer_id);

-- Progress
CREATE INDEX idx_lesson_progress_student_id ON lesson_progress(student_id);
CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress(lesson_id);
CREATE INDEX idx_lesson_progress_status ON lesson_progress(status);

-- AI
CREATE INDEX idx_ai_generation_jobs_requested_by ON ai_generation_jobs(requested_by);
CREATE INDEX idx_ai_generation_jobs_status ON ai_generation_jobs(status);
CREATE INDEX idx_ai_generation_jobs_type ON ai_generation_jobs(generation_type);
CREATE INDEX idx_ai_generated_questions_question_id ON ai_generated_questions(question_id);
CREATE INDEX idx_question_validations_question_id ON question_validations(question_id);

-- Documents / notifications / audit / outbox
CREATE INDEX idx_generated_documents_exam_id ON generated_documents(exam_id);
CREATE INDEX idx_generated_documents_status ON generated_documents(status);
CREATE INDEX idx_notifications_user_id_created_at
    ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread
    ON notifications(user_id, created_at DESC)
    WHERE is_read = FALSE;
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_outbox_events_pending
    ON outbox_events(created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_outbox_events_status ON outbox_events(status);

-- ============================================================
-- 17. UPDATED_AT TRIGGERS
-- ============================================================

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_roles_updated_at
BEFORE UPDATE ON roles
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_oauth_accounts_updated_at
BEFORE UPDATE ON oauth_accounts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_subjects_updated_at
BEFORE UPDATE ON subjects
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_courses_updated_at
BEFORE UPDATE ON courses
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_chapters_updated_at
BEFORE UPDATE ON chapters
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_lessons_updated_at
BEFORE UPDATE ON lessons
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_questions_updated_at
BEFORE UPDATE ON questions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_grading_rubrics_updated_at
BEFORE UPDATE ON grading_rubrics
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_exercises_updated_at
BEFORE UPDATE ON exercises
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_exam_templates_updated_at
BEFORE UPDATE ON exam_templates
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_exams_updated_at
BEFORE UPDATE ON exams
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_exam_blueprints_updated_at
BEFORE UPDATE ON exam_blueprints
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_exam_attempts_updated_at
BEFORE UPDATE ON exam_attempts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_exam_answers_updated_at
BEFORE UPDATE ON exam_answers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_lesson_progress_updated_at
BEFORE UPDATE ON lesson_progress
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- 18. SEED DATA
-- ============================================================

INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'System administrator'),
    ('TEACHER', 'Teacher / instructor'),
    ('STUDENT', 'Student / learner')
ON CONFLICT (name) DO NOTHING;

INSERT INTO subjects (name, code, description)
VALUES
    ('Mathematics', 'MATH', 'Mathematics'),
    ('Physics', 'PHYS', 'Physics'),
    ('English', 'ENG', 'English'),
    ('Computer Science', 'CS', 'Computer Science')
ON CONFLICT (code) DO NOTHING;

COMMIT;
