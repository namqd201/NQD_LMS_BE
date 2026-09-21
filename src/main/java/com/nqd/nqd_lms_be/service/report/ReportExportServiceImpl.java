package com.nqd.nqd_lms_be.service.report;

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
import com.nqd.nqd_lms_be.dto.report.ExportCustomizationParams;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportServiceImpl implements ReportExportService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTeacherCourseGradebookExcel(UUID courseId, ExportCustomizationParams params, UUID teacherId) {
        log.info("Generating teacher course gradebook Excel for course: {}, teacher: {}", courseId, teacherId);
        if (params == null) params = new ExportCustomizationParams();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        // Check ownership or admin
        boolean isAdmin = isUserAdmin(teacherId);
        if (!isAdmin && (course.getCreator() == null || !course.getCreator().getId().equals(teacherId))) {
            throw new ForbiddenOperationException("You are not authorized to export gradebook for this course");
        }

        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByCourseIdWithStudent(courseId);
        List<Exam> exams = examRepository.findByCourseIdAndIsDeletedFalse(courseId);
        long totalLessons = lessonRepository.countByCourseId(courseId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Setup Styles
            DataFormat dataFormat = workbook.createDataFormat();

            // Font styles
            XSSFFont headerOrgFont = workbook.createFont();
            headerOrgFont.setFontName("Calibri");
            headerOrgFont.setFontHeightInPoints((short) 11);
            headerOrgFont.setBold(true);
            headerOrgFont.setColor(new XSSFColor(new Color(30, 41, 59), null));

            XSSFFont headerTitleFont = workbook.createFont();
            headerTitleFont.setFontName("Calibri");
            headerTitleFont.setFontHeightInPoints((short) 16);
            headerTitleFont.setBold(true);
            headerTitleFont.setColor(new XSSFColor(new Color(79, 70, 229), null)); // Indigo 600

            XSSFFont colHeaderFont = workbook.createFont();
            colHeaderFont.setFontName("Calibri");
            colHeaderFont.setFontHeightInPoints((short) 10);
            colHeaderFont.setBold(true);
            colHeaderFont.setColor(new XSSFColor(new Color(255, 255, 255), null));

            XSSFFont bodyFont = workbook.createFont();
            bodyFont.setFontName("Calibri");
            bodyFont.setFontHeightInPoints((short) 10);

            XSSFFont boldBodyFont = workbook.createFont();
            boldBodyFont.setFontName("Calibri");
            boldBodyFont.setFontHeightInPoints((short) 10);
            boldBodyFont.setBold(true);

            // Cell Styles
            XSSFCellStyle colHeaderStyle = workbook.createCellStyle();
            colHeaderStyle.setFont(colHeaderFont);
            colHeaderStyle.setFillForegroundColor(new XSSFColor(new Color(79, 70, 229), null));
            colHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            colHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            colHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            colHeaderStyle.setBorderTop(BorderStyle.THIN);
            colHeaderStyle.setBorderBottom(BorderStyle.THIN);
            colHeaderStyle.setBorderLeft(BorderStyle.THIN);
            colHeaderStyle.setBorderRight(BorderStyle.THIN);
            colHeaderStyle.setWrapText(true);

            XSSFCellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setFont(bodyFont);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setFont(bodyFont);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle dataStylePercent = workbook.createCellStyle();
            dataStylePercent.setFont(boldBodyFont);
            dataStylePercent.setAlignment(HorizontalAlignment.RIGHT);
            dataStylePercent.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStylePercent.setDataFormat(dataFormat.getFormat("0.0%"));
            dataStylePercent.setBorderTop(BorderStyle.THIN);
            dataStylePercent.setBorderBottom(BorderStyle.THIN);
            dataStylePercent.setBorderLeft(BorderStyle.THIN);
            dataStylePercent.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle dataStyleScore = workbook.createCellStyle();
            dataStyleScore.setFont(boldBodyFont);
            dataStyleScore.setAlignment(HorizontalAlignment.RIGHT);
            dataStyleScore.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyleScore.setDataFormat(dataFormat.getFormat("0.0"));
            dataStyleScore.setBorderTop(BorderStyle.THIN);
            dataStyleScore.setBorderBottom(BorderStyle.THIN);
            dataStyleScore.setBorderLeft(BorderStyle.THIN);
            dataStyleScore.setBorderRight(BorderStyle.THIN);

            // ==========================================
            // SHEET 1: BẢNG ĐIỂM CHI TIẾT
            // ==========================================
            Sheet sheet1 = workbook.createSheet("Bảng điểm chi tiết");
            sheet1.setDisplayGridlines(true);

            int rowNum = 0;

            // 1. Top Branding & Header
            Row r0 = sheet1.createRow(rowNum++);
            Cell c0 = r0.createCell(0);
            c0.setCellValue(params.getEffectiveInstitutionName().toUpperCase());
            XSSFCellStyle orgStyle = workbook.createCellStyle();
            orgStyle.setFont(headerOrgFont);
            c0.setCellStyle(orgStyle);

            Row r1 = sheet1.createRow(rowNum++);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Niên khóa / Năm học: " + params.getEffectiveAcademicYear());
            XSSFCellStyle yearStyle = workbook.createCellStyle();
            XSSFFont italicFont = workbook.createFont();
            italicFont.setItalic(true);
            italicFont.setFontHeightInPoints((short) 10);
            yearStyle.setFont(italicFont);
            c1.setCellStyle(yearStyle);

            rowNum++; // Blank row

            // 2. Report Main Title
            String defaultTitle = "BẢNG ĐIỂM TỔNG KẾT KHÓA HỌC: " + course.getName().toUpperCase();
            String titleText = params.getEffectiveReportTitle(defaultTitle);
            Row rTitle = sheet1.createRow(rowNum++);
            Cell cTitle = rTitle.createCell(0);
            cTitle.setCellValue(titleText);
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(headerTitleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            cTitle.setCellStyle(titleStyle);

            // Merge Title across 10 columns
            sheet1.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, Math.max(8, 7 + exams.size())));

            // Course Info Summary
            Row rInfo = sheet1.createRow(rowNum++);
            Cell cInfo = rInfo.createCell(0);
            String teacherName = (course.getCreator() != null) ? course.getCreator().getFullName() : "N/A";
            String subjectName = (course.getSubject() != null) ? course.getSubject().getName() : "N/A";
            cInfo.setCellValue(String.format("Môn học: %s | Giáo viên phụ trách: %s | Tổng số học viên: %d | Ngày xuất file: %s",
                    subjectName, teacherName, enrollments.size(), LocalDateTime.now().format(DATE_TIME_FORMATTER)));
            XSSFCellStyle infoStyle = workbook.createCellStyle();
            infoStyle.setAlignment(HorizontalAlignment.CENTER);
            infoStyle.setFont(italicFont);
            cInfo.setCellStyle(infoStyle);
            sheet1.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, Math.max(8, 7 + exams.size())));

            rowNum++; // Blank row

            // 3. Table Column Headers
            Row headerRow = sheet1.createRow(rowNum++);
            headerRow.setHeightInPoints(28);

            int colIdx = 0;
            String[] baseHeaders = {"STT", "Mã Học Sinh", "Họ và Tên", "Email", "Ngày tham gia", "Tiến độ", "Bài đã học", "Điểm TB (%)"};
            for (String h : baseHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(h);
                cell.setCellStyle(colHeaderStyle);
            }

            // Dynamic columns for each exam in the course
            for (Exam exam : exams) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(exam.getTitle());
                cell.setCellStyle(colHeaderStyle);
            }

            // Final status columns
            Cell cCert = headerRow.createCell(colIdx++);
            cCert.setCellValue("Chứng chỉ");
            cCert.setCellStyle(colHeaderStyle);

            Cell cRank = headerRow.createCell(colIdx++);
            cRank.setCellValue("Xếp loại");
            cRank.setCellStyle(colHeaderStyle);

            // 4. Data Rows
            int stt = 1;
            double sumAvgPercentage = 0;
            int completedCount = 0;

            for (CourseEnrollment enrollment : enrollments) {
                User student = enrollment.getStudent();
                Row row = sheet1.createRow(rowNum++);
                row.setHeightInPoints(20);

                int c = 0;
                // STT
                Cell cellStt = row.createCell(c++);
                cellStt.setCellValue(stt++);
                cellStt.setCellStyle(dataStyleCenter);

                // Mã HS
                Cell cellCode = row.createCell(c++);
                cellCode.setCellValue("HS-" + student.getId().toString().substring(0, 8).toUpperCase());
                cellCode.setCellStyle(dataStyleCenter);

                // Họ tên
                Cell cellName = row.createCell(c++);
                cellName.setCellValue(student.getFullName() != null ? student.getFullName() : "");
                cellName.setCellStyle(dataStyleLeft);

                // Email
                Cell cellEmail = row.createCell(c++);
                cellEmail.setCellValue(student.getEmail());
                cellEmail.setCellStyle(dataStyleLeft);

                // Ngày tham gia
                Cell cellJoined = row.createCell(c++);
                cellJoined.setCellValue(enrollment.getEnrolledAt() != null ? enrollment.getEnrolledAt().format(DATE_FORMATTER) : "");
                cellJoined.setCellStyle(dataStyleCenter);

                // Tiến độ bài học
                List<LessonProgress> progresses = lessonProgressRepository.findByStudentIdAndCourseId(student.getId(), courseId);
                long completedLessons = progresses.stream()
                        .filter(p -> p.getStatus() == LessonProgressStatus.COMPLETED)
                        .count();
                double progressRatio = totalLessons > 0 ? ((double) completedLessons / totalLessons) : (enrollment.getStatus() == EnrollmentStatus.COMPLETED ? 1.0 : 0.0);

                Cell cellProg = row.createCell(c++);
                cellProg.setCellValue(progressRatio);
                cellProg.setCellStyle(dataStylePercent);

                Cell cellLessons = row.createCell(c++);
                cellLessons.setCellValue(String.format("%d/%d", completedLessons, totalLessons));
                cellLessons.setCellStyle(dataStyleCenter);

                // Exam attempts & Average score
                List<ExamAttempt> studentAttempts = examAttemptRepository.findByStudentIdAndCourseId(student.getId(), courseId);
                double avgPercent = studentAttempts.stream()
                        .map(ExamAttempt::getPercentage)
                        .filter(Objects::nonNull)
                        .mapToDouble(BigDecimal::doubleValue)
                        .average()
                        .orElse(0.0);

                sumAvgPercentage += avgPercent;

                Cell cellAvg = row.createCell(c++);
                cellAvg.setCellValue(avgPercent / 100.0);
                cellAvg.setCellStyle(dataStylePercent);

                // Exam columns
                for (Exam exam : exams) {
                    BigDecimal bestScore = studentAttempts.stream()
                            .filter(a -> a.getExam().getId().equals(exam.getId()))
                            .map(ExamAttempt::getTotalScore)
                            .filter(Objects::nonNull)
                            .max(BigDecimal::compareTo)
                            .orElse(null);

                    Cell cellExam = row.createCell(c++);
                    if (bestScore != null) {
                        cellExam.setCellValue(bestScore.doubleValue());
                        cellExam.setCellStyle(dataStyleScore);
                    } else {
                        cellExam.setCellValue("-");
                        cellExam.setCellStyle(dataStyleCenter);
                    }
                }

                // Chứng chỉ
                boolean hasCert = certificateRepository.existsByStudentIdAndCourseId(student.getId(), courseId);
                if (hasCert || enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                    completedCount++;
                }

                Cell cellCertVal = row.createCell(c++);
                cellCertVal.setCellValue(hasCert ? "✓ Đã cấp" : (enrollment.getStatus() == EnrollmentStatus.COMPLETED ? "Đủ điều kiện" : "Chưa đạt"));
                cellCertVal.setCellStyle(dataStyleCenter);

                // Xếp loại
                String rank;
                if (avgPercent >= 90.0) rank = "Xuất sắc";
                else if (avgPercent >= 80.0) rank = "Giỏi";
                else if (avgPercent >= 65.0) rank = "Khá";
                else if (avgPercent >= 50.0) rank = "Trung bình";
                else rank = (studentAttempts.isEmpty() ? "Chưa thi" : "Yếu");

                Cell cellRankVal = row.createCell(c++);
                cellRankVal.setCellValue(rank);
                cellRankVal.setCellStyle(dataStyleCenter);
            }

            // 5. Signatures Block at bottom
            rowNum += 2;
            Row signRow1 = sheet1.createRow(rowNum++);
            int lastCol = Math.max(8, 7 + exams.size());

            Cell cSignDate = signRow1.createCell(lastCol - 2);
            cSignDate.setCellValue("......, ngày ... tháng ... năm 20...");
            cSignDate.setCellStyle(yearStyle);

            Row signRow2 = sheet1.createRow(rowNum++);
            Cell cSignTitle = signRow2.createCell(lastCol - 2);
            cSignTitle.setCellValue(params.getEffectiveSignerTitle().toUpperCase());
            XSSFCellStyle signTitleStyle = workbook.createCellStyle();
            signTitleStyle.setFont(boldBodyFont);
            signTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            cSignTitle.setCellStyle(signTitleStyle);

            Row signRow3 = sheet1.createRow(rowNum);
            Cell cSignSub = signRow3.createCell(lastCol - 2);
            cSignSub.setCellValue("(Ký và ghi rõ họ tên)");
            cSignSub.setCellStyle(yearStyle);

            // Auto-size columns
            for (int i = 0; i <= lastCol; i++) {
                sheet1.autoSizeColumn(i);
                if (sheet1.getColumnWidth(i) < 3000) {
                    sheet1.setColumnWidth(i, 3000);
                }
            }

            // ==========================================
            // SHEET 2: THỐNG KÊ & PHÂN PHỐI ĐIỂM
            // ==========================================
            Sheet sheet2 = workbook.createSheet("Thống kê lớp học");
            sheet2.setDisplayGridlines(true);

            int s2Row = 0;
            Row s2Title = sheet2.createRow(s2Row++);
            Cell cS2Title = s2Title.createCell(0);
            cS2Title.setCellValue("BÁO CÁO THỐNG KÊ VÀ PHỔ ĐIỂM KHÓA HỌC");
            cS2Title.setCellStyle(titleStyle);
            sheet2.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            s2Row++;
            String[][] statsData = {
                    {"Tổng số học viên đăng ký", String.valueOf(enrollments.size())},
                    {"Số học viên hoàn thành khóa học", String.valueOf(completedCount)},
                    {"Tỷ lệ hoàn thành khóa học", enrollments.isEmpty() ? "0%" : String.format("%.1f%%", ((double) completedCount / enrollments.size()) * 100)},
                    {"Điểm thi trung bình cả lớp", enrollments.isEmpty() ? "0.0%" : String.format("%.1f%%", sumAvgPercentage / enrollments.size())},
                    {"Tổng số bài thi trong khóa", String.valueOf(exams.size())},
                    {"Tổng số bài học", String.valueOf(totalLessons)},
            };

            for (String[] stat : statsData) {
                Row r = sheet2.createRow(s2Row++);
                Cell kCell = r.createCell(0);
                kCell.setCellValue(stat[0]);
                kCell.setCellStyle(dataStyleLeft);

                Cell vCell = r.createCell(1);
                vCell.setCellValue(stat[1]);
                vCell.setCellStyle(dataStyleCenter);
            }

            sheet2.autoSizeColumn(0);
            sheet2.autoSizeColumn(1);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate teacher course gradebook Excel", e);
            throw new RuntimeException("Lỗi khi xuất file Excel bảng điểm: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStudentTranscriptPdf(ExportCustomizationParams params, UUID studentId) {
        log.info("Generating student academic transcript PDF for studentId: {}", studentId);
        if (params == null) params = new ExportCustomizationParams();

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentIdWithCourse(studentId);
        List<ExamAttempt> attempts = examAttemptRepository.findByStudentId(studentId).stream()
                .filter(a -> a.getStatus() == ExamAttemptStatus.SUBMITTED)
                .sorted(Comparator.comparing(ExamAttempt::getStartedAt).reversed())
                .collect(Collectors.toList());
        List<Certificate> certificates = certificateRepository.findByStudentIdOrderByIssuedAtDesc(studentId);
        Double avgPercentage = examAttemptRepository.findAveragePercentageByStudentId(studentId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Background Decorative Frame
            PdfContentByte cb = writer.getDirectContent();
            Rectangle pageSize = PageSize.A4;

            // Outer border
            cb.setColorStroke(new Color(30, 41, 59));
            cb.setLineWidth(2f);
            cb.rectangle(20, 20, pageSize.getWidth() - 40, pageSize.getHeight() - 40);
            cb.stroke();

            // Inner subtle border
            cb.setColorStroke(new Color(99, 102, 241)); // Indigo
            cb.setLineWidth(0.8f);
            cb.rectangle(24, 24, pageSize.getWidth() - 48, pageSize.getHeight() - 48);
            cb.stroke();

            // Fonts
            Font orgFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, new Color(79, 70, 229));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, Font.BOLD, new Color(15, 23, 42));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, new Color(100, 116, 139));
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, new Color(30, 41, 59));
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.WHITE);
            Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, new Color(51, 65, 85));
            Font tableBodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, new Color(15, 23, 42));
            Font metaLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, new Color(71, 85, 105));
            Font metaValFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, new Color(15, 23, 42));

            // 1. Institution Branding & Header
            Paragraph pOrg = new Paragraph(params.getEffectiveInstitutionName().toUpperCase(), orgFont);
            pOrg.setAlignment(Element.ALIGN_CENTER);
            document.add(pOrg);

            String defaultTitle = "BANG DIEM HOC TAP CA NHAN / ACADEMIC TRANSCRIPT";
            String titleText = params.getEffectiveReportTitle(defaultTitle);
            Paragraph pTitle = new Paragraph(titleText, titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            pTitle.setSpacingBefore(4);
            document.add(pTitle);

            Paragraph pYear = new Paragraph("Nien khoa / Academic Year: " + params.getEffectiveAcademicYear(), subTitleFont);
            pYear.setAlignment(Element.ALIGN_CENTER);
            pYear.setSpacingBefore(2);
            pYear.setSpacingAfter(12);
            document.add(pYear);

            // 2. Student Info Box
            PdfPTable studentInfoTable = new PdfPTable(2);
            studentInfoTable.setWidthPercentage(100);
            studentInfoTable.setSpacingAfter(14);
            studentInfoTable.setWidths(new float[]{50f, 50f});

            PdfPCell cLeft = new PdfPCell();
            cLeft.setBackgroundColor(new Color(248, 250, 252));
            cLeft.setPadding(8);
            cLeft.setBorderColor(new Color(226, 232, 240));

            Paragraph nameP = new Paragraph("Ho va ten / Student Name: ", metaLabelFont);
            nameP.add(new Chunk(student.getFullName() != null ? student.getFullName() : "N/A", tableBodyBold));
            cLeft.addElement(nameP);

            Paragraph emailP = new Paragraph("Email: ", metaLabelFont);
            emailP.add(new Chunk(student.getEmail(), metaValFont));
            cLeft.addElement(emailP);

            studentInfoTable.addCell(cLeft);

            PdfPCell cRight = new PdfPCell();
            cRight.setBackgroundColor(new Color(248, 250, 252));
            cRight.setPadding(8);
            cRight.setBorderColor(new Color(226, 232, 240));

            Paragraph idP = new Paragraph("Ma hoc vien / Student ID: ", metaLabelFont);
            idP.add(new Chunk("SV-" + student.getId().toString().substring(0, 8).toUpperCase(), tableBodyBold));
            cRight.addElement(idP);

            Paragraph gpaP = new Paragraph("Diem TB tich luy / Overall GPA: ", metaLabelFont);
            gpaP.add(new Chunk(avgPercentage != null ? String.format("%.1f%%", avgPercentage) : "N/A", tableBodyBold));
            cRight.addElement(gpaP);

            studentInfoTable.addCell(cRight);
            document.add(studentInfoTable);

            // 3. Section 1: Enrolled Courses Table
            Paragraph sec1Title = new Paragraph("1. TIEN DO KHOA HOC DA THAM GIA / ENROLLED COURSES (" + enrollments.size() + ")", sectionTitleFont);
            sec1Title.setSpacingAfter(6);
            document.add(sec1Title);

            PdfPTable courseTable = new PdfPTable(4);
            courseTable.setWidthPercentage(100);
            courseTable.setWidths(new float[]{40f, 25f, 15f, 20f});
            courseTable.setSpacingAfter(14);

            String[] courseHeaders = {"Ten Khoa Hoc", "Mon Hoc", "Trang Thai", "Chung Chi"};
            for (String h : courseHeaders) {
                PdfPCell ch = new PdfPCell(new Phrase(h, tableHeaderFont));
                ch.setBackgroundColor(new Color(79, 70, 229));
                ch.setPadding(5);
                ch.setHorizontalAlignment(Element.ALIGN_CENTER);
                courseTable.addCell(ch);
            }

            if (enrollments.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Chua tham gia khoa hoc nao", tableBodyFont));
                emptyCell.setColspan(4);
                emptyCell.setPadding(6);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                courseTable.addCell(emptyCell);
            } else {
                for (CourseEnrollment e : enrollments) {
                    Course c = e.getCourse();
                    boolean hasCert = certificateRepository.existsByStudentIdAndCourseId(student.getId(), c.getId());

                    PdfPCell cellName = new PdfPCell(new Phrase(c.getName(), tableBodyFont));
                    cellName.setPadding(5);
                    courseTable.addCell(cellName);

                    PdfPCell cellSub = new PdfPCell(new Phrase(c.getSubject() != null ? c.getSubject().getName() : "-", tableBodyFont));
                    cellSub.setPadding(5);
                    courseTable.addCell(cellSub);

                    PdfPCell cellStatus = new PdfPCell(new Phrase(e.getStatus() == EnrollmentStatus.COMPLETED ? "Hoan thanh" : "Dang hoc", tableBodyFont));
                    cellStatus.setPadding(5);
                    cellStatus.setHorizontalAlignment(Element.ALIGN_CENTER);
                    courseTable.addCell(cellStatus);

                    PdfPCell cellCert = new PdfPCell(new Phrase(hasCert ? "Da cap" : "-", tableBodyBold));
                    cellCert.setPadding(5);
                    cellCert.setHorizontalAlignment(Element.ALIGN_CENTER);
                    courseTable.addCell(cellCert);
                }
            }
            document.add(courseTable);

            // 4. Section 2: Exam & Assessment Records Table
            Paragraph sec2Title = new Paragraph("2. KET QUA THI & KIEM TRA / EXAM RESULTS (" + attempts.size() + ")", sectionTitleFont);
            sec2Title.setSpacingAfter(6);
            document.add(sec2Title);

            PdfPTable examTable = new PdfPTable(5);
            examTable.setWidthPercentage(100);
            examTable.setWidths(new float[]{40f, 15f, 15f, 15f, 15f});
            examTable.setSpacingAfter(14);

            String[] examHeaders = {"De Thi / Bai Kiem Tra", "Lan Thi", "Diem So", "Ty Le %", "Ket Qua"};
            for (String h : examHeaders) {
                PdfPCell eh = new PdfPCell(new Phrase(h, tableHeaderFont));
                eh.setBackgroundColor(new Color(79, 70, 229));
                eh.setPadding(5);
                eh.setHorizontalAlignment(Element.ALIGN_CENTER);
                examTable.addCell(eh);
            }

            if (attempts.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Chua co lich su lam bai thi nao", tableBodyFont));
                emptyCell.setColspan(5);
                emptyCell.setPadding(6);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                examTable.addCell(emptyCell);
            } else {
                for (ExamAttempt a : attempts) {
                    PdfPCell cTitleCell = new PdfPCell(new Phrase(a.getExam() != null ? a.getExam().getTitle() : "-", tableBodyFont));
                    cTitleCell.setPadding(5);
                    examTable.addCell(cTitleCell);

                    PdfPCell cAttNum = new PdfPCell(new Phrase("Lan " + a.getAttemptNumber(), tableBodyFont));
                    cAttNum.setPadding(5);
                    cAttNum.setHorizontalAlignment(Element.ALIGN_CENTER);
                    examTable.addCell(cAttNum);

                    PdfPCell cScore = new PdfPCell(new Phrase((a.getTotalScore() != null ? a.getTotalScore().toString() : "0") + "/" + (a.getExam() != null ? a.getExam().getTotalMarks() : "10"), tableBodyBold));
                    cScore.setPadding(5);
                    cScore.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    examTable.addCell(cScore);

                    PdfPCell cPct = new PdfPCell(new Phrase(a.getPercentage() != null ? a.getPercentage() + "%" : "0%", tableBodyFont));
                    cPct.setPadding(5);
                    cPct.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    examTable.addCell(cPct);

                    boolean isPassed = Boolean.TRUE.equals(a.getPassed());
                    PdfPCell cPass = new PdfPCell(new Phrase(isPassed ? "DAT" : "CHUA DAT", tableBodyBold));
                    cPass.setPadding(5);
                    cPass.setHorizontalAlignment(Element.ALIGN_CENTER);
                    examTable.addCell(cPass);
                }
            }
            document.add(examTable);

            // 5. Section 3: Certificates Obtained (if any)
            if (!certificates.isEmpty()) {
                Paragraph sec3Title = new Paragraph("3. CHUNG CHI DAT DUOC / CERTIFICATES (" + certificates.size() + ")", sectionTitleFont);
                sec3Title.setSpacingAfter(6);
                document.add(sec3Title);

                PdfPTable certTable = new PdfPTable(3);
                certTable.setWidthPercentage(100);
                certTable.setWidths(new float[]{45f, 30f, 25f});
                certTable.setSpacingAfter(14);

                String[] certHeaders = {"Khoa Hoc", "Ma Chung Chi", "Ngay Cap"};
                for (String h : certHeaders) {
                    PdfPCell ch = new PdfPCell(new Phrase(h, tableHeaderFont));
                    ch.setBackgroundColor(new Color(79, 70, 229));
                    ch.setPadding(5);
                    ch.setHorizontalAlignment(Element.ALIGN_CENTER);
                    certTable.addCell(ch);
                }

                for (Certificate c : certificates) {
                    PdfPCell cCourse = new PdfPCell(new Phrase(c.getCourse() != null ? c.getCourse().getName() : "-", tableBodyFont));
                    cCourse.setPadding(5);
                    certTable.addCell(cCourse);

                    PdfPCell cCode = new PdfPCell(new Phrase(c.getCertificateCode(), tableBodyBold));
                    cCode.setPadding(5);
                    cCode.setHorizontalAlignment(Element.ALIGN_CENTER);
                    certTable.addCell(cCode);

                    PdfPCell cDate = new PdfPCell(new Phrase(c.getIssuedAt() != null ? c.getIssuedAt().format(DATE_FORMATTER) : "-", tableBodyFont));
                    cDate.setPadding(5);
                    cDate.setHorizontalAlignment(Element.ALIGN_CENTER);
                    certTable.addCell(cDate);
                }
                document.add(certTable);
            }

            // 6. Signatures & Verification QR Code Footer Table
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{45f, 55f});
            footerTable.setSpacingBefore(10);

            // QR Code cell
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            try {
                String verifyUrl = frontendUrl + "/profile/progress";
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(verifyUrl, BarcodeFormat.QR_CODE, 80, 80);
                ByteArrayOutputStream qrStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrStream);
                Image qrImage = Image.getInstance(qrStream.toByteArray());
                qrImage.scaleToFit(55, 55);
                qrCell.addElement(qrImage);

                Paragraph qrText = new Paragraph("Quet de xac thuc ho so online", FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, new Color(148, 163, 184)));
                qrCell.addElement(qrText);
            } catch (Exception ignored) {}
            footerTable.addCell(qrCell);

            // Signature Cell
            PdfPCell signCell = new PdfPCell();
            signCell.setBorder(Rectangle.NO_BORDER);
            signCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph pDateNow = new Paragraph("Ngay xuat: " + LocalDateTime.now().format(DATE_TIME_FORMATTER), FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, new Color(100, 116, 139)));
            pDateNow.setAlignment(Element.ALIGN_RIGHT);
            signCell.addElement(pDateNow);

            Paragraph pSigner = new Paragraph(params.getEffectiveSignerTitle().toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, new Color(15, 23, 42)));
            pSigner.setAlignment(Element.ALIGN_RIGHT);
            pSigner.setSpacingBefore(4);
            signCell.addElement(pSigner);

            Paragraph pSignNote = new Paragraph("(Ky ten va dong dau xac nhan)", FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, new Color(148, 163, 184)));
            pSignNote.setAlignment(Element.ALIGN_RIGHT);
            signCell.addElement(pSignNote);

            footerTable.addCell(signCell);
            document.add(footerTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate student transcript PDF", e);
            throw new RuntimeException("Lỗi khi xuất bảng điểm PDF: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateAdminPlatformOverviewExcel(ExportCustomizationParams params, UUID adminId) {
        log.info("Generating admin platform overview Excel for adminId: {}", adminId);
        if (!isUserAdmin(adminId)) {
            throw new ForbiddenOperationException("Only administrators are permitted to export platform reports");
        }
        if (params == null) params = new ExportCustomizationParams();

        long totalUsers = userRepository.count();
        long totalCourses = courseRepository.count();
        long totalExams = examRepository.count();
        long totalEnrollments = courseEnrollmentRepository.count();
        List<Order> orders = orderRepository.findAll();
        List<Course> courses = courseRepository.findAll();
        List<Exam> exams = examRepository.findAll();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DataFormat dataFormat = workbook.createDataFormat();

            // Styling fonts
            XSSFFont headerOrgFont = workbook.createFont();
            headerOrgFont.setFontName("Calibri");
            headerOrgFont.setFontHeightInPoints((short) 11);
            headerOrgFont.setBold(true);

            XSSFFont titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(new XSSFColor(new Color(79, 70, 229), null));

            XSSFFont colHeaderFont = workbook.createFont();
            colHeaderFont.setFontName("Calibri");
            colHeaderFont.setFontHeightInPoints((short) 10);
            colHeaderFont.setBold(true);
            colHeaderFont.setColor(new XSSFColor(new Color(255, 255, 255), null));

            XSSFFont bodyFont = workbook.createFont();
            bodyFont.setFontName("Calibri");
            bodyFont.setFontHeightInPoints((short) 10);

            XSSFFont boldBodyFont = workbook.createFont();
            boldBodyFont.setFontName("Calibri");
            boldBodyFont.setFontHeightInPoints((short) 10);
            boldBodyFont.setBold(true);

            // Cell Styles
            XSSFCellStyle colHeaderStyle = workbook.createCellStyle();
            colHeaderStyle.setFont(colHeaderFont);
            colHeaderStyle.setFillForegroundColor(new XSSFColor(new Color(79, 70, 229), null));
            colHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            colHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            colHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            colHeaderStyle.setBorderTop(BorderStyle.THIN);
            colHeaderStyle.setBorderBottom(BorderStyle.THIN);
            colHeaderStyle.setBorderLeft(BorderStyle.THIN);
            colHeaderStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setFont(bodyFont);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setFont(bodyFont);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle dataStyleCurrency = workbook.createCellStyle();
            dataStyleCurrency.setFont(boldBodyFont);
            dataStyleCurrency.setAlignment(HorizontalAlignment.RIGHT);
            dataStyleCurrency.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyleCurrency.setDataFormat(dataFormat.getFormat("#,##0 ₫"));
            dataStyleCurrency.setBorderTop(BorderStyle.THIN);
            dataStyleCurrency.setBorderBottom(BorderStyle.THIN);
            dataStyleCurrency.setBorderLeft(BorderStyle.THIN);
            dataStyleCurrency.setBorderRight(BorderStyle.THIN);

            // ==========================================
            // SHEET 1: TỔNG QUAN NỀN TẢNG (EXECUTIVE SUMMARY)
            // ==========================================
            Sheet sheet1 = workbook.createSheet("Tổng quan nền tảng");
            sheet1.setDisplayGridlines(true);

            int rNum = 0;
            Row rOrg = sheet1.createRow(rNum++);
            Cell cOrg = rOrg.createCell(0);
            cOrg.setCellValue(params.getEffectiveInstitutionName().toUpperCase());
            cOrg.setCellStyle(colHeaderStyle);

            Row rTitle = sheet1.createRow(rNum++);
            Cell cTitle = rTitle.createCell(0);
            cTitle.setCellValue(params.getEffectiveReportTitle("BÁO CÁO TỔNG QUAN VẬN HÀNH NỀN TẢNG NQD LMS"));
            XSSFCellStyle tStyle = workbook.createCellStyle();
            tStyle.setFont(titleFont);
            cTitle.setCellStyle(tStyle);

            Row rMeta = sheet1.createRow(rNum++);
            Cell cMeta = rMeta.createCell(0);
            cMeta.setCellValue("Niên khóa / Kỷ báo cáo: " + params.getEffectiveAcademicYear() + " | Xuất ngày: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));

            rNum++; // Blank row

            String[][] kpiData = {
                    {"Chỉ số vận hành", "Số lượng / Giá trị"},
                    {"Tổng số tài khoản người dùng", String.valueOf(totalUsers)},
                    {"Tổng số khóa học trên hệ thống", String.valueOf(totalCourses)},
                    {"Tổng lượt ghi danh học viên", String.valueOf(totalEnrollments)},
                    {"Tổng số đề thi & kiểm tra", String.valueOf(totalExams)},
                    {"Tổng số đơn hàng phát sinh", String.valueOf(orders.size())},
                    {"Tổng doanh thu thực tế (VND)", String.format("%,d ₫", totalRevenue.longValue())},
            };

            for (int i = 0; i < kpiData.length; i++) {
                Row r = sheet1.createRow(rNum++);
                Cell k = r.createCell(0);
                k.setCellValue(kpiData[i][0]);
                Cell v = r.createCell(1);
                v.setCellValue(kpiData[i][1]);

                if (i == 0) {
                    k.setCellStyle(colHeaderStyle);
                    v.setCellStyle(colHeaderStyle);
                } else {
                    k.setCellStyle(dataStyleLeft);
                    v.setCellStyle(dataStyleCenter);
                }
            }

            sheet1.autoSizeColumn(0);
            sheet1.autoSizeColumn(1);

            // ==========================================
            // SHEET 2: DANH SÁCH KHÓA HỌC
            // ==========================================
            Sheet sheet2 = workbook.createSheet("Khóa học");
            sheet2.setDisplayGridlines(true);

            int s2R = 0;
            Row h2 = sheet2.createRow(s2R++);
            String[] s2Headers = {"STT", "Mã Khóa", "Tên Khóa Học", "Môn Học", "Giáo Viên", "Trạng Thái", "Lượt Đăng Ký"};
            for (int i = 0; i < s2Headers.length; i++) {
                Cell c = h2.createCell(i);
                c.setCellValue(s2Headers[i]);
                c.setCellStyle(colHeaderStyle);
            }

            int cStt = 1;
            for (Course course : courses) {
                Row r = sheet2.createRow(s2R++);
                int col = 0;

                Cell c1 = r.createCell(col++);
                c1.setCellValue(cStt++);
                c1.setCellStyle(dataStyleCenter);

                Cell c2 = r.createCell(col++);
                c2.setCellValue(course.getCode() != null ? course.getCode() : "CRS-" + course.getId().toString().substring(0, 6));
                c2.setCellStyle(dataStyleCenter);

                Cell c3 = r.createCell(col++);
                c3.setCellValue(course.getName());
                c3.setCellStyle(dataStyleLeft);

                Cell c4 = r.createCell(col++);
                c4.setCellValue(course.getSubject() != null ? course.getSubject().getName() : "-");
                c4.setCellStyle(dataStyleLeft);

                Cell c5 = r.createCell(col++);
                c5.setCellValue(course.getCreator() != null ? course.getCreator().getFullName() : "-");
                c5.setCellStyle(dataStyleLeft);

                Cell c6 = r.createCell(col++);
                c6.setCellValue(course.getStatus() != null ? course.getStatus().name() : "ACTIVE");
                c6.setCellStyle(dataStyleCenter);

                long enrCount = courseEnrollmentRepository.countByCourseId(course.getId());
                Cell c7 = r.createCell(col++);
                c7.setCellValue(enrCount);
                c7.setCellStyle(dataStyleCenter);
            }

            for (int i = 0; i < s2Headers.length; i++) {
                sheet2.autoSizeColumn(i);
            }

            // ==========================================
            // SHEET 3: THỐNG KÊ ĐỀ THI
            // ==========================================
            Sheet sheet3 = workbook.createSheet("Đề thi & Kiểm tra");
            sheet3.setDisplayGridlines(true);

            int s3R = 0;
            Row h3 = sheet3.createRow(s3R++);
            String[] s3Headers = {"STT", "Mã Đề", "Tên Đề Thi", "Môn Học", "Giáo Viên", "Thời Lượng (Phút)", "Số Lượt Nộp", "Giám Sát"};
            for (int i = 0; i < s3Headers.length; i++) {
                Cell c = h3.createCell(i);
                c.setCellValue(s3Headers[i]);
                c.setCellStyle(colHeaderStyle);
            }

            int eStt = 1;
            for (Exam exam : exams) {
                Row r = sheet3.createRow(s3R++);
                int col = 0;

                Cell c1 = r.createCell(col++);
                c1.setCellValue(eStt++);
                c1.setCellStyle(dataStyleCenter);

                Cell c2 = r.createCell(col++);
                c2.setCellValue(exam.getCode() != null ? exam.getCode() : "EXM-" + exam.getId().toString().substring(0, 6));
                c2.setCellStyle(dataStyleCenter);

                Cell c3 = r.createCell(col++);
                c3.setCellValue(exam.getTitle());
                c3.setCellStyle(dataStyleLeft);

                Cell c4 = r.createCell(col++);
                c4.setCellValue(exam.getSubject() != null ? exam.getSubject().getName() : "-");
                c4.setCellStyle(dataStyleLeft);

                Cell c5 = r.createCell(col++);
                c5.setCellValue(exam.getCreator() != null ? exam.getCreator().getFullName() : "-");
                c5.setCellStyle(dataStyleLeft);

                Cell c6 = r.createCell(col++);
                c6.setCellValue(exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 45);
                c6.setCellStyle(dataStyleCenter);

                long attCount = examAttemptRepository.countByExamId(exam.getId());
                Cell c7 = r.createCell(col++);
                c7.setCellValue(attCount);
                c7.setCellStyle(dataStyleCenter);

                Cell c8 = r.createCell(col++);
                c8.setCellValue(Boolean.TRUE.equals(exam.getEnableProctoring()) ? "Bật (Tối đa " + exam.getMaxViolationCount() + " vi phạm)" : "Không");
                c8.setCellStyle(dataStyleCenter);
            }

            for (int i = 0; i < s3Headers.length; i++) {
                sheet3.autoSizeColumn(i);
            }

            // ==========================================
            // SHEET 4: GIAO DỊCH & DOANH THU
            // ==========================================
            Sheet sheet4 = workbook.createSheet("Giao dịch & Doanh thu");
            sheet4.setDisplayGridlines(true);

            int s4R = 0;
            Row h4 = sheet4.createRow(s4R++);
            String[] s4Headers = {"STT", "Mã Đơn Hàng", "Người Mua", "Email", "Số Tiền (VND)", "Trạng Thái", "Ngày Giao Dịch"};
            for (int i = 0; i < s4Headers.length; i++) {
                Cell c = h4.createCell(i);
                c.setCellValue(s4Headers[i]);
                c.setCellStyle(colHeaderStyle);
            }

            int oStt = 1;
            for (Order order : orders) {
                Row r = sheet4.createRow(s4R++);
                int col = 0;

                Cell c1 = r.createCell(col++);
                c1.setCellValue(oStt++);
                c1.setCellStyle(dataStyleCenter);

                Cell c2 = r.createCell(col++);
                c2.setCellValue(order.getOrderCode());
                c2.setCellStyle(dataStyleCenter);

                Cell c3 = r.createCell(col++);
                c3.setCellValue(order.getUser() != null ? order.getUser().getFullName() : "-");
                c3.setCellStyle(dataStyleLeft);

                Cell c4 = r.createCell(col++);
                c4.setCellValue(order.getUser() != null ? order.getUser().getEmail() : "-");
                c4.setCellStyle(dataStyleLeft);

                Cell c5 = r.createCell(col++);
                c5.setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
                c5.setCellStyle(dataStyleCurrency);

                Cell c6 = r.createCell(col++);
                c6.setCellValue(order.getStatus() != null ? order.getStatus().name() : "-");
                c6.setCellStyle(dataStyleCenter);

                Cell c7 = r.createCell(col++);
                c7.setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_TIME_FORMATTER) : "-");
                c7.setCellStyle(dataStyleCenter);
            }

            for (int i = 0; i < s4Headers.length; i++) {
                sheet4.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate admin platform overview Excel", e);
            throw new RuntimeException("Lỗi khi xuất báo cáo admin: " + e.getMessage(), e);
        }
    }

    private boolean isUserAdmin(UUID userId) {
        if (userId == null) return false;
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        return roles.stream().anyMatch(ur -> {
            if (ur.getRole() != null && ur.getRole().getName() != null) {
                return ur.getRole().getName().toUpperCase().contains("ADMIN");
            }
            if (ur.getRoleId() != null) {
                return roleRepository.findById(ur.getRoleId())
                        .map(r -> r.getName().toUpperCase().contains("ADMIN"))
                        .orElse(false);
            }
            return false;
        });
    }
}
