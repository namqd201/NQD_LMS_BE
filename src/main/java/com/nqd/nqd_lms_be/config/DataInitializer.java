package com.nqd.nqd_lms_be.config;

import com.nqd.nqd_lms_be.entity.Question;
import com.nqd.nqd_lms_be.entity.QuestionCategory;
import com.nqd.nqd_lms_be.entity.Role;
import com.nqd.nqd_lms_be.entity.Subject;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.UserRole;
import com.nqd.nqd_lms_be.entity.enums.QuestionCategoryVisibility;
import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionCategoryRepository questionCategoryRepository;
    private final QuestionRepository questionRepository;

    public static final List<String> STANDARD_GRADES = List.of(
            "Lớp 1", "Lớp 2", "Lớp 3", "Lớp 4", "Lớp 5", "Lớp 6",
            "Lớp 7", "Lớp 8", "Lớp 9", "Lớp 10", "Lớp 11", "Lớp 12", "Đại học"
    );

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing baseline system roles, subjects and administrative accounts...");

        // 1. Ensure system roles exist
        Role adminRole = getOrCreateRole("ADMIN", "System Administrator with full permissions");
        Role teacherRole = getOrCreateRole("TEACHER", "Teacher and course instructor");
        Role studentRole = getOrCreateRole("STUDENT", "Student and learner");

        // 2. Ensure initial admin user (namqd1403@gmail.com) has ADMIN role if registered
        List<String> bootstrapAdminEmails = List.of("namqd1403@gmail.com");
        for (String email : bootstrapAdminEmails) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), adminRole.getId())) {
                    UserRole userRole = UserRole.builder()
                            .userId(user.getId())
                            .roleId(adminRole.getId())
                            .user(user)
                            .role(adminRole)
                            .createdAt(LocalDateTime.now())
                            .build();
                    userRoleRepository.save(userRole);
                    log.info("Granted ADMIN role to bootstrap user: {}", email);
                }
            }
        }

        // 3. Ensure baseline Academic Subjects exist
        getOrCreateSubject("Toán học", "MATH", "Môn Toán học từ tiểu học đến phổ thông và đại học");
        getOrCreateSubject("Tin học & Lập trình", "IT", "Khoa học máy tính, thuật toán và lập trình phần mềm");
        getOrCreateSubject("Tiếng Anh", "ENG", "Ngoại ngữ tiếng Anh giao tiếp và học thuật");
        getOrCreateSubject("Khoa học Tự nhiên", "SCI", "Vật lý, Hóa học và Sinh học");

        // 4. Ensure default PUBLIC categories exist for all subjects & grades, and migrate Grade 1 questions
        initDefaultQuestionCategoriesAndMigrateGrade1();
    }

    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name(name)
                            .description(description)
                            .build();
                    return roleRepository.save(newRole);
                });
    }

    private void getOrCreateSubject(String name, String code, String description) {
        if (subjectRepository.findByCode(code).isEmpty()) {
            Subject subject = Subject.builder()
                    .name(name)
                    .code(code)
                    .description(description)
                    .status(SubjectStatus.ACTIVE)
                    .build();
            subjectRepository.save(subject);
            log.info("Created baseline subject: {} ({})", name, code);
        }
    }

    private void initDefaultQuestionCategoriesAndMigrateGrade1() {
        List<Subject> subjects = subjectRepository.findAll();

        // 1. Ensure for every subject and every standard grade, a default PUBLIC category exists
        for (Subject subject : subjects) {
            for (String grade : STANDARD_GRADES) {
                getOrCreateDefaultPublicCategory(subject, grade);
            }
        }

        // 2. Migrate all existing unassigned questions for Grade 1 into the default public category
        List<Question> allQuestions = questionRepository.findAll();
        int migratedCount = 0;
        for (Question q : allQuestions) {
            if (Boolean.TRUE.equals(q.getIsDeleted())) {
                continue;
            }
            if (q.getCategory() == null) {
                String grade = q.getGradeLevel();
                boolean isGrade1 = grade != null && (
                        grade.trim().equalsIgnoreCase("Lớp 1") ||
                        grade.trim().equalsIgnoreCase("1") ||
                        grade.trim().toLowerCase().contains("lớp 1")
                );

                if (isGrade1 && q.getSubject() != null) {
                    QuestionCategory defaultPubCat = getOrCreateDefaultPublicCategory(q.getSubject(), "Lớp 1");
                    q.setCategory(defaultPubCat);
                    questionRepository.save(q);
                    migratedCount++;
                    log.info("Migrated Grade 1 Question [ID: {}, Subject: {}] to default PUBLIC category '{}' (ID: {})",
                            q.getId(), q.getSubject().getName(), defaultPubCat.getName(), defaultPubCat.getId());
                }
            }
        }

        if (migratedCount > 0) {
            log.info("Successfully migrated {} Grade 1 questions to default PUBLIC categories.", migratedCount);
        }
    }

    public QuestionCategory getOrCreateDefaultPublicCategory(Subject subject, String gradeLevel) {
        String cleanGrade = (gradeLevel != null && !gradeLevel.trim().isEmpty()) ? gradeLevel.trim() : "Lớp 1";
        return questionCategoryRepository
                .findFirstBySubjectIdAndGradeLevelAndVisibilityAndIsDeletedFalse(
                        subject.getId(), cleanGrade, QuestionCategoryVisibility.PUBLIC
                )
                .orElseGet(() -> {
                    String cleanGradeCode = cleanGrade.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                    QuestionCategory cat = QuestionCategory.builder()
                            .name("Chuyên đề chung")
                            .code("GENERAL_" + subject.getCode() + "_" + cleanGradeCode)
                            .description("Chuyên đề chung mặc định công khai cho " + subject.getName() + " - " + cleanGrade)
                            .subject(subject)
                            .gradeLevel(cleanGrade)
                            .visibility(QuestionCategoryVisibility.PUBLIC)
                            .isSystem(true)
                            .displayOrder(0)
                            .build();
                    QuestionCategory saved = questionCategoryRepository.save(cat);
                    log.info("Provisioned default PUBLIC category: '{}' for Subject {} - Grade {}",
                            saved.getName(), subject.getName(), cleanGrade);
                    return saved;
                });
    }
}
