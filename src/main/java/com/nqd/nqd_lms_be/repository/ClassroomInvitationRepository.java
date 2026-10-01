package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ClassroomInvitation;
import com.nqd.nqd_lms_be.entity.enums.ClassInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomInvitationRepository extends JpaRepository<ClassroomInvitation, UUID> {

    List<ClassroomInvitation> findByInvitedEmailIgnoreCaseAndStatus(String invitedEmail, ClassInvitationStatus status);

    Optional<ClassroomInvitation> findByClassroomIdAndInvitedEmailIgnoreCase(UUID classroomId, String invitedEmail);

    List<ClassroomInvitation> findByClassroomIdAndStatus(UUID classroomId, ClassInvitationStatus status);

    void deleteByClassroomIdAndInvitedEmailIgnoreCase(UUID classroomId, String invitedEmail);
}
