package com.nqd.nqd_lms_be.service.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend.url:https://nqdlms.online}")
    private String frontendUrl;

    @Override
    @Async
    public void sendClassroomInvitationEmail(
            String recipientEmail,
            String teacherName,
            String teacherEmail,
            String classroomName,
            String classroomCode,
            UUID classroomId,
            String requestMessage
    ) {
        String cleanRecipient = recipientEmail != null ? recipientEmail.trim() : "";
        if (cleanRecipient.isBlank()) {
            log.warn("Cannot send invitation email: recipient email is blank");
            return;
        }

        String safeTeacher = teacherName != null && !teacherName.isBlank() ? teacherName : "Giáo viên phụ trách";
        String safeTeacherEmail = teacherEmail != null ? teacherEmail : "";
        String subject = "[NQD-LMS] " + safeTeacher + " đã mời bạn tham gia lớp học \"" + classroomName + "\"";

        String joinUrl = String.format("%s/login?redirect=/classrooms/%s&invitedEmail=%s",
                frontendUrl.replaceAll("/$", ""),
                classroomId.toString(),
                cleanRecipient);

        String messageHtml = "";
        if (requestMessage != null && !requestMessage.isBlank()) {
            messageHtml = """
                <div style="background-color: #f8fafc; border-left: 4px solid #3b82f6; padding: 12px 16px; margin: 18px 0; border-radius: 6px; font-style: italic; color: #334155; font-size: 14px;">
                    <strong>Lời nhắn từ giáo viên:</strong> &ldquo;%s&rdquo;
                </div>
            """.formatted(escapeHtml(requestMessage));
        }

        String htmlContent = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; line-height: 1.6;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f1f5f9; padding: 30px 10px;">
                    <tr>
                        <td align="center">
                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08); border: 1px solid #e2e8f0;">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #1e293b 0%%, #0f172a 100%%); padding: 32px 30px; text-align: center;">
                                        <div style="font-size: 26px; font-weight: 900; letter-spacing: -0.5px; color: #ffffff;">
                                            <span style="color: #60a5fa;">[</span>
                                            <span style="color: #ffffff;">NQD</span><span style="color: #83c75d;">-LMS</span>
                                            <span style="color: #a78bfa;">]</span>
                                        </div>
                                        <p style="margin: 6px 0 0 0; color: #94a3b8; font-size: 13px; font-weight: 500;">
                                            Tự tin học hỏi &bull; Vững bước tương lai
                                        </p>
                                    </td>
                                </tr>

                                <!-- Body -->
                                <tr>
                                    <td style="padding: 36px 32px 28px 32px;">
                                        <h2 style="margin: 0 0 16px 0; color: #0f172a; font-size: 20px; font-weight: 800;">
                                            Xin chào bạn! 👋
                                        </h2>
                                        <p style="margin: 0 0 16px 0; font-size: 15px; color: #334155;">
                                            Thầy/Cô <strong>%s</strong> (%s) đã gửi lời mời bạn tham gia vào lớp học trực tuyến tại hệ thống <strong>NQD-LMS</strong>:
                                        </p>

                                        <!-- Class Card -->
                                        <div style="background-color: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 14px; padding: 20px; margin: 20px 0; text-align: left;">
                                            <div style="font-size: 11px; text-transform: uppercase; font-weight: 800; color: #15803d; letter-spacing: 0.5px; margin-bottom: 4px;">
                                                LỚP HỌC TRỰC TUYẾN
                                            </div>
                                            <div style="font-size: 19px; font-weight: 800; color: #14532d; margin-bottom: 6px;">
                                                %s
                                            </div>
                                            <div style="font-size: 13px; color: #166534; font-family: monospace;">
                                                Mã tham gia lớp: <strong>%s</strong>
                                            </div>
                                        </div>

                                        %s

                                        <!-- Call to Action Button -->
                                        <div style="text-align: center; margin: 32px 0;">
                                            <a href="%s" target="_blank" style="background-color: #83C75D; color: #ffffff; padding: 15px 36px; text-decoration: none; font-weight: 800; font-size: 15px; border-radius: 14px; display: inline-block; box-shadow: 0 4px 14px rgba(131, 199, 93, 0.4); text-transform: uppercase; letter-spacing: 0.3px;">
                                                Tham Gia Lớp Học Ngay &rarr;
                                            </a>
                                        </div>

                                        <!-- Guide Steps -->
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 14px; padding: 18px 20px; margin-top: 24px; font-size: 13px; color: #475569;">
                                            <div style="font-weight: 700; color: #0f172a; margin-bottom: 8px;">
                                                💡 Hướng dẫn tham gia cực nhanh:
                                            </div>
                                            <ol style="margin: 0; padding-left: 20px; line-height: 1.7;">
                                                <li>Bấm nút <strong>"Tham Gia Lớp Học Ngay"</strong> ở trên.</li>
                                                <li>Đăng nhập bằng tài khoản Google (Gmail: <strong>%s</strong>).</li>
                                                <li>Chọn vai trò <strong>Học sinh / Sinh viên</strong> tại bước khởi tạo.</li>
                                                <li>Hệ thống sẽ <strong>tự động thêm bạn vào lớp</strong> ngay lập tức!</li>
                                            </ol>
                                        </div>

                                        <p style="margin: 24px 0 0 0; font-size: 13px; color: #64748b; line-height: 1.5;">
                                            Nếu nút bấm trên không hoạt động, bạn có thể sao chép và dán liên kết sau vào trình duyệt:<br>
                                            <a href="%s" style="color: #2563eb; word-break: break-all; font-size: 12px;">%s</a>
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 30px; text-align: center; font-size: 12px; color: #94a3b8;">
                                        <p style="margin: 0 0 4px 0;">
                                            Email này được gửi tự động từ hệ thống quản lý học tập <strong>NQD-LMS</strong>.
                                        </p>
                                        <p style="margin: 0;">
                                            &copy; 2026 NQD-LMS Platform. Mọi quyền được bảo lưu.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.formatted(
                escapeHtml(subject),
                escapeHtml(safeTeacher),
                escapeHtml(safeTeacherEmail),
                escapeHtml(classroomName),
                escapeHtml(classroomCode),
                messageHtml,
                joinUrl,
                escapeHtml(cleanRecipient),
                joinUrl,
                joinUrl
        );

        sendHtmlEmail(cleanRecipient, subject, htmlContent);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("Cannot send email to {}: spring.mail.username is not configured", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(mailUsername, "NQD-LMS Platform", "UTF-8"));
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent email successfully to {} with subject '{}'", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
