package com.nqd.nqd_lms_be.marketplace;

import com.nqd.nqd_lms_be.billing.service.EntitlementActivationService;
import com.nqd.nqd_lms_be.billing.service.OrderService;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.dto.marketplace.*;
import com.nqd.nqd_lms_be.dto.student.StudentLessonDetailResponse;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.course.CourseWorkflowService;
import com.nqd.nqd_lms_be.service.marketplace.MarketplaceService;
import com.nqd.nqd_lms_be.service.student.StudentCourseStructureService;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseService;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseStructureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MarketplaceAndCoursePublishWorkflowTest {

    @Autowired
    private TeacherCourseService teacherCourseService;

    @Autowired
    private TeacherCourseStructureService teacherCourseStructureService;

    @Autowired
    private CourseWorkflowService courseWorkflowService;

    @Autowired
    private MarketplaceService marketplaceService;

    @Autowired
    private StudentCourseStructureService studentCourseStructureService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EntitlementActivationService entitlementActivationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Autowired
    private ProductRepository productRepository;

    private User teacherUser;
    private User studentUser;
    private User adminUser;
    private Subject mathSubject;

    @BeforeEach
    void setUp() {
        teacherUser = userRepository.save(User.builder()
                .email("marketplace_teacher_" + UUID.randomUUID() + "@nqd.edu.vn")
                .fullName("Thầy Giáo Marketplace")
                .status(UserStatus.ACTIVE)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("marketplace_student_" + UUID.randomUUID() + "@nqd.edu.vn")
                .fullName("Học Sinh Marketplace")
                .status(UserStatus.ACTIVE)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("marketplace_admin_" + UUID.randomUUID() + "@nqd.edu.vn")
                .fullName("Quản Trị Viên Marketplace")
                .status(UserStatus.ACTIVE)
                .build());

        mathSubject = subjectRepository.save(Subject.builder()
                .name("Toán Học " + UUID.randomUUID().toString().substring(0, 5))
                .code("MKT-MATH-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                .description("Toán luyện thi đại học")
                .build());
    }

    private void authenticateAs(User user, String role) {
        com.nqd.nqd_lms_be.config.security.AppUserPrincipal principal = com.nqd.nqd_lms_be.config.security.AppUserPrincipal.create(
                user,
                java.util.Set.of("ROLE_" + role),
                java.util.Map.of()
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Teacher can create a paid course with preview and submit for review")
    void testTeacherCourseCreateAndSubmitReview() {
        authenticateAs(teacherUser, "TEACHER");

        TeacherCourseRequest request = TeacherCourseRequest.builder()
                .subjectId(mathSubject.getId())
                .name("Khóa Học Toán Nâng Cao 12")
                .code("MATH12-PRO-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                .description("Toán nâng cao ôn thi THPT Quốc Gia")
                .gradeLevel("12")
                .pricingType(CoursePricingType.PAID)
                .price(new BigDecimal("499000.00"))
                .salePrice(new BigDecimal("399000.00"))
                .currency("VND")
                .build();

        TeacherCourseResponse courseResponse = teacherCourseService.createCourse(request, teacherUser.getId());
        assertThat(courseResponse).isNotNull();
        assertThat(courseResponse.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(courseResponse.getPricingType()).isEqualTo(CoursePricingType.PAID);
        assertThat(courseResponse.getPrice()).isEqualByComparingTo("499000.00");

        // Add Chapter & Lessons
        TeacherChapterResponse chapter = teacherCourseStructureService.createChapter(
                courseResponse.getId(),
                TeacherChapterRequest.builder().title("Chương 1: Khảo sát hàm số").displayOrder(1).build(),
                teacherUser.getId()
        );

        TeacherLessonResponse previewLesson = teacherCourseStructureService.createLesson(
                chapter.getId(),
                TeacherLessonRequest.builder()
                        .title("Bài 1: Tính đơn điệu của hàm số (Học thử)")
                        .content("Nội dung bài học thử miễn phí...")
                        .status(LessonStatus.PUBLISHED)
                        .isPreview(true)
                        .build(),
                teacherUser.getId()
        );
        assertThat(previewLesson.getIsPreview()).isTrue();

        TeacherLessonResponse lockedLesson = teacherCourseStructureService.createLesson(
                chapter.getId(),
                TeacherLessonRequest.builder()
                        .title("Bài 2: Cực trị của hàm số (Khóa)")
                        .content("Nội dung bài học chuyên sâu có phí...")
                        .status(LessonStatus.PUBLISHED)
                        .isPreview(false)
                        .build(),
                teacherUser.getId()
        );
        assertThat(lockedLesson.getIsPreview()).isFalse();

        // Submit for Review
        TeacherCourseResponse submitted = courseWorkflowService.submitForReview(courseResponse.getId(), teacherUser.getId());
        assertThat(submitted.getStatus()).isEqualTo(CourseStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("Admin can approve course to publish or reject with reason")
    void testAdminPublishWorkflow() {
        authenticateAs(teacherUser, "TEACHER");

        TeacherCourseResponse course = teacherCourseService.createCourse(
                TeacherCourseRequest.builder()
                        .subjectId(mathSubject.getId())
                        .name("Khóa Học Vật Lý 11")
                        .code("PHY11-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                        .pricingType(CoursePricingType.PAID)
                        .price(new BigDecimal("299000.00"))
                        .build(),
                teacherUser.getId()
        );

        teacherCourseStructureService.createChapter(
                course.getId(),
                TeacherChapterRequest.builder().title("Chương 1").build(),
                teacherUser.getId()
        );

        courseWorkflowService.submitForReview(course.getId(), teacherUser.getId());

        // Admin Rejects
        authenticateAs(adminUser, "ADMIN");
        TeacherCourseResponse rejected = courseWorkflowService.rejectCourse(course.getId(), adminUser.getId(), "Cần bổ sung thêm bài học thử");
        assertThat(rejected.getStatus()).isEqualTo(CourseStatus.REJECTED);
        assertThat(rejected.getRejectReason()).isEqualTo("Cần bổ sung thêm bài học thử");

        // Teacher resubmits
        authenticateAs(teacherUser, "TEACHER");
        courseWorkflowService.submitForReview(course.getId(), teacherUser.getId());

        // Admin Approves
        authenticateAs(adminUser, "ADMIN");
        TeacherCourseResponse approved = courseWorkflowService.approveCourse(course.getId(), adminUser.getId());
        assertThat(approved.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(approved.getPublishedAt()).isNotNull();

        // Verify product synchronized
        Product product = productRepository.findByTargetEntityIdAndProductTypeAndIsDeletedFalse(course.getId(), ProductType.COURSE)
                .orElse(null);
        assertThat(product).isNotNull();
        assertThat(product.getTitle()).isEqualTo("Khóa Học Vật Lý 11");
        assertThat(product.getBasePrice()).isEqualByComparingTo("299000.00");
    }

    @Test
    @DisplayName("Marketplace search and filter works accurately")
    void testMarketplaceSearchAndFilters() {
        authenticateAs(teacherUser, "TEACHER");

        // Create Course 1: Grade 12, Paid 500k
        TeacherCourseResponse c1 = teacherCourseService.createCourse(
                TeacherCourseRequest.builder()
                        .subjectId(mathSubject.getId())
                        .name("Luyện Thi Đại Học Toán 12 VIP")
                        .code("TOAN12-VIP-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                        .gradeLevel("12")
                        .pricingType(CoursePricingType.PAID)
                        .price(new BigDecimal("500000.00"))
                        .build(),
                teacherUser.getId()
        );
        teacherCourseStructureService.createChapter(c1.getId(), TeacherChapterRequest.builder().title("C1").build(), teacherUser.getId());
        courseWorkflowService.submitForReview(c1.getId(), teacherUser.getId());

        // Create Course 2: Grade 10, Free
        TeacherCourseResponse c2 = teacherCourseService.createCourse(
                TeacherCourseRequest.builder()
                        .subjectId(mathSubject.getId())
                        .name("Toán Cơ Bản Lớp 10 Miễn Phí")
                        .code("TOAN10-FREE-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                        .gradeLevel("10")
                        .pricingType(CoursePricingType.FREE)
                        .price(BigDecimal.ZERO)
                        .build(),
                teacherUser.getId()
        );
        teacherCourseStructureService.createChapter(c2.getId(), TeacherChapterRequest.builder().title("C1").build(), teacherUser.getId());
        courseWorkflowService.submitForReview(c2.getId(), teacherUser.getId());

        // Approve both
        authenticateAs(adminUser, "ADMIN");
        courseWorkflowService.approveCourse(c1.getId(), adminUser.getId());
        courseWorkflowService.approveCourse(c2.getId(), adminUser.getId());

        // Test Filter by Free
        MarketplaceFilterRequest freeFilter = MarketplaceFilterRequest.builder()
                .isFree(true)
                .build();
        PageResponse<MarketplaceCourseCardResponse> freeResults = marketplaceService.searchCourses(freeFilter, PageRequest.of(0, 10));
        assertThat(freeResults.getItems()).anyMatch(c -> c.getId().equals(c2.getId()));
        assertThat(freeResults.getItems()).noneMatch(c -> c.getId().equals(c1.getId()));

        // Test Filter by Grade 12
        MarketplaceFilterRequest grade12Filter = MarketplaceFilterRequest.builder()
                .gradeLevel("12")
                .build();
        PageResponse<MarketplaceCourseCardResponse> grade12Results = marketplaceService.searchCourses(grade12Filter, PageRequest.of(0, 10));
        assertThat(grade12Results.getItems()).anyMatch(c -> c.getId().equals(c1.getId()));
        assertThat(grade12Results.getItems()).noneMatch(c -> c.getId().equals(c2.getId()));

        // Test Keyword search
        MarketplaceFilterRequest keywordFilter = MarketplaceFilterRequest.builder()
                .keyword("Đại Học")
                .build();
        PageResponse<MarketplaceCourseCardResponse> keywordResults = marketplaceService.searchCourses(keywordFilter, PageRequest.of(0, 10));
        assertThat(keywordResults.getItems()).anyMatch(c -> c.getId().equals(c1.getId()));
    }

    @Test
    @DisplayName("Student preview gating vs non-preview restriction on paid courses")
    void testStudentPreviewAccessAndGating() {
        authenticateAs(teacherUser, "TEACHER");

        TeacherCourseResponse course = teacherCourseService.createCourse(
                TeacherCourseRequest.builder()
                        .subjectId(mathSubject.getId())
                        .name("Khóa Học Hóa Học 12")
                        .code("CHEM12-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                        .pricingType(CoursePricingType.PAID)
                        .price(new BigDecimal("350000.00"))
                        .build(),
                teacherUser.getId()
        );

        TeacherChapterResponse chapter = teacherCourseStructureService.createChapter(
                course.getId(),
                TeacherChapterRequest.builder().title("Chương 1").build(),
                teacherUser.getId()
        );

        TeacherLessonResponse previewLesson = teacherCourseStructureService.createLesson(
                chapter.getId(),
                TeacherLessonRequest.builder()
                        .title("Bài 1: Este - Lipit (Học thử)")
                        .content("Nội dung học thử Este...")
                        .status(LessonStatus.PUBLISHED)
                        .isPreview(true)
                        .build(),
                teacherUser.getId()
        );

        TeacherLessonResponse lockedLesson = teacherCourseStructureService.createLesson(
                chapter.getId(),
                TeacherLessonRequest.builder()
                        .title("Bài 2: Cacbohidrat (Khóa)")
                        .content("Nội dung chuyên sâu Cacbohidrat...")
                        .status(LessonStatus.PUBLISHED)
                        .isPreview(false)
                        .build(),
                teacherUser.getId()
        );

        courseWorkflowService.submitForReview(course.getId(), teacherUser.getId());
        authenticateAs(adminUser, "ADMIN");
        courseWorkflowService.approveCourse(course.getId(), adminUser.getId());

        // Authenticate as Student who hasn't purchased
        authenticateAs(studentUser, "STUDENT");

        // 1. Preview lesson should be accessible
        StudentLessonDetailResponse previewDetail = studentCourseStructureService.getPublishedLesson(
                previewLesson.getId(), studentUser.getId()
        );
        assertThat(previewDetail).isNotNull();
        assertThat(previewDetail.getTitle()).isEqualTo("Bài 1: Este - Lipit (Học thử)");
        assertThat(previewDetail.getContent()).isEqualTo("Nội dung học thử Este...");

        // 2. Locked lesson should throw ForbiddenOperationException
        assertThatThrownBy(() -> studentCourseStructureService.getPublishedLesson(lockedLesson.getId(), studentUser.getId()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Vui lòng mua khóa học");

        // 3. Purchase Course Flow
        Order order = orderService.createCourseOrder(studentUser.getId(), course.getId(), null, "IDEMP-MKT-" + UUID.randomUUID());
        order.setStatus(OrderStatus.PAID);

        // Fulfill entitlement
        entitlementActivationService.activateOrderFulfillment(order);

        // 4. Locked lesson should now be accessible!
        StudentLessonDetailResponse unlockedDetail = studentCourseStructureService.getPublishedLesson(
                lockedLesson.getId(), studentUser.getId()
        );
        assertThat(unlockedDetail).isNotNull();
        assertThat(unlockedDetail.getTitle()).isEqualTo("Bài 2: Cacbohidrat (Khóa)");
        assertThat(unlockedDetail.getContent()).isEqualTo("Nội dung chuyên sâu Cacbohidrat...");
    }

    @Test
    @DisplayName("Course review eligibility, 1-review constraint and rating aggregation")
    void testCourseReviewEligibilityAndConstraint() {
        authenticateAs(teacherUser, "TEACHER");

        TeacherCourseResponse course = teacherCourseService.createCourse(
                TeacherCourseRequest.builder()
                        .subjectId(mathSubject.getId())
                        .name("Khóa Học Sinh Học 12")
                        .code("BIO12-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                        .pricingType(CoursePricingType.PAID)
                        .price(new BigDecimal("199000.00"))
                        .build(),
                teacherUser.getId()
        );

        teacherCourseStructureService.createChapter(course.getId(), TeacherChapterRequest.builder().title("Chương 1").build(), teacherUser.getId());
        courseWorkflowService.submitForReview(course.getId(), teacherUser.getId());

        authenticateAs(adminUser, "ADMIN");
        courseWorkflowService.approveCourse(course.getId(), adminUser.getId());

        authenticateAs(studentUser, "STUDENT");

        // 1. Unenrolled / Unpurchased student cannot review
        assertThatThrownBy(() -> marketplaceService.createCourseReview(
                course.getId(),
                studentUser.getId(),
                CreateCourseReviewRequest.builder().rating(5).comment("Khóa học rất hay!").build()
        )).isInstanceOf(ForbiddenOperationException.class)
          .hasMessageContaining("Chỉ học viên đã");

        // 2. Purchase course
        Order order = orderService.createCourseOrder(studentUser.getId(), course.getId(), null, "IDEMP-REV-" + UUID.randomUUID());
        order.setStatus(OrderStatus.PAID);
        entitlementActivationService.activateOrderFulfillment(order);

        // 3. Review successfully
        CourseReviewResponse review = marketplaceService.createCourseReview(
                course.getId(),
                studentUser.getId(),
                CreateCourseReviewRequest.builder().rating(5).comment("Khóa học quá xuất sắc!").build()
        );
        assertThat(review).isNotNull();
        assertThat(review.getRating()).isEqualTo(5);

        // 4. Verify Course Average Rating is updated
        Course updatedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updatedCourse.getReviewCount()).isEqualTo(1);
        assertThat(updatedCourse.getAverageRating()).isEqualTo(5.0);

        // 5. Duplicate review attempt is blocked
        assertThatThrownBy(() -> marketplaceService.createCourseReview(
                course.getId(),
                studentUser.getId(),
                CreateCourseReviewRequest.builder().rating(4).comment("Đánh giá lần 2").build()
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Bạn đã đánh giá khóa học này rồi");

        // 6. Update review to 4-star
        CourseReviewResponse updatedReview = marketplaceService.updateCourseReview(
                course.getId(),
                review.getId(),
                studentUser.getId(),
                UpdateCourseReviewRequest.builder().rating(4).comment("Sửa lại 4 sao").build()
        );
        assertThat(updatedReview.getRating()).isEqualTo(4);

        Course refreshedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(refreshedCourse.getAverageRating()).isEqualTo(4.0);
    }
}
