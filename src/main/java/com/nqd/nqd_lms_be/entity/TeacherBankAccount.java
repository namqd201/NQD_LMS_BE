package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "teacher_bank_accounts",
    indexes = {
        @Index(name = "idx_teacher_bank_accounts_teacher", columnList = "teacher_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class TeacherBankAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "bank_name", nullable = false, length = 255)
    private String bankName;

    @Column(name = "bank_code", nullable = false, length = 50)
    private String bankCode;

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false, length = 255)
    private String accountHolderName;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = true;

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        int len = accountNumber.length();
        return "*".repeat(len - 4) + accountNumber.substring(len - 4);
    }
}
