# Quy trình Rút tiền & Quản lý Tài khoản Ngân hàng (Payout & Withdrawal Flow)

## 1. Quản lý Tài khoản Ngân hàng (Teacher Bank Accounts)
- Giảng viên có thể lưu nhiều tài khoản ngân hàng nhận tiền.
- Hỗ trợ thiết lập tài khoản mặc định (`is_default = true`).
- Số tài khoản hiển thị trên UI và DTO được che mặt nạ bảo mật (VD: `******6789`).

## 2. Kiểm soát An toàn Đồng thời & Chống Rút Âm (Pessimistic Lock & Concurrency Safety)
- Khi nhận yêu cầu rút tiền (`POST /api/v1/teacher/finance/withdrawals`):
  1. **Idempotency Check**: Nếu có `idempotencyKey`, kiểm tra bản ghi rút tiền trùng lặp và trả về ngay nếu đã tồn tại.
  2. **Pessimistic Locking**: Sử dụng khóa hàng bi quan (`@Lock(LockModeType.PESSIMISTIC_WRITE)` trên `User`) để đồng bộ hóa và tuần tự hóa các yêu cầu rút tiền của cùng một giảng viên.
  3. **Tính toán số dư khả dụng thực tế**:
     \[
     AvailableBalance = \sum AvailableEarnings - \sum CommittedWithdrawals(PENDING, PROCESSING, COMPLETED)
     \]
  4. Nếu số tiền yêu cầu > số dư khả dụng, hệ thống từ chối ngay lập tức và ném lỗi `IllegalArgumentException` (400 Bad Request).
  5. Tạo bản ghi `TeacherWithdrawal` ở trạng thái `PENDING` với mã duy nhất (VD: `WTD_1788691234_A1B2C3D4`).

## 3. Quy trình Phê duyệt Payout của Ban quản trị (Admin Lifecycle)
1. Giảng viên gửi yêu cầu rút tiền: Trạng thái ban đầu = `PENDING`.
2. Admin xem danh sách và duyệt: `POST /api/v1/admin/finance/withdrawals/{id}/approve` -> Chuyển sang `PROCESSING`.
3. Admin thực hiện chuyển khoản ngân hàng qua kênh tài chính / SePay / VietQR.
4. Admin xác nhận hoàn tất kèm mã giao dịch ủy nhiệm chi: `POST /api/v1/admin/finance/withdrawals/{id}/complete` -> Chuyển sang `COMPLETED`, lưu `referenceCode` (VD: `FT2609068899`), `processedBy`, `processedAt`.
5. Nếu yêu cầu có sai sót / nghi vấn gian lận, Admin từ chối: `POST /api/v1/admin/finance/withdrawals/{id}/reject` kèm `rejectionReason` -> Chuyển sang `REJECTED`. Khi đó, số tiền bị khóa được giải phóng trở lại vào số dư khả dụng của giảng viên.