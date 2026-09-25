package com.nqd.nqd_lms_be.entity.enums;

public enum ClassEnrollmentStatus {
    ENROLLED,            // Đang theo học chính thức
    PENDING_APPROVAL,    // Học sinh xin vào bằng mã, đang chờ giáo viên duyệt
    INVITED,             // Giáo viên mời học sinh, đang chờ học sinh đồng ý
    REJECTED,            // Giáo viên từ chối học sinh
    DECLINED,            // Học sinh từ chối lời mời của giáo viên
    DROPPED              // Đã rời khỏi lớp học hoặc bị xóa khỏi lớp
}
