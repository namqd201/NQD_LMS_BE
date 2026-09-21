package com.nqd.nqd_lms_be.dto;

import com.nqd.nqd_lms_be.entity.enums.TeacherApplicantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherApplicationRequest {

    @NotNull(message = "Vui lòng chọn loại đối tượng giảng dạy")
    private TeacherApplicantType applicantType;

    @NotBlank(message = "Vui lòng nhập họ và tên")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    private String phoneNumber;

    @NotBlank(message = "Vui lòng nhập email liên hệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập tên trường hoặc đơn vị công tác")
    private String institutionName;

    @NotBlank(message = "Vui lòng nhập chuyên ngành hoặc môn giảng dạy")
    private String majorOrSubject;

    private String bio;

    private List<String> documentUrls;

    private String idCardFrontUrl;

    private String idCardBackUrl;

    private String sampleVideoUrl;
}
