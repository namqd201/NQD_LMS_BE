# NQD-LMS Usage Limits & Concurrency Safety

## 1. Concurrency Protection Design

To guarantee that users cannot bypass quotas (e.g. sending 10 concurrent requests when only 1 remaining quota is available), NQD-LMS employs a robust multi-layered synchronization strategy:

### Layer 1: Database Pessimistic Row Locking (`SELECT ... FOR UPDATE`)
- In `UserUsageRepository.java`:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM UserUsageRecord u WHERE u.user.id = :userId AND u.featureKey = :featureKey AND u.periodKey = :periodKey")
  Optional<UserUsageRecord> findByUserIdAndFeatureKeyAndPeriodKeyWithLock(UUID userId, FeatureKey featureKey, String periodKey);
  ```
- Ensures serialization of quota evaluation and counter increments for an existing usage record.

### Layer 2: Concurrent Insert Conflict Recovery Loop
- If no record exists for the current period (e.g. first request of the day/week), concurrent threads might attempt simultaneous `INSERT`s.
- Handled gracefully in `MembershipEntitlementServiceImpl.java` using a retry loop catching `DataIntegrityViolationException` / `OptimisticLockException` and immediately acquiring the pessimistic lock on the newly inserted row.

### Layer 3: Database Unique Constraint
- Unique constraint on table `user_usage_records (user_id, feature_key, period_key)` prevents duplicate counters.

---

## 2. Period Formats & Key Generation

The system computes partition period keys based on the feature's `UsagePeriodType`:

- **DAILY**: `DAILY_YYYY-MM-DD` (e.g. `DAILY_2026-09-06`)
- **WEEKLY**: `WEEKLY_YYYY-Www` (e.g. `WEEKLY_2026-W36` based on ISO week dates)
- **MONTHLY**: `MONTHLY_YYYY-MM` (e.g. `MONTHLY_2026-09`)
- **TOTAL**: `TOTAL`

Counters reset naturally on period boundary changes without requiring batch deletion jobs.

---

## 3. Endpoints for Usage Status

### GET `/api/v1/membership/usage`
Returns current usage against plan limits for the authenticated user.

**Response Example (Student)**:
```json
{
  "planCode": "FREE_STUDENT",
  "planName": "Gói Học sinh Cơ bản",
  "vip": false,
  "features": [
    {
      "featureKey": "AI_TUTOR",
      "featureName": "Trợ lý AI Gia sư",
      "allowed": true,
      "limit": 5,
      "used": 2,
      "remaining": 3,
      "periodKey": "DAILY_2026-09-06",
      "unit": "câu hỏi / ngày"
    },
    {
      "featureKey": "EXAM_LIMIT",
      "featureName": "Lượt làm bài thi",
      "allowed": true,
      "limit": 3,
      "used": 1,
      "remaining": 2,
      "periodKey": "WEEKLY_2026-W36",
      "unit": "đề thi / tuần"
    }
  ]
}
```
