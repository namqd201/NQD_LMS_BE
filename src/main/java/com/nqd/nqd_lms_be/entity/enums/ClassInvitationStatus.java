package com.nqd.nqd_lms_be.entity.enums;

public enum ClassInvitationStatus {
    PENDING,    // Đã gửi email mời, đang chờ học sinh đăng ký / chấp nhận
    ACCEPTED,   // Học sinh đã tạo tài khoản và tự động gia nhập lớp
    CANCELLED,  // Giáo viên thu hồi lời mời
    EXPIRED     // Lời mời hết hạn
}
