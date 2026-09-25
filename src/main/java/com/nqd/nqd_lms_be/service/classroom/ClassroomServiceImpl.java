package com.nqd.nqd_lms_be.service.classroom;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.classroom.*;
import com.nqd.nqd_lms_be.entity.Classroom;
import com.nqd.nqd_lms_be.entity.ClassroomStudent;
import com.nqd.nqd_lms_be.entity.Subject;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.ClassroomStatus;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.repository.ClassroomRepository;
import com.nqd.nqd_lms_be.repository.ClassroomStudentRepository;
import com.nqd.nqd_lms_be.repository.SubjectRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateUniqueClassCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("CL");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (classroomRepository.existsByCode(code));
        return code;
    }

    @Override
    @Transactional
    public ClassroomResponse createClassroom(ClassroomRequest request, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        String code = request.getCode() != null && !request.getCode().trim().isEmpty()
                ? request.getCode().trim().toUpperCase()
                : generateUniqueClassCode();

        if (classroomRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Mã lớp học '" + code + "' đã tồn tại trên hệ thống. Vui lòng chọn mã khác.");
        }

        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId()).orElse(null);
        }

        Classroom classroom = Classroom.builder()
                .name(request.getName().trim())
                .code(code)
                .description(request.getDescription())
                .gradeLevel(request.getGradeLevel())
                .subject(subject)
                .teacher(teacher)
                .status(ClassroomStatus.ACTIVE)
                .studentCount(0)
                .coverImageUrl(request.getCoverImageUrl())
                .build();

        classroom = classroomRepository.save(classroom);
        log.info("Teacher {} created classroom: {} (Code: {})", teacher.getEmail(), classroom.getName(), classroom.getCode());
        return mapToClassroomResponse(classroom, teacherId);
    }

    @Override
    @Transactional
    public ClassroomResponse updateClassroom(UUID classroomId, ClassroomRequest request, UUID teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        if (!classroom.getTeacher().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("Chỉ giáo viên phụ trách lớp học mới có quyền chỉnh sửa thông tin lớp.");
        }

        classroom.setName(request.getName().trim());
        classroom.setDescription(request.getDescription());
        classroom.setGradeLevel(request.getGradeLevel());
        if (request.getCoverImageUrl() != null) {
            classroom.setCoverImageUrl(request.getCoverImageUrl());
        }

        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId()).orElse(null);
            classroom.setSubject(subject);
        } else {
            classroom.setSubject(null);
        }

        classroom = classroomRepository.save(classroom);
        log.info("Teacher {} updated classroom: {}", teacherId, classroom.getId());
        return mapToClassroomResponse(classroom, teacherId);
    }

    @Override
    @Transactional
    public void deleteClassroom(UUID classroomId, UUID teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        if (!classroom.getTeacher().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("Chỉ giáo viên phụ trách lớp học mới có quyền xóa/lưu trữ lớp học này.");
        }

        classroom.setStatus(ClassroomStatus.ARCHIVED);
        classroomRepository.save(classroom);
        log.info("Teacher {} archived classroom: {}", teacherId, classroomId);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomResponse getClassroomById(UUID classroomId, UUID currentUserId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        return mapToClassroomResponse(classroom, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> getTeachingClassrooms(UUID teacherId) {
        List<Classroom> classrooms = classroomRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(teacherId, ClassroomStatus.ACTIVE);
        return classrooms.stream()
                .map(c -> mapToClassroomResponse(c, teacherId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> getEnrolledClassrooms(UUID studentId) {
        List<Classroom> classrooms = classroomRepository.findEnrolledClassroomsByStudentId(studentId);
        return classrooms.stream()
                .map(c -> mapToClassroomResponse(c, studentId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomStudentResponse> getPendingInvitations(UUID studentId) {
        List<ClassroomStudent> invitations = classroomStudentRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(
                studentId, ClassEnrollmentStatus.INVITED
        );
        return invitations.stream()
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassroomStudentResponse requestToJoinByCode(JoinClassroomRequest request, UUID studentId) {
        String code = request.getCode().trim().toUpperCase();
        Classroom classroom = classroomRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học với mã: " + code));

        if (classroom.getStatus() != ClassroomStatus.ACTIVE) {
            throw new IllegalArgumentException("Lớp học này hiện không còn hoạt động.");
        }

        if (classroom.getTeacher().getId().equals(studentId)) {
            throw new IllegalArgumentException("Bạn là giáo viên phụ trách lớp học này, không thể tham gia với tư cách học sinh.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        Optional<ClassroomStudent> existingOpt = classroomStudentRepository.findByClassroomIdAndStudentId(classroom.getId(), studentId);

        ClassroomStudent cs;
        if (existingOpt.isPresent()) {
            cs = existingOpt.get();
            if (cs.getStatus() == ClassEnrollmentStatus.ENROLLED) {
                throw new IllegalArgumentException("Bạn đã là thành viên chính thức của lớp học '" + classroom.getName() + "'.");
            }
            if (cs.getStatus() == ClassEnrollmentStatus.PENDING_APPROVAL) {
                throw new IllegalArgumentException("Bạn đã gửi yêu cầu tham gia lớp học này rồi. Vui lòng chờ giáo viên phê duyệt.");
            }
            if (cs.getStatus() == ClassEnrollmentStatus.INVITED) {
                // If teacher already invited student, joining automatically accepts!
                cs.setStatus(ClassEnrollmentStatus.ENROLLED);
                cs.setJoinedAt(LocalDateTime.now());
                cs = classroomStudentRepository.save(cs);
                updateClassroomStudentCount(classroom.getId());
                return mapToStudentResponse(cs);
            }
            // If DROPPED or REJECTED or DECLINED -> Re-apply
            cs.setStatus(ClassEnrollmentStatus.PENDING_APPROVAL);
            cs.setRequestMessage(request.getMessage());
            cs = classroomStudentRepository.save(cs);
        } else {
            cs = ClassroomStudent.builder()
                    .classroom(classroom)
                    .student(student)
                    .status(ClassEnrollmentStatus.PENDING_APPROVAL)
                    .requestMessage(request.getMessage())
                    .build();
            cs = classroomStudentRepository.save(cs);
        }

        // Notify Teacher about student request
        String teacherTitle = "Yêu cầu xin vào lớp: " + classroom.getName();
        String teacherBody = "Học sinh " + student.getFullName() + " (" + student.getEmail() + ") đã gửi yêu cầu xin vào lớp học '" + classroom.getName() + "'. Vui lòng phê duyệt.";
        String teacherLink = "/classrooms/" + classroom.getId() + "?tab=requests";
        kafkaNotificationProducer.sendNotification(classroom.getTeacher().getId(), "CLASSROOM_JOIN_REQUEST", teacherTitle, teacherBody, teacherLink);

        log.info("Student {} requested to join classroom {} ({})", student.getEmail(), classroom.getName(), classroom.getCode());
        return mapToStudentResponse(cs);
    }

    @Override
    @Transactional
    public ClassroomStudentResponse approveStudentRequest(UUID classroomId, UUID studentId, UUID teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        if (!classroom.getTeacher().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("Chỉ giáo viên phụ trách lớp học mới có quyền phê duyệt học sinh.");
        }

        ClassroomStudent cs = classroomStudentRepository.findByClassroomIdAndStudentId(classroomId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu tham gia của học sinh không tồn tại"));

        if (cs.getStatus() != ClassEnrollmentStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Yêu cầu này không ở trạng thái chờ duyệt (trạng thái hiện tại: " + cs.getStatus() + ").");
        }

        cs.setStatus(ClassEnrollmentStatus.ENROLLED);
        cs.setJoinedAt(LocalDateTime.now());
        cs = classroomStudentRepository.save(cs);

        updateClassroomStudentCount(classroomId);

        // Notify Student about approval
        String studentTitle = "Yêu cầu vào lớp học được chấp thuận 🎉";
        String studentBody = "Chúc mừng! Giáo viên " + classroom.getTeacher().getFullName() + " đã phê duyệt cho bạn vào lớp '" + classroom.getName() + "'.";
        String studentLink = "/classrooms/" + classroom.getId();
        kafkaNotificationProducer.sendNotification(studentId, "CLASSROOM_JOIN_APPROVED", studentTitle, studentBody, studentLink);

        log.info("Teacher {} approved student {} into classroom {}", teacherId, studentId, classroomId);
        return mapToStudentResponse(cs);
    }

    @Override
    @Transactional
    public void rejectStudentRequest(UUID classroomId, UUID studentId, UUID teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        if (!classroom.getTeacher().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("Chỉ giáo viên phụ trách lớp học mới có quyền từ chối học sinh.");
        }

        ClassroomStudent cs = classroomStudentRepository.findByClassroomIdAndStudentId(classroomId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu của học sinh không tồn tại"));

        cs.setStatus(ClassEnrollmentStatus.REJECTED);
        classroomStudentRepository.save(cs);

        // Notify Student
        String studentTitle = "Yêu cầu vào lớp bị từ chối";
        String studentBody = "Rất tiếc, yêu cầu tham gia lớp học '" + classroom.getName() + "' của bạn đã bị từ chối.";
        String studentLink = "/classrooms";
        kafkaNotificationProducer.sendNotification(studentId, "CLASSROOM_JOIN_REJECTED", studentTitle, studentBody, studentLink);

        log.info("Teacher {} rejected student {} request for classroom {}", teacherId, studentId, classroomId);
    }

    @Override
    @Transactional
    public ClassroomStudentResponse inviteStudent(UUID classroomId, InviteStudentRequest request, UUID teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        if (!classroom.getTeacher().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("Chỉ giáo viên phụ trách lớp học mới có quyền mời học sinh vào lớp.");
        }

        String email = request.getEmail().trim().toLowerCase();
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email: " + email + ". Học sinh cần đăng ký tài khoản trước."));

        if (student.getId().equals(teacherId)) {
            throw new IllegalArgumentException("Bạn không thể tự mời chính mình vào lớp học.");
        }

        Optional<ClassroomStudent> existingOpt = classroomStudentRepository.findByClassroomIdAndStudentId(classroomId, student.getId());
        ClassroomStudent cs;
        if (existingOpt.isPresent()) {
            cs = existingOpt.get();
            if (cs.getStatus() == ClassEnrollmentStatus.ENROLLED) {
                throw new IllegalArgumentException("Học sinh '" + student.getFullName() + "' đã ở trong lớp học rồi.");
            }
            if (cs.getStatus() == ClassEnrollmentStatus.INVITED) {
                throw new IllegalArgumentException("Lời mời tham gia lớp học đã được gửi tới học sinh này trước đó.");
            }
            if (cs.getStatus() == ClassEnrollmentStatus.PENDING_APPROVAL) {
                // Direct approval if student had already requested
                cs.setStatus(ClassEnrollmentStatus.ENROLLED);
                cs.setJoinedAt(LocalDateTime.now());
                cs = classroomStudentRepository.save(cs);
                updateClassroomStudentCount(classroomId);
                return mapToStudentResponse(cs);
            }
            cs.setStatus(ClassEnrollmentStatus.INVITED);
            cs.setRequestMessage(request.getMessage());
            cs = classroomStudentRepository.save(cs);
        } else {
            cs = ClassroomStudent.builder()
                    .classroom(classroom)
                    .student(student)
                    .status(ClassEnrollmentStatus.INVITED)
                    .requestMessage(request.getMessage())
                    .build();
            cs = classroomStudentRepository.save(cs);
        }

        // Notify Student about invitation
        String studentTitle = "Lời mời tham gia lớp học: " + classroom.getName() + " 📚";
        String studentBody = "Giáo viên " + classroom.getTeacher().getFullName() + " đã gửi lời mời bạn tham gia lớp học '" + classroom.getName() + "'. Bấm vào để xác nhận.";
        String studentLink = "/classrooms?tab=invitations";
        kafkaNotificationProducer.sendNotification(student.getId(), "CLASSROOM_INVITATION", studentTitle, studentBody, studentLink);

        log.info("Teacher {} invited student {} into classroom {}", teacherId, student.getEmail(), classroom.getName());
        return mapToStudentResponse(cs);
    }

    @Override
    @Transactional
    public ClassroomStudentResponse acceptInvitation(UUID classroomId, UUID studentId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        ClassroomStudent cs = classroomStudentRepository.findByClassroomIdAndStudentId(classroomId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời tham gia lớp học này"));

        if (cs.getStatus() != ClassEnrollmentStatus.INVITED) {
            throw new IllegalArgumentException("Lời mời không ở trạng thái chờ chấp nhận (trạng thái hiện tại: " + cs.getStatus() + ").");
        }

        cs.setStatus(ClassEnrollmentStatus.ENROLLED);
        cs.setJoinedAt(LocalDateTime.now());
        cs = classroomStudentRepository.save(cs);

        updateClassroomStudentCount(classroomId);

        // Notify Teacher about student acceptance
        User student = cs.getStudent();
        String teacherTitle = "Học sinh đã chấp nhận lời mời vào lớp";
        String teacherBody = "Học sinh " + student.getFullName() + " (" + student.getEmail() + ") đã đồng ý tham gia lớp học '" + classroom.getName() + "'.";
        String teacherLink = "/classrooms/" + classroom.getId();
        kafkaNotificationProducer.sendNotification(classroom.getTeacher().getId(), "CLASSROOM_INVITATION_ACCEPTED", teacherTitle, teacherBody, teacherLink);

        log.info("Student {} accepted invitation to classroom {}", studentId, classroomId);
        return mapToStudentResponse(cs);
    }

    @Override
    @Transactional
    public void declineInvitation(UUID classroomId, UUID studentId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        ClassroomStudent cs = classroomStudentRepository.findByClassroomIdAndStudentId(classroomId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời tham gia lớp học"));

        cs.setStatus(ClassEnrollmentStatus.DECLINED);
        classroomStudentRepository.save(cs);

        // Notify Teacher
        User student = cs.getStudent();
        String teacherTitle = "Học sinh từ chối lời mời vào lớp";
        String teacherBody = "Học sinh " + student.getFullName() + " đã từ chối lời mời tham gia lớp học '" + classroom.getName() + "'.";
        String teacherLink = "/classrooms/" + classroom.getId();
        kafkaNotificationProducer.sendNotification(classroom.getTeacher().getId(), "CLASSROOM_INVITATION_DECLINED", teacherTitle, teacherBody, teacherLink);

        log.info("Student {} declined invitation to classroom {}", studentId, classroomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomStudentResponse> getClassroomStudents(UUID classroomId, ClassEnrollmentStatus status, UUID currentUserId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        boolean isTeacher = classroom.getTeacher().getId().equals(currentUserId);

        List<ClassroomStudent> students;
        if (status != null) {
            // Only teacher can view pending requests or invited students
            if (!isTeacher && status != ClassEnrollmentStatus.ENROLLED) {
                throw new ForbiddenOperationException("Bạn không có quyền xem danh sách này.");
            }
            students = classroomStudentRepository.findByClassroomIdAndStatusOrderByJoinedAtDesc(classroomId, status);
        } else {
            students = classroomStudentRepository.findByClassroomIdAndStatusOrderByJoinedAtDesc(classroomId, ClassEnrollmentStatus.ENROLLED);
        }

        return students.stream()
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeStudent(UUID classroomId, UUID studentId, UUID teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        if (!classroom.getTeacher().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("Chỉ giáo viên phụ trách lớp học mới có quyền xóa học sinh khỏi lớp.");
        }

        ClassroomStudent cs = classroomStudentRepository.findByClassroomIdAndStudentId(classroomId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Học sinh không ở trong lớp này"));

        cs.setStatus(ClassEnrollmentStatus.DROPPED);
        classroomStudentRepository.save(cs);

        updateClassroomStudentCount(classroomId);

        // Notify Student
        String studentTitle = "Bạn đã được đưa ra khỏi lớp học";
        String studentBody = "Giáo viên " + classroom.getTeacher().getFullName() + " đã đưa bạn ra khỏi lớp học '" + classroom.getName() + "'.";
        String studentLink = "/classrooms";
        kafkaNotificationProducer.sendNotification(studentId, "CLASSROOM_STUDENT_REMOVED", studentTitle, studentBody, studentLink);

        log.info("Teacher {} removed student {} from classroom {}", teacherId, studentId, classroomId);
    }

    @Override
    @Transactional
    public void leaveClassroom(UUID classroomId, UUID studentId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        ClassroomStudent cs = classroomStudentRepository.findByClassroomIdAndStudentId(classroomId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không tham gia lớp học này"));

        cs.setStatus(ClassEnrollmentStatus.DROPPED);
        classroomStudentRepository.save(cs);

        updateClassroomStudentCount(classroomId);

        // Notify Teacher
        User student = cs.getStudent();
        String teacherTitle = "Học sinh rời khỏi lớp học";
        String teacherBody = "Học sinh " + student.getFullName() + " (" + student.getEmail() + ") đã rời khỏi lớp học '" + classroom.getName() + "'.";
        String teacherLink = "/classrooms/" + classroom.getId();
        kafkaNotificationProducer.sendNotification(classroom.getTeacher().getId(), "CLASSROOM_STUDENT_LEFT", teacherTitle, teacherBody, teacherLink);

        log.info("Student {} left classroom {}", studentId, classroomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSuggestionResponse> searchUsersForInvitation(String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.searchUsers(query.trim(), UserStatus.ACTIVE);
        return users.stream()
                .limit(10)
                .map(u -> UserSuggestionResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .avatarUrl(u.getAvatarUrl())
                        .roles("STUDENT")
                        .build())
                .collect(Collectors.toList());
    }

    private void updateClassroomStudentCount(UUID classroomId) {
        long count = classroomStudentRepository.countByClassroomIdAndStatus(classroomId, ClassEnrollmentStatus.ENROLLED);
        classroomRepository.findById(classroomId).ifPresent(c -> {
            c.setStudentCount((int) count);
            classroomRepository.save(c);
        });
    }

    private ClassroomResponse mapToClassroomResponse(Classroom c, UUID currentUserId) {
        long pendingCount = 0;
        String currentUserRole = "NONE";
        ClassEnrollmentStatus currentUserStatus = null;

        if (c.getTeacher().getId().equals(currentUserId)) {
            currentUserRole = "TEACHER";
            pendingCount = classroomStudentRepository.countByClassroomIdAndStatus(c.getId(), ClassEnrollmentStatus.PENDING_APPROVAL);
        } else if (currentUserId != null) {
            Optional<ClassroomStudent> csOpt = classroomStudentRepository.findByClassroomIdAndStudentId(c.getId(), currentUserId);
            if (csOpt.isPresent()) {
                currentUserRole = "STUDENT";
                currentUserStatus = csOpt.get().getStatus();
            }
        }

        return ClassroomResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .code(c.getCode())
                .description(c.getDescription())
                .gradeLevel(c.getGradeLevel())
                .subjectId(c.getSubject() != null ? c.getSubject().getId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getName() : null)
                .teacherId(c.getTeacher().getId())
                .teacherName(c.getTeacher().getFullName())
                .teacherEmail(c.getTeacher().getEmail())
                .teacherAvatarUrl(c.getTeacher().getAvatarUrl())
                .studentCount(c.getStudentCount() != null ? c.getStudentCount() : 0)
                .pendingRequestCount(pendingCount)
                .status(c.getStatus())
                .currentUserRole(currentUserRole)
                .currentUserEnrollmentStatus(currentUserStatus)
                .coverImageUrl(c.getCoverImageUrl())
                .larkMeetingUrl(c.getLarkMeetingUrl())
                .meetingId(c.getMeetingId())
                .passcode(c.getPasscode())
                .meetingNote(c.getMeetingNote())
                .isLiveNow(Boolean.TRUE.equals(c.getIsLiveNow()))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private ClassroomStudentResponse mapToStudentResponse(ClassroomStudent cs) {
        return ClassroomStudentResponse.builder()
                .id(cs.getId())
                .classroomId(cs.getClassroom().getId())
                .classroomName(cs.getClassroom().getName())
                .classroomCode(cs.getClassroom().getCode())
                .studentId(cs.getStudent().getId())
                .studentName(cs.getStudent().getFullName())
                .studentEmail(cs.getStudent().getEmail())
                .studentAvatarUrl(cs.getStudent().getAvatarUrl())
                .status(cs.getStatus())
                .joinedAt(cs.getJoinedAt())
                .requestMessage(cs.getRequestMessage())
                .createdAt(cs.getCreatedAt())
                .build();
    }
}
