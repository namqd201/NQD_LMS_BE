package com.nqd.nqd_lms_be.service.classroom;

import com.nqd.nqd_lms_be.dto.classroom.ClassroomStudentResponse;
import com.nqd.nqd_lms_be.entity.Classroom;
import com.nqd.nqd_lms_be.entity.ClassroomInvitation;
import com.nqd.nqd_lms_be.entity.ClassroomStudent;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.ClassInvitationStatus;
import com.nqd.nqd_lms_be.repository.ClassroomInvitationRepository;
import com.nqd.nqd_lms_be.repository.ClassroomRepository;
import com.nqd.nqd_lms_be.repository.ClassroomStudentRepository;
import com.nqd.nqd_lms_be.service.email.EmailService;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomInvitationServiceImpl implements ClassroomInvitationService {

    private final ClassroomInvitationRepository invitationRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomRepository classroomRepository;
    private final EmailService emailService;
    private final KafkaNotificationProducer kafkaNotificationProducer;

    @Override
    @Transactional
    public ClassroomStudentResponse inviteByEmail(Classroom classroom, String email, String message, User teacher) {
        String cleanEmail = email.trim().toLowerCase();

        // Check if an invitation already exists for this classroom and email
        Optional<ClassroomInvitation> existingInvOpt = invitationRepository
                .findByClassroomIdAndInvitedEmailIgnoreCase(classroom.getId(), cleanEmail);

        ClassroomInvitation inv;
        if (existingInvOpt.isPresent()) {
            inv = existingInvOpt.get();
            if (inv.getStatus() == ClassInvitationStatus.ACCEPTED) {
                throw new IllegalArgumentException("Học sinh với email '" + cleanEmail + "' đã chấp nhận lời mời và ở trong lớp rồi.");
            }
            inv.setStatus(ClassInvitationStatus.PENDING);
            inv.setRequestMessage(message);
            inv.setExpiresAt(LocalDateTime.now().plusDays(30));
            inv = invitationRepository.save(inv);
            log.info("Re-activated pending invitation for email {} in classroom {}", cleanEmail, classroom.getName());
        } else {
            inv = ClassroomInvitation.builder()
                    .classroom(classroom)
                    .invitedEmail(cleanEmail)
                    .teacher(teacher)
                    .status(ClassInvitationStatus.PENDING)
                    .invitationCode(UUID.randomUUID().toString())
                    .requestMessage(message)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();
            inv = invitationRepository.save(inv);
            log.info("Created new classroom invitation for unregistered email {} in classroom {}", cleanEmail, classroom.getName());
        }

        // Send Email via Gmail
        try {
            emailService.sendClassroomInvitationEmail(
                    cleanEmail,
                    teacher.getFullName() != null ? teacher.getFullName() : teacher.getEmail(),
                    teacher.getEmail(),
                    classroom.getName(),
                    classroom.getCode(),
                    classroom.getId(),
                    message
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch invitation email to {}: {}", cleanEmail, e.getMessage());
        }

        return ClassroomStudentResponse.builder()
                .id(inv.getId())
                .classroomId(classroom.getId())
                .classroomName(classroom.getName())
                .classroomCode(classroom.getCode())
                .studentId(null)
                .studentName("Học sinh được mời (" + cleanEmail + ")")
                .studentEmail(cleanEmail)
                .studentAvatarUrl(null)
                .status(ClassEnrollmentStatus.INVITED)
                .requestMessage(message)
                .createdAt(inv.getCreatedAt() != null ? inv.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public int processPendingInvitationsForUser(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return 0;
        }

        String userEmail = user.getEmail().trim().toLowerCase();
        List<ClassroomInvitation> pendingList = invitationRepository
                .findByInvitedEmailIgnoreCaseAndStatus(userEmail, ClassInvitationStatus.PENDING);

        if (pendingList.isEmpty()) {
            return 0;
        }

        int enrolledCount = 0;
        for (ClassroomInvitation inv : pendingList) {
            try {
                Classroom classroom = inv.getClassroom();
                if (classroom == null) continue;

                // Check if already in classroom
                Optional<ClassroomStudent> existingCsOpt = classroomStudentRepository
                        .findByClassroomIdAndStudentId(classroom.getId(), user.getId());

                if (existingCsOpt.isPresent()) {
                    ClassroomStudent cs = existingCsOpt.get();
                    if (cs.getStatus() != ClassEnrollmentStatus.ENROLLED) {
                        cs.setStatus(ClassEnrollmentStatus.ENROLLED);
                        cs.setJoinedAt(LocalDateTime.now());
                        classroomStudentRepository.save(cs);
                        enrolledCount++;
                    }
                } else {
                    ClassroomStudent newCs = ClassroomStudent.builder()
                            .classroom(classroom)
                            .student(user)
                            .status(ClassEnrollmentStatus.ENROLLED)
                            .joinedAt(LocalDateTime.now())
                            .requestMessage(inv.getRequestMessage())
                            .build();
                    classroomStudentRepository.save(newCs);
                    enrolledCount++;
                }

                // Update classroom student count
                long count = classroomStudentRepository.countByClassroomIdAndStatus(classroom.getId(), ClassEnrollmentStatus.ENROLLED);
                classroom.setStudentCount((int) count);
                classroomRepository.save(classroom);

                // Update invitation status
                inv.setStatus(ClassInvitationStatus.ACCEPTED);
                inv.setAcceptedAt(LocalDateTime.now());
                invitationRepository.save(inv);

                // Send In-App notification to student
                String notifTitle = "Chào mừng bạn đến với lớp: " + classroom.getName() + " 🎉";
                String notifBody = "Bạn đã được tự động thêm vào lớp học theo lời mời của giáo viên " + inv.getTeacher().getFullName() + ".";
                String notifLink = "/classrooms/" + classroom.getId();
                kafkaNotificationProducer.sendNotification(user.getId(), "CLASSROOM_ENROLLED", notifTitle, notifBody, notifLink);

                log.info("Auto-enrolled newly registered student {} into classroom '{}'", userEmail, classroom.getName());
            } catch (Exception e) {
                log.error("Failed to auto-enroll user {} for invitation {}: {}", userEmail, inv.getId(), e.getMessage(), e);
            }
        }

        return enrolledCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomStudentResponse> getPendingInvitationsAsStudentResponses(UUID classroomId) {
        return invitationRepository.findByClassroomIdAndStatus(classroomId, ClassInvitationStatus.PENDING)
                .stream()
                .map(inv -> ClassroomStudentResponse.builder()
                        .id(inv.getId())
                        .classroomId(inv.getClassroom().getId())
                        .classroomName(inv.getClassroom().getName())
                        .classroomCode(inv.getClassroom().getCode())
                        .studentId(null)
                        .studentName(inv.getInvitedEmail())
                        .studentEmail(inv.getInvitedEmail())
                        .studentAvatarUrl(null)
                        .status(ClassEnrollmentStatus.INVITED)
                        .requestMessage(inv.getRequestMessage())
                        .createdAt(inv.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID classroomId, String email, UUID teacherId) {
        invitationRepository.findByClassroomIdAndInvitedEmailIgnoreCase(classroomId, email.trim().toLowerCase())
                .ifPresent(inv -> {
                    inv.setStatus(ClassInvitationStatus.CANCELLED);
                    invitationRepository.save(inv);
                    log.info("Teacher {} cancelled invitation for email {} in classroom {}", teacherId, email, classroomId);
                });
    }
}
