package com.nqd.nqd_lms_be.service.classroom;

import com.nqd.nqd_lms_be.dto.classroom.*;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;

import java.util.List;
import java.util.UUID;

public interface ClassroomService {

    ClassroomResponse createClassroom(ClassroomRequest request, UUID teacherId);

    ClassroomResponse updateClassroom(UUID classroomId, ClassroomRequest request, UUID teacherId);

    void deleteClassroom(UUID classroomId, UUID teacherId);

    ClassroomResponse getClassroomById(UUID classroomId, UUID currentUserId);

    List<ClassroomResponse> getTeachingClassrooms(UUID teacherId);

    List<ClassroomResponse> getEnrolledClassrooms(UUID studentId);

    List<ClassroomStudentResponse> getPendingInvitations(UUID studentId);

    ClassroomStudentResponse requestToJoinByCode(JoinClassroomRequest request, UUID studentId);

    ClassroomStudentResponse approveStudentRequest(UUID classroomId, UUID studentId, UUID teacherId);

    void rejectStudentRequest(UUID classroomId, UUID studentId, UUID teacherId);

    ClassroomStudentResponse inviteStudent(UUID classroomId, InviteStudentRequest request, UUID teacherId);

    ClassroomStudentResponse acceptInvitation(UUID classroomId, UUID studentId);

    void declineInvitation(UUID classroomId, UUID studentId);

    List<ClassroomStudentResponse> getClassroomStudents(UUID classroomId, ClassEnrollmentStatus status, UUID currentUserId);

    void removeStudent(UUID classroomId, UUID studentId, UUID teacherId);

    void leaveClassroom(UUID classroomId, UUID studentId);

    List<UserSuggestionResponse> searchUsersForInvitation(String query);
}
