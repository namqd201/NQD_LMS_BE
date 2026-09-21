# NQD-LMS Entitlement System & Access Control

## 1. Architecture

The Entitlement System is the single point of truth for verifying whether a user has permission to use a feature or has remaining quota before performing an action.

```
+-----------------------------------------------------------+
|                     Client Request                        |
+-----------------------------+-----------------------------+
                              |
                              v
+-----------------------------+-----------------------------+
|             Spring Security Filter Chain                  |
|             (Authentication & Role Check)                 |
+-----------------------------+-----------------------------+
                              |
                              v
+-----------------------------+-----------------------------+
|             Controllers & Application Services            |
|   (StudentAiTutorService, StudentExamService,             |
|    TeacherQuestionService, TeacherCourseService, etc.)    |
+-----------------------------+-----------------------------+
                              |
                              v
+-----------------------------+-----------------------------+
|             MembershipEntitlementService                  |
|   1. hasFeature(userId, featureKey)                       |
|   2. checkLimit(userId, featureKey)                       |
|   3. enforceAndConsumeUsage(userId, featureKey, amount)   |
+-----------------------------+-----------------------------+
                              |
                +-------------+-------------+
                |                           |
                v                           v
+---------------+---------------+  +--------+------------------+
|      Subscription & Plan      |  |  UserUsageRecord (DB)     |
|   (Active Plan / Default Free)|  |  (Pessimistic Lock &      |
|                               |  |   Concurrency Control)    |
+-------------------------------+  +---------------------------+
```

---

## 2. Core Service Methods

### `hasFeature(UUID userId, FeatureKey featureKey): boolean`
Checks if the user's active membership plan (or default free plan if none is active) explicitly enables the feature.

### `checkLimit(UUID userId, FeatureKey featureKey): UsageLimitCheckResult`
Returns quota inspection details without consuming usage:
- `allowed`: `boolean`
- `limit`: `Long` (null for unlimited)
- `used`: `long`
- `remaining`: `Long`
- `periodKey`: `String`

### `enforceAndConsumeUsage(UUID userId, FeatureKey featureKey, long amount): void`
Atomically evaluates quota and consumes usage in a single transaction. Throws `LimitExceededException` (HTTP 429) if the limit is exceeded.

---

## 3. Backend Enforcements Implemented

1. **Student AI Tutor Chat**:
   - Class: `StudentAiTutorServiceImpl.java`
   - Quota: `FeatureKey.AI_TUTOR`
   - Limit: 5 questions/day on Free Plan, Unlimited on VIP.
   - Throws: `LimitExceededException` when daily quota reached.

2. **Student Exam Attempts**:
   - Class: `StudentExamServiceImpl.java`
   - Quota: `FeatureKey.EXAM_LIMIT`
   - Limit: 3 exams/week on Free Plan, Unlimited on VIP.
   - Throws: `LimitExceededException` when weekly quota reached.

3. **Teacher Question Bank**:
   - Class: `TeacherQuestionServiceImpl.java`
   - Quota: `FeatureKey.QUESTION_BANK_LIMIT`
   - Limit: Max 50 questions total on Free Plan, Unlimited on Pro.
   - Throws: `LimitExceededException` when bank capacity reached.

4. **Teacher Class / Course Creation**:
   - Class: `TeacherCourseServiceImpl.java`
   - Quota: `FeatureKey.CLASS_LIMIT`
   - Limit: Max 2 classes on Free Plan, Unlimited on Pro.
   - Throws: `LimitExceededException` when class creation limit reached.

5. **Teacher AI Exam Generation**:
   - Class: `TeacherAiGenerationServiceImpl.java`
   - Feature: `FeatureKey.AI_EXAM_GENERATION`
   - Gate: Requires active Teacher Pro plan (`hasFeature`).
   - Throws: `AccessDeniedException` if teacher is on Free Plan.
