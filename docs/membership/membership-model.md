# NQD-LMS Membership & Subscription Model

## 1. Overview
The Membership & Subscription system is designed for both **Students** and **Teachers**, enabling commercial EdTech monetization with flexible plans, automatic subscription management, and quota-based feature access.

---

## 2. Membership Plans

### Student Plans
| Plan Code | Name | Price (VND) | Period | Exam Limit | AI Question Limit | Features |
|---|---|---|---|---|---|---|
| `FREE_STUDENT` | Gói Học sinh Cơ bản | 0 | LIFETIME | 3 / week | 5 / day | `AI_TUTOR`, `EXAM_LIMIT` |
| `VIP_STUDENT_MONTHLY` | VIP Học sinh Theo tháng | 99,000 | MONTHLY | Unlimited | Unlimited | `AI_TUTOR`, `PREMIUM_COURSES`, `PREMIUM_EXAMS`, `PDF_DOWNLOAD`, `VIDEO_HIGH_QUALITY`, `EXAM_LIMIT` |
| `VIP_STUDENT_YEARLY` | VIP Học sinh Theo năm | 799,000 | YEARLY | Unlimited | Unlimited | `AI_TUTOR`, `PREMIUM_COURSES`, `PREMIUM_EXAMS`, `PDF_DOWNLOAD`, `VIDEO_HIGH_QUALITY`, `EXAM_LIMIT` |

### Teacher Plans
| Plan Code | Name | Price (VND) | Period | Max Classes | Question Bank Limit | Features |
|---|---|---|---|---|---|---|
| `FREE_TEACHER` | Gói Giáo viên Cơ bản | 0 | LIFETIME | 5 classes | 100 questions | `CLASS_LIMIT`, `QUESTION_BANK_LIMIT` |
| `TEACHER_PRO_MONTHLY` | Giáo viên Pro Theo tháng | 149,000 | MONTHLY | Unlimited | Unlimited | `CLASS_LIMIT`, `QUESTION_BANK_LIMIT`, `AI_EXAM_GENERATION`, `ADVANCED_ANALYTICS` |
| `TEACHER_PRO_YEARLY` | Giáo viên Pro Theo năm | 1,200,000 | YEARLY | Unlimited | Unlimited | `CLASS_LIMIT`, `QUESTION_BANK_LIMIT`, `AI_EXAM_GENERATION`, `ADVANCED_ANALYTICS` |

All plan attributes (pricing, limits, active status, feature sets) are dynamically configurable in the database and manageable via Admin APIs.

---

## 3. Database Schema

### `membership_plans`
- `id` (UUID PK)
- `code` (VARCHAR unique)
- `name` (VARCHAR)
- `description` (TEXT)
- `user_type` (VARCHAR: `STUDENT`, `TEACHER`, `ALL`)
- `price` (NUMERIC)
- `currency` (VARCHAR: `VND`)
- `billing_period` (VARCHAR: `MONTHLY`, `YEARLY`, `LIFETIME`)
- `active` (BOOLEAN)
- `features` (TEXT / JSON string)
- `exam_limit_per_week` (INTEGER, null = unlimited)
- `ai_question_limit_per_day` (INTEGER, null = unlimited)
- `max_classes_limit` (INTEGER, null = unlimited)
- `max_questions_limit` (INTEGER, null = unlimited)

### `subscriptions`
- `id` (UUID PK)
- `user_id` (UUID FK -> users)
- `plan_id` (UUID FK -> membership_plans)
- `status` (VARCHAR: `ACTIVE`, `CANCELLED`, `EXPIRED`, `PENDING`)
- `start_at` (TIMESTAMP)
- `expires_at` (TIMESTAMP, null for lifetime)
- `cancelled_at` (TIMESTAMP)
- `auto_renew` (BOOLEAN)
- `payment_reference` (VARCHAR)

### `user_usage_records`
- `id` (UUID PK)
- `user_id` (UUID FK -> users)
- `feature_key` (VARCHAR: `AI_TUTOR`, `EXAM_LIMIT`, etc.)
- `period_key` (VARCHAR: e.g. `DAILY_2026-09-06`, `WEEKLY_2026-W36`)
- `used_count` (BIGINT)
- `last_used_at` (TIMESTAMP)
- Unique Index: `(user_id, feature_key, period_key)`

---

## 4. Lifecycle & Expiration Logic
1. **Purchase & Activation**: Triggered automatically upon order payment confirmation (`OrderStatus.PAID`) or admin manual assignment. Cancels previous active subscriptions of the same user.
2. **Expiration Enforcement**:
   - Real-time validity checks on every entitlement access (`subscription.getExpiresAt().isBefore(LocalDateTime.now())`).
   - Background Quartz / Spring Scheduler (`SubscriptionExpirationScheduler`) runs every 5 minutes:
     `UPDATE subscriptions SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at <= NOW()`
3. **Cancellation**: Users or Admins can cancel auto-renewal. The subscription remains `ACTIVE` until `expires_at`, after which it falls back to the default `FREE` plan.
