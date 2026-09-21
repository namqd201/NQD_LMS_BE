package com.nqd.nqd_lms_be.service.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.nqd.nqd_lms_be.dto.report.ExamPaperExportParams;
import com.nqd.nqd_lms_be.dto.report.QuestionPaperExportRequest;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamVisibility;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamPaperExportServiceImpl implements ExamPaperExportService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExamDocx(UUID examId, ExamPaperExportParams params, UUID teacherId) {
        Exam exam = getAndVerifyExam(examId, teacherId);
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByDisplayOrderAsc(examId);

        if (params == null) {
            params = new ExamPaperExportParams();
        }

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Set page size A4 and margins (Top: 2cm, Bottom: 2cm, Left: 2.5cm, Right: 2cm)
            setDocxPageMargins(document);

            // 1. Header 2 columns table (Borderless)
            renderDocxHeaderTable(document, exam, params);

            // Spacing
            addEmptyParagraph(document, 4);

            // 2. Student Info line: Họ và tên: ....................................... Lớp: .....................
            XWPFParagraph infoPara = document.createParagraph();
            infoPara.setSpacingAfter(100);
            XWPFRun infoRun = infoPara.createRun();
            infoRun.setFontFamily("Times New Roman");
            infoRun.setFontSize(12);
            infoRun.setText("Họ và tên: ................................................................................................ Lớp: .................................");

            // 3. Scoring & Examiner Box (Bordered Table: 2 columns)
            renderDocxScoringTable(document);

            addEmptyParagraph(document, 8);

            // Classify questions into Multiple Choice & Essay
            List<ExamQuestion> mcQuestions = new ArrayList<>();
            List<ExamQuestion> essayQuestions = new ArrayList<>();

            for (ExamQuestion eq : examQuestions) {
                Question q = eq.getQuestion();
                if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE || q.getQuestionType() == QuestionType.TRUE_FALSE) {
                    mcQuestions.add(eq);
                } else {
                    List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
                    if (options != null && !options.isEmpty()) {
                        mcQuestions.add(eq);
                    } else {
                        essayQuestions.add(eq);
                    }
                }
            }

            // Calculate section marks
            BigDecimal mcMarks = mcQuestions.stream()
                    .map(eq -> eq.getMarks() != null ? eq.getMarks() : BigDecimal.ONE)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal essayMarks = essayQuestions.stream()
                    .map(eq -> eq.getMarks() != null ? eq.getMarks() : BigDecimal.ONE)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int currentQuestionNumber = 1;

            // 4. Section A: Multiple Choice
            if (!mcQuestions.isEmpty()) {
                XWPFParagraph secAPara = document.createParagraph();
                secAPara.setSpacingBefore(120);
                secAPara.setSpacingAfter(60);
                XWPFRun secARun = secAPara.createRun();
                secARun.setFontFamily("Times New Roman");
                secARun.setFontSize(13);
                secARun.setBold(true);
                secARun.setUnderline(UnderlinePatterns.SINGLE);
                secARun.setText("A. Trắc nghiệm : ( " + formatMarks(mcMarks) + " điểm)");

                // Instruction note
                XWPFParagraph notePara = document.createParagraph();
                notePara.setSpacingAfter(120);
                XWPFRun noteRun = notePara.createRun();
                noteRun.setFontFamily("Times New Roman");
                noteRun.setFontSize(12);
                noteRun.setBold(true);
                noteRun.setText(params.getEffectiveInstructionNote());

                for (ExamQuestion eq : mcQuestions) {
                    Question q = eq.getQuestion();
                    List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());

                    // Question title & content
                    XWPFParagraph qPara = document.createParagraph();
                    qPara.setSpacingBefore(80);
                    qPara.setSpacingAfter(40);
                    
                    XWPFRun qTitleRun = qPara.createRun();
                    qTitleRun.setFontFamily("Times New Roman");
                    qTitleRun.setFontSize(12);
                    qTitleRun.setBold(true);
                    qTitleRun.setText("Câu " + currentQuestionNumber + " : ");

                    XWPFRun qContentRun = qPara.createRun();
                    qContentRun.setFontFamily("Times New Roman");
                    qContentRun.setFontSize(12);
                    qContentRun.setText(q.getContent() != null ? q.getContent() : "");

                    // Options layout
                    if (isFillInOrShortAnswer(q, options)) {
                        String content = q.getContent() != null ? q.getContent().trim() : "";
                        boolean hasDots = content.contains("...") || content.contains("…") || content.contains(".....");
                        if (!hasDots) {
                            XWPFParagraph ansLinePara = document.createParagraph();
                            ansLinePara.setSpacingBefore(20);
                            ansLinePara.setSpacingAfter(30);
                            ansLinePara.setIndentationLeft(280);
                            XWPFRun rAns = ansLinePara.createRun();
                            rAns.setFontFamily("Times New Roman");
                            rAns.setFontSize(11);
                            rAns.setItalic(true);
                            rAns.setText("Đáp số: ................................................................");
                        }
                    } else {
                        renderDocxOptions(document, options);
                    }

                    currentQuestionNumber++;
                }
            }

            // 5. Section B: Essay
            if (!essayQuestions.isEmpty()) {
                XWPFParagraph secBPara = document.createParagraph();
                secBPara.setSpacingBefore(180);
                secBPara.setSpacingAfter(80);
                XWPFRun secBRun = secBPara.createRun();
                secBRun.setFontFamily("Times New Roman");
                secBRun.setFontSize(13);
                secBRun.setBold(true);
                secBRun.setUnderline(UnderlinePatterns.SINGLE);
                secBRun.setText("B. Tự luận : ( " + formatMarks(essayMarks) + " điểm)");

                int essayIndex = 1;
                for (ExamQuestion eq : essayQuestions) {
                    Question q = eq.getQuestion();
                    BigDecimal qMarks = eq.getMarks() != null ? eq.getMarks() : BigDecimal.ONE;

                    XWPFParagraph ePara = document.createParagraph();
                    ePara.setSpacingBefore(100);
                    ePara.setSpacingAfter(60);

                    XWPFRun eTitleRun = ePara.createRun();
                    eTitleRun.setFontFamily("Times New Roman");
                    eTitleRun.setFontSize(12);
                    eTitleRun.setBold(true);
                    eTitleRun.setText("Bài " + essayIndex + " . ( " + formatMarks(qMarks) + " điểm) . ");

                    XWPFRun eContentRun = ePara.createRun();
                    eContentRun.setFontFamily("Times New Roman");
                    eContentRun.setFontSize(12);
                    eContentRun.setText(q.getContent() != null ? q.getContent() : "");

                    // Dotted lines for answering
                    int numDottedLines = (q.getQuestionType() == QuestionType.ESSAY) ? 5 : 2;
                    if (q.getQuestionType() == QuestionType.ESSAY) {
                        XWPFParagraph workPara = document.createParagraph();
                        workPara.setSpacingAfter(40);
                        XWPFRun workRun = workPara.createRun();
                        workRun.setFontFamily("Times New Roman");
                        workRun.setFontSize(11);
                        workRun.setItalic(true);
                        workRun.setText("Bài làm:");
                    }

                    for (int line = 0; line < numDottedLines; line++) {
                        XWPFParagraph dotPara = document.createParagraph();
                        dotPara.setSpacingAfter(60);
                        XWPFRun dotRun = dotPara.createRun();
                        dotRun.setFontFamily("Times New Roman");
                        dotRun.setFontSize(11);
                        dotRun.setText("...................................................................................................................................................................");
                    }

                    essayIndex++;
                    currentQuestionNumber++;
                }
            }

            // 6. End of exam footer line
            addEmptyParagraph(document, 12);
            XWPFParagraph endPara = document.createParagraph();
            endPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun endRun = endPara.createRun();
            endRun.setFontFamily("Times New Roman");
            endRun.setFontSize(11);
            endRun.setBold(true);
            endRun.setText("----------------- HẾT -----------------");

            XWPFParagraph subEndPara = document.createParagraph();
            subEndPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subEndRun = subEndPara.createRun();
            subEndRun.setFontFamily("Times New Roman");
            subEndRun.setFontSize(10);
            subEndRun.setItalic(true);
            subEndRun.setText("(Cán bộ coi thi không giải thích gì thêm)");

            // 7. Optional Answer Key Page (for teachers)
            if (Boolean.TRUE.equals(params.getIncludeAnswerKey())) {
                renderDocxAnswerKeyPage(document, exam, params, mcQuestions, essayQuestions);
            }

            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting exam docx: ", e);
            throw new RuntimeException("Không thể tạo file Word đề thi: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExamPdf(UUID examId, ExamPaperExportParams params, UUID teacherId) {
        Exam exam = getAndVerifyExam(examId, teacherId);
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByDisplayOrderAsc(examId);

        if (params == null) {
            params = new ExamPaperExportParams();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(PageSize.A4, 40, 40, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = getBaseFont(false, false);
            BaseFont baseFontBold = getBaseFont(true, false);
            BaseFont baseFontItalic = getBaseFont(false, true);

            Font fontTitle = new Font(baseFontBold, 13, Font.NORMAL);
            Font fontSub = new Font(baseFont, 11, Font.NORMAL);
            Font fontSubBold = new Font(baseFontBold, 11, Font.NORMAL);
            Font fontRegular = new Font(baseFont, 11, Font.NORMAL);
            Font fontBold = new Font(baseFontBold, 11, Font.NORMAL);
            Font fontItalic = new Font(baseFontItalic, 10, Font.NORMAL);

            // 1. Header Table (2 columns)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{40f, 60f});

            // Left Cell: School name
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            if (params.getDepartmentName() != null && !params.getDepartmentName().trim().isEmpty()) {
                Paragraph pDept = new Paragraph(params.getDepartmentName().trim().toUpperCase(), fontSub);
                pDept.setAlignment(Element.ALIGN_CENTER);
                leftCell.addElement(pDept);
            }

            Paragraph pInst = new Paragraph(params.getEffectiveInstitutionName(), fontTitle);
            pInst.setAlignment(Element.ALIGN_CENTER);
            leftCell.addElement(pInst);

            Paragraph pDash = new Paragraph("-------------", fontSub);
            pDash.setAlignment(Element.ALIGN_CENTER);
            leftCell.addElement(pDash);

            headerTable.addCell(leftCell);

            // Right Cell: Exam Title, Year, Subject, Time
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            String defTitle = (exam.getTitle() != null ? exam.getTitle().toUpperCase() : "ĐỀ KIỂM TRA");
            Paragraph pTitle = new Paragraph(params.getEffectiveExamTitle(defTitle), fontTitle);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(pTitle);

            Paragraph pYear = new Paragraph(params.getEffectiveAcademicYear(), fontSub);
            pYear.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(pYear);

            String subName = (params.getSubjectName() != null && !params.getSubjectName().trim().isEmpty())
                    ? params.getSubjectName().trim()
                    : (exam.getSubject() != null ? exam.getSubject().getName() : "Chung");
            Paragraph pSub = new Paragraph("Môn : " + subName, fontSubBold);
            pSub.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(pSub);

            int duration = params.getDurationMinutes() != null ? params.getDurationMinutes() :
                    (exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 45);
            Paragraph pTime = new Paragraph("Thời gian làm bài: ( " + duration + " phút)", fontSubBold);
            pTime.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(pTime);

            headerTable.addCell(rightCell);
            document.add(headerTable);

            // 2. Student Info line
            Paragraph pStudent = new Paragraph("Họ và tên: ..................................................................................... Lớp: .............................", fontRegular);
            pStudent.setSpacingBefore(10);
            pStudent.setSpacingAfter(8);
            document.add(pStudent);

            // 3. Scoring & Examiner Box (Table 2 columns with border)
            PdfPTable scoreTable = new PdfPTable(2);
            scoreTable.setWidthPercentage(100);
            scoreTable.setWidths(new float[]{35f, 65f});

            PdfPCell c1 = new PdfPCell();
            c1.setPadding(8);
            Paragraph pScoreHead = new Paragraph("Điểm", fontBold);
            pScoreHead.setAlignment(Element.ALIGN_CENTER);
            c1.addElement(pScoreHead);
            c1.addElement(new Paragraph("Bằng chữ: .................................", fontRegular));
            c1.addElement(new Paragraph("Bằng số: ....................................", fontRegular));
            scoreTable.addCell(c1);

            PdfPCell c2 = new PdfPCell();
            c2.setPadding(8);
            c2.addElement(new Paragraph("Giáo viên coi thi: .................................................................................", fontRegular));
            c2.addElement(new Paragraph(" ", fontRegular));
            c2.addElement(new Paragraph("Giáo viên chấm thi: ...............................................................................", fontRegular));
            scoreTable.addCell(c2);

            document.add(scoreTable);

            // Classify questions
            List<ExamQuestion> mcQuestions = new ArrayList<>();
            List<ExamQuestion> essayQuestions = new ArrayList<>();

            for (ExamQuestion eq : examQuestions) {
                Question q = eq.getQuestion();
                if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE || q.getQuestionType() == QuestionType.TRUE_FALSE) {
                    mcQuestions.add(eq);
                } else {
                    List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
                    if (options != null && !options.isEmpty()) {
                        mcQuestions.add(eq);
                    } else {
                        essayQuestions.add(eq);
                    }
                }
            }

            BigDecimal mcMarks = mcQuestions.stream()
                    .map(eq -> eq.getMarks() != null ? eq.getMarks() : BigDecimal.ONE)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal essayMarks = essayQuestions.stream()
                    .map(eq -> eq.getMarks() != null ? eq.getMarks() : BigDecimal.ONE)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int qNum = 1;

            // 4. Section A: Multiple Choice
            if (!mcQuestions.isEmpty()) {
                Paragraph pSecA = new Paragraph("A. Trắc nghiệm : ( " + formatMarks(mcMarks) + " điểm)", fontTitle);
                pSecA.setSpacingBefore(12);
                pSecA.setSpacingAfter(4);
                document.add(pSecA);

                Paragraph pNote = new Paragraph(params.getEffectiveInstructionNote(), fontBold);
                pNote.setSpacingAfter(8);
                document.add(pNote);

                for (ExamQuestion eq : mcQuestions) {
                    Question q = eq.getQuestion();
                    List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());

                    Paragraph pQ = new Paragraph();
                    pQ.setSpacingBefore(6);
                    pQ.setSpacingAfter(3);
                    pQ.add(new Chunk("Câu " + qNum + " : ", fontBold));
                    pQ.add(new Chunk(q.getContent() != null ? q.getContent() : "", fontRegular));
                    document.add(pQ);

                    // Options
                    if (isFillInOrShortAnswer(q, options)) {
                        String content = q.getContent() != null ? q.getContent().trim() : "";
                        boolean hasDots = content.contains("...") || content.contains("…") || content.contains(".....");
                        if (!hasDots) {
                            Paragraph pAns = new Paragraph("Đáp số: ................................................................", fontItalic);
                            pAns.setIndentationLeft(20);
                            pAns.setSpacingBefore(2);
                            pAns.setSpacingAfter(4);
                            document.add(pAns);
                        }
                    } else if (options != null && !options.isEmpty()) {
                        boolean allShort = options.stream().allMatch(o -> (o.getOptionText() != null ? o.getOptionText().length() : 0) <= 20);
                        if (allShort && options.size() <= 4) {
                            Paragraph pOpts = new Paragraph();
                            pOpts.setIndentationLeft(20);
                            for (QuestionOption opt : options) {
                                pOpts.add(new Chunk(opt.getOptionKey() + ". ", fontBold));
                                pOpts.add(new Chunk(opt.getOptionText() + "      ", fontRegular));
                            }
                            document.add(pOpts);
                        } else {
                            PdfPTable optTable = new PdfPTable(2);
                            optTable.setWidthPercentage(95);
                            optTable.setHorizontalAlignment(Element.ALIGN_LEFT);
                            for (QuestionOption opt : options) {
                                PdfPCell optCell = new PdfPCell();
                                optCell.setBorder(Rectangle.NO_BORDER);
                                optCell.setPaddingBottom(3);
                                Paragraph pCell = new Paragraph();
                                pCell.add(new Chunk(opt.getOptionKey() + ". ", fontBold));
                                pCell.add(new Chunk(opt.getOptionText() != null ? opt.getOptionText() : "", fontRegular));
                                optCell.addElement(pCell);
                                optTable.addCell(optCell);
                            }
                            if (options.size() % 2 != 0) {
                                PdfPCell emptyCell = new PdfPCell();
                                emptyCell.setBorder(Rectangle.NO_BORDER);
                                optTable.addCell(emptyCell);
                            }
                            document.add(optTable);
                        }
                    }

                    qNum++;
                }
            }

            // 5. Section B: Essay
            if (!essayQuestions.isEmpty()) {
                Paragraph pSecB = new Paragraph("B. Tự luận : ( " + formatMarks(essayMarks) + " điểm)", fontTitle);
                pSecB.setSpacingBefore(14);
                pSecB.setSpacingAfter(6);
                document.add(pSecB);

                int eIdx = 1;
                for (ExamQuestion eq : essayQuestions) {
                    Question q = eq.getQuestion();
                    BigDecimal marks = eq.getMarks() != null ? eq.getMarks() : BigDecimal.ONE;

                    Paragraph pE = new Paragraph();
                    pE.setSpacingBefore(8);
                    pE.setSpacingAfter(4);
                    pE.add(new Chunk("Bài " + eIdx + " . ( " + formatMarks(marks) + " điểm) . ", fontBold));
                    pE.add(new Chunk(q.getContent() != null ? q.getContent() : "", fontRegular));
                    document.add(pE);

                    if (q.getQuestionType() == QuestionType.ESSAY) {
                        Paragraph pWork = new Paragraph("Bài làm:", fontItalic);
                        pWork.setSpacingAfter(3);
                        document.add(pWork);
                    }

                    int lines = (q.getQuestionType() == QuestionType.ESSAY) ? 4 : 2;
                    for (int l = 0; l < lines; l++) {
                        Paragraph pDots = new Paragraph(".........................................................................................................................................................................", fontRegular);
                        pDots.setSpacingAfter(4);
                        document.add(pDots);
                    }

                    eIdx++;
                    qNum++;
                }
            }

            // 6. End Notice
            Paragraph pEnd = new Paragraph("----------------- HẾT -----------------", fontBold);
            pEnd.setAlignment(Element.ALIGN_CENTER);
            pEnd.setSpacingBefore(16);
            document.add(pEnd);

            Paragraph pEndSub = new Paragraph("(Cán bộ coi thi không giải thích gì thêm)", fontItalic);
            pEndSub.setAlignment(Element.ALIGN_CENTER);
            document.add(pEndSub);

            // 7. Optional Answer Key Page
            if (Boolean.TRUE.equals(params.getIncludeAnswerKey())) {
                document.newPage();
                Paragraph pKeyTitle = new Paragraph("HƯỚNG DẪN CHẤM & ĐÁP ÁN ĐỀ THI", fontTitle);
                pKeyTitle.setAlignment(Element.ALIGN_CENTER);
                pKeyTitle.setSpacingBefore(10);
                document.add(pKeyTitle);

                Paragraph pKeySub = new Paragraph("Môn: " + subName + " - " + params.getEffectiveAcademicYear(), fontSub);
                pKeySub.setAlignment(Element.ALIGN_CENTER);
                pKeySub.setSpacingAfter(15);
                document.add(pKeySub);

                if (!mcQuestions.isEmpty()) {
                    Paragraph pKeySecA = new Paragraph("1. ĐÁP ÁN PHẦN TRẮC NGHIỆM & ĐIỀN ĐÁP ÁN:", fontBold);
                    pKeySecA.setSpacingAfter(6);
                    document.add(pKeySecA);

                    int chunkSize = 10;
                    for (int chunkStart = 0; chunkStart < mcQuestions.size(); chunkStart += chunkSize) {
                        int chunkEnd = Math.min(chunkStart + chunkSize, mcQuestions.size());
                        int numItems = chunkEnd - chunkStart;

                        PdfPTable ansTable = new PdfPTable(numItems + 1);
                        ansTable.setWidthPercentage(100);

                        // Row 1: Header Câu
                        PdfPCell hCell = new PdfPCell(new Phrase("Câu", fontBold));
                        hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        hCell.setBackgroundColor(new java.awt.Color(240, 240, 240));
                        ansTable.addCell(hCell);

                        for (int i = 0; i < numItems; i++) {
                            PdfPCell c = new PdfPCell(new Phrase(String.valueOf(chunkStart + i + 1), fontBold));
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            c.setBackgroundColor(new java.awt.Color(240, 240, 240));
                            ansTable.addCell(c);
                        }

                        // Row 2: Đáp án
                        PdfPCell aCell = new PdfPCell(new Phrase("Đáp án", fontBold));
                        aCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        ansTable.addCell(aCell);

                        for (int i = 0; i < numItems; i++) {
                            ExamQuestion eq = mcQuestions.get(chunkStart + i);
                            List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(eq.getQuestion().getId());
                            String correct = getCorrectAnswerDisplay(eq.getQuestion(), options);
                            PdfPCell c = new PdfPCell(new Phrase(correct, fontBold));
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            ansTable.addCell(c);
                        }

                        document.add(ansTable);
                        if (chunkEnd < mcQuestions.size()) {
                            Paragraph pSpace = new Paragraph(" ", fontRegular);
                            pSpace.setSpacingAfter(4);
                            document.add(pSpace);
                        }
                    }
                }

                if (!essayQuestions.isEmpty()) {
                    Paragraph pKeySecB = new Paragraph("2. HƯỚNG DẪN CHẤM PHẦN TỰ LUẬN:", fontBold);
                    pKeySecB.setSpacingBefore(12);
                    pKeySecB.setSpacingAfter(6);
                    document.add(pKeySecB);

                    int eIdx = 1;
                    for (ExamQuestion eq : essayQuestions) {
                        Question q = eq.getQuestion();
                        Paragraph pItem = new Paragraph();
                        pItem.setSpacingBefore(4);
                        pItem.add(new Chunk("Bài " + eIdx + " (" + formatMarks(eq.getMarks()) + " điểm): ", fontBold));
                        String expl = (q.getExplanation() != null && !q.getExplanation().trim().isEmpty())
                                ? q.getExplanation().trim()
                                : "Học sinh thực hiện đúng các bước và ghi rõ kết quả đạt điểm tối đa.";
                        pItem.add(new Chunk(expl, fontRegular));
                        document.add(pItem);
                        eIdx++;
                    }
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting exam pdf: ", e);
            throw new RuntimeException("Không thể tạo file PDF đề thi: " + e.getMessage(), e);
        }
    }

    private Exam getAndVerifyExam(UUID examId, UUID teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (!exam.getIsDeleted() && !SecurityUtils.isAdmin() && (exam.getCreator() == null || !exam.getCreator().getId().equals(teacherId))) {
            boolean isSharedOrPublic = exam.getStatus() == ExamStatus.PUBLISHED &&
                    (exam.getVisibility() == ExamVisibility.PUBLIC || exam.getVisibility() == ExamVisibility.SUBJECT_SHARED);
            if (!isSharedOrPublic) {
                throw new ForbiddenOperationException("Bạn không có quyền tải đề thi này");
            }
        }
        return exam;
    }

    private void setDocxPageMargins(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(1134)); // ~2cm
        pageMar.setBottom(BigInteger.valueOf(1134));
        pageMar.setLeft(BigInteger.valueOf(1417)); // ~2.5cm
        pageMar.setRight(BigInteger.valueOf(1134));
    }

    private void renderDocxHeaderTable(XWPFDocument document, Exam exam, ExamPaperExportParams params) {
        XWPFTable headerTable = document.createTable(1, 2);
        headerTable.setWidth("100%");
        headerTable.removeBorders();

        XWPFTableRow row = headerTable.getRow(0);

        // Left cell: Institution
        XWPFTableCell leftCell = row.getCell(0);
        leftCell.setWidth("40%");
        XWPFParagraph lp = leftCell.getParagraphArray(0);
        lp.setAlignment(ParagraphAlignment.CENTER);
        lp.setSpacingAfter(40);

        if (params.getDepartmentName() != null && !params.getDepartmentName().trim().isEmpty()) {
            XWPFRun rDept = lp.createRun();
            rDept.setFontFamily("Times New Roman");
            rDept.setFontSize(11);
            rDept.setText(params.getDepartmentName().trim().toUpperCase());
            rDept.addBreak();
        }

        XWPFRun rInst = lp.createRun();
        rInst.setFontFamily("Times New Roman");
        rInst.setFontSize(13);
        rInst.setBold(true);
        rInst.setText(params.getEffectiveInstitutionName());
        rInst.addBreak();

        XWPFRun rDash = lp.createRun();
        rDash.setFontFamily("Times New Roman");
        rDash.setFontSize(11);
        rDash.setText("-----------------");

        // Right cell: Exam Title, Year, Subject, Time
        XWPFTableCell rightCell = row.getCell(1);
        rightCell.setWidth("60%");
        XWPFParagraph rp = rightCell.getParagraphArray(0);
        rp.setAlignment(ParagraphAlignment.CENTER);
        rp.setSpacingAfter(40);

        String defTitle = (exam != null && exam.getTitle() != null ? exam.getTitle().toUpperCase() : "ĐỀ KIỂM TRA ĐÁNH GIÁ NĂNG LỰC");
        XWPFRun rTitle = rp.createRun();
        rTitle.setFontFamily("Times New Roman");
        rTitle.setFontSize(13);
        rTitle.setBold(true);
        rTitle.setText(params.getEffectiveExamTitle(defTitle));
        rTitle.addBreak();

        XWPFRun rYear = rp.createRun();
        rYear.setFontFamily("Times New Roman");
        rYear.setFontSize(12);
        rYear.setText(params.getEffectiveAcademicYear());
        rYear.addBreak();

        String subName = (params.getSubjectName() != null && !params.getSubjectName().trim().isEmpty())
                ? params.getSubjectName().trim()
                : (exam != null && exam.getSubject() != null ? exam.getSubject().getName() : "Chung");
        XWPFRun rSub = rp.createRun();
        rSub.setFontFamily("Times New Roman");
        rSub.setFontSize(12);
        rSub.setBold(true);
        rSub.setText("Môn : " + subName);
        rSub.addBreak();

        int duration = params.getDurationMinutes() != null ? params.getDurationMinutes() :
                (exam != null && exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 40);
        XWPFRun rTime = rp.createRun();
        rTime.setFontFamily("Times New Roman");
        rTime.setFontSize(12);
        rTime.setBold(true);
        rTime.setText("Thời gian làm bài: ( " + duration + " phút)");
    }

    private void renderDocxScoringTable(XWPFDocument document) {
        XWPFTable scoreTable = document.createTable(1, 2);
        scoreTable.setWidth("100%");

        XWPFTableRow row = scoreTable.getRow(0);

        // Cell 1: Điểm
        XWPFTableCell c1 = row.getCell(0);
        c1.setWidth("35%");
        XWPFParagraph p1 = c1.getParagraphArray(0);
        p1.setAlignment(ParagraphAlignment.CENTER);
        p1.setSpacingAfter(40);
        XWPFRun rHead = p1.createRun();
        rHead.setFontFamily("Times New Roman");
        rHead.setFontSize(12);
        rHead.setBold(true);
        rHead.setUnderline(UnderlinePatterns.SINGLE);
        rHead.setText("Điểm");

        XWPFParagraph pWord = c1.addParagraph();
        pWord.setSpacingAfter(40);
        XWPFRun rWord = pWord.createRun();
        rWord.setFontFamily("Times New Roman");
        rWord.setFontSize(11);
        rWord.setText("Bằng chữ:.......................");

        XWPFParagraph pNum = c1.addParagraph();
        pNum.setSpacingAfter(40);
        XWPFRun rNum = pNum.createRun();
        rNum.setFontFamily("Times New Roman");
        rNum.setFontSize(11);
        rNum.setText("Bằng số:.........................");

        // Cell 2: Giám thị
        XWPFTableCell c2 = row.getCell(1);
        c2.setWidth("65%");
        XWPFParagraph p2 = c2.getParagraphArray(0);
        p2.setSpacingAfter(60);
        XWPFRun rCoi = p2.createRun();
        rCoi.setFontFamily("Times New Roman");
        rCoi.setFontSize(11);
        rCoi.setText("Giáo viên coi thi:......................................................................................");

        XWPFParagraph pCham = c2.addParagraph();
        pCham.setSpacingAfter(40);
        XWPFRun rCham = pCham.createRun();
        rCham.setFontFamily("Times New Roman");
        rCham.setFontSize(11);
        rCham.setText("Giáo viên chấm thi:...................................................................................");
    }

    private void renderDocxOptions(XWPFDocument document, List<QuestionOption> options) {
        if (options == null || options.isEmpty()) return;

        boolean allShort = options.stream().allMatch(o -> (o.getOptionText() != null ? o.getOptionText().length() : 0) <= 22);

        if (allShort && options.size() <= 4) {
            // Render on 1 line spaced
            XWPFParagraph optPara = document.createParagraph();
            optPara.setSpacingAfter(40);
            optPara.setIndentationLeft(280);

            for (QuestionOption opt : options) {
                XWPFRun rKey = optPara.createRun();
                rKey.setFontFamily("Times New Roman");
                rKey.setFontSize(12);
                rKey.setBold(true);
                rKey.setText(opt.getOptionKey() + ". ");

                XWPFRun rText = optPara.createRun();
                rText.setFontFamily("Times New Roman");
                rText.setFontSize(12);
                rText.setText(opt.getOptionText() + "          ");
            }
        } else {
            // Render 2 columns table
            XWPFTable optTable = document.createTable();
            optTable.setWidth("95%");
            optTable.removeBorders();

            for (int i = 0; i < options.size(); i += 2) {
                XWPFTableRow row = (i == 0) ? optTable.getRow(0) : optTable.createRow();
                QuestionOption opt1 = options.get(i);
                QuestionOption opt2 = (i + 1 < options.size()) ? options.get(i + 1) : null;

                XWPFTableCell c1 = row.getCell(0);
                c1.setWidth("50%");
                XWPFParagraph p1 = c1.getParagraphArray(0);
                p1.setSpacingAfter(30);
                XWPFRun rKey1 = p1.createRun();
                rKey1.setFontFamily("Times New Roman");
                rKey1.setFontSize(12);
                rKey1.setBold(true);
                rKey1.setText(opt1.getOptionKey() + ". ");
                XWPFRun rText1 = p1.createRun();
                rText1.setFontFamily("Times New Roman");
                rText1.setFontSize(12);
                rText1.setText(opt1.getOptionText() != null ? opt1.getOptionText() : "");

                XWPFTableCell c2 = (row.getTableCells().size() > 1) ? row.getCell(1) : row.createCell();
                c2.setWidth("50%");
                if (opt2 != null) {
                    XWPFParagraph p2 = c2.getParagraphArray(0);
                    p2.setSpacingAfter(30);
                    XWPFRun rKey2 = p2.createRun();
                    rKey2.setFontFamily("Times New Roman");
                    rKey2.setFontSize(12);
                    rKey2.setBold(true);
                    rKey2.setText(opt2.getOptionKey() + ". ");
                    XWPFRun rText2 = p2.createRun();
                    rText2.setFontFamily("Times New Roman");
                    rText2.setFontSize(12);
                    rText2.setText(opt2.getOptionText() != null ? opt2.getOptionText() : "");
                }
            }
        }
    }

    private void renderDocxAnswerKeyPage(
            XWPFDocument document,
            Exam exam,
            ExamPaperExportParams params,
            List<ExamQuestion> mcQuestions,
            List<ExamQuestion> essayQuestions
    ) {
        XWPFParagraph pageBreakPara = document.createParagraph();
        XWPFRun breakRun = pageBreakPara.createRun();
        breakRun.addBreak(BreakType.PAGE);

        XWPFParagraph headerPara = document.createParagraph();
        headerPara.setAlignment(ParagraphAlignment.CENTER);
        headerPara.setSpacingAfter(40);
        XWPFRun hRun = headerPara.createRun();
        hRun.setFontFamily("Times New Roman");
        hRun.setFontSize(14);
        hRun.setBold(true);
        hRun.setText("HƯỚNG DẪN CHẤM & ĐÁP ÁN ĐỀ THI");

        XWPFParagraph subPara = document.createParagraph();
        subPara.setAlignment(ParagraphAlignment.CENTER);
        subPara.setSpacingAfter(140);
        XWPFRun sRun = subPara.createRun();
        sRun.setFontFamily("Times New Roman");
        sRun.setFontSize(12);
        sRun.setItalic(true);
        sRun.setText("Môn: " + (exam.getSubject() != null ? exam.getSubject().getName() : "") + " - " + params.getEffectiveAcademicYear());

        // 1. Multiple Choice Answers Table
        if (!mcQuestions.isEmpty()) {
            XWPFParagraph mcTitle = document.createParagraph();
            mcTitle.setSpacingAfter(60);
            XWPFRun mcTitleRun = mcTitle.createRun();
            mcTitleRun.setFontFamily("Times New Roman");
            mcTitleRun.setFontSize(12);
            mcTitleRun.setBold(true);
            mcTitleRun.setText("1. ĐÁP ÁN PHẦN TRẮC NGHIỆM & ĐIỀN ĐÁP ÁN:");

            int chunkSize = 10;
            for (int chunkStart = 0; chunkStart < mcQuestions.size(); chunkStart += chunkSize) {
                int chunkEnd = Math.min(chunkStart + chunkSize, mcQuestions.size());
                int numItems = chunkEnd - chunkStart;
                int cols = numItems + 1;

                XWPFTable ansTable = document.createTable(2, cols);
                ansTable.setWidth("100%");

                XWPFTableRow r0 = ansTable.getRow(0);
                r0.getCell(0).setText("Câu");
                for (int i = 0; i < numItems; i++) {
                    r0.getCell(i + 1).setText(String.valueOf(chunkStart + i + 1));
                }

                XWPFTableRow r1 = ansTable.getRow(1);
                r1.getCell(0).setText("Đáp án");
                for (int i = 0; i < numItems; i++) {
                    ExamQuestion eq = mcQuestions.get(chunkStart + i);
                    List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(eq.getQuestion().getId());
                    String correct = getCorrectAnswerDisplay(eq.getQuestion(), options);
                    r1.getCell(i + 1).setText(correct);
                }

                if (chunkEnd < mcQuestions.size()) {
                    addEmptyParagraph(document, 4);
                }
            }
        }

        // 2. Essay Explanation
        if (!essayQuestions.isEmpty()) {
            XWPFParagraph eTitle = document.createParagraph();
            eTitle.setSpacingBefore(120);
            eTitle.setSpacingAfter(60);
            XWPFRun eTitleRun = eTitle.createRun();
            eTitleRun.setFontFamily("Times New Roman");
            eTitleRun.setFontSize(12);
            eTitleRun.setBold(true);
            eTitleRun.setText("2. HƯỚNG DẪN CHẤM PHẦN TỰ LUẬN:");

            int eIdx = 1;
            for (ExamQuestion eq : essayQuestions) {
                Question q = eq.getQuestion();
                XWPFParagraph eItem = document.createParagraph();
                eItem.setSpacingBefore(40);
                eItem.setSpacingAfter(20);

                XWPFRun rKey = eItem.createRun();
                rKey.setFontFamily("Times New Roman");
                rKey.setFontSize(12);
                rKey.setBold(true);
                rKey.setText("Bài " + eIdx + " (" + formatMarks(eq.getMarks()) + " điểm): ");

                XWPFRun rExp = eItem.createRun();
                rExp.setFontFamily("Times New Roman");
                rExp.setFontSize(12);
                String expl = (q.getExplanation() != null && !q.getExplanation().trim().isEmpty())
                        ? q.getExplanation().trim()
                        : "Học sinh thực hiện đúng các bước và ghi rõ kết quả đạt điểm tối đa.";
                rExp.setText(expl);
                eIdx++;
            }
        }
    }

    private void addEmptyParagraph(XWPFDocument document, int spacingAfter) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(spacingAfter);
    }

    private boolean isFillInOrShortAnswer(Question q, List<QuestionOption> options) {
        if (q != null && (q.getQuestionType() == QuestionType.SHORT_ANSWER || q.getQuestionType() == QuestionType.FILL_IN_THE_BLANK)) {
            return true;
        }
        if (options != null && options.size() == 1) {
            String key = options.get(0).getOptionKey();
            if (key != null && (key.equalsIgnoreCase("ANS") || key.equalsIgnoreCase("ANSWER") || key.equalsIgnoreCase("DA"))) {
                return true;
            }
        }
        if (options != null && !options.isEmpty()) {
            return options.stream().allMatch(o -> o.getOptionKey() != null &&
                    (o.getOptionKey().equalsIgnoreCase("ANS") || o.getOptionKey().equalsIgnoreCase("ANSWER")));
        }
        return false;
    }

    private String getCorrectAnswerDisplay(Question q, List<QuestionOption> options) {
        if (options == null || options.isEmpty()) return "-";
        QuestionOption correctOpt = options.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                .findFirst()
                .orElse(options.get(0));

        if (isFillInOrShortAnswer(q, options)) {
            return correctOpt.getOptionText() != null && !correctOpt.getOptionText().trim().isEmpty()
                    ? correctOpt.getOptionText().trim()
                    : "-";
        }
        return correctOpt.getOptionKey() != null ? correctOpt.getOptionKey().trim() : "-";
    }

    private String formatMarks(BigDecimal marks) {
        if (marks == null) return "0";
        if (marks.scale() <= 0 || marks.stripTrailingZeros().scale() <= 0) {
            return String.valueOf(marks.intValue());
        }
        return marks.stripTrailingZeros().toPlainString();
    }

    private BaseFont getBaseFont(boolean bold, boolean italic) {
        try {
            String fontPath = bold ? "C:/Windows/Fonts/timesbd.ttf" : (italic ? "C:/Windows/Fonts/timesi.ttf" : "C:/Windows/Fonts/times.ttf");
            if (new File(fontPath).exists()) {
                return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
            String arialPath = bold ? "C:/Windows/Fonts/arialbd.ttf" : "C:/Windows/Fonts/arial.ttf";
            if (new File(arialPath).exists()) {
                return BaseFont.createFont(arialPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
            return BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            try {
                return BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception ex) {
                throw new RuntimeException("Font creation failed", ex);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportQuestionsDocx(QuestionPaperExportRequest request, UUID userId) {
        if (request == null) {
            request = new QuestionPaperExportRequest();
        }
        ExamPaperExportParams params = request.getEffectiveParams();
        List<Question> questions = resolveQuestionsForExport(request, userId);

        if ((params.getSubjectName() == null || params.getSubjectName().isBlank()) && !questions.isEmpty()) {
            if (questions.get(0).getSubject() != null) {
                params.setSubjectName(questions.get(0).getSubject().getName());
            }
        }

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            setDocxPageMargins(document);

            // 1. Header 2 columns table
            renderDocxHeaderTable(document, null, params);

            addEmptyParagraph(document, 4);

            // 2. Student Info line
            XWPFParagraph infoPara = document.createParagraph();
            infoPara.setSpacingAfter(100);
            XWPFRun infoRun = infoPara.createRun();
            infoRun.setFontFamily("Times New Roman");
            infoRun.setFontSize(12);
            infoRun.setText("Họ và tên: ................................................................................................ Lớp: .................................");

            // 3. Scoring & Examiner Box
            renderDocxScoringTable(document);

            addEmptyParagraph(document, 8);

            // Classify questions
            List<Question> mcQuestions = new ArrayList<>();
            List<Question> essayQuestions = new ArrayList<>();

            for (Question q : questions) {
                if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE || q.getQuestionType() == QuestionType.TRUE_FALSE) {
                    mcQuestions.add(q);
                } else {
                    List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
                    if (options != null && !options.isEmpty()) {
                        mcQuestions.add(q);
                    } else {
                        essayQuestions.add(q);
                    }
                }
            }

            renderDocxQuestionsBody(document, mcQuestions, essayQuestions, params);

            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting questions docx: ", e);
            throw new RuntimeException("Không thể tạo file Word câu hỏi: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportQuestionsPdf(QuestionPaperExportRequest request, UUID userId) {
        if (request == null) {
            request = new QuestionPaperExportRequest();
        }
        ExamPaperExportParams params = request.getEffectiveParams();
        List<Question> questions = resolveQuestionsForExport(request, userId);

        if ((params.getSubjectName() == null || params.getSubjectName().isBlank()) && !questions.isEmpty()) {
            if (questions.get(0).getSubject() != null) {
                params.setSubjectName(questions.get(0).getSubject().getName());
            }
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(PageSize.A4, 40, 40, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = getBaseFont(false, false);
            BaseFont baseFontBold = getBaseFont(true, false);
            BaseFont baseFontItalic = getBaseFont(false, true);

            Font fontTitle = new Font(baseFontBold, 13, Font.NORMAL);
            Font fontSub = new Font(baseFont, 11, Font.NORMAL);
            Font fontSubBold = new Font(baseFontBold, 11, Font.NORMAL);
            Font fontRegular = new Font(baseFont, 11, Font.NORMAL);
            Font fontBold = new Font(baseFontBold, 11, Font.NORMAL);
            Font fontItalic = new Font(baseFontItalic, 10, Font.NORMAL);

            // 1. Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{40f, 60f});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            if (params.getDepartmentName() != null && !params.getDepartmentName().trim().isEmpty()) {
                Paragraph pDept = new Paragraph(params.getDepartmentName().trim().toUpperCase(), fontSub);
                pDept.setAlignment(Element.ALIGN_CENTER);
                leftCell.addElement(pDept);
            }

            Paragraph pInst = new Paragraph(params.getEffectiveInstitutionName(), fontTitle);
            pInst.setAlignment(Element.ALIGN_CENTER);
            leftCell.addElement(pInst);

            Paragraph pDash = new Paragraph("-------------", fontSub);
            pDash.setAlignment(Element.ALIGN_CENTER);
            leftCell.addElement(pDash);

            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            String defTitle = "BỘ CÂU HỎI ÔN TẬP";
            Paragraph pTitle = new Paragraph(params.getEffectiveExamTitle(defTitle), fontTitle);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(pTitle);

            Paragraph pYear = new Paragraph(params.getEffectiveAcademicYear(), fontSub);
            pYear.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(pYear);

            String subName = (params.getSubjectName() != null && !params.getSubjectName().trim().isEmpty())
                    ? params.getSubjectName().trim()
                    : (questions.get(0).getSubject() != null ? questions.get(0).getSubject().getName() : "Chung");
            Paragraph pSub = new Paragraph("Môn : " + subName, fontSubBold);
            pSub.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(pSub);

            int duration = params.getDurationMinutes() != null ? params.getDurationMinutes() : 40;
            Paragraph pTime = new Paragraph("Thời gian làm bài: ( " + duration + " phút)", fontSubBold);
            pTime.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(pTime);

            headerTable.addCell(rightCell);
            document.add(headerTable);

            // 2. Student Info line
            Paragraph pStudent = new Paragraph("Họ và tên: ..................................................................................... Lớp: .............................", fontRegular);
            pStudent.setSpacingBefore(10);
            pStudent.setSpacingAfter(8);
            document.add(pStudent);

            // 3. Scoring Box
            PdfPTable scoreTable = new PdfPTable(2);
            scoreTable.setWidthPercentage(100);
            scoreTable.setWidths(new float[]{35f, 65f});

            PdfPCell c1 = new PdfPCell();
            c1.setPadding(8);
            Paragraph pScoreHead = new Paragraph("Điểm", fontBold);
            pScoreHead.setAlignment(Element.ALIGN_CENTER);
            c1.addElement(pScoreHead);
            scoreTable.addCell(c1);

            PdfPCell c2 = new PdfPCell();
            c2.setPadding(8);
            c2.addElement(new Paragraph("Giáo viên coi thi: .................................................................................", fontRegular));
            c2.addElement(new Paragraph(" ", fontRegular));
            c2.addElement(new Paragraph("Giáo viên chấm thi: ...............................................................................", fontRegular));
            scoreTable.addCell(c2);

            document.add(scoreTable);

            // Classify questions
            List<Question> mcQuestions = new ArrayList<>();
            List<Question> essayQuestions = new ArrayList<>();

            for (Question q : questions) {
                if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE || q.getQuestionType() == QuestionType.TRUE_FALSE) {
                    mcQuestions.add(q);
                } else {
                    List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
                    if (options != null && !options.isEmpty()) {
                        mcQuestions.add(q);
                    } else {
                        essayQuestions.add(q);
                    }
                }
            }

            renderPdfQuestionsBody(document, mcQuestions, essayQuestions, params, fontTitle, fontBold, fontRegular, fontItalic);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting questions pdf: ", e);
            throw new RuntimeException("Không thể tạo file PDF câu hỏi: " + e.getMessage(), e);
        }
    }

    private List<Question> resolveQuestionsForExport(QuestionPaperExportRequest request, UUID userId) {
        List<Question> questions = new ArrayList<>();

        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            Map<UUID, Question> map = questionRepository.findAllById(request.getQuestionIds()).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
            for (UUID qId : request.getQuestionIds()) {
                Question q = map.get(qId);
                if (q != null && !q.isDeleted()) {
                    if (SecurityUtils.isStudent() && q.getStatus() != com.nqd.nqd_lms_be.entity.enums.QuestionStatus.APPROVED) {
                        continue;
                    }
                    questions.add(q);
                }
            }
        } else if (request.getSubjectId() != null) {
            List<Question> list = questionRepository.findBySubjectId(request.getSubjectId());
            for (Question q : list) {
                if (!q.isDeleted()) {
                    if (request.getGradeLevel() != null && !request.getGradeLevel().equalsIgnoreCase("ALL")
                            && !request.getGradeLevel().equalsIgnoreCase(q.getGradeLevel())) {
                        continue;
                    }
                    if (request.getCategoryId() != null && (q.getCategory() == null || !q.getCategory().getId().equals(request.getCategoryId()))) {
                        continue;
                    }
                    if (SecurityUtils.isStudent() && q.getStatus() != com.nqd.nqd_lms_be.entity.enums.QuestionStatus.APPROVED) {
                        continue;
                    }
                    questions.add(q);
                }
            }
        }

        if (questions.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy câu hỏi nào phù hợp để xuất tài liệu.");
        }

        return questions;
    }

    private void renderDocxQuestionsBody(
            XWPFDocument document,
            List<Question> mcQuestions,
            List<Question> essayQuestions,
            ExamPaperExportParams params
    ) {
        BigDecimal mcMarks = mcQuestions.stream()
                .map(q -> q.getDefaultMarks() != null ? q.getDefaultMarks() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal essayMarks = essayQuestions.stream()
                .map(q -> q.getDefaultMarks() != null ? q.getDefaultMarks() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int currentQuestionNumber = 1;

        if (!mcQuestions.isEmpty()) {
            XWPFParagraph secAPara = document.createParagraph();
            secAPara.setSpacingBefore(120);
            secAPara.setSpacingAfter(60);
            XWPFRun secARun = secAPara.createRun();
            secARun.setFontFamily("Times New Roman");
            secARun.setFontSize(13);
            secARun.setBold(true);
            secARun.setUnderline(UnderlinePatterns.SINGLE);
            secARun.setText("A. Trắc nghiệm : ( " + formatMarks(mcMarks) + " điểm)");

            XWPFParagraph notePara = document.createParagraph();
            notePara.setSpacingAfter(120);
            XWPFRun noteRun = notePara.createRun();
            noteRun.setFontFamily("Times New Roman");
            noteRun.setFontSize(12);
            noteRun.setBold(true);
            noteRun.setText(params.getEffectiveInstructionNote());

            for (Question q : mcQuestions) {
                List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());

                XWPFParagraph qPara = document.createParagraph();
                qPara.setSpacingBefore(80);
                qPara.setSpacingAfter(40);

                XWPFRun qTitleRun = qPara.createRun();
                qTitleRun.setFontFamily("Times New Roman");
                qTitleRun.setFontSize(12);
                qTitleRun.setBold(true);
                qTitleRun.setText("Câu " + currentQuestionNumber + " : ");

                XWPFRun qContentRun = qPara.createRun();
                qContentRun.setFontFamily("Times New Roman");
                qContentRun.setFontSize(12);
                qContentRun.setText(q.getContent() != null ? q.getContent() : "");

                if (isFillInOrShortAnswer(q, options)) {
                    String content = q.getContent() != null ? q.getContent().trim() : "";
                    boolean hasDots = content.contains("...") || content.contains("…") || content.contains(".....");
                    if (!hasDots) {
                        XWPFParagraph ansLinePara = document.createParagraph();
                        ansLinePara.setSpacingBefore(20);
                        ansLinePara.setSpacingAfter(30);
                        ansLinePara.setIndentationLeft(280);
                        XWPFRun rAns = ansLinePara.createRun();
                        rAns.setFontFamily("Times New Roman");
                        rAns.setFontSize(11);
                        rAns.setItalic(true);
                        rAns.setText("Đáp số: ................................................................");
                    }
                } else {
                    renderDocxOptions(document, options);
                }

                currentQuestionNumber++;
            }
        }

        if (!essayQuestions.isEmpty()) {
            XWPFParagraph secBPara = document.createParagraph();
            secBPara.setSpacingBefore(180);
            secBPara.setSpacingAfter(80);
            XWPFRun secBRun = secBPara.createRun();
            secBRun.setFontFamily("Times New Roman");
            secBRun.setFontSize(13);
            secBRun.setBold(true);
            secBRun.setUnderline(UnderlinePatterns.SINGLE);
            secBRun.setText("B. Tự luận : ( " + formatMarks(essayMarks) + " điểm)");

            int essayIndex = 1;
            for (Question q : essayQuestions) {
                BigDecimal qMarks = q.getDefaultMarks() != null ? q.getDefaultMarks() : BigDecimal.ONE;

                XWPFParagraph ePara = document.createParagraph();
                ePara.setSpacingBefore(100);
                ePara.setSpacingAfter(60);

                XWPFRun eTitleRun = ePara.createRun();
                eTitleRun.setFontFamily("Times New Roman");
                eTitleRun.setFontSize(12);
                eTitleRun.setBold(true);
                eTitleRun.setText("Bài " + essayIndex + " . ( " + formatMarks(qMarks) + " điểm) . ");

                XWPFRun eContentRun = ePara.createRun();
                eContentRun.setFontFamily("Times New Roman");
                eContentRun.setFontSize(12);
                eContentRun.setText(q.getContent() != null ? q.getContent() : "");

                int numDottedLines = (q.getQuestionType() == QuestionType.ESSAY) ? 5 : 2;
                if (q.getQuestionType() == QuestionType.ESSAY) {
                    XWPFParagraph workPara = document.createParagraph();
                    workPara.setSpacingAfter(40);
                    XWPFRun workRun = workPara.createRun();
                    workRun.setFontFamily("Times New Roman");
                    workRun.setFontSize(11);
                    workRun.setItalic(true);
                    workRun.setText("Bài làm:");
                }

                for (int line = 0; line < numDottedLines; line++) {
                    XWPFParagraph dotPara = document.createParagraph();
                    dotPara.setSpacingAfter(60);
                    XWPFRun dotRun = dotPara.createRun();
                    dotRun.setFontFamily("Times New Roman");
                    dotRun.setFontSize(11);
                    dotRun.setText("...................................................................................................................................................................");
                }

                essayIndex++;
                currentQuestionNumber++;
            }
        }

        addEmptyParagraph(document, 12);
        XWPFParagraph endPara = document.createParagraph();
        endPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun endRun = endPara.createRun();
        endRun.setFontFamily("Times New Roman");
        endRun.setFontSize(11);
        endRun.setBold(true);
        endRun.setText("----------------- HẾT -----------------");

        XWPFParagraph subEndPara = document.createParagraph();
        subEndPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun subEndRun = subEndPara.createRun();
        subEndRun.setFontFamily("Times New Roman");
        subEndRun.setFontSize(10);
        subEndRun.setItalic(true);
        subEndRun.setText("(Cán bộ coi thi không giải thích gì thêm)");
    }

    private void renderPdfQuestionsBody(
            com.lowagie.text.Document document,
            List<Question> mcQuestions,
            List<Question> essayQuestions,
            ExamPaperExportParams params,
            Font fontTitle,
            Font fontBold,
            Font fontRegular,
            Font fontItalic
    ) throws DocumentException {
        BigDecimal mcMarks = mcQuestions.stream()
                .map(q -> q.getDefaultMarks() != null ? q.getDefaultMarks() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal essayMarks = essayQuestions.stream()
                .map(q -> q.getDefaultMarks() != null ? q.getDefaultMarks() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int qNum = 1;

        if (!mcQuestions.isEmpty()) {
            Paragraph pSecA = new Paragraph("A. Trắc nghiệm : ( " + formatMarks(mcMarks) + " điểm)", fontTitle);
            pSecA.setSpacingBefore(12);
            pSecA.setSpacingAfter(4);
            document.add(pSecA);

            Paragraph pNote = new Paragraph(params.getEffectiveInstructionNote(), fontBold);
            pNote.setSpacingAfter(8);
            document.add(pNote);

            for (Question q : mcQuestions) {
                List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());

                Paragraph pQ = new Paragraph();
                pQ.setSpacingBefore(6);
                pQ.setSpacingAfter(3);
                pQ.add(new Chunk("Câu " + qNum + " : ", fontBold));
                pQ.add(new Chunk(q.getContent() != null ? q.getContent() : "", fontRegular));
                document.add(pQ);

                if (isFillInOrShortAnswer(q, options)) {
                    String content = q.getContent() != null ? q.getContent().trim() : "";
                    boolean hasDots = content.contains("...") || content.contains("…") || content.contains(".....");
                    if (!hasDots) {
                        Paragraph pAns = new Paragraph("Đáp số: ................................................................", fontItalic);
                        pAns.setIndentationLeft(20);
                        pAns.setSpacingBefore(2);
                        pAns.setSpacingAfter(4);
                        document.add(pAns);
                    }
                } else if (options != null && !options.isEmpty()) {
                    boolean allShort = options.stream().allMatch(o -> (o.getOptionText() != null ? o.getOptionText().length() : 0) <= 20);
                    if (allShort && options.size() <= 4) {
                        Paragraph pOpts = new Paragraph();
                        pOpts.setIndentationLeft(20);
                        for (QuestionOption opt : options) {
                            pOpts.add(new Chunk(opt.getOptionKey() + ". ", fontBold));
                            pOpts.add(new Chunk((opt.getOptionText() != null ? opt.getOptionText() : "") + "      ", fontRegular));
                        }
                        document.add(pOpts);
                    } else {
                        PdfPTable optTable = new PdfPTable(2);
                        optTable.setWidthPercentage(95);
                        optTable.setHorizontalAlignment(Element.ALIGN_LEFT);
                        for (QuestionOption opt : options) {
                            PdfPCell optCell = new PdfPCell();
                            optCell.setBorder(Rectangle.NO_BORDER);
                            optCell.setPaddingBottom(3);
                            Paragraph pCell = new Paragraph();
                            pCell.add(new Chunk(opt.getOptionKey() + ". ", fontBold));
                            pCell.add(new Chunk(opt.getOptionText() != null ? opt.getOptionText() : "", fontRegular));
                            optCell.addElement(pCell);
                            optTable.addCell(optCell);
                        }
                        if (options.size() % 2 != 0) {
                            PdfPCell empty = new PdfPCell();
                            empty.setBorder(Rectangle.NO_BORDER);
                            optTable.addCell(empty);
                        }
                        document.add(optTable);
                    }
                }

                qNum++;
            }
        }

        if (!essayQuestions.isEmpty()) {
            Paragraph pSecB = new Paragraph("B. Tự luận : ( " + formatMarks(essayMarks) + " điểm)", fontTitle);
            pSecB.setSpacingBefore(14);
            pSecB.setSpacingAfter(4);
            document.add(pSecB);

            int essayIdx = 1;
            for (Question q : essayQuestions) {
                BigDecimal qMarks = q.getDefaultMarks() != null ? q.getDefaultMarks() : BigDecimal.ONE;

                Paragraph pE = new Paragraph();
                pE.setSpacingBefore(8);
                pE.setSpacingAfter(4);
                pE.add(new Chunk("Bài " + essayIdx + " . ( " + formatMarks(qMarks) + " điểm) . ", fontBold));
                pE.add(new Chunk(q.getContent() != null ? q.getContent() : "", fontRegular));
                document.add(pE);

                int numDottedLines = (q.getQuestionType() == QuestionType.ESSAY) ? 5 : 2;
                if (q.getQuestionType() == QuestionType.ESSAY) {
                    Paragraph pWork = new Paragraph("Bài làm:", fontItalic);
                    pWork.setSpacingBefore(2);
                    pWork.setSpacingAfter(2);
                    document.add(pWork);
                }

                for (int line = 0; line < numDottedLines; line++) {
                    Paragraph pDot = new Paragraph("...................................................................................................................................................................", fontRegular);
                    pDot.setSpacingAfter(4);
                    document.add(pDot);
                }

                essayIdx++;
                qNum++;
            }
        }

        Paragraph pEnd = new Paragraph("----------------- HẾT -----------------", fontBold);
        pEnd.setAlignment(Element.ALIGN_CENTER);
        pEnd.setSpacingBefore(16);
        pEnd.setSpacingAfter(2);
        document.add(pEnd);

        Paragraph pSubEnd = new Paragraph("(Cán bộ coi thi không giải thích gì thêm)", fontItalic);
        pSubEnd.setAlignment(Element.ALIGN_CENTER);
        document.add(pSubEnd);
    }
}
