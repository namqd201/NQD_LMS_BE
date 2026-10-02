package com.nqd.nqd_lms_be.service.slide;

import com.nqd.nqd_lms_be.dto.slide.GenerateSlideRequest;
import com.nqd.nqd_lms_be.dto.slide.LessonSlideResponse;
import com.nqd.nqd_lms_be.entity.enums.SlideTargetType;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface LessonSlideService {

    LessonSlideResponse getSlide(SlideTargetType targetType, UUID targetId, UUID currentUserId);

    LessonSlideResponse generateSlideWithAi(GenerateSlideRequest request, UUID currentUserId);

    LessonSlideResponse uploadSlideFile(SlideTargetType targetType, UUID targetId, MultipartFile file, UUID currentUserId);

    void deleteSlide(UUID slideId, UUID currentUserId);

    byte[] exportPptx(SlideTargetType targetType, UUID targetId);
}
