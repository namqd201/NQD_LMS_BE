package com.nqd.nqd_lms_be.service.email;

import java.util.UUID;

public interface EmailService {

    void sendClassroomInvitationEmail(
            String recipientEmail,
            String teacherName,
            String teacherEmail,
            String classroomName,
            String classroomCode,
            UUID classroomId,
            String requestMessage
    );

    void sendHtmlEmail(String to, String subject, String htmlContent);
}
