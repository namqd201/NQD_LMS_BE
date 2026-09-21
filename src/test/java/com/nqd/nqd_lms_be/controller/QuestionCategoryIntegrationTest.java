package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.question.QuestionCategoryRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionOptionDto;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionRequest;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class QuestionCategoryIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private QuestionCategoryRepository questionCategoryRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;
    private User teacherUser;
    private Subject mathSubject;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TEACHER").build()));

        teacherUser = userRepository.save(User.builder()
                .email("teacher_cat_" + UUID.randomUUID() + "@test.com")
                .fullName("Thầy Giáo Test")
                .status(UserStatus.ACTIVE)
                .build());

        userRoleRepository.save(UserRole.builder()
                .userId(teacherUser.getId())
                .roleId(teacherRole.getId())
                .user(teacherUser)
                .role(teacherRole)
                .build());

        mathSubject = subjectRepository.save(Subject.builder()
                .name("Toán Học " + UUID.randomUUID())
                .code("MATH_" + UUID.randomUUID().toString().substring(0, 8))
                .build());
    }

    private OAuth2AuthenticationToken createAuthToken(User user) {
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                Set.of("ROLE_TEACHER"),
                Map.of("sub", "sub-" + user.getId(), "email", user.getEmail()),
                null,
                null
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    @DisplayName("Tạo mới chuyên đề câu hỏi (QuestionCategory) và lấy danh sách thành công")
    void testCreateAndGetCategories() throws Exception {
        QuestionCategoryRequest request = QuestionCategoryRequest.builder()
                .name("Cộng trừ trong phạm vi 100")
                .code("TOAN1_CONGTRU_100")
                .description("Các bài toán cộng trừ không nhớ và có nhớ trong phạm vi 100")
                .subjectId(mathSubject.getId())
                .gradeLevel("Lớp 1")
                .displayOrder(1)
                .build();

        OAuth2AuthenticationToken auth = createAuthToken(teacherUser);

        // 1. POST create category
        MvcResult result = mockMvc.perform(post("/api/v1/teacher/question-categories")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Cộng trừ trong phạm vi 100"))
                .andExpect(jsonPath("$.gradeLevel").value("Lớp 1"))
                .andReturn();

        // 2. GET categories by subject and grade
        mockMvc.perform(get("/api/v1/teacher/question-categories")
                        .with(authentication(auth))
                        .param("subjectId", mathSubject.getId().toString())
                        .param("gradeLevel", "Lớp 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].name", hasItem("Cộng trừ trong phạm vi 100")));
    }

    @Test
    @DisplayName("Tạo câu hỏi gắn với Category và lọc câu hỏi theo categoryId thành công")
    void testCreateQuestionWithCategoryAndFilter() throws Exception {
        // 1. Tạo category
        QuestionCategory cat = questionCategoryRepository.save(QuestionCategory.builder()
                .name("Cộng trừ trong phạm vi 100")
                .subject(mathSubject)
                .gradeLevel("Lớp 1")
                .creator(teacherUser)
                .displayOrder(1)
                .build());

        OAuth2AuthenticationToken auth = createAuthToken(teacherUser);

        // 2. Tạo câu hỏi gắn categoryId
        TeacherQuestionRequest qReq = TeacherQuestionRequest.builder()
                .subjectId(mathSubject.getId())
                .categoryId(cat.getId())
                .gradeLevel("Lớp 1")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .content("Tính: 35 + 24 = ?")
                .explanation("35 + 24 = 59")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.APPROVED)
                .options(List.of(
                        TeacherQuestionOptionDto.builder().optionKey("A").optionText("59").isCorrect(true).displayOrder(1).build(),
                        TeacherQuestionOptionDto.builder().optionKey("B").optionText("58").isCorrect(false).displayOrder(2).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/teacher/questions")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(cat.getId().toString()))
                .andExpect(jsonPath("$.categoryName").value("Cộng trừ trong phạm vi 100"));

        // 3. Lọc câu hỏi bằng categoryId
        mockMvc.perform(get("/api/v1/teacher/questions")
                        .with(authentication(auth))
                        .param("categoryId", cat.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Tính: 35 + 24 = ?"))
                .andExpect(jsonPath("$[0].categoryName").value("Cộng trừ trong phạm vi 100"));
    }

    @Test
    @DisplayName("Cập nhật và xóa mềm chuyên đề câu hỏi")
    void testUpdateAndDeleteCategory() throws Exception {
        QuestionCategory cat = questionCategoryRepository.save(QuestionCategory.builder()
                .name("Hình học trực quan")
                .subject(mathSubject)
                .gradeLevel("Lớp 1")
                .creator(teacherUser)
                .displayOrder(2)
                .build());

        OAuth2AuthenticationToken auth = createAuthToken(teacherUser);

        // Update
        QuestionCategoryRequest updateReq = QuestionCategoryRequest.builder()
                .name("Hình học và Đo lường")
                .subjectId(mathSubject.getId())
                .gradeLevel("Lớp 1")
                .displayOrder(2)
                .build();

        mockMvc.perform(put("/api/v1/teacher/question-categories/" + cat.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hình học và Đo lường"));

        // Delete
        mockMvc.perform(delete("/api/v1/teacher/question-categories/" + cat.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk());

        QuestionCategory deleted = questionCategoryRepository.findById(cat.getId()).orElseThrow();
        assertTrue(deleted.getIsDeleted());
    }
    @Test
    @DisplayName("Kiểm tra 3 cấp độ hiển thị (PUBLIC, TEACHER_SHARED, PRIVATE) và quyền xem của giáo viên")
    void testCategoryVisibilityLevels() throws Exception {
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER").orElseThrow();
        User otherTeacher = userRepository.save(User.builder()
                .email("other_teacher_" + UUID.randomUUID() + "@test.com")
                .fullName("Giáo Viên Khác")
                .status(UserStatus.ACTIVE)
                .build());

        userRoleRepository.save(UserRole.builder()
                .userId(otherTeacher.getId())
                .roleId(teacherRole.getId())
                .user(otherTeacher)
                .role(teacherRole)
                .build());

        // 1. teacherUser creates a PRIVATE category
        QuestionCategory privateCat = questionCategoryRepository.save(QuestionCategory.builder()
                .name("Chuyên đề bí mật cá nhân")
                .subject(mathSubject)
                .gradeLevel("Lớp 1")
                .visibility(QuestionCategoryVisibility.PRIVATE)
                .creator(teacherUser)
                .displayOrder(1)
                .build());

        // 2. teacherUser creates a TEACHER_SHARED category
        QuestionCategory teacherSharedCat = questionCategoryRepository.save(QuestionCategory.builder()
                .name("Chuyên đề nội bộ giáo viên")
                .subject(mathSubject)
                .gradeLevel("Lớp 1")
                .visibility(QuestionCategoryVisibility.TEACHER_SHARED)
                .creator(teacherUser)
                .displayOrder(2)
                .build());

        // 3. teacherUser creates a PUBLIC category
        QuestionCategory publicCat = questionCategoryRepository.save(QuestionCategory.builder()
                .name("Chuyên đề công khai toàn trường")
                .subject(mathSubject)
                .gradeLevel("Lớp 1")
                .visibility(QuestionCategoryVisibility.PUBLIC)
                .creator(teacherUser)
                .displayOrder(3)
                .build());

        OAuth2AuthenticationToken authTeacher = createAuthToken(teacherUser);
        OAuth2AuthenticationToken authOther = createAuthToken(otherTeacher);

        // teacherUser (creator) sees all 3 categories
        mockMvc.perform(get("/api/v1/teacher/question-categories")
                        .with(authentication(authTeacher))
                        .param("subjectId", mathSubject.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // otherTeacher sees TEACHER_SHARED and PUBLIC, but NOT PRIVATE
        mockMvc.perform(get("/api/v1/teacher/question-categories")
                        .with(authentication(authOther))
                        .param("subjectId", mathSubject.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", not(hasItem("Chuyên đề bí mật cá nhân"))))
                .andExpect(jsonPath("$[*].name", hasItem("Chuyên đề nội bộ giáo viên")))
                .andExpect(jsonPath("$[*].name", hasItem("Chuyên đề công khai toàn trường")));
    }

    @Test
    @DisplayName("Luôn có 1 chuyên đề PUBLIC cho mọi môn/lớp và di chuyển câu hỏi Lớp 1 vào chuyên đề PUBLIC đó")
    void testDefaultPublicCategoryAndGrade1Migration() throws Exception {
        OAuth2AuthenticationToken auth = createAuthToken(teacherUser);

        // 1. Kiểm tra khi truy vấn môn học & lớp chưa có category, hệ thống tự động sinh chuyên đề PUBLIC 'Chuyên đề chung'
        mockMvc.perform(get("/api/v1/teacher/question-categories")
                        .with(authentication(auth))
                        .param("subjectId", mathSubject.getId().toString())
                        .param("gradeLevel", "Lớp 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Chuyên đề chung")))
                .andExpect(jsonPath("$[?(@.name == 'Chuyên đề chung')].visibility").value(hasItem("PUBLIC")))
                .andExpect(jsonPath("$[?(@.name == 'Chuyên đề chung')].isSystem").value(hasItem(true)));

        // 2. Tạo câu hỏi Lớp 1 không truyền categoryId -> hệ thống tự động gán vào chuyên đề PUBLIC mặc định
        TeacherQuestionRequest qReq = TeacherQuestionRequest.builder()
                .subjectId(mathSubject.getId())
                .gradeLevel("Lớp 1")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .content("Bé có 5 quả táo, mẹ cho thêm 3 quả táo. Hỏi bé có tất cả bao nhiêu quả táo?")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.APPROVED)
                .options(List.of(
                        TeacherQuestionOptionDto.builder().optionKey("A").optionText("8").isCorrect(true).displayOrder(1).build(),
                        TeacherQuestionOptionDto.builder().optionKey("B").optionText("7").isCorrect(false).displayOrder(2).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/teacher/questions")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").isNotEmpty())
                .andExpect(jsonPath("$.categoryName").value("Chuyên đề chung"));
    }
}
