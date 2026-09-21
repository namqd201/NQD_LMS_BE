# Hệ thống Chia sẻ Doanh thu & Sổ cái Thu nhập (Revenue Sharing & Earning Ledger)

## 1. Nguyên tắc cốt lõi (Core Financial Principles)
- **Không bao giờ dùng phép cộng dồn biến động số dư (`balance += amount`)**: Mọi số dư (khả dụng, chờ duyệt, đã rút, hoàn tiền) được tính toán theo mô hình sổ cái bất biến (immutable double-entry ledger) từ bảng `teacher_earnings` và `teacher_withdrawals`.
- **Snapshot tỷ lệ hoa hồng (Commission Policy Snapshotting)**: Tỷ lệ phí nền tảng (`platform_fee_rate`, mặc định 20%) và tỷ lệ chia sẻ cho giảng viên (`teacher_share_rate`, mặc định 80%) được ghi lại vĩnh viễn trên từng dòng thu nhập (`teacher_earnings`) ngay tại thời điểm kích hoạt đơn hàng thành công. Nếu ban quản trị thay đổi chính sách hoa hồng trong tương lai, các thu nhập trong quá khứ tuyệt đối không bị tính toán lại.
- **Tính Idempotent & Chống tạo trùng**: Cột `order_item_id` trên `teacher_earnings` được ràng buộc duy nhất (`UNIQUE KEY`), đảm bảo một mặt hàng trong đơn hàng chỉ tạo ra một bản ghi doanh thu duy nhất dù webhook hay background worker có retry nhiều lần.

## 2. Công thức tính toán chia sẻ doanh thu
Với mỗi khóa học bán thành công với tổng giá trị \( Gross \):
- \( Fee_{platform} = Gross \times Rate_{platform} \) (làm tròn 2 chữ số thập phân, `RoundingMode.HALF_UP`)
- \( Income_{teacher} = Gross - Fee_{platform} \)

**Ví dụ:**
Khóa học bán giá **299,000 VND** với tỷ lệ hoa hồng nền tảng **20%** (`0.2000`):
- Doanh thu gộp (Gross): `299,000 VND`
- Phí nền tảng (Platform Fee): `59,800 VND`
- Thu nhập giảng viên (Teacher Amount): `239,200 VND`

## 3. Vòng đời Trạng thái Thu nhập (`EarningStatus`)
- `PENDING`: Đang trong thời gian giữ tiền tạm khóa (Hold Period / Escrow Window, mặc định 7 ngày).
- `AVAILABLE`: Đã qua thời gian tạm khóa hoặc được phê duyệt khả dụng cho việc rút tiền.
- `PAID`: Đã được thanh toán / chuyển tiền cho giảng viên.
- `REVERSED`: Bị đảo ngược / hoàn tiền khi đơn hàng bị hoàn (`REFUNDED`). Không xóa dữ liệu mà ghi nhận lý do đảo ngược (`reversal_reason`) và thời gian đảo ngược (`reversed_at`).