# Implementation Status: Phase 1 — Authentication (Google Login & Logout)

## Overview
Phase 1 implements a production-grade **Google OAuth 2.0 Login & Logout** system using Spring Security. The system supports automated user provisioning, account linking via `OAuthAccount`, role preservation from the database, secure session invalidation on logout, and safe user data representation.

---

## 1. Files Created & Modified

### Modified Files:
- [`build.gradle`](file:///f:/Project/NQD_LMS_BE/build.gradle): Added `spring-boot-starter-oauth2-client` and `h2` test dependency.
- [`application.properties`](file:///f:/Project/NQD_LMS_BE/src/main/resources/application.properties): Configured Google OAuth2 registration (`client-id`, `client-secret`, `scope`, `redirect-uri`) and PostgreSQL datasource.

### Created Files:
- [`.env.example`](file:///f:/Project/NQD_LMS_BE/.env.example): Environment variable template for Google credentials and database connection.
- [`src/test/resources/application.properties`](file:///f:/Project/NQD_LMS_BE/src/test/resources/application.properties): H2 in-memory test database and mock OAuth configuration.
- [`UserRepository.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/repository/UserRepository.java): Repository for User entity.
- [`RoleRepository.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/repository/RoleRepository.java): Repository for Role entity.
- [`OAuthAccountRepository.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/repository/OAuthAccountRepository.java): Repository for OAuthAccount entity.
- [`UserRoleRepository.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/repository/UserRoleRepository.java): Repository for UserRole entity.
- [`AuthUserResponse.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/dto/AuthUserResponse.java): Safe DTO returning user info and assigned roles.
- [`MessageResponse.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/dto/MessageResponse.java): Generic response DTO for message feedback.
- [`AppUserPrincipal.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/config/security/AppUserPrincipal.java): Security principal implementing `OAuth2User`, `OidcUser`, containing user info and authorities.
- [`CustomOAuth2UserService.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/config/security/CustomOAuth2UserService.java): Custom OAuth2 user service for Google OAuth2 login.
- [`CustomOidcUserService.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/config/security/CustomOidcUserService.java): Custom OIDC user service for OpenID Connect Google login.
- [`SecurityConfig.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/config/SecurityConfig.java): Spring Security filter chain configuration.
- [`AuthService.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/service/AuthService.java): Authentication service interface.
- [`AuthServiceImpl.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/service/AuthServiceImpl.java): Authentication service implementation.
- [`AuthController.java`](file:///f:/Project/NQD_LMS_BE/src/main/java/com/nqd/nqd_lms_be/controller/AuthController.java): Authentication REST controller.
- [`AuthControllerTest.java`](file:///f:/Project/NQD_LMS_BE/src/test/java/com/nqd/nqd_lms_be/controller/AuthControllerTest.java): MockMvc tests for authentication endpoints and logout.
- [`AuthServiceTest.java`](file:///f:/Project/NQD_LMS_BE/src/test/java/com/nqd/nqd_lms_be/service/AuthServiceTest.java): Unit/Integration tests for Google login, provisioning, and role preservation.
- [`SecurityConfigTest.java`](file:///f:/Project/NQD_LMS_BE/src/test/java/com/nqd/nqd_lms_be/config/SecurityConfigTest.java): Tests for endpoint access and authorization rules.

---

## 2. Authentication Flow

```
[User Browser]
       |
       | 1. GET /oauth2/authorization/google
       v
[Spring Security OAuth2]
       |
       | 2. Redirect to Google OAuth Consent
       v
[Google Identity Platform]
       |
       | 3. Callback /login/oauth2/code/google with Auth Code
       v
[CustomOAuth2UserService / CustomOidcUserService]
       |
       | 4. Fetch Google User Profile (sub, email, name, picture)
       v
[AuthServiceImpl.processGoogleLogin]
       |
       +---> Check OAuthAccount table by (provider='GOOGLE', provider_user_id=sub)
       |        |-- Found: Update lastLoginAt, avatarUrl, fullName (if missing)
       |
       +---> If not found: Check User table by email
       |        |-- Found: Link new OAuthAccount record, update login info
       |
       +---> If not found in User table:
       |        |-- Provision new User entity (status: ACTIVE)
       |        |-- Assign default 'STUDENT' role via UserRole table
       |        |-- Create OAuthAccount record
       |
       +---> Load existing roles from database (UserRole -> Role)
       |        |-- Preserves all assigned roles (TEACHER, ADMIN, etc.)
       |        |-- Never trusts external/frontend role claims
       |
       v
[AppUserPrincipal created & stored in SecurityContext]
```

---

## 3. Endpoints Implemented

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/oauth2/authorization/google` | `permitAll()` | Redirects to Google login |
| `GET` | `/login/oauth2/code/google` | `permitAll()` | Google OAuth2 callback endpoint |
| `GET` | `/api/v1/auth/me` | `authenticated()` | Returns current user profile (`AuthUserResponse`) |
| `POST` | `/api/v1/auth/logout` | `authenticated()` | Invalidates session and clears `SecurityContext` |
| `GET` | `/v3/api-docs/**`, `/swagger-ui/**` | `permitAll()` | OpenAPI Documentation |

---

## 4. Test Coverage & Verification

All automated tests passed successfully with H2 in-memory DB:

1. **Authentication Configuration Loading**: `NqdLmsBeApplicationTests.contextLoads()` (Passed)
2. **Unauthenticated Access to `/auth/me`**: Returns 401 Unauthorized (Passed)
3. **Authenticated Access to `/auth/me`**: Returns 200 OK with `AuthUserResponse` (Passed)
4. **Logout State Invalidation**: `POST /api/v1/auth/logout` invalidates session and clears context (Passed)
5. **No Duplicate User Creation**: Subsequent logins for the same Google user update existing records without creating duplicates (Passed)
6. **New User Provisioning**: New Google users receive `ACTIVE` status, default `STUDENT` role, and an `OAuthAccount` entry (Passed)
7. **Role Preservation**: Existing roles (e.g. `TEACHER`, `ADMIN`) are strictly preserved (Passed)
8. **Privilege Escalation Protection**: External/rogue role claims in Google tokens are ignored; users never arbitrarily receive `ADMIN` (Passed)

---

## 5. Build Status
- `./gradlew test`: **BUILD SUCCESSFUL** (11 tests passed, 0 failed, 0 skipped)
- `./gradlew build`: **BUILD SUCCESSFUL** (All tasks UP-TO-DATE / Executed cleanly)
