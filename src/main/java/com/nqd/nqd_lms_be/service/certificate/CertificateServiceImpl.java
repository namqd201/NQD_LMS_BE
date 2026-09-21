package com.nqd.nqd_lms_be.service.certificate;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.certificate.CertificateEligibilityResponse;
import com.nqd.nqd_lms_be.dto.certificate.CertificateResponse;
import com.nqd.nqd_lms_be.dto.certificate.CertificateVerificationResponse;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonResourceRepository lessonResourceRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public CertificateEligibilityResponse checkEligibility(UUID studentId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        // 1. Check if already issued
        Optional<Certificate> existingCert = certificateRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existingCert.isPresent()) {
            Certificate cert = existingCert.get();
            return CertificateEligibilityResponse.builder()
                    .eligible(true)
                    .alreadyIssued(true)
                    .certificateCode(cert.getCertificateCode())
                    .certificateId(cert.getId())
                    .lessonsCompleted(true)
                    .videosCompleted(true)
                    .exercisesCompleted(true)
                    .examsCompleted(true)
                    .message("Chứng chỉ đã được cấp thành công.")
                    .build();
        }

        List<String> missing = new ArrayList<>();

        // 2. Condition 1 & 2: Lessons and Videos
        List<Lesson> publishedLessons = lessonRepository.findPublishedLessonsByCourseId(courseId);
        int totalLessons = publishedLessons.size();
        List<LessonProgress> progresses = lessonProgressRepository.findByStudentIdAndCourseId(studentId, courseId);
        Map<UUID, LessonProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p, (p1, p2) -> p1));

        int completedLessons = 0;
        int totalVideos = 0;
        int watchedVideos = 0;

        for (Lesson l : publishedLessons) {
            LessonProgress lp = progressMap.get(l.getId());
            boolean isCompleted = lp != null && (lp.getStatus() == LessonProgressStatus.COMPLETED ||
                    (lp.getProgressPercent() != null && lp.getProgressPercent().compareTo(BigDecimal.valueOf(100)) >= 0));
            if (isCompleted) {
                completedLessons++;
            }

            boolean hasVideo = (l.getVideoUrl() != null && !l.getVideoUrl().trim().isEmpty());
            if (!hasVideo) {
                List<LessonResource> resList = lessonResourceRepository.findByLessonIdOrderByDisplayOrderAsc(l.getId());
                hasVideo = resList.stream().anyMatch(r -> r.getResourceType() == ResourceType.VIDEO);
            }

            if (hasVideo) {
                totalVideos++;
                if (lp != null && Boolean.TRUE.equals(lp.getVideoWatched())) {
                    watchedVideos++;
                }
            }
        }

        boolean lessonsCompleted = totalLessons == 0 || completedLessons >= totalLessons;
        if (!lessonsCompleted) {
            missing.add("Chưa hoàn thành tất cả bài học (" + completedLessons + "/" + totalLessons + " bài)");
        }

        boolean videosCompleted = totalVideos == 0 || watchedVideos >= totalVideos;
        if (!videosCompleted) {
            missing.add("Chưa xem hết video bài giảng (" + watchedVideos + "/" + totalVideos + " video)");
        }

        // 3. Condition 3: Exercises
        List<Exercise> allExercises = new ArrayList<>();
        for (Lesson l : publishedLessons) {
            allExercises.addAll(exerciseRepository.findByLessonIdAndStatusAndIsDeletedFalse(l.getId(), ExerciseStatus.PUBLISHED));
        }
        int totalExercises = allExercises.size();
        int passedExercises = 0;
        List<ExerciseAttempt> studentAttempts = exerciseAttemptRepository.findByStudentIdOrderByStartedAtDesc(studentId);
        for (Exercise ex : allExercises) {
            boolean passed = studentAttempts.stream()
                    .anyMatch(att -> att.getExercise().getId().equals(ex.getId()) &&
                            att.getStatus() == ExerciseAttemptStatus.COMPLETED &&
                            Boolean.TRUE.equals(att.getPassed()));
            if (passed) {
                passedExercises++;
            }
        }
        boolean exercisesCompleted = totalExercises == 0 || passedExercises >= totalExercises;
        if (!exercisesCompleted) {
            missing.add("Chưa làm hoặc chưa đạt 100% tất cả bài tập (" + passedExercises + "/" + totalExercises + " bài tập)");
        }

        // 4. Condition 4: Exams
        List<Exam> courseExams = examRepository.findByCourseIdAndIsDeletedFalse(courseId).stream()
                .filter(e -> e.getStatus() == ExamStatus.PUBLISHED)
                .collect(Collectors.toList());
        int totalExams = courseExams.size();
        int passedExams = 0;
        for (Exam exam : courseExams) {
            List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStudentIdOrderByAttemptNumberAsc(exam.getId(), studentId);
            boolean passed = attempts.stream()
                    .anyMatch(att -> att.getStatus() == ExamAttemptStatus.SUBMITTED &&
                            (Boolean.TRUE.equals(att.getPassed()) ||
                                    (att.getTotalScore() != null && exam.getPassingMarks() != null &&
                                            att.getTotalScore().compareTo(exam.getPassingMarks()) >= 0)));
            if (passed) {
                passedExams++;
            }
        }
        boolean examsCompleted = totalExams == 0 || passedExams >= totalExams;
        if (!examsCompleted) {
            missing.add("Chưa vượt qua tất cả bài kiểm tra (" + passedExams + "/" + totalExams + " bài thi)");
        }

        boolean eligible = lessonsCompleted && videosCompleted && exercisesCompleted && examsCompleted;
        String message = eligible
                ? "Bạn đã hoàn thành đầy đủ tất cả các điều kiện để nhận chứng chỉ!"
                : "Bạn chưa đủ điều kiện nhận chứng chỉ: " + String.join(", ", missing);

        return CertificateEligibilityResponse.builder()
                .eligible(eligible)
                .alreadyIssued(false)
                .lessonsCompleted(lessonsCompleted)
                .completedLessons(completedLessons)
                .totalLessons(totalLessons)
                .videosCompleted(videosCompleted)
                .watchedVideos(watchedVideos)
                .totalVideos(totalVideos)
                .exercisesCompleted(exercisesCompleted)
                .passedExercises(passedExercises)
                .totalExercises(totalExercises)
                .examsCompleted(examsCompleted)
                .passedExams(passedExams)
                .totalExams(totalExams)
                .missingRequirements(missing)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public Optional<CertificateResponse> checkAndAutoIssueCertificate(UUID studentId, UUID courseId) {
        try {
            if (certificateRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                return Optional.empty();
            }
            CertificateEligibilityResponse eligibility = checkEligibility(studentId, courseId);
            if (eligibility.isEligible()) {
                log.info("Student {} meets all 4 requirements for course {}. Auto-issuing certificate.", studentId, courseId);
                return Optional.of(issueCertificate(studentId, courseId));
            }
        } catch (Exception ex) {
            log.warn("Auto certificate check failed for student {} in course {}: {}", studentId, courseId, ex.getMessage());
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public CertificateResponse issueCertificate(UUID studentId, UUID courseId) {
        log.info("Requesting certificate issuance for student {} in course {}", studentId, courseId);

        // 1. Check idempotency: If already issued, return existing certificate
        Optional<Certificate> existingCert = certificateRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existingCert.isPresent()) {
            log.info("Certificate already exists for student {} and course {}: code={}",
                    studentId, courseId, existingCert.get().getCertificateCode());
            return mapToResponse(existingCert.get());
        }

        // 2. Validate all 4 eligibility conditions
        CertificateEligibilityResponse eligibility = checkEligibility(studentId, courseId);
        if (!eligibility.isEligible()) {
            throw new ForbiddenOperationException("Bạn chưa đủ điều kiện để nhận chứng chỉ: " +
                    String.join("; ", eligibility.getMissingRequirements()));
        }

        // 2. Validate course, student & enrollment
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        CourseEnrollment enrollment = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseGet(() -> {
                    log.info("Auto-creating completed enrollment for student {} in course {}", studentId, courseId);
                    CourseEnrollment newEnrollment = CourseEnrollment.builder()
                            .course(course)
                            .student(student)
                            .status(EnrollmentStatus.COMPLETED)
                            .enrolledAt(LocalDateTime.now())
                            .completedAt(LocalDateTime.now())
                            .build();
                    return courseEnrollmentRepository.save(newEnrollment);
                });

        // Mark enrollment as completed if not already
        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
            courseEnrollmentRepository.save(enrollment);
        }

        // 3. Generate unique certificate code: e.g. CERT-NQD-XXXXX
        String code = generateUniqueCertificateCode();

        Certificate certificate = Certificate.builder()
                .certificateCode(code)
                .enrollment(enrollment)
                .course(course)
                .student(student)
                .issuedAt(LocalDateTime.now())
                .isRevoked(false)
                .build();

        certificate = certificateRepository.save(certificate);
        log.info("Successfully issued certificate {} for student {} in course {}", code, studentId, courseId);

        // 4. Send Kafka notification to student
        String verifyUrl = frontendUrl + "/verify/" + code;
        try {
            kafkaNotificationProducer.sendNotification(
                    studentId,
                    "CERTIFICATE_ISSUED",
                    "Chúc mừng bạn đã nhận Chứng chỉ hoàn thành khóa học!",
                    "Bạn đã hoàn thành 100% khóa học '" + course.getName() + "'. Mã chứng chỉ của bạn là " + code + ".",
                    verifyUrl
            );
        } catch (Exception e) {
            log.warn("Failed to publish certificate notification: {}", e.getMessage());
        }

        return mapToResponse(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateResponse> getMyCertificates(UUID studentId) {
        List<Certificate> certs = certificateRepository.findByStudentIdOrderByIssuedAtDesc(studentId);
        return certs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateResponse getCertificateById(UUID certificateId, UUID currentUserId, boolean isPrivileged) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", certificateId));

        if (!isPrivileged && (currentUserId == null || !cert.getStudent().getId().equals(currentUserId))) {
            throw new ForbiddenOperationException("Bạn không có quyền xem chứng chỉ này.");
        }

        return mapToResponse(cert);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateVerificationResponse verifyCertificate(String certificateCode) {
        if (certificateCode == null || certificateCode.trim().isEmpty()) {
            return CertificateVerificationResponse.builder()
                    .isValid(false)
                    .build();
        }

        Optional<Certificate> certOpt = certificateRepository.findByCertificateCode(certificateCode.trim().toUpperCase());
        if (certOpt.isEmpty()) {
            return CertificateVerificationResponse.builder()
                    .isValid(false)
                    .certificateCode(certificateCode)
                    .build();
        }

        Certificate cert = certOpt.get();
        boolean isRevoked = Boolean.TRUE.equals(cert.getIsRevoked());
        boolean isExpired = cert.getExpiryDate() != null && cert.getExpiryDate().isBefore(LocalDateTime.now());
        boolean isValid = !isRevoked && !isExpired;

        return CertificateVerificationResponse.builder()
                .isValid(isValid)
                .certificateCode(cert.getCertificateCode())
                .studentName(cert.getStudent() != null ? cert.getStudent().getFullName() : "Học viên")
                .courseName(cert.getCourse() != null ? cert.getCourse().getName() : "Khóa học")
                .courseCode(cert.getCourse() != null ? cert.getCourse().getCode() : "")
                .subjectName(cert.getCourse() != null && cert.getCourse().getSubject() != null ? cert.getCourse().getSubject().getName() : "")
                .issuedAt(cert.getIssuedAt())
                .expiryDate(cert.getExpiryDate())
                .isRevoked(isRevoked)
                .revocationReason(cert.getRevocationReason())
                .issuerName("NQD Learning Management System")
                .verificationUrl(frontendUrl + "/verify/" + cert.getCertificateCode())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateCertificatePdf(UUID certificateId, UUID currentUserId, boolean isPrivileged) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", certificateId));

        // Security check: only certificate owner or admin/teacher can download
        if (!isPrivileged && (currentUserId == null || !cert.getStudent().getId().equals(currentUserId))) {
            throw new ForbiddenOperationException("Bạn không có quyền tải chứng chỉ này.");
        }

        String studentName = cert.getStudent() != null ? cert.getStudent().getFullName() : "Học viên";
        String courseName = cert.getCourse() != null ? cert.getCourse().getName() : "Khóa học";
        String courseCode = cert.getCourse() != null ? cert.getCourse().getCode() : "";
        String certCode = cert.getCertificateCode();
        String issueDateStr = cert.getIssuedAt() != null
                ? cert.getIssuedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String verifyUrl = frontendUrl + "/verify/" + certCode;

        try {
            // Landscape A4 (842 x 595 pt)
            Rectangle pageSize = PageSize.A4.rotate();
            Document document = new Document(pageSize, 36, 36, 36, 36);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Background canvas for ornamental frames
            PdfContentByte cb = writer.getDirectContent();

            // Outer dark blue / gold borders
            cb.setColorStroke(new Color(15, 23, 42)); // Slate 900
            cb.setLineWidth(4f);
            cb.rectangle(20, 20, pageSize.getWidth() - 40, pageSize.getHeight() - 40);
            cb.stroke();

            cb.setColorStroke(new Color(217, 119, 6)); // Amber / Gold border
            cb.setLineWidth(1.5f);
            cb.rectangle(26, 26, pageSize.getWidth() - 52, pageSize.getHeight() - 52);
            cb.stroke();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, Font.BOLD, new Color(15, 23, 42));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.ITALIC, new Color(100, 116, 139));
            Font presentedToFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, new Color(71, 85, 105));
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Font.BOLD, new Color(5, 150, 105)); // Emerald 600
            Font reasonFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, new Color(51, 65, 85));
            Font courseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, new Color(30, 41, 59));
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, new Color(100, 116, 139));
            Font metaBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, new Color(30, 41, 59));

            // Header Section
            Paragraph orgName = new Paragraph("NQD LEARNING MANAGEMENT SYSTEM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, new Color(16, 185, 129)));
            orgName.setAlignment(Element.ALIGN_CENTER);
            document.add(orgName);

            Paragraph title = new Paragraph("CERTIFICATE OF COMPLETION", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            document.add(title);

            Paragraph vnSubtitle = new Paragraph("CHỨNG NHẬN HOÀN THÀNH KHÓA HỌC", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Font.BOLD, new Color(217, 119, 6)));
            vnSubtitle.setAlignment(Element.ALIGN_CENTER);
            vnSubtitle.setSpacingBefore(2);
            document.add(vnSubtitle);

            Paragraph certSubtitle = new Paragraph("This is to certify that", presentedToFont);
            certSubtitle.setAlignment(Element.ALIGN_CENTER);
            certSubtitle.setSpacingBefore(16);
            document.add(certSubtitle);

            // Student Name
            Paragraph studentPara = new Paragraph(studentName.toUpperCase(), nameFont);
            studentPara.setAlignment(Element.ALIGN_CENTER);
            studentPara.setSpacingBefore(8);
            document.add(studentPara);

            // Reason paragraph
            Paragraph reasonPara = new Paragraph("has successfully completed the online course with distinction", reasonFont);
            reasonPara.setAlignment(Element.ALIGN_CENTER);
            reasonPara.setSpacingBefore(10);
            document.add(reasonPara);

            // Course Title
            Paragraph coursePara = new Paragraph(courseName, courseFont);
            coursePara.setAlignment(Element.ALIGN_CENTER);
            coursePara.setSpacingBefore(8);
            document.add(coursePara);

            // Footer Table (Metadata, QR Code, Signature)
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(90);
            footerTable.setSpacingBefore(30);
            footerTable.setWidths(new float[]{35f, 30f, 35f});

            // Column 1: Date & Certificate ID
            PdfPCell cell1 = new PdfPCell();
            cell1.setBorder(Rectangle.NO_BORDER);
            Paragraph dateP = new Paragraph("Ngày cấp / Issue Date:\n" + issueDateStr, metaFont);
            Paragraph codeP = new Paragraph("Mã chứng chỉ / Code:\n" + certCode, metaBold);
            codeP.setSpacingBefore(5);
            cell1.addElement(dateP);
            cell1.addElement(codeP);
            footerTable.addCell(cell1);

            // Column 2: QR Code pointing to verification URL
            PdfPCell cell2 = new PdfPCell();
            cell2.setBorder(Rectangle.NO_BORDER);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            try {
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(verifyUrl, BarcodeFormat.QR_CODE, 100, 100);
                ByteArrayOutputStream qrStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrStream);
                Image qrImage = Image.getInstance(qrStream.toByteArray());
                qrImage.scaleToFit(70, 70);
                qrImage.setAlignment(Image.ALIGN_CENTER);
                cell2.addElement(qrImage);

                Paragraph scanText = new Paragraph("Quét để xác thực", FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(148, 163, 184)));
                scanText.setAlignment(Element.ALIGN_CENTER);
                cell2.addElement(scanText);
            } catch (Exception ex) {
                log.warn("Could not generate QR code for certificate: {}", ex.getMessage());
            }
            footerTable.addCell(cell2);

            // Column 3: Authorized Signature & Seal
            PdfPCell cell3 = new PdfPCell();
            cell3.setBorder(Rectangle.NO_BORDER);
            cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph sigTitle = new Paragraph("HỘI ĐỒNG ĐÀO TẠO NQD-LMS", metaBold);
            sigTitle.setAlignment(Element.ALIGN_RIGHT);
            Paragraph sigNote = new Paragraph("Chữ ký điện tử đã được xác thực\nDigitally Certified", metaFont);
            sigNote.setAlignment(Element.ALIGN_RIGHT);
            sigNote.setSpacingBefore(8);
            cell3.addElement(sigTitle);
            cell3.addElement(sigNote);
            footerTable.addCell(cell3);

            document.add(footerTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for certificate {}: {}", certificateId, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo tệp PDF chứng chỉ: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CertificateResponse revokeCertificate(UUID certificateId, String reason) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", certificateId));

        cert.setIsRevoked(true);
        cert.setRevocationReason(reason);
        cert = certificateRepository.save(cert);
        log.info("Certificate {} revoked with reason: {}", cert.getCertificateCode(), reason);
        return mapToResponse(cert);
    }

    private String generateUniqueCertificateCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("CERT-");
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (certificateRepository.findByCertificateCode(code).isEmpty()) {
                return code;
            }
        }
        return "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CertificateResponse mapToResponse(Certificate c) {
        Course course = c.getCourse();
        User student = c.getStudent();
        String code = c.getCertificateCode();

        return CertificateResponse.builder()
                .id(c.getId())
                .certificateCode(code)
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .courseCode(course != null ? course.getCode() : null)
                .courseThumbnailUrl(course != null ? course.getThumbnailUrl() : null)
                .subjectName(course != null && course.getSubject() != null ? course.getSubject().getName() : null)
                .gradeLevel(course != null ? course.getGradeLevel() : null)
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getFullName() : null)
                .studentEmail(student != null ? student.getEmail() : null)
                .studentAvatarUrl(student != null ? student.getAvatarUrl() : null)
                .issuedAt(c.getIssuedAt())
                .expiryDate(c.getExpiryDate())
                .isRevoked(Boolean.TRUE.equals(c.getIsRevoked()))
                .revocationReason(c.getRevocationReason())
                .finalGrade(c.getFinalGrade())
                .verificationUrl(frontendUrl + "/verify/" + code)
                .downloadUrl("/api/v1/certificates/" + c.getId() + "/download")
                .build();
    }
}
