package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamVisibility;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherExamServiceImpl implements TeacherExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionTagRelationRepository questionTagRelationRepository;
    private final UserRepository userRepository;
    private final ExamAttemptEventRepository examAttemptEventRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherExamResponse> getExams(
            UUID subjectId,
            UUID courseId,
            String gradeLevel,
            ExamStatus status,
            String keyword,
            UUID teacherId
    ) {
        Specification<Exam> spec = (root, query, cb) -> cb.isFalse(root.get("isDeleted"));

        if (!SecurityUtils.isAdmin()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("creator").get("id"), teacherId));
        }

        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId));
        }

        if (courseId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("course").get("id"), courseId));
        }

        if (gradeLevel != null && !gradeLevel.trim().isEmpty() && !gradeLevel.equalsIgnoreCase("ALL")) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gradeLevel"), gradeLevel.trim()));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        return examRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToExamResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherExamResponse getExamById(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (!exam.getIsDeleted() && !SecurityUtils.isAdmin() && (exam.getCreator() == null || !exam.getCreator().getId().equals(teacherId))) {
            boolean isSharedOrPublic = exam.getStatus() == ExamStatus.PUBLISHED &&
                    (exam.getVisibility() == ExamVisibility.PUBLIC || exam.getVisibility() == ExamVisibility.SUBJECT_SHARED);
            if (!isSharedOrPublic) {
                throw new ForbiddenOperationException("You do not have permission to view this exam");
            }
        } else if (exam.getIsDeleted() && !SecurityUtils.isAdmin() && (exam.getCreator() == null || !exam.getCreator().getId().equals(teacherId))) {
            throw new ForbiddenOperationException("You do not have permission to view this exam");
        }

        return mapToExamResponse(exam);
    }

    @Override
    @Transactional
    public TeacherExamResponse createExam(TeacherExamRequest request, UUID teacherId) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));

        User creator = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));
        }

        String effectiveGradeLevel = request.getGradeLevel();
        if ((effectiveGradeLevel == null || effectiveGradeLevel.trim().isEmpty()) && course != null) {
            effectiveGradeLevel = course.getGradeLevel();
        }

        String code = request.getCode();
        if (code == null || code.trim().isEmpty()) {
            String subPrefix = getPrefixFromSubject(subject);
            String gradeSuffix = getSuffixFromGradeLevel(effectiveGradeLevel);
            int rand = (int) (1000 + Math.random() * 9000);
            code = "DE_" + subPrefix + (gradeSuffix.isEmpty() ? "" : "_" + gradeSuffix) + "_" + rand;
        }

        Exam exam = Exam.builder()
                .subject(subject)
                .course(course)
                .code(code.trim().toUpperCase())
                .gradeLevel(effectiveGradeLevel)
                .title(request.getTitle())
                .description(request.getDescription())
                .instructions(request.getInstructions())
                .durationMinutes(request.getDurationMinutes())
                .totalMarks(request.getTotalMarks() != null ? request.getTotalMarks() : new BigDecimal("10.00"))
                .passingMarks(request.getPassingMarks() != null ? request.getPassingMarks() : new BigDecimal("5.00"))
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 1)
                .shuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()))
                .shuffleOptions(Boolean.TRUE.equals(request.getShuffleOptions()))
                .status(request.getStatus() != null ? request.getStatus() : ExamStatus.DRAFT)
                .visibility(request.getVisibility() != null ? request.getVisibility() : ExamVisibility.PRIVATE)
                .enableProctoring(Boolean.TRUE.equals(request.getEnableProctoring()))
                .maxViolationCount(request.getMaxViolationCount() != null ? request.getMaxViolationCount() : 5)
                .creator(creator)
                .build();

        exam = examRepository.save(exam);

        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            int order = 1;
            for (UUID qId : request.getQuestionIds()) {
                Question question = questionRepository.findById(qId)
                        .orElseThrow(() -> new ResourceNotFoundException("Question", qId));

                ExamQuestion eq = ExamQuestion.builder()
                        .exam(exam)
                        .question(question)
                        .displayOrder(order++)
                        .marks(question.getDefaultMarks())
                        .build();
                examQuestionRepository.save(eq);
            }
        }

        log.info("Teacher {} created exam: {} ({})", creator.getEmail(), exam.getTitle(), exam.getCode());
        return mapToExamResponse(exam);
    }

    @Override
    @Transactional
    public TeacherExamResponse updateExam(UUID examId, TeacherExamRequest request, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        verifyExamOwnership(exam, teacherId);

        if (request.getSubjectId() != null && !request.getSubjectId().equals(exam.getSubject().getId())) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));
            exam.setSubject(subject);
        }

        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));
            exam.setCourse(course);
        } else {
            exam.setCourse(null);
        }

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            exam.setCode(request.getCode().trim().toUpperCase());
        }

        if (request.getGradeLevel() != null) {
            exam.setGradeLevel(request.getGradeLevel());
        }

        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setInstructions(request.getInstructions());
        exam.setDurationMinutes(request.getDurationMinutes());
        if (request.getTotalMarks() != null) {
            exam.setTotalMarks(request.getTotalMarks());
        }
        if (request.getPassingMarks() != null) {
            exam.setPassingMarks(request.getPassingMarks());
        }
        if (request.getMaxAttempts() != null) {
            exam.setMaxAttempts(request.getMaxAttempts());
        }
        if (request.getShuffleQuestions() != null) {
            exam.setShuffleQuestions(request.getShuffleQuestions());
        }
        if (request.getShuffleOptions() != null) {
            exam.setShuffleOptions(request.getShuffleOptions());
        }
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
        }
        if (request.getVisibility() != null) {
            exam.setVisibility(request.getVisibility());
        }
        if (request.getEnableProctoring() != null) {
            exam.setEnableProctoring(request.getEnableProctoring());
        }
        if (request.getMaxViolationCount() != null) {
            exam.setMaxViolationCount(request.getMaxViolationCount());
        }

        exam = examRepository.saveAndFlush(exam);

        if (request.getQuestionIds() != null) {
            examQuestionRepository.deleteByExamId(exam.getId());
            if (!request.getQuestionIds().isEmpty()) {
                List<ExamQuestion> examQuestions = new ArrayList<>();
                int order = 1;
                for (UUID qId : request.getQuestionIds()) {
                    Question question = questionRepository.findById(qId)
                            .orElseThrow(() -> new ResourceNotFoundException("Question", qId));

                    ExamQuestion eq = ExamQuestion.builder()
                            .exam(exam)
                            .question(question)
                            .displayOrder(order++)
                            .marks(question.getDefaultMarks())
                            .build();
                    examQuestions.add(eq);
                }
                examQuestionRepository.saveAllAndFlush(examQuestions);
            }
        }

        log.info("Teacher {} updated exam: {}", teacherId, exam.getId());
        return mapToExamResponse(exam);
    }

    @Override
    @Transactional
    public TeacherExamResponse publishExam(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (!SecurityUtils.isAdmin()) {
            verifyExamOwnership(exam, teacherId);
        }
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);
        log.info("User {} published exam: {}", teacherId, examId);
        return mapToExamResponse(exam);
    }

    @Override
    @Transactional
    public TeacherExamResponse archiveExam(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (!SecurityUtils.isAdmin()) {
            verifyExamOwnership(exam, teacherId);
        }
        exam.setStatus(ExamStatus.ARCHIVED);
        exam = examRepository.save(exam);
        log.info("User {} archived exam: {}", teacherId, examId);
        return mapToExamResponse(exam);
    }

    @Override
    @Transactional
    public void deleteExam(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        verifyExamOwnership(exam, teacherId);

        User user = userRepository.findById(teacherId).orElse(null);
        String userEmail = user != null ? user.getEmail() : teacherId.toString();

        exam.setIsDeleted(true);
        exam.setDeletedAt(LocalDateTime.now());
        exam.setDeletedBy(userEmail);
        examRepository.save(exam);

        log.info("Teacher/User {} soft-deleted exam: {}", userEmail, exam.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherExamResponse> getDeletedExams(UUID teacherId) {
        Specification<Exam> spec = (root, query, cb) -> cb.isTrue(root.get("isDeleted"));

        if (!SecurityUtils.isAdmin()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("creator").get("id"), teacherId));
        }

        List<Exam> deletedExams = examRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "deletedAt"));
        return deletedExams.stream()
                .map(this::mapToExamResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherExamResponse restoreExam(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        verifyExamOwnership(exam, teacherId);

        exam.setIsDeleted(false);
        exam.setDeletedAt(null);
        exam.setDeletedBy(null);
        exam = examRepository.save(exam);

        log.info("Teacher/User {} restored exam: {}", teacherId, exam.getId());
        return mapToExamResponse(exam);
    }

    @Override
    @Transactional
    public TeacherBlueprintResponse createOrUpdateBlueprint(UUID examId, TeacherBlueprintRequest request, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        verifyExamOwnership(exam, teacherId);

        User creator = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        ExamBlueprint blueprint = examBlueprintRepository.findByExamId(examId)
                .orElse(ExamBlueprint.builder().exam(exam).creator(creator).build());

        blueprint.setName(request.getName());
        blueprint.setDescription(request.getDescription());
        blueprint.setTotalQuestions(request.getTotalQuestions());
        blueprint.setTotalMarks(request.getTotalMarks());
        blueprint.setConfiguration(request.getConfiguration());

        blueprint = examBlueprintRepository.save(blueprint);
        log.info("Teacher {} saved blueprint for exam: {}", teacherId, examId);

        return TeacherBlueprintResponse.builder()
                .id(blueprint.getId())
                .examId(examId)
                .name(blueprint.getName())
                .description(blueprint.getDescription())
                .totalQuestions(blueprint.getTotalQuestions())
                .totalMarks(blueprint.getTotalMarks())
                .configuration(blueprint.getConfiguration())
                .creatorId(creator.getId())
                .createdAt(blueprint.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherBlueprintResponse getBlueprint(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        verifyExamOwnership(exam, teacherId);

        ExamBlueprint blueprint = examBlueprintRepository.findByExamId(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Blueprint for exam", examId));

        return TeacherBlueprintResponse.builder()
                .id(blueprint.getId())
                .examId(examId)
                .name(blueprint.getName())
                .description(blueprint.getDescription())
                .totalQuestions(blueprint.getTotalQuestions())
                .totalMarks(blueprint.getTotalMarks())
                .configuration(blueprint.getConfiguration())
                .creatorId(blueprint.getCreator() != null ? blueprint.getCreator().getId() : null)
                .createdAt(blueprint.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void gradeAttempt(UUID attemptId, TeacherGradeAttemptRequest request, UUID teacherId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttempt", attemptId));

        verifyExamOwnership(attempt.getExam(), teacherId);

        attempt.setTotalScore(request.getTotalScore());
        if (attempt.getExam().getTotalMarks() != null && attempt.getExam().getTotalMarks().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = request.getTotalScore()
                    .multiply(new BigDecimal("100"))
                    .divide(attempt.getExam().getTotalMarks(), 2, RoundingMode.HALF_UP);
            attempt.setPercentage(percentage);
            attempt.setPassed(request.getTotalScore().compareTo(attempt.getExam().getPassingMarks()) >= 0);
        }
        attempt.setStatus(ExamAttemptStatus.GRADED);
        attempt.setGradedAt(LocalDateTime.now());

        examAttemptRepository.save(attempt);
        log.info("Teacher {} graded attempt: {}", teacherId, attemptId);
    }

    private void verifyExamOwnership(Exam exam, UUID teacherId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (exam.getCreator() == null || !exam.getCreator().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("You do not have permission to modify this exam");
        }
    }

    private TeacherExamResponse mapToExamResponse(Exam exam) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByDisplayOrderAsc(exam.getId());

        List<TeacherQuestionResponse> questionDtos = new ArrayList<>();
        if (examQuestions != null) {
            for (ExamQuestion eq : examQuestions) {
                Question q = eq.getQuestion();
                List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
                List<TeacherQuestionOptionDto> optionDtos = options != null ? options.stream()
                        .map(opt -> TeacherQuestionOptionDto.builder()
                                .id(opt.getId())
                                .optionKey(opt.getOptionKey())
                                .optionText(opt.getOptionText())
                                .isCorrect(opt.getIsCorrect())
                                .displayOrder(opt.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList()) : Collections.emptyList();

                List<String> tags = questionTagRelationRepository.findTagNamesByQuestionId(q.getId());

                questionDtos.add(TeacherQuestionResponse.builder()
                        .id(q.getId())
                        .subjectId(q.getSubject() != null ? q.getSubject().getId() : null)
                        .subjectName(q.getSubject() != null ? q.getSubject().getName() : null)
                        .courseId(q.getCourse() != null ? q.getCourse().getId() : null)
                        .courseName(q.getCourse() != null ? q.getCourse().getName() : null)
                        .lessonId(q.getLesson() != null ? q.getLesson().getId() : null)
                        .lessonTitle(q.getLesson() != null ? q.getLesson().getTitle() : null)
                        .gradeLevel(q.getGradeLevel())
                        .questionType(q.getQuestionType())
                        .difficulty(q.getDifficulty())
                        .content(q.getContent())
                        .explanation(q.getExplanation())
                        .defaultMarks(eq.getMarks() != null ? eq.getMarks() : q.getDefaultMarks())
                        .status(q.getStatus())
                        .creatorId(q.getCreator() != null ? q.getCreator().getId() : null)
                        .creatorName(q.getCreator() != null ? q.getCreator().getFullName() : null)
                        .tags(tags)
                        .options(optionDtos)
                        .createdAt(q.getCreatedAt())
                        .updatedAt(q.getUpdatedAt())
                        .build());
            }
        }

        return TeacherExamResponse.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .gradeLevel(exam.getGradeLevel())
                .subjectId(exam.getSubject() != null ? exam.getSubject().getId() : null)
                .subjectName(exam.getSubject() != null ? exam.getSubject().getName() : null)
                .courseId(exam.getCourse() != null ? exam.getCourse().getId() : null)
                .courseName(exam.getCourse() != null ? exam.getCourse().getName() : null)
                .title(exam.getTitle())
                .description(exam.getDescription())
                .instructions(exam.getInstructions())
                .durationMinutes(exam.getDurationMinutes())
                .totalMarks(exam.getTotalMarks())
                .passingMarks(exam.getPassingMarks())
                .maxAttempts(exam.getMaxAttempts())
                .shuffleQuestions(exam.getShuffleQuestions())
                .shuffleOptions(exam.getShuffleOptions())
                .status(exam.getStatus())
                .visibility(exam.getVisibility() != null ? exam.getVisibility() : ExamVisibility.PRIVATE)
                .audioUrl(exam.getAudioUrl())
                .audioScript(exam.getAudioScript())
                .maxListeningPlays(exam.getMaxListeningPlays())
                .enableProctoring(exam.getEnableProctoring())
                .maxViolationCount(exam.getMaxViolationCount())
                .originExamId(exam.getOriginExam() != null ? exam.getOriginExam().getId() : null)
                .originExamTitle(exam.getOriginExam() != null ? exam.getOriginExam().getTitle() : null)
                .creatorId(exam.getCreator() != null ? exam.getCreator().getId() : null)
                .creatorName(exam.getCreator() != null ? exam.getCreator().getFullName() : null)
                .questionCount(examQuestions != null ? examQuestions.size() : 0)
                .questions(questionDtos)
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .isDeleted(exam.getIsDeleted())
                .deletedAt(exam.getDeletedAt())
                .deletedBy(exam.getDeletedBy())
                .build();
    }

    private String getPrefixFromSubject(Subject sub) {
        if (sub == null) return "MON";
        String name = sub.getName().toLowerCase();
        String subCode = sub.getCode() != null ? sub.getCode().toUpperCase() : "";
        if (name.contains("toán") || subCode.contains("MATH")) return "TOAN";
        if (name.contains("tin") || subCode.contains("IT") || name.contains("lập trình")) return "TIN";
        if (name.contains("tiếng anh") || name.contains("anh") || subCode.contains("ENG")) return "ANH";
        if (name.contains("khoa học") || name.contains("vật lý") || name.contains("lý") || subCode.contains("PHYS")) return "LY";
        if (name.contains("hóa") || subCode.contains("CHEM")) return "HOA";
        if (name.contains("sinh") || subCode.contains("BIO")) return "SINH";
        if (name.contains("văn") || name.contains("ngữ văn") || subCode.contains("LIT")) return "VAN";
        if (name.contains("sử") || subCode.contains("HIST")) return "SU";
        if (name.contains("địa") || subCode.contains("GEO")) return "DIA";
        return subCode.length() >= 3 ? subCode.substring(0, Math.min(4, subCode.length())) : "MON";
    }

    private String getSuffixFromGradeLevel(String grade) {
        if (grade == null || grade.trim().isEmpty()) return "";
        String g = grade.trim();
        if (g.toLowerCase().contains("năm nhất") || g.toLowerCase().contains("năm 1")) return "N1";
        if (g.toLowerCase().contains("năm 2") || g.toLowerCase().contains("năm hai")) return "N2";
        if (g.toLowerCase().contains("năm 3") || g.toLowerCase().contains("năm ba")) return "N3";
        if (g.toLowerCase().contains("năm 4") || g.toLowerCase().contains("năm tư") || g.toLowerCase().contains("năm bốn")) return "N4";
        if (g.contains("12")) return "L12";
        if (g.contains("11")) return "L11";
        if (g.contains("10")) return "L10";
        if (g.contains("9")) return "L9";
        if (g.contains("8")) return "L8";
        if (g.contains("7")) return "L7";
        if (g.contains("6")) return "L6";
        if (g.contains("5")) return "L5";
        if (g.contains("4")) return "L4";
        if (g.contains("3")) return "L3";
        if (g.contains("2")) return "L2";
        if (g.contains("1")) return "L1";
        if (g.toLowerCase().contains("tiểu học")) return "TH";
        if (g.toLowerCase().contains("trung học cơ sở") || g.toLowerCase().contains("thcs")) return "THCS";
        if (g.toLowerCase().contains("trung học phổ thông") || g.toLowerCase().contains("thpt")) return "THPT";
        if (g.toLowerCase().contains("đại học") || g.toLowerCase().contains("cao đẳng")) return "DH";
        return "L" + g.replaceAll("\\D", "");
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherExamStudentCandidateResponse> getEligibleStudentsForExam(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (!exam.getCreator().getId().equals(teacherId) && !SecurityUtils.isAdmin()) {
            throw new ForbiddenOperationException("You do not have permission to manage this exam");
        }

        // Get existing assigned student IDs
        List<ExamAssignment> currentAssignments = examAssignmentRepository.findByExamId(examId);
        Set<UUID> assignedStudentIds = currentAssignments.stream()
                .map(ea -> ea.getStudent().getId())
                .collect(Collectors.toSet());

        Map<UUID, LocalDateTime> assignedAtMap = currentAssignments.stream()
                .collect(Collectors.toMap(ea -> ea.getStudent().getId(), ExamAssignment::getAssignedAt, (a, b) -> a));

        List<CourseEnrollment> enrollments;
        if (exam.getCourse() != null) {
            // Get enrolled students for the specific course
            enrollments = courseEnrollmentRepository.findByCourseId(exam.getCourse().getId());
        } else {
            // Get enrolled students across all courses created by this teacher
            List<Course> teacherCourses = courseRepository.findCoursesByTeacherId(teacherId);
            enrollments = teacherCourses.stream()
                    .flatMap(c -> courseEnrollmentRepository.findByCourseId(c.getId()).stream())
                    .collect(Collectors.toList());
        }

        // Deduplicate students by studentId (keep course info)
        Map<UUID, TeacherExamStudentCandidateResponse> studentMap = new LinkedHashMap<>();
        for (CourseEnrollment ce : enrollments) {
            User student = ce.getStudent();
            UUID sId = student.getId();
            if (!studentMap.containsKey(sId)) {
                studentMap.put(sId, TeacherExamStudentCandidateResponse.builder()
                        .studentId(sId)
                        .studentName(student.getFullName() != null ? student.getFullName() : student.getEmail())
                        .studentEmail(student.getEmail())
                        .avatarUrl(student.getAvatarUrl())
                        .courseId(ce.getCourse().getId())
                        .courseName(ce.getCourse().getName())
                        .isAssigned(assignedStudentIds.contains(sId))
                        .assignedAt(assignedAtMap.get(sId))
                        .build());
            }
        }

        return new ArrayList<>(studentMap.values());
    }

    @Override
    @Transactional
    public void assignStudentsToExam(UUID examId, AssignStudentsToExamRequest request, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (!exam.getCreator().getId().equals(teacherId) && !SecurityUtils.isAdmin()) {
            throw new ForbiddenOperationException("You do not have permission to assign students to this exam");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        List<UUID> targetStudentIds = request.getStudentIds() != null ? request.getStudentIds() : Collections.emptyList();

        List<ExamAssignment> currentAssignments = examAssignmentRepository.findByExamId(examId);
        Set<UUID> currentAssignedIds = currentAssignments.stream()
                .map(ea -> ea.getStudent().getId())
                .collect(Collectors.toSet());

        // Find newly assigned students
        List<UUID> newlyAssignedIds = targetStudentIds.stream()
                .filter(id -> !currentAssignedIds.contains(id))
                .collect(Collectors.toList());

        // Find removed students
        List<UUID> removedIds = currentAssignedIds.stream()
                .filter(id -> !targetStudentIds.contains(id))
                .collect(Collectors.toList());

        // Remove unselected
        for (UUID removedId : removedIds) {
            examAssignmentRepository.deleteByExamIdAndStudentId(examId, removedId);
        }

        // Add new assignments and send notifications
        LocalDateTime now = LocalDateTime.now();
        String subjectName = exam.getSubject() != null ? exam.getSubject().getName() : "Môn học";
        String teacherName = teacher.getFullName() != null ? teacher.getFullName() : "Giáo viên";

        for (UUID newStudentId : newlyAssignedIds) {
            User student = userRepository.findById(newStudentId).orElse(null);
            if (student == null) continue;

            ExamAssignment assignment = ExamAssignment.builder()
                    .exam(exam)
                    .student(student)
                    .assignedAt(now)
                    .assignedBy(teacher)
                    .build();
            examAssignmentRepository.save(assignment);

            // Send notification to student via Kafka / DB
            String notifTitle = "Bài kiểm tra mới: " + exam.getTitle();
            String notifBody = "Giáo viên " + teacherName + " đã giao cho bạn bài kiểm tra \"" + exam.getTitle() + "\" (" + subjectName + "). Hãy vào làm bài ngay!";
            String notifLink = exam.getCourse() != null ? "/courses/" + exam.getCourse().getId() : "/courses";

            kafkaNotificationProducer.sendNotification(
                    student.getId(),
                    "EXAM_ASSIGNED",
                    notifTitle,
                    notifBody,
                    notifLink
            );
        }

        log.info("Teacher {} updated exam {} assignments: added {}, removed {}",
                teacher.getEmail(), examId, newlyAssignedIds.size(), removedIds.size());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherExamResultsSummaryResponse getExamResults(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (!exam.getCreator().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("You can only view results for your own exams");
        }

        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdOrderBySubmittedAtDescStartedAtDesc(examId);
        long totalAssigned = examAssignmentRepository.countByExamId(examId);
        long totalSubmissions = attempts.size();
        long passedCount = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();

        BigDecimal highestScore = BigDecimal.ZERO;
        BigDecimal sumScore = BigDecimal.ZERO;
        long gradedAttemptsCount = 0;

        List<TeacherExamStudentAttemptResponse> attemptDtos = new ArrayList<>();
        int questionCount = (int) examQuestionRepository.countByExamId(examId);

        for (ExamAttempt a : attempts) {
            if (a.getTotalScore() != null) {
                sumScore = sumScore.add(a.getTotalScore());
                gradedAttemptsCount++;
                if (a.getTotalScore().compareTo(highestScore) > 0) {
                    highestScore = a.getTotalScore();
                }
            }

            Long durationSec = null;
            if (a.getStartedAt() != null && a.getSubmittedAt() != null) {
                durationSec = java.time.Duration.between(a.getStartedAt(), a.getSubmittedAt()).getSeconds();
            }

            List<ExamAnswer> answers = examAnswerRepository.findByAttemptId(a.getId());
            long correctCount = answers.stream().filter(ans -> Boolean.TRUE.equals(ans.getIsCorrect())).count();

            attemptDtos.add(TeacherExamStudentAttemptResponse.builder()
                    .attemptId(a.getId())
                    .studentId(a.getStudent().getId())
                    .studentName(a.getStudent().getFullName())
                    .studentEmail(a.getStudent().getEmail())
                    .studentAvatar(a.getStudent().getAvatarUrl())
                    .attemptNumber(a.getAttemptNumber())
                    .status(a.getStatus())
                    .startedAt(a.getStartedAt())
                    .submittedAt(a.getSubmittedAt())
                    .durationSeconds(durationSec)
                    .totalScore(a.getTotalScore())
                    .maxScore(exam.getTotalMarks())
                    .percentage(a.getPercentage())
                    .passed(a.getPassed())
                    .correctAnswersCount(correctCount)
                    .totalQuestionsCount((long) questionCount)
                    .build());
        }

        BigDecimal averageScore = gradedAttemptsCount > 0
                ? sumScore.divide(new BigDecimal(gradedAttemptsCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return TeacherExamResultsSummaryResponse.builder()
                .examId(exam.getId())
                .examCode(exam.getCode())
                .title(exam.getTitle())
                .subjectName(exam.getSubject() != null ? exam.getSubject().getName() : null)
                .gradeLevel(exam.getGradeLevel())
                .durationMinutes(exam.getDurationMinutes())
                .totalMarks(exam.getTotalMarks())
                .passingMarks(exam.getPassingMarks())
                .maxAttempts(exam.getMaxAttempts())
                .questionCount(questionCount)
                .totalAssigned(totalAssigned)
                .totalSubmissions(totalSubmissions)
                .passedCount(passedCount)
                .averageScore(averageScore)
                .highestScore(highestScore)
                .attempts(attemptDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherExamAttemptDetailResponse getAttemptDetail(UUID attemptId, UUID teacherId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttempt", attemptId));

        Exam exam = attempt.getExam();
        if (!exam.getCreator().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("You can only view attempt details for your own exams");
        }

        Long durationSec = null;
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            durationSec = java.time.Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();
        }

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByDisplayOrderAsc(exam.getId());
        List<ExamAnswer> answers = examAnswerRepository.findByAttemptId(attemptId);
        Map<UUID, ExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(ans -> ans.getQuestion().getId(), ans -> ans, (a1, a2) -> a1));

        List<TeacherExamAttemptAnswerDetailResponse> answerDtos = new ArrayList<>();

        for (ExamQuestion eq : examQuestions) {
            Question q = eq.getQuestion();
            ExamAnswer ans = answerMap.get(q.getId());

            List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
            List<TeacherQuestionOptionDto> optionDtos = options != null ? options.stream()
                    .map(opt -> TeacherQuestionOptionDto.builder()
                            .id(opt.getId())
                            .optionKey(opt.getOptionKey())
                            .optionText(opt.getOptionText())
                            .isCorrect(opt.getIsCorrect())
                            .displayOrder(opt.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList()) : Collections.emptyList();

            QuestionOption correctOpt = options != null ? options.stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                    .findFirst()
                    .orElse(null) : null;

            QuestionOption studentOpt = ans != null ? ans.getSelectedOption() : null;

            answerDtos.add(TeacherExamAttemptAnswerDetailResponse.builder()
                    .questionId(q.getId())
                    .displayOrder(eq.getDisplayOrder())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .difficulty(q.getDifficulty())
                    .maxMarks(eq.getMarks() != null ? eq.getMarks() : q.getDefaultMarks())
                    .marksAwarded(ans != null ? ans.getMarksAwarded() : BigDecimal.ZERO)
                    .isCorrect(ans != null ? ans.getIsCorrect() : false)
                    .studentSelectedOptionId(studentOpt != null ? studentOpt.getId() : null)
                    .studentSelectedOptionKey(studentOpt != null ? studentOpt.getOptionKey() : null)
                    .studentSelectedOptionText(studentOpt != null ? studentOpt.getOptionText() : null)
                    .studentAnswerText(ans != null ? ans.getAnswerText() : null)
                    .correctOptionKey(correctOpt != null ? correctOpt.getOptionKey() : null)
                    .correctOptionText(correctOpt != null ? correctOpt.getOptionText() : null)
                    .explanation(q.getExplanation())
                    .options(optionDtos)
                    .build());
        }

        List<ExamAttemptEvent> events = examAttemptEventRepository.findByAttemptIdOrderByOccurredAtAsc(attemptId);
        List<com.nqd.nqd_lms_be.dto.student.ExamAttemptEventDto> eventDtos = events != null ? events.stream()
                .map(ev -> com.nqd.nqd_lms_be.dto.student.ExamAttemptEventDto.builder()
                        .id(ev.getId())
                        .eventType(ev.getEventType())
                        .occurredAt(ev.getOccurredAt())
                        .metadata(ev.getMetadata())
                        .isViolation(ev.getIsViolation())
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        return TeacherExamAttemptDetailResponse.builder()
                .attemptId(attempt.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .examCode(exam.getCode())
                .studentId(attempt.getStudent().getId())
                .studentName(attempt.getStudent().getFullName())
                .studentEmail(attempt.getStudent().getEmail())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .durationSeconds(durationSec)
                .totalScore(attempt.getTotalScore())
                .maxScore(exam.getTotalMarks())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .violationCount(attempt.getViolationCount() != null ? attempt.getViolationCount() : 0)
                .isFlagged(Boolean.TRUE.equals(attempt.getIsFlagged()))
                .flagReason(attempt.getFlagReason())
                .answers(answerDtos)
                .events(eventDtos)
                .build();
    }

    @Override
    @Transactional
    public TeacherExamResponse cloneExam(UUID examId, UUID teacherId) {
        Exam original = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (!SecurityUtils.isAdmin() && (original.getCreator() == null || !original.getCreator().getId().equals(teacherId))) {
            boolean isSharedOrPublic = original.getStatus() == ExamStatus.PUBLISHED &&
                    (original.getVisibility() == ExamVisibility.PUBLIC || original.getVisibility() == ExamVisibility.SUBJECT_SHARED);
            if (!isSharedOrPublic) {
                throw new ForbiddenOperationException("You do not have permission to clone this exam");
            }
        }

        User currentUser = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        String subPrefix = getPrefixFromSubject(original.getSubject());
        String gradeSuffix = getSuffixFromGradeLevel(original.getGradeLevel());
        int rand = (int) (1000 + Math.random() * 9000);
        String newCode = "CLONE_" + subPrefix + (gradeSuffix.isEmpty() ? "" : "_" + gradeSuffix) + "_" + rand;

        Exam clonedExam = Exam.builder()
                .subject(original.getSubject())
                .course(null)
                .code(newCode)
                .gradeLevel(original.getGradeLevel())
                .title("[Bản sao] " + original.getTitle())
                .description(original.getDescription())
                .instructions(original.getInstructions())
                .durationMinutes(original.getDurationMinutes() != null ? original.getDurationMinutes() : 60)
                .totalMarks(original.getTotalMarks() != null ? original.getTotalMarks() : new BigDecimal("10.00"))
                .passingMarks(original.getPassingMarks() != null ? original.getPassingMarks() : new BigDecimal("5.00"))
                .maxAttempts(original.getMaxAttempts() != null ? original.getMaxAttempts() : 1)
                .shuffleQuestions(Boolean.TRUE.equals(original.getShuffleQuestions()))
                .shuffleOptions(Boolean.TRUE.equals(original.getShuffleOptions()))
                .status(ExamStatus.DRAFT)
                .visibility(ExamVisibility.PRIVATE)
                .originExam(original)
                .creator(currentUser)
                .build();

        clonedExam = examRepository.save(clonedExam);

        List<ExamQuestion> originalQuestions = examQuestionRepository.findByExamIdOrderByDisplayOrderAsc(original.getId());
        if (originalQuestions != null && !originalQuestions.isEmpty()) {
            List<ExamQuestion> clonedQuestions = new ArrayList<>();
            int order = 1;
            for (ExamQuestion oq : originalQuestions) {
                BigDecimal marks = oq.getMarks() != null ? oq.getMarks() :
                        (oq.getQuestion() != null && oq.getQuestion().getDefaultMarks() != null ? oq.getQuestion().getDefaultMarks() : BigDecimal.ONE);
                clonedQuestions.add(ExamQuestion.builder()
                        .exam(clonedExam)
                        .question(oq.getQuestion())
                        .displayOrder(oq.getDisplayOrder() != null ? oq.getDisplayOrder() : order++)
                        .marks(marks)
                        .build());
            }
            examQuestionRepository.saveAll(clonedQuestions);
        }

        Optional<ExamBlueprint> originalBlueprintOpt = examBlueprintRepository.findByExamId(original.getId());
        if (originalBlueprintOpt.isPresent()) {
            ExamBlueprint origBp = originalBlueprintOpt.get();
            ExamBlueprint clonedBp = ExamBlueprint.builder()
                    .exam(clonedExam)
                    .creator(currentUser)
                    .name(origBp.getName())
                    .description(origBp.getDescription())
                    .totalQuestions(origBp.getTotalQuestions())
                    .totalMarks(origBp.getTotalMarks())
                    .configuration(origBp.getConfiguration())
                    .build();
            examBlueprintRepository.save(clonedBp);
        }

        log.info("Teacher {} cloned exam {} into new exam: {} ({})", currentUser.getEmail(), original.getId(), clonedExam.getTitle(), clonedExam.getCode());
        return mapToExamResponse(clonedExam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherExamResponse> getSharedExamLibrary(
            UUID subjectId,
            String gradeLevel,
            String keyword,
            UUID teacherId
    ) {
        Specification<Exam> spec = (root, query, cb) -> cb.isFalse(root.get("isDeleted"));

        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), ExamStatus.PUBLISHED));
        spec = spec.and((root, query, cb) -> root.get("visibility").in(ExamVisibility.PUBLIC, ExamVisibility.SUBJECT_SHARED));

        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId));
        }

        if (gradeLevel != null && !gradeLevel.trim().isEmpty() && !gradeLevel.equalsIgnoreCase("ALL")) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gradeLevel"), gradeLevel.trim()));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        return examRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToExamResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherExamResponse updateExamVisibility(UUID examId, ExamVisibility visibility, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        verifyExamOwnership(exam, teacherId);

        exam.setVisibility(visibility != null ? visibility : ExamVisibility.PRIVATE);
        exam = examRepository.save(exam);

        log.info("Teacher/Admin {} updated visibility of exam {} to {}", teacherId, examId, exam.getVisibility());
        return mapToExamResponse(exam);
    }
}

