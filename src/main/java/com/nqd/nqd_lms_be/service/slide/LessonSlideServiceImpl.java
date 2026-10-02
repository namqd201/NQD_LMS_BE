package com.nqd.nqd_lms_be.service.slide;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.ai.GeminiDirectAIProvider;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.slide.GenerateSlideRequest;
import com.nqd.nqd_lms_be.dto.slide.LessonSlideResponse;
import com.nqd.nqd_lms_be.dto.slide.SlideItemDto;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import com.nqd.nqd_lms_be.entity.enums.SlideTargetType;
import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeLesson;
import com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeLessonRepository;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonSlideServiceImpl implements LessonSlideService {

    private final LessonSlideRepository lessonSlideRepository;
    private final LessonRepository lessonRepository;
    private final ClassroomMaterialRepository classroomMaterialRepository;
    private final KnowledgeLessonRepository knowledgeLessonRepository;
    private final UserRepository userRepository;
    private final MembershipEntitlementService membershipEntitlementService;
    private final GeminiDirectAIProvider geminiDirectAIProvider;

    private static final String UPLOAD_SLIDES_DIR = "uploads/slides";
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    @Transactional(readOnly = true)
    public LessonSlideResponse getSlide(SlideTargetType targetType, UUID targetId, UUID currentUserId) {
        LessonSlide slide = lessonSlideRepository.findByTargetTypeAndTargetId(targetType, targetId)
                .orElse(null);

        boolean canManage = canUserManageLesson(targetType, targetId, currentUserId);

        if (slide == null) {
            return LessonSlideResponse.builder()
                    .targetType(targetType)
                    .targetId(targetId)
                    .title(getLessonTitle(targetType, targetId))
                    .slideCount(0)
                    .canManage(canManage)
                    .slides(Collections.emptyList())
                    .build();
        }

        List<SlideItemDto> items = parseSlideItems(slide.getSlideContentJson());
        enrichGeometricSvgIfMissing(items);

        return LessonSlideResponse.builder()
                .id(slide.getId())
                .targetType(slide.getTargetType())
                .targetId(slide.getTargetId())
                .title(slide.getTitle())
                .slideUrl(slide.getSlideUrl())
                .fileName(slide.getFileName())
                .slideContentJson(slide.getSlideContentJson())
                .slides(items)
                .slideSource(slide.getSlideSource())
                .slideCount(slide.getSlideCount() != null ? slide.getSlideCount() : items.size())
                .creatorId(slide.getCreatorId())
                .canManage(canManage)
                .createdAt(slide.getCreatedAt())
                .updatedAt(slide.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public LessonSlideResponse generateSlideWithAi(GenerateSlideRequest request, UUID currentUserId) {
        // Enforce RBAC & Pro entitlement
        enforceAiGenerationPermission(currentUserId);

        SlideTargetType targetType = request.getTargetType();
        UUID targetId = request.getTargetId();

        String lessonTitle = getLessonTitle(targetType, targetId);
        String lessonContent = getLessonContent(targetType, targetId);

        int count = request.getSlideCount() != null && request.getSlideCount() >= 4 && request.getSlideCount() <= 15
                ? request.getSlideCount()
                : 8;

        List<SlideItemDto> generatedSlides = callAiToGenerateSlideItems(lessonTitle, lessonContent, count, request.getStyle());

        String json;
        try {
            json = objectMapper.writeValueAsString(generatedSlides);
        } catch (Exception e) {
            log.error("Failed to serialize generated slide items: {}", e.getMessage());
            json = "[]";
        }

        LessonSlide slide = lessonSlideRepository.findByTargetTypeAndTargetId(targetType, targetId)
                .orElseGet(() -> LessonSlide.builder()
                        .targetType(targetType)
                        .targetId(targetId)
                        .build());

        slide.setTitle(lessonTitle);
        slide.setSlideContentJson(json);
        slide.setSlideCount(generatedSlides.size());
        slide.setSlideSource("AI_GENERATED");
        slide.setFileName(null);
        slide.setCreatorId(currentUserId);
        slide.setSlideUrl("/api/v1/slides/download?targetType=" + targetType.name() + "&targetId=" + targetId);

        LessonSlide saved = lessonSlideRepository.save(slide);

        return LessonSlideResponse.builder()
                .id(saved.getId())
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .title(saved.getTitle())
                .slideUrl(saved.getSlideUrl())
                .slideContentJson(saved.getSlideContentJson())
                .slides(generatedSlides)
                .slideSource(saved.getSlideSource())
                .slideCount(saved.getSlideCount())
                .creatorId(saved.getCreatorId())
                .canManage(true)
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public LessonSlideResponse uploadSlideFile(SlideTargetType targetType, UUID targetId, MultipartFile file, UUID currentUserId) {
        if (!canUserManageLesson(targetType, targetId, currentUserId)) {
            throw new ForbiddenOperationException("Bạn không có quyền tải lên slide cho bài học này.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được để trống.");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "slide.pptx";
        String ext = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = originalFilename.substring(dotIdx).toLowerCase();
        }

        if (!ext.equals(".pptx") && !ext.equals(".ppt") && !ext.equals(".pdf")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file trình chiếu định dạng .pptx, .ppt hoặc tài liệu .pdf");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_SLIDES_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String storedFilename = UUID.randomUUID() + ext;
            Path targetLocation = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/api/v1/public/slides/files/" + storedFilename;
            String lessonTitle = getLessonTitle(targetType, targetId);

            LessonSlide slide = lessonSlideRepository.findByTargetTypeAndTargetId(targetType, targetId)
                    .orElseGet(() -> LessonSlide.builder()
                            .targetType(targetType)
                            .targetId(targetId)
                            .build());

            slide.setTitle(lessonTitle);
            slide.setSlideUrl(fileUrl);
            slide.setFileName(originalFilename);
            slide.setSlideSource("MANUAL_UPLOAD");
            slide.setCreatorId(currentUserId);
            // In manual upload, if there was no JSON, keep existing or provide simple card
            if (slide.getSlideContentJson() == null || slide.getSlideContentJson().isBlank()) {
                SlideItemDto singlePage = SlideItemDto.builder()
                        .slideNumber(1)
                        .title(lessonTitle)
                        .subtitle("File trình chiếu đính kèm: " + originalFilename)
                        .layout("TITLE")
                        .bulletPoints(List.of("File trình chiếu do giáo viên tải lên: " + originalFilename, "Nhấn 'Tải về Slide' để mở xem toàn bộ bài thuyết trình."))
                        .build();
                slide.setSlideContentJson(objectMapper.writeValueAsString(List.of(singlePage)));
                slide.setSlideCount(1);
            }

            LessonSlide saved = lessonSlideRepository.save(slide);

            return LessonSlideResponse.builder()
                    .id(saved.getId())
                    .targetType(saved.getTargetType())
                    .targetId(saved.getTargetId())
                    .title(saved.getTitle())
                    .slideUrl(saved.getSlideUrl())
                    .fileName(saved.getFileName())
                    .slideContentJson(saved.getSlideContentJson())
                    .slides(parseSlideItems(saved.getSlideContentJson()))
                    .slideSource(saved.getSlideSource())
                    .slideCount(saved.getSlideCount())
                    .creatorId(saved.getCreatorId())
                    .canManage(true)
                    .createdAt(saved.getCreatedAt())
                    .updatedAt(saved.getUpdatedAt())
                    .build();

        } catch (IOException e) {
            log.error("Failed to save uploaded slide file: {}", e.getMessage(), e);
            throw new RuntimeException("Lưu file slide thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteSlide(UUID slideId, UUID currentUserId) {
        LessonSlide slide = lessonSlideRepository.findById(slideId)
                .orElseThrow(() -> new ResourceNotFoundException("LessonSlide", slideId));

        if (!canUserManageLesson(slide.getTargetType(), slide.getTargetId(), currentUserId)) {
            throw new ForbiddenOperationException("Bạn không có quyền xóa slide này.");
        }

        lessonSlideRepository.delete(slide);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPptx(SlideTargetType targetType, UUID targetId) {
        LessonSlide slide = lessonSlideRepository.findByTargetTypeAndTargetId(targetType, targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Slide not found for " + targetType + " " + targetId));

        List<SlideItemDto> items = parseSlideItems(slide.getSlideContentJson());
        if (items.isEmpty()) {
            items = createFallbackSlideItems(slide.getTitle(), "Nội dung bài học NQD-LMS", 6);
        }

        return buildPptxPresentation(slide.getTitle(), items);
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private void enforceAiGenerationPermission(UUID userId) {
        if (SecurityUtils.isAdmin()) {
            return; // Admin always permitted
        }

        if (SecurityUtils.isTeacher()) {
            boolean isPro = false;
            try {
                var sub = membershipEntitlementService.getEffectiveSubscription(userId);
                if (sub != null && sub.getStatus() == SubscriptionStatus.ACTIVE) {
                    isPro = true;
                }
            } catch (Exception ignored) {}

            if (!isPro) {
                try {
                    var plan = membershipEntitlementService.getEffectivePlan(userId);
                    if (plan != null && (plan.getPrice().compareTo(BigDecimal.ZERO) > 0
                            || plan.hasFeature(FeatureKey.AI_SLIDE_GENERATION)
                            || plan.hasFeature(FeatureKey.AI_EXAM_GENERATION))) {
                        isPro = true;
                    }
                } catch (Exception ignored) {}
            }

            if (isPro) {
                return;
            }

            throw new ForbiddenOperationException("Chức năng tạo Slide tự động bằng AI yêu cầu tài khoản Quản trị viên hoặc Giáo viên có Gói Hội viên PRO.");
        }

        throw new ForbiddenOperationException("Bạn không có quyền sử dụng tính năng tạo slide bằng AI.");
    }

    private boolean canUserManageLesson(SlideTargetType targetType, UUID targetId, UUID userId) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        if (userId == null) {
            return false;
        }

        try {
            switch (targetType) {
                case COURSE_LESSON -> {
                    Lesson lesson = lessonRepository.findById(targetId).orElse(null);
                    if (lesson != null && lesson.getChapter() != null && lesson.getChapter().getCourse() != null) {
                        return lesson.getChapter().getCourse().getCreator() != null
                                && lesson.getChapter().getCourse().getCreator().getId().equals(userId);
                    }
                }
                case CLASSROOM_MATERIAL -> {
                    ClassroomMaterial mat = classroomMaterialRepository.findById(targetId).orElse(null);
                    if (mat != null && mat.getClassroom() != null) {
                        return mat.getClassroom().getTeacher() != null
                                && mat.getClassroom().getTeacher().getId().equals(userId);
                    }
                }
                case KNOWLEDGE_LESSON -> {
                    // Knowledge lessons can be managed by teachers or admin
                    return SecurityUtils.isTeacher();
                }
            }
        } catch (Exception e) {
            log.warn("Error checking lesson management permission: {}", e.getMessage());
        }
        return false;
    }

    private String getLessonTitle(SlideTargetType targetType, UUID targetId) {
        switch (targetType) {
            case COURSE_LESSON -> {
                return lessonRepository.findById(targetId)
                        .map(Lesson::getTitle)
                        .orElse("Bài học khóa học");
            }
            case CLASSROOM_MATERIAL -> {
                return classroomMaterialRepository.findById(targetId)
                        .map(ClassroomMaterial::getTitle)
                        .orElse("Bài giảng lớp học");
            }
            case KNOWLEDGE_LESSON -> {
                return knowledgeLessonRepository.findById(targetId)
                        .map(KnowledgeLesson::getTitle)
                        .orElse("Kiến thức cơ bản");
            }
        }
        return "Bài học";
    }

    private String getLessonContent(SlideTargetType targetType, UUID targetId) {
        switch (targetType) {
            case COURSE_LESSON -> {
                return lessonRepository.findById(targetId)
                        .map(l -> (l.getSummary() != null ? l.getSummary() + "\n\n" : "") + (l.getContent() != null ? l.getContent() : ""))
                        .orElse("");
            }
            case CLASSROOM_MATERIAL -> {
                return classroomMaterialRepository.findById(targetId)
                        .map(m -> (m.getDescription() != null ? m.getDescription() + "\n\n" : "") + (m.getContent() != null ? m.getContent() : ""))
                        .orElse("");
            }
            case KNOWLEDGE_LESSON -> {
                return knowledgeLessonRepository.findById(targetId)
                        .map(k -> (k.getSummary() != null ? k.getSummary() + "\n\n" : "") + (k.getTheoryMarkdown() != null ? k.getTheoryMarkdown() : ""))
                        .orElse("");
            }
        }
        return "";
    }

    private List<SlideItemDto> callAiToGenerateSlideItems(String title, String content, int count, String style) {
        String systemPrompt = """
                Bạn là một chuyên gia sư phạm và thiết kế bài giảng trình chiếu chuyên nghiệp hàng đầu tại NQD-LMS.
                Nhiệm vụ của bạn là đọc kỹ nội dung bài học, sau đó trích xuất và thiết kế thành bộ Slide bài giảng logic, súc tích, trực quan, có tính sư phạm cao gồm đúng %d trang slide.
                
                QUY TẮC CẤU TRÚC SLIDE:
                - Slide 1: Trang bìa (layout: "TITLE") - Tiêu đề bài học, phụ đề dẫn nhập, lời chào.
                - Slide 2: Mục tiêu bài học (layout: "INTRO") - 3 đến 4 mục tiêu học sinh cần đạt được.
                - Slide 3 đến %d: Các đơn vị kiến thức trọng tâm (layout: "CONTENT" hoặc "SPLIT" hoặc "FORMULA") - mỗi slide tập trung vào 1 khái niệm, có 3-4 bullet points súc tích, nếu có công thức/code thì điền vào trường formula, ghi chú quan trọng vào callout.
                - Slide %d: Câu hỏi củng cố / Điểm lưu ý (layout: "KEYNOTE") - 2 đến 3 câu hỏi nhanh hoặc lưu ý tránh bẫy.
                - Slide %d: Tổng kết & Lời dặn (layout: "SUMMARY") - Tóm lược cốt lõi và bài tập/hành động tiếp theo.
                
                QUY TẮC MINH HỌA HÌNH ẢNH & HÌNH HỌC (RẤT QUAN TRỌNG):
                - Đối với bài học về HÌNH HỌC (Toán học, Vật lý...), các slide giảng về hình cụ thể (hình vuông, hình chữ nhật, hình tròn, tam giác, hình thoi, hình thang, hình bình hành, hình lập phương...), bạn CẦN cung cấp mã SVG trực quan hoặc mô tả rõ ràng để hệ thống vẽ đồ họa vector chính xác.
                - Nếu bạn xuất mã SVG, hãy điền vào trường `svgDiagram` (dạng thẻ `<svg viewBox="0 0 280 210" ...>...</svg>` với màu sắc sinh động, có chú thích đỉnh và kích thước). Nếu không tự tạo được SVG chuẩn, bạn có thể để null hoặc "", hệ thống NQD-LMS sẽ tự động nhận diện và vẽ hình học vector chuẩn mực tương ứng.
                - Đối với chủ đề khác cần ảnh minh họa, điền mô tả ảnh vào `imagePrompt`.
                
                ĐỊNH DẠNG ĐẦU RA BẮT BUỘC:
                Chỉ trả về DUY NHẤT một mảng JSON thuần túy (không bọc text giải thích bên ngoài, không thêm markdown codeblock thừa):
                [
                  {
                    "slideNumber": 1,
                    "title": "Tiêu đề trang slide",
                    "subtitle": "Phụ đề (nếu có)",
                    "layout": "TITLE",
                    "bulletPoints": ["Gạch đầu dòng 1", "Gạch đầu dòng 2"],
                    "formula": "Công thức toán học hoặc mã lệnh (nếu có)",
                    "callout": "Ghi chú/Điểm nhấn (nếu có)",
                    "speakerNotes": "Gợi ý lời giảng ngắn gọn cho người thuyết trình",
                    "svgDiagram": "<svg ...>...</svg> (mã SVG trực quan cho hình học nếu có, hoặc null)",
                    "imagePrompt": "Mô tả ảnh minh họa nếu cần tạo ảnh, hoặc null"
                  }
                ]
                Ngôn ngữ: Tiếng Việt sư phạm chuẩn mực, chuyên nghiệp.
                """.formatted(count, Math.max(3, count - 2), count - 1, count);

        String userPrompt = """
                Tiêu đề bài học: %s
                Phong cách mong muốn: %s
                Nội dung bài học:
                %s
                """.formatted(title, style != null ? style : "STANDARD", content.isBlank() ? title : content);

        try {
            if (geminiDirectAIProvider.isAvailable()) {
                String raw = geminiDirectAIProvider.generateChatResponse(systemPrompt, userPrompt, Collections.emptyList());
                if (raw != null && !raw.isBlank()) {
                    String cleanJson = raw.trim();
                    if (cleanJson.startsWith("```json")) {
                        cleanJson = cleanJson.substring(7);
                    } else if (cleanJson.startsWith("```")) {
                        cleanJson = cleanJson.substring(3);
                    }
                    if (cleanJson.endsWith("```")) {
                        cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                    }
                    cleanJson = cleanJson.trim();

                    List<SlideItemDto> parsed = objectMapper.readValue(cleanJson, new TypeReference<List<SlideItemDto>>() {});
                    if (parsed != null && !parsed.isEmpty()) {
                        // Normalize slide numbers & enrich missing geometric SVGs
                        for (int i = 0; i < parsed.size(); i++) {
                            parsed.get(i).setSlideNumber(i + 1);
                        }
                        enrichGeometricSvgIfMissing(parsed);
                        return parsed;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Gemini slide generation failed or unparseable, generating fallback slides: {}", e.getMessage());
        }

        return createFallbackSlideItems(title, content, count);
    }

    private List<SlideItemDto> createFallbackSlideItems(String title, String content, int count) {
        List<SlideItemDto> list = new ArrayList<>();
        list.add(SlideItemDto.builder()
                .slideNumber(1)
                .title(title)
                .subtitle("Hệ thống bài giảng thông minh • NQD-LMS Platform")
                .layout("TITLE")
                .bulletPoints(List.of(
                        "Bài giảng trực quan dành cho học viên",
                        "Biên soạn & Giảng dạy trực tuyến",
                        "NQD-LMS AI Learning System"
                ))
                .speakerNotes("Chào mừng các bạn đến với bài học " + title)
                .build());

        list.add(SlideItemDto.builder()
                .slideNumber(2)
                .title("Mục tiêu bài học")
                .subtitle("Yêu cầu cần đạt sau khi hoàn thành")
                .layout("INTRO")
                .bulletPoints(List.of(
                        "Nắm vững các khái niệm và định lý cơ bản",
                        "Phân tích và vận dụng linh hoạt vào các dạng bài tập thực tiễn",
                        "Rèn luyện tư duy logic và kỹ năng giải quyết vấn đề"
                ))
                .callout("Ghi nhớ: Hãy ghi chép cẩn thận các ý chính trong bài giảng!")
                .speakerNotes("Đây là mục tiêu chính chúng ta cần đạt được hôm nay.")
                .build());

        list.add(SlideItemDto.builder()
                .slideNumber(3)
                .title("Nội dung cốt lõi")
                .subtitle("Khái niệm & Định lý trọng tâm")
                .layout("CONTENT")
                .bulletPoints(List.of(
                        "Định nghĩa chuẩn xác và các tính chất kèm theo",
                        "Các bước phân tích dữ kiện đề bài",
                        "Phương pháp suy luận tối ưu và tránh sai sót thường gặp"
                ))
                .formula(content.length() > 50 ? content.substring(0, Math.min(100, content.length())) : "f(x) = y")
                .speakerNotes("Chúng ta bắt đầu đi sâu vào phần lý thuyết nền tảng.")
                .build());

        list.add(SlideItemDto.builder()
                .slideNumber(4)
                .title("Ví dụ minh họa & Phân tích")
                .subtitle("Áp dụng thực hành từng bước")
                .layout("SPLIT")
                .bulletPoints(List.of(
                        "Bước 1: Nhận diện dạng bài và điều kiện bài toán",
                        "Bước 2: Vận dụng công thức và biến đổi hợp lý",
                        "Bước 3: Kiểm tra nghiệm và kết luận bài toán"
                ))
                .callout("Lưu ý: Luôn kiểm tra điều kiện xác định trước khi tính toán!")
                .speakerNotes("Cùng xem qua ví dụ minh họa cụ thể để nắm rõ cách làm.")
                .build());

        list.add(SlideItemDto.builder()
                .slideNumber(5)
                .title("Điểm cần lưu ý & Củng cố")
                .subtitle("Tránh các lỗi sai phổ biến")
                .layout("KEYNOTE")
                .bulletPoints(List.of(
                        "Không bỏ sót các trường hợp đặc biệt",
                        "Thực hiện biến đổi cẩn thận, tránh nhầm lẫn dấu",
                        "Trình bày bài giải mạch lạc, rõ ràng từng bước"
                ))
                .callout("Mẹo: Đọc kỹ đề và vẽ sơ đồ/hình ảnh tóm tắt nếu cần thiết.")
                .speakerNotes("Hãy chú ý những bẫy thường gặp trong các kỳ thi.")
                .build());

        list.add(SlideItemDto.builder()
                .slideNumber(6)
                .title("Tổng kết & Bài tập tự luyện")
                .subtitle("Chúc các bạn học tập tốt!")
                .layout("SUMMARY")
                .bulletPoints(List.of(
                        "Ôn lại các công thức và ví dụ đã phân tích",
                        "Hoàn thành các bài tập tự luyện trong phần Bài tập",
                        "Trao đổi cùng Giảng viên hoặc AI Tutor nếu có thắc mắc"
                ))
                .speakerNotes("Cảm ơn các bạn đã lắng nghe bài giảng. Hẹn gặp lại ở bài học tiếp theo!")
                .build());

        enrichGeometricSvgIfMissing(list);
        return list;
    }

    private void enrichGeometricSvgIfMissing(List<SlideItemDto> items) {
        if (items == null) return;
        for (SlideItemDto item : items) {
            if ((item.getSvgDiagram() == null || item.getSvgDiagram().isBlank())
                    && (item.getImageUrl() == null || item.getImageUrl().isBlank())) {
                String bulletText = item.getBulletPoints() != null ? String.join(" ", item.getBulletPoints()) : "";
                String svg = GeometricSvgHelper.detectAndGenerateSvg(item.getTitle(), item.getSubtitle(), bulletText);
                if (svg != null) {
                    item.setSvgDiagram(svg);
                }
            }
        }
    }

    private List<SlideItemDto> parseSlideItems(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SlideItemDto>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse slideContentJson: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Build standard 16:9 widescreen PowerPoint presentation (.pptx)
     */
    private byte[] buildPptxPresentation(String presentationTitle, List<SlideItemDto> slides) {
        try (XMLSlideShow ppt = new XMLSlideShow();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Set 16:9 Widescreen dimensions: 960 x 540 pt
            ppt.setPageSize(new Dimension(960, 540));

            Color bgDark = new Color(15, 23, 42);      // Slate 900
            Color textWhite = new Color(248, 250, 252); // Slate 50
            Color accentGreen = new Color(131, 199, 93); // Brand Green #83C75D
            Color accentIndigo = new Color(99, 102, 241); // Indigo 500
            Color bgLight = new Color(248, 250, 252);   // Slate 50
            Color textDark = new Color(30, 41, 59);     // Slate 800
            Color textMuted = new Color(100, 116, 139); // Slate 500

            for (SlideItemDto item : slides) {
                XSLFSlide slide = ppt.createSlide();
                boolean isCoverOrSummary = "TITLE".equalsIgnoreCase(item.getLayout()) || "SUMMARY".equalsIgnoreCase(item.getLayout());

                // Slide Background Shape
                XSLFAutoShape bg = slide.createAutoShape();
                bg.setShapeType(ShapeType.RECT);
                bg.setAnchor(new Rectangle(0, 0, 960, 540));
                bg.setFillColor(isCoverOrSummary ? bgDark : bgLight);
                bg.setLineColor(isCoverOrSummary ? bgDark : bgLight);

                // Top Accent Bar
                XSLFAutoShape accentBar = slide.createAutoShape();
                accentBar.setShapeType(ShapeType.RECT);
                accentBar.setAnchor(new Rectangle(0, 0, 960, 8));
                accentBar.setFillColor(accentGreen);
                accentBar.setLineColor(accentGreen);

                if (isCoverOrSummary) {
                    // Cover Slide Layout
                    XSLFTextBox titleBox = slide.createTextBox();
                    titleBox.setAnchor(new Rectangle(60, 140, 840, 160));
                    XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
                    XSLFTextRun titleRun = titlePara.addNewTextRun();
                    titleRun.setText(item.getTitle() != null ? item.getTitle() : presentationTitle);
                    titleRun.setFontSize(34.0);
                    titleRun.setBold(true);
                    titleRun.setFontColor(textWhite);
                    titleRun.setFontFamily("Arial");

                    if (item.getSubtitle() != null && !item.getSubtitle().isBlank()) {
                        XSLFTextParagraph subPara = titleBox.addNewTextParagraph();
                        subPara.setSpaceBefore(12.0);
                        XSLFTextRun subRun = subPara.addNewTextRun();
                        subRun.setText(item.getSubtitle());
                        subRun.setFontSize(18.0);
                        subRun.setFontColor(accentGreen);
                        subRun.setFontFamily("Arial");
                    }

                    // Bullet points / highlights on cover/summary
                    if (item.getBulletPoints() != null && !item.getBulletPoints().isEmpty()) {
                        XSLFTextBox pointsBox = slide.createTextBox();
                        pointsBox.setAnchor(new Rectangle(60, 310, 840, 150));
                        for (String bp : item.getBulletPoints()) {
                            XSLFTextParagraph p = pointsBox.addNewTextParagraph();
                            p.setBullet(true);
                            p.setSpaceBefore(6.0);
                            XSLFTextRun r = p.addNewTextRun();
                            r.setText(bp);
                            r.setFontSize(14.0);
                            r.setFontColor(new Color(203, 213, 225));
                            r.setFontFamily("Arial");
                        }
                    }

                    // Branding badge at bottom
                    XSLFTextBox brandBox = slide.createTextBox();
                    brandBox.setAnchor(new Rectangle(60, 480, 840, 30));
                    XSLFTextParagraph brandPara = brandBox.addNewTextParagraph();
                    XSLFTextRun brandRun = brandPara.addNewTextRun();
                    brandRun.setText("NQD-LMS • Nền Tảng Học Tập & Quản Lý Giáo Dục Thông Minh");
                    brandRun.setFontSize(11.0);
                    brandRun.setFontColor(new Color(148, 163, 184));
                    brandRun.setFontFamily("Arial");

                } else {
                    // Content Slide Layout
                    // Title Box
                    XSLFTextBox headerBox = slide.createTextBox();
                    headerBox.setAnchor(new Rectangle(60, 35, 840, 70));
                    XSLFTextParagraph hPara = headerBox.addNewTextParagraph();
                    XSLFTextRun hRun = hPara.addNewTextRun();
                    hRun.setText(item.getTitle() != null ? item.getTitle() : "Nội dung bài học");
                    hRun.setFontSize(26.0);
                    hRun.setBold(true);
                    hRun.setFontColor(textDark);
                    hRun.setFontFamily("Arial");

                    if (item.getSubtitle() != null && !item.getSubtitle().isBlank()) {
                        XSLFTextParagraph subPara = headerBox.addNewTextParagraph();
                        XSLFTextRun subRun = subPara.addNewTextRun();
                        subRun.setText(item.getSubtitle());
                        subRun.setFontSize(13.0);
                        subRun.setFontColor(textMuted);
                        subRun.setFontFamily("Arial");
                    }

                    // Check if slide has diagram or image
                    boolean hasVisual = (item.getSvgDiagram() != null && !item.getSvgDiagram().isBlank())
                            || (item.getImageUrl() != null && !item.getImageUrl().isBlank());
                    ShapeType detectedPoiShape = GeometricSvgHelper.detectPoiShapeType(
                            item.getTitle(),
                            item.getSubtitle(),
                            item.getBulletPoints() != null ? String.join(" ", item.getBulletPoints()) : ""
                    );
                    if (detectedPoiShape != null) {
                        hasVisual = true;
                    }

                    int contentWidth = hasVisual ? 510 : 840;

                    // Content Box
                    XSLFTextBox contentBox = slide.createTextBox();
                    contentBox.setAnchor(new Rectangle(60, 115, contentWidth, 275));

                    if (item.getBulletPoints() != null) {
                        for (String bp : item.getBulletPoints()) {
                            XSLFTextParagraph p = contentBox.addNewTextParagraph();
                            p.setBullet(true);
                            p.setSpaceBefore(8.0);
                            XSLFTextRun r = p.addNewTextRun();
                            r.setText(bp);
                            r.setFontSize(15.0);
                            r.setFontColor(textDark);
                            r.setFontFamily("Arial");
                        }
                    }

                    // If hasVisual, draw diagram card on the right
                    if (hasVisual) {
                        int cardX = 590;
                        int cardY = 115;
                        int cardW = 310;
                        int cardH = 275;

                        // Visual card container
                        XSLFAutoShape cardBg = slide.createAutoShape();
                        cardBg.setShapeType(ShapeType.ROUND_RECT);
                        cardBg.setAnchor(new Rectangle(cardX, cardY, cardW, cardH));
                        cardBg.setFillColor(new Color(241, 245, 249)); // Slate 100
                        cardBg.setLineColor(new Color(203, 213, 225)); // Slate 300

                        // Header badge inside card
                        XSLFTextBox cardHeader = slide.createTextBox();
                        cardHeader.setAnchor(new Rectangle(cardX + 10, cardY + 8, cardW - 20, 25));
                        XSLFTextParagraph chp = cardHeader.addNewTextParagraph();
                        XSLFTextRun chr = chp.addNewTextRun();
                        chr.setText("📐 HÌNH MINH HỌA TRỰC QUAN");
                        chr.setFontSize(10.0);
                        chr.setBold(true);
                        chr.setFontColor(new Color(79, 70, 229)); // Indigo 600

                        // Render geometric shape in center of card if detected
                        ShapeType shapeToDraw = detectedPoiShape != null ? detectedPoiShape : ShapeType.ROUND_RECT;
                        XSLFAutoShape geoShape = slide.createAutoShape();
                        geoShape.setShapeType(shapeToDraw);
                        int shapeSize = 130;
                        int shapeX = cardX + (cardW - shapeSize) / 2;
                        int shapeY = cardY + 40;
                        geoShape.setAnchor(new Rectangle(shapeX, shapeY, shapeSize, shapeSize));
                        geoShape.setFillColor(new Color(224, 231, 255)); // Indigo 100
                        geoShape.setLineColor(new Color(79, 70, 229));  // Indigo 600

                        // Subtitle caption under shape
                        XSLFTextBox cardFooter = slide.createTextBox();
                        cardFooter.setAnchor(new Rectangle(cardX + 10, cardY + 185, cardW - 20, 80));
                        XSLFTextParagraph cfp = cardFooter.addNewTextParagraph();
                        XSLFTextRun cfr = cfp.addNewTextRun();
                        cfr.setText(item.getTitle() != null ? item.getTitle() : "Hình học trực quan");
                        cfr.setFontSize(12.0);
                        cfr.setBold(true);
                        cfr.setFontColor(textDark);

                        if (item.getFormula() != null && !item.getFormula().isBlank()) {
                            XSLFTextParagraph fmlaPara = cardFooter.addNewTextParagraph();
                            fmlaPara.setSpaceBefore(4.0);
                            XSLFTextRun fmlaRun = fmlaPara.addNewTextRun();
                            fmlaRun.setText(item.getFormula());
                            fmlaRun.setFontSize(11.0);
                            fmlaRun.setFontColor(new Color(22, 101, 52));
                            fmlaRun.setBold(true);
                        }
                    }

                    // Formula / Callout Box if present
                    if (item.getCallout() != null && !item.getCallout().isBlank()) {
                        XSLFAutoShape calloutCard = slide.createAutoShape();
                        calloutCard.setShapeType(ShapeType.ROUND_RECT);
                        calloutCard.setAnchor(new Rectangle(60, 400, 840, 60));
                        calloutCard.setFillColor(new Color(240, 253, 244)); // Emerald 50
                        calloutCard.setLineColor(accentGreen);

                        XSLFTextBox calloutText = slide.createTextBox();
                        calloutText.setAnchor(new Rectangle(70, 405, 820, 50));
                        XSLFTextParagraph cPara = calloutText.addNewTextParagraph();
                        XSLFTextRun cRun = cPara.addNewTextRun();
                        cRun.setText("💡 " + item.getCallout());
                        cRun.setFontSize(13.0);
                        cRun.setBold(true);
                        cRun.setFontColor(new Color(22, 101, 52)); // Emerald 800
                        cRun.setFontFamily("Arial");
                    }

                    // Footer with page number
                    XSLFTextBox footerBox = slide.createTextBox();
                    footerBox.setAnchor(new Rectangle(60, 490, 840, 30));
                    XSLFTextParagraph fPara = footerBox.addNewTextParagraph();
                    XSLFTextRun fRun = fPara.addNewTextRun();
                    fRun.setText("NQD-LMS | Slide " + (item.getSlideNumber() != null ? item.getSlideNumber() : "") + " / " + slides.size());
                    fRun.setFontSize(10.0);
                    fRun.setFontColor(textMuted);
                    fRun.setFontFamily("Arial");
                }
            }

            ppt.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to build PPTX file: {}", e.getMessage(), e);
            throw new RuntimeException("Tạo file PowerPoint thất bại: " + e.getMessage());
        }
    }
}
