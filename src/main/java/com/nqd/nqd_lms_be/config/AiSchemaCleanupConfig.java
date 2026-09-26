package com.nqd.nqd_lms_be.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
@Slf4j
public class AiSchemaCleanupConfig {

    @Bean
    public static BeanPostProcessor aiTablesCleanupPostProcessor() {
        return new BeanPostProcessor() {
            private boolean executed = false;

            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (!executed && bean instanceof DataSource dataSource) {
                    executed = true;
                    try (Connection connection = dataSource.getConnection();
                         Statement statement = connection.createStatement()) {

                        log.info("Checking for legacy AI tables schema before Hibernate initializes...");

                        statement.execute("""
                            DO $$
                            DECLARE
                                tbl text;
                                tables text[] := ARRAY[
                                    'questions', 'question_options', 'question_tags',
                                    'exams', 'exam_questions', 'exam_assignments', 'exam_attempts', 'exam_blueprints', 'exam_templates', 'exam_versions',
                                    'courses', 'chapters', 'lessons', 'lesson_resources', 'lesson_progresses', 'course_enrollments',
                                    'subjects', 'users', 'roles', 'audit_logs',
                                    'ai_tutor_conversations', 'ai_tutor_messages', 'ai_generation_jobs', 'ai_grading_runs',
                                    'exercises', 'grading_rubrics', 'notifications',
                                    'products', 'orders', 'order_items', 'payment_transactions',
                                    'membership_plans', 'subscriptions', 'entitlements', 'coupons'
                                ];
                            BEGIN
                                -- If legacy 'generation_type' column exists, drop the legacy AI tables once
                                IF EXISTS (
                                    SELECT 1 FROM information_schema.columns 
                                    WHERE table_name = 'ai_generation_jobs' AND column_name = 'generation_type'
                                ) THEN
                                    RAISE NOTICE 'Detected legacy AI tables schema. Dropping obsolete tables for clean recreation...';
                                    DROP TABLE IF EXISTS ai_generated_options CASCADE;
                                    DROP TABLE IF EXISTS ai_generated_questions CASCADE;
                                    DROP TABLE IF EXISTS ai_generation_jobs CASCADE;
                                END IF;

                                -- Ensure soft delete columns exist on all tables
                                FOREACH tbl IN ARRAY tables LOOP
                                    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = tbl) THEN
                                        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false', tbl);
                                        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP', tbl);
                                        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255)', tbl);
                                        EXECUTE format('UPDATE %I SET is_deleted = false WHERE is_deleted IS NULL', tbl);
                                    END IF;
                                END LOOP;

                                -- Ensure image_url columns exist for AI image generation
                                IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'questions') THEN
                                    ALTER TABLE questions ADD COLUMN IF NOT EXISTS image_url TEXT;
                                END IF;
                                IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ai_generated_questions') THEN
                                    ALTER TABLE ai_generated_questions ADD COLUMN IF NOT EXISTS image_url TEXT;
                                END IF;
                            END $$;
                        """);

                        log.info("Legacy AI tables and soft-delete columns schema check completed successfully.");
                    } catch (Exception e) {
                        log.warn("Notice during AI schema cleanup check: {}", e.getMessage());
                    }
                }
                return bean;
            }
        };
    }
}
