package com.nqd.nqd_lms_be.service.classroom;

import com.nqd.nqd_lms_be.dto.classroom.ClassroomStudentResponse;
import com.nqd.nqd_lms_be.entity.Classroom;
import com.nqd.nqd_lms_be.entity.User;

import java.util.List;
import java.util.UUID;

public interface ClassroomInvitationService {

    ClassroomStudentResponse inviteByEmail(Classroom classroom, String email, String message, User teacher);

    int processPendingInvitationsForUser(User user);

    List<ClassroomStudentResponse> getPendingInvitationsAsStudentResponses(UUID classroomId);

    void cancelInvitation(UUID classroomId, String email, UUID teacherId);
}
