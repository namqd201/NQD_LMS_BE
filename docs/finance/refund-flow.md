# Quy trình Hoàn tiền & Đảo ngược Sổ cái (Refund & Reversal Flow)

## 1. Nguyên tắc Toàn vẹn Tài chính (Financial Integrity)
- Tuyệt đối không xóa bất kỳ bản ghi tài chính nào khi có khiếu nại hoặc hoàn tiền.
- Mọi thao tác hoàn tiền đều được ghi nhận bằng một chuỗi hành động nguyên tử (`@Transactional`):
  1. Chuyển trạng thái đơn hàng (`Order`) từ `PAID` sang `REFUNDED`.
  2. Đảo ngược toàn bộ các bản ghi thu nhập liên quan (`TeacherEarning`):
     - `status = REVERSED`
     - `reversed_at = NOW()`
     - `reversal_reason = [Lý do hoàn tiền]`
  3. Thu hồi quyền học tập (`Entitlement`):
     - `status = EXPIRED`
     - `valid_until = NOW()`
  4. Hủy đăng ký khóa học (`CourseEnrollment`):
     - Đánh dấu `is_deleted = true` hoặc cập nhật trạng thái hủy quyền truy cập.

## 2. Tác động đến Số dư của Giảng viên & Báo cáo Nền tảng
- Bản ghi có `status = REVERSED` tự động bị loại khỏi tổng thu nhập khả dụng (`availableBalance`) và tổng đã kiếm (`totalEarned`).
- Doanh thu gộp nền tảng và phí thu của nền tảng tự động trừ đi phần doanh thu bị hoàn trả.
- Báo cáo tài chính Admin cung cấp trường `reversedAmount` để theo dõi tổng số tiền đã xử lý hoàn cho học viên.