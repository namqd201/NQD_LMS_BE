package com.nqd.nqd_lms_be.service.teacher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.dto.RejectTeacherApplicationRequest;
import com.nqd.nqd_lms_be.dto.TeacherApplicationRequest;
import com.nqd.nqd_lms_be.dto.TeacherApplicationResponse;
import com.nqd.nqd_lms_be.entity.Role;
import com.nqd.nqd_lms_be.entity.TeacherApplication;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.UserRole;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicantType;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicationStatus;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.repository.RoleRepository;
import com.nqd.nqd_lms_be.repository.TeacherApplicationRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.repository.UserRoleRepository;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherApplicationServiceImpl implements TeacherApplicationService {

    private final TeacherApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final KafkaNotificationProducer notificationProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String UPLOAD_DIR = "uploads/teacher-docs";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    @Override
    public String uploadVerificationDocument(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tập tin tải lên không được để trống");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng tập tin không được vượt quá 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng tập tin không hợp lệ. Chỉ chấp nhận JPG, PNG, WEBP hoặc PDF");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String storedFilename = UUID.randomUUID().toString() + "." + extension;
            Path targetLocation = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/api/v1/teacher-applications/documents/" + storedFilename;
        } catch (IOException e) {
            log.error("Failed to store verification document: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lưu tập tin tải lên: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TeacherApplicationResponse submitApplication(UUID userId, TeacherApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check if already a teacher
        boolean isTeacher = userRoleRepository.findRoleNamesByUserId(userId).stream()
                .anyMatch(r -> r.equalsIgnoreCase("TEACHER") || r.equalsIgnoreCase("ROLE_TEACHER"));
        if (isTeacher) {
            throw new IllegalArgumentException("Tài khoản của bạn đã là Giáo viên chính thức");
        }

        // Check pending application
        if (applicationRepository.existsByUserIdAndStatusAndIsDeletedFalse(userId, TeacherApplicationStatus.PENDING)) {
            throw new IllegalArgumentException("Bạn đang có một hồ sơ xét duyệt đang chờ xử lý");
        }

        String docsJson = serializeDocUrls(request.getDocumentUrls());

        TeacherApplication app = TeacherApplication.builder()
                .user(user)
                .applicantType(request.getApplicantType())
                .fullName(request.getFullName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .email(request.getEmail().trim())
                .institutionName(request.getInstitutionName().trim())
                .majorOrSubject(request.getMajorOrSubject().trim())
                .bio(request.getBio())
                .documentUrls(docsJson)
                .idCardFrontUrl(request.getIdCardFrontUrl())
                .idCardBackUrl(request.getIdCardBackUrl())
                .sampleVideoUrl(request.getSampleVideoUrl())
                .status(TeacherApplicationStatus.PENDING)
                .build();

        app = applicationRepository.save(app);
        log.info("User {} submitted teacher application {}", userId, app.getId());

        // Mark user as onboarded
        user.setIsOnboarded(true);
        userRepository.save(user);

        // Notify all Admins about new application
        notifyAdminsAboutApplication(app, false);

        return mapToResponse(app);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherApplicationResponse getMyApplication(UUID userId) {
        return applicationRepository.findTopByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public TeacherApplicationResponse updateMyApplication(UUID userId, TeacherApplicationRequest request) {
        TeacherApplication app = applicationRepository.findTopByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ của bạn"));

        if (app.getStatus() == TeacherApplicationStatus.APPROVED) {
            throw new IllegalArgumentException("Hồ sơ của bạn đã được phê duyệt, không thể chỉnh sửa");
        }

        String docsJson = serializeDocUrls(request.getDocumentUrls());

        app.setApplicantType(request.getApplicantType());
        app.setFullName(request.getFullName().trim());
        app.setPhoneNumber(request.getPhoneNumber().trim());
        app.setEmail(request.getEmail().trim());
        app.setInstitutionName(request.getInstitutionName().trim());
        app.setMajorOrSubject(request.getMajorOrSubject().trim());
        app.setBio(request.getBio());
        app.setDocumentUrls(docsJson);
        app.setIdCardFrontUrl(request.getIdCardFrontUrl());
        app.setIdCardBackUrl(request.getIdCardBackUrl());
        app.setSampleVideoUrl(request.getSampleVideoUrl());
        app.setStatus(TeacherApplicationStatus.PENDING); // Reset to PENDING for admin re-evaluation
        app.setRejectReason(null);

        app = applicationRepository.save(app);
        log.info("User {} updated and re-submitted teacher application {}", userId, app.getId());

        // Notify all Admins about updated application
        notifyAdminsAboutApplication(app, true);

        return mapToResponse(app);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherApplicationResponse> getApplications(TeacherApplicationStatus status, TeacherApplicantType applicantType, String keyword, Pageable pageable) {
        return applicationRepository.searchApplications(status, applicantType, keyword, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherApplicationResponse getApplicationById(UUID id) {
        TeacherApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherApplication", id));
        return mapToResponse(app);
    }

    @Override
    @Transactional
    public TeacherApplicationResponse approveApplication(UUID adminId, UUID applicationId) {
        TeacherApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherApplication", applicationId));

        if (app.getStatus() == TeacherApplicationStatus.APPROVED) {
            throw new IllegalArgumentException("Hồ sơ này đã được phê duyệt trước đó");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));

        app.setStatus(TeacherApplicationStatus.APPROVED);
        app.setReviewedBy(admin);
        app.setReviewedAt(LocalDateTime.now());
        app.setRejectReason(null);
        app = applicationRepository.save(app);

        // Assign ROLE_TEACHER to student
        User applicant = app.getUser();
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .or(() -> roleRepository.findByName("TEACHER"))
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_TEACHER")
                        .description("Giáo viên / Giảng viên")
                        .build()));

        boolean alreadyHasRole = userRoleRepository.existsByUserIdAndRoleId(applicant.getId(), teacherRole.getId());
        if (!alreadyHasRole) {
            UserRole userRole = UserRole.builder()
                    .userId(applicant.getId())
                    .roleId(teacherRole.getId())
                    .user(applicant)
                    .role(teacherRole)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
            log.info("Assigned ROLE_TEACHER to user {}", applicant.getId());
        }

        applicant.setIsOnboarded(true);
        userRepository.save(applicant);

        // Send Kafka Notification to applicant
        notificationProducer.sendNotification(
                applicant.getId(),
                "TEACHER_APPLICATION_APPROVED",
                "Hồ sơ Giảng dạy đã được phê duyệt! 🎉",
                "Chúc mừng bạn! Hồ sơ đăng ký giảng dạy của bạn đã được Ban Quản Trị phê duyệt. Bây giờ bạn có thể tạo khóa học, bài giảng và đề thi trên NQD LMS.",
                "/teacher/courses"
        );

        log.info("Admin {} approved teacher application {}", adminId, applicationId);
        return mapToResponse(app);
    }

    @Override
    @Transactional
    public TeacherApplicationResponse rejectApplication(UUID adminId, UUID applicationId, RejectTeacherApplicationRequest request) {
        TeacherApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherApplication", applicationId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason().trim()
                : "Hồ sơ không đáp ứng yêu cầu thẩm định.";

        app.setStatus(TeacherApplicationStatus.REJECTED);
        app.setReviewedBy(admin);
        app.setReviewedAt(LocalDateTime.now());
        app.setRejectReason(reason);
        app = applicationRepository.save(app);

        // Send Kafka Notification to applicant
        notificationProducer.sendNotification(
                app.getUser().getId(),
                "TEACHER_APPLICATION_REJECTED",
                "Hồ sơ Giảng dạy cần bổ sung thông tin",
                "Hồ sơ của bạn chưa được duyệt với lý do: " + reason + ". Vui lòng bấm vào đây để cập nhật và nộp lại hồ sơ.",
                "/become-teacher"
        );

        log.info("Admin {} rejected teacher application {}: {}", adminId, applicationId, reason);
        return mapToResponse(app);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingApplications() {
        return applicationRepository.countByStatusAndIsDeletedFalse(TeacherApplicationStatus.PENDING);
    }

    private void notifyAdminsAboutApplication(TeacherApplication app, boolean isUpdate) {
        try {
            Set<UUID> notifiedAdminIds = new HashSet<>();
            List<User> admins = new ArrayList<>();

            List<User> roleAdmins = userRoleRepository.findUsersByRoleName("ROLE_ADMIN");
            if (roleAdmins != null) admins.addAll(roleAdmins);

            List<User> directAdmins = userRoleRepository.findUsersByRoleName("ADMIN");
            if (directAdmins != null) admins.addAll(directAdmins);

            String title = isUpdate
                    ? "Hồ sơ Giảng viên cập nhật: " + app.getFullName()
                    : "Hồ sơ đăng ký Giáo viên mới: " + app.getFullName();

            String applicantTypeName = switch (app.getApplicantType()) {
                case STUDENT_TUTOR -> "Sinh viên làm thêm / Gia sư";
                case CERTIFIED_TEACHER -> "Giáo viên có bằng cấp";
                case INDUSTRY_EXPERT -> "Chuyên gia kỹ năng thực tế";
            };

            String body = isUpdate
                    ? app.getFullName() + " vừa cập nhật và nộp lại hồ sơ xét duyệt giảng viên (" + applicantTypeName + "). Vui lòng thẩm định lại."
                    : app.getFullName() + " vừa nộp hồ sơ xét duyệt giảng viên (" + applicantTypeName + "). Vui lòng kiểm tra và thẩm định.";

            for (User admin : admins) {
                if (admin != null && admin.getId() != null && notifiedAdminIds.add(admin.getId())) {
                    notificationProducer.sendNotification(
                            admin.getId(),
                            isUpdate ? "TEACHER_APPLICATION_UPDATED" : "TEACHER_APPLICATION_SUBMITTED",
                            title,
                            body,
                            "/admin/teacher-verifications"
                    );
                }
            }
            log.info("Sent teacher application notifications to {} admins for application {}", notifiedAdminIds.size(), app.getId());
        } catch (Exception e) {
            log.error("Failed to notify admins about teacher application {}: {}", app.getId(), e.getMessage(), e);
        }
    }

    private String serializeDocUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (Exception e) {
            log.error("Failed to serialize document URLs: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> deserializeDocUrls(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize document URLs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private TeacherApplicationResponse mapToResponse(TeacherApplication app) {
        return TeacherApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUser().getId())
                .applicantType(app.getApplicantType())
                .fullName(app.getFullName())
                .phoneNumber(app.getPhoneNumber())
                .email(app.getEmail())
                .institutionName(app.getInstitutionName())
                .majorOrSubject(app.getMajorOrSubject())
                .bio(app.getBio())
                .documentUrls(deserializeDocUrls(app.getDocumentUrls()))
                .idCardFrontUrl(app.getIdCardFrontUrl())
                .idCardBackUrl(app.getIdCardBackUrl())
                .sampleVideoUrl(app.getSampleVideoUrl())
                .status(app.getStatus())
                .rejectReason(app.getRejectReason())
                .reviewedById(app.getReviewedBy() != null ? app.getReviewedBy().getId() : null)
                .reviewedByName(app.getReviewedBy() != null ? app.getReviewedBy().getFullName() : null)
                .reviewedAt(app.getReviewedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
