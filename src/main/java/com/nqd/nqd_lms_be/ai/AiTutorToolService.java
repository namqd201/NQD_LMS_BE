package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;
import com.nqd.nqd_lms_be.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Safe, read-only data access gateway for AI Tutor.
 * All queries enforce role-based access control and strict data sanitization — AI never touches private/sensitive data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTutorToolService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository lessonResourceRepository;
    private final QuestionRepository questionRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final SubjectRepository subjectRepository;

    // ========== DTOs for safe data transfer to AI context ==========

    @Data
    @Builder
    public static class PublicCourseSummary {
        private String name;
        private String code;
        private String subjectName;
        private String teacherName;
        private BigDecimal price;
        private BigDecimal salePrice;
        private Boolean isFree;
        private String gradeLevel;
        private Double rating;
        private Integer enrollmentCount;
        private String description;
    }

    @Data
    @Builder
    public static class PublicPlanSummary {
        private String name;
        private String planCode;
        private String userType;
        private BigDecimal price;
        private String billingCycle;
        private String description;
        private Set<String> features;
    }

    @Data
    @Builder
    public static class CourseInfo {
        private UUID id;
        private String name;
        private String code;
        private String description;
        private String gradeLevel;
        private String subjectName;
    }

    @Data
    @Builder
    public static class ChapterInfo {
        private UUID id;
        private String title;
        private String description;
        private int displayOrder;
    }

    @Data
    @Builder
    public static class LessonInfo {
        private UUID id;
        private String title;
        private String summary;
        private String content;
        private int displayOrder;
        private String chapterTitle;
    }

    @Data
    @Builder
    public static class QuestionInfo {
        private UUID id;
        private String content;
        private String questionType;
        private String difficulty;
        private String explanation;
    }

    // ========== Safe Query Methods ==========

    /**
     * Search courses by keyword with role-based filtering.
     */
    @Transactional(readOnly = true)
    public List<CourseInfo> searchCourses(String keyword, UUID userId, String userRole) {
        log.info("AI Tool: searchCourses(keyword='{}', userId={}, role={})", keyword, userId, userRole);

        List<Course> candidates;
        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";

        switch (roleUpper) {
            case "ADMIN" -> candidates = courseRepository.findAll();
            case "TEACHER" -> candidates = courseRepository.findCoursesByTeacherId(userId);
            default -> {
                // STUDENT: only enrolled courses with ACTIVE status
                List<CourseEnrollment> enrollments = enrollmentRepository.findByStudentId(userId);
                candidates = enrollments.stream()
                        .map(CourseEnrollment::getCourse)
                        .filter(Objects::nonNull)
                        .filter(c -> c.getStatus() == CourseStatus.ACTIVE)
                        .collect(Collectors.toList());
            }
        }

        // Filter by keyword (case-insensitive, match name or code)
        if (keyword != null && !keyword.isBlank()) {
            String lowerKw = keyword.toLowerCase(Locale.ROOT).trim();
            // Split keyword into tokens for flexible matching
            String[] tokens = lowerKw.split("\\s+");

            candidates = candidates.stream()
                    .filter(c -> {
                        String searchTarget = ((c.getName() != null ? c.getName() : "") + " " +
                                (c.getCode() != null ? c.getCode() : "") + " " +
                                (c.getDescription() != null ? c.getDescription() : "")).toLowerCase(Locale.ROOT);
                        // Match if all tokens are found in the search target
                        return Arrays.stream(tokens).allMatch(searchTarget::contains);
                    })
                    .collect(Collectors.toList());
        }

        return candidates.stream()
                .limit(10)
                .map(c -> CourseInfo.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .code(c.getCode())
                        .description(c.getDescription())
                        .gradeLevel(c.getGradeLevel())
                        .subjectName(c.getSubject() != null ? c.getSubject().getName() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get chapters of a course with access check.
     */
    @Transactional(readOnly = true)
    public List<ChapterInfo> getChaptersByCourse(UUID courseId, UUID userId, String userRole) {
        log.info("AI Tool: getChaptersByCourse(courseId={}, userId={}, role={})", courseId, userId, userRole);

        if (!hasAccessToCourse(courseId, userId, userRole)) {
            log.warn("AI Tool: Access denied for user {} to course {}", userId, courseId);
            return Collections.emptyList();
        }

        return chapterRepository.findByCourseIdOrderByDisplayOrderAsc(courseId).stream()
                .map(ch -> ChapterInfo.builder()
                        .id(ch.getId())
                        .title(ch.getTitle())
                        .description(ch.getDescription())
                        .displayOrder(ch.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get lessons by chapter with access check.
     * Students only see PUBLISHED lessons.
     */
    @Transactional(readOnly = true)
    public List<LessonInfo> getLessonsByChapter(UUID chapterId, UUID userId, String userRole) {
        log.info("AI Tool: getLessonsByChapter(chapterId={}, userId={}, role={})", chapterId, userId, userRole);

        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";
        List<Lesson> lessons;

        if ("STUDENT".equals(roleUpper)) {
            lessons = lessonRepository.findByChapterIdAndStatusOrderByDisplayOrderAsc(chapterId, LessonStatus.PUBLISHED);
        } else {
            lessons = lessonRepository.findByChapterIdOrderByDisplayOrderAsc(chapterId);
        }

        // Verify course access through chapter
        if (!lessons.isEmpty()) {
            Lesson first = lessons.get(0);
            if (first.getChapter() != null && first.getChapter().getCourse() != null) {
                UUID courseId = first.getChapter().getCourse().getId();
                if (!hasAccessToCourse(courseId, userId, userRole)) {
                    log.warn("AI Tool: Access denied for user {} to chapter {}", userId, chapterId);
                    return Collections.emptyList();
                }
            }
        }

        return lessons.stream()
                .map(l -> LessonInfo.builder()
                        .id(l.getId())
                        .title(l.getTitle())
                        .summary(l.getSummary())
                        .content(truncateContent(l.getContent(), 2000))
                        .displayOrder(l.getDisplayOrder())
                        .chapterTitle(l.getChapter() != null ? l.getChapter().getTitle() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get full lesson content for a specific lesson with access check.
     */
    @Transactional(readOnly = true)
    public LessonInfo getLessonContent(UUID lessonId, UUID userId, String userRole) {
        log.info("AI Tool: getLessonContent(lessonId={}, userId={}, role={})", lessonId, userId, userRole);

        Optional<Lesson> opt = lessonRepository.findById(lessonId);
        if (opt.isEmpty()) return null;

        Lesson lesson = opt.get();
        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";

        // Students can only see PUBLISHED lessons
        if ("STUDENT".equals(roleUpper) && lesson.getStatus() != LessonStatus.PUBLISHED) {
            log.warn("AI Tool: Student {} tried to access non-published lesson {}", userId, lessonId);
            return null;
        }

        // Check course access
        if (lesson.getChapter() != null && lesson.getChapter().getCourse() != null) {
            UUID courseId = lesson.getChapter().getCourse().getId();
            if (!hasAccessToCourse(courseId, userId, userRole)) {
                return null;
            }
        }

        return LessonInfo.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .summary(lesson.getSummary())
                .content(truncateContent(lesson.getContent(), 4000))
                .displayOrder(lesson.getDisplayOrder())
                .chapterTitle(lesson.getChapter() != null ? lesson.getChapter().getTitle() : null)
                .build();
    }

    /**
     * Get existing questions for a course/lesson. Only for TEACHER and ADMIN.
     */
    @Transactional(readOnly = true)
    public List<QuestionInfo> getExistingQuestions(UUID courseId, UUID lessonId, UUID userId, String userRole) {
        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";

        // Students cannot access question bank
        if ("STUDENT".equals(roleUpper)) {
            log.warn("AI Tool: Student {} tried to access question bank", userId);
            return Collections.emptyList();
        }

        // Teacher must own the course
        if ("TEACHER".equals(roleUpper) && courseId != null && !hasAccessToCourse(courseId, userId, userRole)) {
            return Collections.emptyList();
        }

        List<Question> questions;
        if (lessonId != null) {
            questions = questionRepository.findByLessonId(lessonId);
        } else if (courseId != null) {
            questions = questionRepository.findByCourseId(courseId);
        } else {
            return Collections.emptyList();
        }

        return questions.stream()
                .limit(20)
                .map(q -> QuestionInfo.builder()
                        .id(q.getId())
                        .content(q.getContent())
                        .questionType(q.getQuestionType() != null ? q.getQuestionType().name() : null)
                        .difficulty(q.getDifficulty() != null ? q.getDifficulty().name() : null)
                        .explanation(q.getExplanation())
                        .build())
                .collect(Collectors.toList());
    }

    // ========== Access Control Helpers ==========

    private boolean hasAccessToCourse(UUID courseId, UUID userId, String userRole) {
        if (courseId == null || userId == null) return false;
        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";

        return switch (roleUpper) {
            case "ADMIN" -> true;
            case "TEACHER" -> courseRepository.isTeacherOwnerOrAssigned(courseId, userId);
            default -> enrollmentRepository.existsByCourseIdAndStudentId(courseId, userId);
        };
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "... (nội dung đã được rút gọn)";
    }

    // ========== Format Fetched Data as Context for AI Prompt ==========

    /**
     * Format fetched course + chapter + lesson data into a human-readable context string
     * to be injected into the AI prompt.
     */
    public String formatCourseContext(List<CourseInfo> courses, List<ChapterInfo> chapters, List<LessonInfo> lessons) {
        StringBuilder sb = new StringBuilder();

        if (courses != null && !courses.isEmpty()) {
            sb.append("\n=== THÔNG TIN KHÓA HỌC TÌM ĐƯỢC ===\n");
            for (CourseInfo c : courses) {
                sb.append(String.format("- Khóa học: \"%s\" (Mã: %s)", c.getName(), c.getCode()));
                if (c.getSubjectName() != null) sb.append(" | Môn: ").append(c.getSubjectName());
                if (c.getGradeLevel() != null) sb.append(" | Khối: ").append(c.getGradeLevel());
                sb.append("\n");
                if (c.getDescription() != null && !c.getDescription().isBlank()) {
                    sb.append("  Mô tả: ").append(c.getDescription()).append("\n");
                }
            }
        }

        if (chapters != null && !chapters.isEmpty()) {
            sb.append("\n=== DANH SÁCH CHƯƠNG ===\n");
            for (ChapterInfo ch : chapters) {
                sb.append(String.format("- Chương %d: \"%s\"", ch.getDisplayOrder(), ch.getTitle()));
                if (ch.getDescription() != null) sb.append(" — ").append(ch.getDescription());
                sb.append("\n");
            }
        }

        if (lessons != null && !lessons.isEmpty()) {
            sb.append("\n=== NỘI DUNG BÀI GIẢNG ===\n");
            for (LessonInfo l : lessons) {
                sb.append(String.format("--- Bài %d: \"%s\"", l.getDisplayOrder(), l.getTitle()));
                if (l.getChapterTitle() != null) sb.append(" (Chương: ").append(l.getChapterTitle()).append(")");
                sb.append(" ---\n");
                if (l.getSummary() != null && !l.getSummary().isBlank()) {
                    sb.append("Tóm tắt: ").append(l.getSummary()).append("\n");
                }
                if (l.getContent() != null && !l.getContent().isBlank()) {
                    sb.append("Nội dung:\n").append(l.getContent()).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    // =========================================================================
    // SAFE PUBLIC PLATFORM DATA ACCESS (NO SENSITIVE / PRIVATE USER DATA)
    // =========================================================================

    /**
     * Retrieve public published marketplace courses.
     * Guaranteed sanitization: ONLY public course details and teacher full name.
     * Absolutely NO email, phone, passwords, or private student data.
     */
    @Transactional(readOnly = true)
    public List<PublicCourseSummary> getPublicMarketplaceCourses(String subjectOrKeyword) {
        try {
            List<Course> published = courseRepository.findByStatusInAndIsDeletedFalse(
                    List.of(CourseStatus.PUBLISHED, CourseStatus.ACTIVE)
            );

            return published.stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsPrivate()) && !Boolean.TRUE.equals(c.getIsDisabled()))
                    .filter(c -> {
                        if (subjectOrKeyword == null || subjectOrKeyword.isBlank()) return true;
                        String normKw = normalizeText(subjectOrKeyword);
                        String target = normalizeText(
                                (c.getName() != null ? c.getName() : "") + " " +
                                (c.getSubject() != null ? c.getSubject().getName() : "") + " " +
                                (c.getCreator() != null ? c.getCreator().getFullName() : "") + " " +
                                (c.getDescription() != null ? c.getDescription() : "")
                        );
                        return target.contains(normKw);
                    })
                    .sorted((a, b) -> {
                        int countA = a.getEnrollmentCount() != null ? a.getEnrollmentCount() : 0;
                        int countB = b.getEnrollmentCount() != null ? b.getEnrollmentCount() : 0;
                        return Integer.compare(countB, countA);
                    })
                    .limit(10)
                    .map(c -> PublicCourseSummary.builder()
                            .name(c.getName())
                            .code(c.getCode())
                            .subjectName(c.getSubject() != null ? c.getSubject().getName() : "Khác")
                            .teacherName(c.getCreator() != null ? c.getCreator().getFullName() : "Giảng viên NQD-LMS")
                            .price(c.getPrice())
                            .salePrice(c.getSalePrice())
                            .isFree(c.getPrice() == null || c.getPrice().compareTo(BigDecimal.ZERO) == 0)
                            .gradeLevel(c.getGradeLevel())
                            .rating(c.getAverageRating() != null ? c.getAverageRating() : 5.0)
                            .enrollmentCount(c.getEnrollmentCount() != null ? c.getEnrollmentCount() : 0)
                            .description(c.getDescription() != null && c.getDescription().length() > 200
                                    ? c.getDescription().substring(0, 200) + "..." : c.getDescription())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to retrieve public marketplace courses: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieve public active membership plans (Pro packages) optionally filtered by user role.
     */
    @Transactional(readOnly = true)
    public List<PublicPlanSummary> getPublicMembershipPlans() {
        return getPublicMembershipPlans(null);
    }

    @Transactional(readOnly = true)
    public List<PublicPlanSummary> getPublicMembershipPlans(String userRole) {
        try {
            List<MembershipPlan> plans = membershipPlanRepository.findByStatusAndIsDeletedFalseOrderByPriceAsc(ProductStatus.PUBLISHED);

            String roleUpper = userRole != null ? userRole.toUpperCase(Locale.ROOT) : "";
            boolean isTeacher = roleUpper.contains("TEACHER");
            boolean isAdmin = roleUpper.contains("ADMIN");

            return plans.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getActive()))
                    .filter(p -> {
                        if (isAdmin || userRole == null || userRole.isBlank()) {
                            return true;
                        }
                        if (isTeacher) {
                            return p.getUserType() == PlanUserType.TEACHER || p.getUserType() == PlanUserType.ALL;
                        }
                        // Default to STUDENT for students / non-teachers
                        return p.getUserType() == PlanUserType.STUDENT || p.getUserType() == PlanUserType.ALL;
                    })
                    .map(p -> PublicPlanSummary.builder()
                            .name(p.getName())
                            .planCode(p.getPlanCode())
                            .userType(p.getUserType() != null ? p.getUserType().name() : "ALL")
                            .price(p.getPrice())
                            .billingCycle(p.getBillingCycle() != null ? p.getBillingCycle().name() : "MONTHLY")
                            .description(p.getDescription())
                            .features(p.getFeatureSet())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to retrieve public membership plans: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieve public active subjects list.
     */
    @Transactional(readOnly = true)
    public List<String> getPublicSubjects() {
        try {
            return subjectRepository.findByStatusAndIsDeletedFalse(SubjectStatus.ACTIVE).stream()
                    .map(Subject::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to retrieve public subjects: {}", e.getMessage());
            return List.of("Toán", "Vật lý", "Hóa học", "Sinh học", "Ngữ văn", "Tiếng Anh", "Lịch sử", "Địa lý", "Tin học");
        }
    }

    /**
     * Formats safe public context for system questions (e.g. buying courses, pro plans, teacher recommendations).
     */
    @Transactional(readOnly = true)
    public String formatSystemAdvisoryContext(String query) {
        return formatSystemAdvisoryContext(query, null);
    }

    @Transactional(readOnly = true)
    public String formatSystemAdvisoryContext(String query, String userRole) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== THÔNG TIN CÔNG KHAI HỆ THỐNG NQD-LMS (DÙNG ĐỂ TƯ VẤN NGƯỜI DÙNG) ===\n");
        sb.append("LƯU Ý BẢO MẬT: Mọi thông tin dưới đây là dữ liệu công khai trên nền tảng. Tuyệt đối không bịa đặt hoặc tìm kiếm thông tin riêng tư (như mật khẩu, email, số điện thoại).\n\n");

        String roleUpper = userRole != null ? userRole.toUpperCase(Locale.ROOT) : "";
        boolean isTeacher = roleUpper.contains("TEACHER");
        boolean isAdmin = roleUpper.contains("ADMIN");

        // 1. Membership / PRO Plans (Filtered strictly by user role)
        List<PublicPlanSummary> plans = getPublicMembershipPlans(userRole);
        if (isTeacher) {
            sb.append("### 1. CÁC GÓI HỘI VIÊN PRO DÀNH CHO GIÁO VIÊN TRÊN NQD-LMS:\n");
            sb.append("⚠️ QUY TẮC BẮT BUỘC: Người dùng hiện tại có vai trò là GIÁO VIÊN (TEACHER). Bạn CHỈ ĐƯỢC PHÉP tư vấn và giới thiệu các gói dành riêng cho Giáo viên (Gói Giáo viên Pro tháng, năm... với công cụ AI tạo slide bài giảng, soạn đề thi, quản lý lớp). TUYỆT ĐỐI KHÔNG liệt kê hay giới thiệu các gói của Học sinh!\n");
        } else if (isAdmin) {
            sb.append("### 1. CÁC GÓI HỘI VIÊN PRO TRÊN NQD-LMS (QUẢN TRỊ VIÊN XEM TOÀN BỘ):\n");
        } else {
            sb.append("### 1. CÁC GÓI HỘI VIÊN PRO DÀNH CHO HỌC SINH TRÊN NQD-LMS:\n");
            sb.append("⚠️ QUY TẮC BẮT BUỘC: Người dùng hiện tại có vai trò là HỌC SINH (STUDENT). Bạn CHỈ ĐƯỢC PHÉP tư vấn và giới thiệu các gói dành riêng cho Học sinh (Gói Học sinh Pro tháng, năm, Học sinh Ultra, luyện thi và AI Tutor không giới hạn). TUYỆT ĐỐI KHÔNG liệt kê hay giới thiệu các gói của Giáo viên!\n");
        }

        if (plans.isEmpty()) {
            if (isTeacher) {
                sb.append("- Gói Giáo viên PRO: 149.000 đ/tháng (Tạo slide bài học tự động bằng AI, soạn ngân hàng câu hỏi & đề thi bằng AI, quản lý lớp học nâng cao)\n");
                sb.append("- Gói Giáo viên PRO (Năm): 1.200.000 đ/năm (Toàn quyền sử dụng bộ công cụ AI giáo dục chuyên nghiệp trong 12 tháng)\n");
            } else {
                sb.append("- Gói Học sinh PRO: 99.000 đ/tháng (Học tập thông minh, trợ lý AI không giới hạn, xem giải thích chi tiết đề thi, giảm 20% mua khóa lẻ)\n");
                sb.append("- Gói Học sinh PRO (Năm): 799.000 đ/năm (Tiết kiệm hơn 30% chi phí trong 12 tháng)\n");
                sb.append("- Gói Học sinh ULTRA: 1.500.000 đ/tháng (Học FREE toàn bộ khóa học trên hệ thống, AI Tutor không giới hạn)\n");
            }
        } else {
            for (PublicPlanSummary p : plans) {
                String targetRole = "TEACHER".equalsIgnoreCase(p.getUserType()) ? "Giáo viên" : "Học sinh & Thành viên";
                sb.append(String.format("* **%s** (Mã: `%s` | Dành cho: %s):\n", p.getName(), p.getPlanCode(), targetRole));
                sb.append(String.format("  - Mức phí: **%s** / %s\n",
                        formatVnd(p.getPrice()),
                        "YEARLY".equalsIgnoreCase(p.getBillingCycle()) ? "năm" : "tháng"));
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    sb.append("  - Mô tả: ").append(p.getDescription()).append("\n");
                }
                if (p.getFeatures() != null && !p.getFeatures().isEmpty()) {
                    sb.append("  - Quyền lợi nổi bật: ").append(describeFeatures(p.getFeatures())).append("\n");
                }
            }
        }
        sb.append("👉 **Cách nâng cấp Gói PRO:** Truy cập mục **Bảng giá / Nâng cấp Hội viên** (`/pricing`), chọn gói mong muốn và bấm **\"Nâng cấp ngay\"**, quét mã QR thanh toán PayOS tự động.\n\n");

        // 2. Public Courses & Teachers recommendations
        String detectedSubject = extractSubjectFromQuery(query);
        List<PublicCourseSummary> courses = getPublicMarketplaceCourses(detectedSubject);

        sb.append("### 2. DANH MỤC KHÓA HỌC & GIẢNG VIÊN NỔI BẬT:\n");
        if (detectedSubject != null && !detectedSubject.isBlank()) {
            sb.append(String.format("(Hệ thống đã tự động lọc các khóa học và giáo viên thuộc môn/chủ đề: \"%s\")\n", detectedSubject));
        }

        if (courses.isEmpty()) {
            sb.append("- Hiện tại chưa có khóa học công khai nào khớp chính xác với từ khóa này. Bạn hãy gợi ý người dùng xem danh mục đầy đủ tại mục Khóa học (`/courses`).\n");
        } else {
            for (PublicCourseSummary c : courses) {
                sb.append(String.format("* Khóa học: **%s** (Môn: %s | Khối: %s)\n",
                        c.getName(), c.getSubjectName(), c.getGradeLevel() != null ? c.getGradeLevel() : "Tất cả"));
                sb.append(String.format("  - Giảng viên phụ trách: **%s**\n", c.getTeacherName()));
                sb.append(String.format("  - Học phí: **%s**%s\n",
                        formatVnd(c.getSalePrice() != null ? c.getSalePrice() : c.getPrice()),
                        c.getSalePrice() != null && c.getPrice() != null && c.getPrice().compareTo(c.getSalePrice()) > 0
                                ? " (Giá gốc: " + formatVnd(c.getPrice()) + ")" : ""));
                sb.append(String.format("  - Đánh giá: ⭐ %.1f/5.0 (%d học viên đã tham gia)\n", c.getRating(), c.getEnrollmentCount()));
                if (c.getDescription() != null && !c.getDescription().isBlank()) {
                    sb.append("  - Tóm tắt: ").append(c.getDescription()).append("\n");
                }
            }
        }

        // 3. Purchase instructions
        sb.append("\n### 3. HƯỚNG DẪN MUA KHÓA HỌC TRÊN NQD-LMS:\n");
        sb.append("1. Truy cập mục **Khóa học** (đường dẫn: `/courses`) trên thanh điều hướng.\n");
        sb.append("2. Lựa chọn môn học hoặc tìm tên khóa học mong muốn, bấm vào xem chi tiết nội dung và đánh giá của các học viên.\n");
        sb.append("3. Nhấn nút **\"Mua khóa học\"** (hoặc \"Vào học ngay\" nếu khóa học miễn phí).\n");
        sb.append("4. Quét mã QR thanh toán nhanh qua cổng **PayOS** (hỗ trợ mọi ngân hàng Việt Nam 24/7 và ứng dụng ngân hàng/MoMo quét mã chuyển khoản tức thì).\n");
        sb.append("5. Hệ thống kích hoạt khóa học tự động ngay khi giao dịch thành công. Người học có thể vào học ngay lập tức tại mục **Khóa học của tôi**.\n");
        sb.append("====================================================================\n");

        return sb.toString();
    }

    private String formatVnd(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) return "Miễn phí";
        return String.format("%,d đ", price.longValue());
    }

    private String describeFeatures(Set<String> featureKeys) {
        if (featureKeys == null || featureKeys.isEmpty()) return "Các quyền lợi hội viên cao cấp";
        List<String> list = new ArrayList<>();
        for (String fk : featureKeys) {
            switch (fk) {
                case "AI_TUTOR" -> list.add("Gia sư AI học tập thông minh không giới hạn");
                case "AI_SLIDE_GENERATION" -> list.add("Tạo Slide bài giảng tự động bằng AI");
                case "AI_EXAM_GENERATION" -> list.add("Soạn đề thi & câu hỏi tự động bằng AI");
                case "ADVANCED_ANALYTICS" -> list.add("Báo cáo phân tích học tập chuyên sâu");
                case "PRIORITY_SUPPORT" -> list.add("Hỗ trợ kỹ thuật ưu tiên 24/7");
                case "DOWNLOAD_RESOURCES" -> list.add("Tải tài liệu và bài giảng không giới hạn");
                default -> list.add(fk);
            }
        }
        return String.join(", ", list);
    }

    public String extractSubjectFromQuery(String query) {
        if (query == null) return null;
        String lower = normalizeText(query);
        if (lower.contains("toan")) return "Toán";
        if (lower.contains("tieng anh") || lower.contains("anh van") || lower.contains("english")) return "Tiếng Anh";
        if (lower.contains("vat ly") || lower.contains("vat li") || lower.contains("ly")) return "Vật lý";
        if (lower.contains("hoa hoc") || lower.contains("hoa")) return "Hóa học";
        if (lower.contains("ngu van") || lower.contains("van hoc") || lower.contains("van")) return "Ngữ văn";
        if (lower.contains("sinh hoc") || lower.contains("sinh")) return "Sinh học";
        if (lower.contains("lich su") || lower.contains("su")) return "Lịch sử";
        if (lower.contains("dia ly") || lower.contains("dia li") || lower.contains("dia")) return "Địa lý";
        if (lower.contains("tin hoc") || lower.contains("lap trinh") || lower.contains("code") || lower.contains("cntt")) return "Tin học";
        return null;
    }

    private String normalizeText(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(Locale.ROOT);
        String nfd = Normalizer.normalize(lower, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace('đ', 'd').replace('Đ', 'd');
    }
}
