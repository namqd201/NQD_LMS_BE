# NQD-LMS Course Marketplace & Monetization Architecture

## 1. Overview
NQD-LMS Phase 3 introduces a decentralized **Course Marketplace** enabling Teachers to package, price, publish, and sell courses to Students with robust workflow governance, preview access controls, secure order-entitlement fulfillment, and peer reviews.

---

## 2. Course Pricing & Monetization Model

### Pricing Options
- **FREE**: `pricingType = CoursePricingType.FREE`, `price = 0`.
- **PAID**: `pricingType = CoursePricingType.PAID`, `price > 0`, optional `salePrice`, currency (default `VND`).

### Decoupling Course from Payment
- `Course` entity stores course content, structure, and merchandising metadata.
- `Product` entity (`ProductType.COURSE`) is automatically synchronized upon course approval.
- Billing and Payment systems only interact with `Product` / `Order` / `Entitlement`, ensuring zero coupling between core LMS logic and payment gateways.

---

## 3. Publish & Review Workflow

```
+-------------------------------------------------------------+
| Teacher creates Course (DRAFT)                              |
+------------------------------+------------------------------+
                               |
                               | Teacher clicks "Submit for Review"
                               v
+-------------------------------------------------------------+
| Course status: PENDING_REVIEW                               |
+------------------------------+------------------------------+
                               |
                +--------------+--------------+
                |                             |
                v                             v
+-------------------------------+ +---------------------------+
| Admin Approves                | | Admin Rejects             |
| status -> PUBLISHED           | | status -> REJECTED        |
| sets publishedAt = NOW()      | | sets rejectReason         |
| provisions Product in billing | | sends notification        |
+-------------------------------+ +---------------------------+
```

### Access Control Rules:
- Only courses with status `PUBLISHED` (or legacy `ACTIVE`) appear in the public marketplace and are accessible by students.
- `DRAFT`, `PENDING_REVIEW`, `REJECTED`, or `ARCHIVED` courses are hidden from the marketplace search and direct student access.
- Teachers can only view and manage their own courses.
- Admins are the sole authority to approve or reject courses submitted for review.

---

## 4. Course Preview (Học thử)

Teachers can mark specific lessons and lesson resources with `isPreview = true`.

### Preview Access Matrix:
| User State | Preview Lesson (`isPreview = true`) | Locked Lesson (`isPreview = false`) | Preview Resources | Locked Resources |
|---|---|---|---|---|
| **Guest / Unauthenticated** | View title & summary | Hidden / Locked | View / Stream | Blocked |
| **Unenrolled Student (Free Course)** | Full access | Full access | Full access | Full access |
| **Unenrolled Student (Paid Course)** | **Full access (Free Preview)** | **403 Forbidden ("Vui lòng mua khóa học")** | **Accessible** | **403 Forbidden** |
| **Enrolled / Purchased Student** | Full access | Full access | Full access | Full access |
| **Teacher (Owner) / Admin** | Full access | Full access | Full access | Full access |

---

## 5. Purchase & Entitlement Flow

```
1. Student clicks "Mua khóa học" (Marketplace)
   -> POST /api/v1/orders (productType: COURSE, targetEntityId: courseId)
   -> Order created with status: PENDING

2. Payment Gateway transaction (PayOS / VietQR / Bank Transfer)
   -> Student transfers funds or approves payment

3. Webhook received & verified
   -> Order status updated to PAID
   -> EntitlementActivationService.activateOrderFulfillment(order)
   -> Creates CourseEnrollment (status: ENROLLED)
   -> Creates Entitlement (entitlementType: COURSE_ACCESS, status: ACTIVE)

4. Instant Access Unlocked
   -> Student visits /courses/{id} or /student/courses
   -> All lessons and resources are unlocked immediately
```

---

## 6. Course Reviews & Ratings

### Rules:
1. **Purchase Verification**: Only students who have purchased the course or enrolled in the course can submit a review. Unenrolled students are blocked with HTTP 403.
2. **One Review per Student**: A student can submit at most 1 review per course. Subsequent reviews must be updates (`PUT /api/v1/marketplace/courses/{id}/reviews/{reviewId}`).
3. **Rating Aggregation**: Adding, updating, or deleting a review automatically recalculates the `averageRating` (rounded to 1 decimal place) and `reviewCount` directly on the `Course` entity.

---

## 7. Marketplace Search & Filter API

### Endpoint: `GET /api/v1/marketplace/courses`

| Parameter | Type | Description |
|---|---|---|
| `keyword` | `String` | Fuzzy search across course name, description, and course code |
| `subjectId` | `UUID` | Filter by Subject ID |
| `gradeLevel` | `String` | Filter by Grade (e.g. `10`, `11`, `12`) |
| `teacherId` | `UUID` | Filter by Teacher / Creator ID |
| `isFree` | `Boolean` | `true` for free courses, `false` for paid courses |
| `minPrice` | `BigDecimal` | Minimum price filter |
| `maxPrice` | `BigDecimal` | Maximum price filter |
| `minRating` | `Double` | Minimum average rating (e.g. `4.0` for 4+ stars) |
| `sortBy` | `String` | `newest`, `popular`, `rating`, `price_asc`, `price_desc` |
| `page` / `size` | `Integer` | Standard Spring Pageable pagination (default size: 12) |
