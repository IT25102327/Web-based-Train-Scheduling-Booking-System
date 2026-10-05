# Task Progress: IT25101520 — Dukshanth K.

**Module:** UC-01: Passenger Profile & Account Management  
**Role:** Passenger (End-User)  
**Priority:** 4  
**Package:** `com.trainbooking.it25101520`  
**Templates:** `src/main/resources/templates/it25101520/`  
**Tests:** `src/test/java/com/trainbooking/it25101520/`  

---

## ✅ DONE
- [x] **Use Case Specification (Lab 03)**: Documented UC-01 preconditions, postconditions, actors, main scenarios, extensions (password reset & PDF redownload).
- [x] **Activity Diagram (Lab 04)**: Modeled authentication decision nodes, password reset flow, and parallel profile options (history, favorites, contact details).
- [x] **Entity Models**:
  - `User.java`: Stores passenger credentials, encrypted BCrypt password, contact phone, role (`ROLE_PASSENGER`, `ROLE_COORDINATOR`, `ROLE_ADMIN`, `ROLE_STATION_STAFF`).
  - `FavoriteRoute.java`: Stores saved passenger origin-destination pairs.
- [x] **Repositories**:
  - `UserRepository.java`: Custom queries `findByEmail`, `existsByEmail`.
  - `FavoriteRouteRepository.java`: Custom query `findByUserId`.
- [x] **Data Transfer Objects (DTOs)**:
  - `LoginRequest.java`, `RegisterRequest.java`, `AuthResponse.java`, `UserProfileDto.java`, `FavoriteRouteDto.java`.
- [x] **Service Layer**:
  - `AuthService.java`: Full user registration logic with BCrypt hashing, duplicate email rejection with user-friendly messages, password reset dispatching.
  - `PassengerService.java`: Full profile update logic, favorite routes add/delete, user lookup by email/ID.
- [x] **Controller Layer**:
  - `AuthController.java`: Web endpoints for `/login` (with success banner on redirect), `/register` (with duplicate email error reporting), `/forgot-password`.
  - `PassengerController.java`: Web endpoints for `/profile`, `/passenger/profile`, `/favorites`, `/passenger/favorites/add`, `/passenger/favorites/{id}/delete`, `/booking-history`, `/passenger/bookings` linked to authenticated user.
- [x] **Thymeleaf UI Views**:
  - `it25101520/login.html`: Responsive login form with error & success alerts.
  - `it25101520/register.html`: Registration form with validation & duplicate email handling.
  - `it25101520/forgot-password.html`: Password recovery form with confirmation alert.
  - `it25101520/profile.html`: Profile detail editor and favorite routes manager.
  - `it25101520/booking-history.html`: Booking history with status badges and ticket view links.
- [x] **Unit Testing (JUnit 5 + Mockito)**:
  - `AuthServiceTest.java`: Verified registration success, duplicate email rejection, and password reset token creation/validation/resetting lifecycle.
  - `PassengerServiceTest.java`: Verified profile fetch, profile update, favorite route addition and listing.
- [x] **Phase 2 Deliverables**:
  - `PasswordResetToken.java` & `PasswordResetTokenRepository.java`: Cryptographic 30-minute self-service password reset tokens.
  - `it25101520/reset-password.html`: Dedicated reset password interface with match validation and security safeguards.
  - Quick-booking shortcuts linking saved favorite routes to search with 1-click URL parameters.
  - Enhanced booking history view with direct links to confirmed ticket inspection and PDF downloads.

- [x] **Phase 3 & Phase 4 Deliverables (Complete System)**:
  - Multi-Channel Travel Alert Preferences: `notifyByEmail`, `notifyBySms`, and `delayAlertThresholdMinutes` on `User.java` and `UserProfileDto.java`.
  - `PassengerService.java`: Profile update handling notification preferences and `getUserProfile(userId)`.
  - `profile.html`: Integrated Multi-Channel Travel Alerts card with checkboxes and delay threshold dropdown selector.
  - `booking-history.html`: Prominent one-click "Download PDF Ticket" and "View Ticket" buttons.
  - `PassengerServiceTest.java`: Unit tested preference updates, retrieval, and profile modifications.

- [x] **Phase 5 Deliverables (GoF Design Patterns — Template Method Pattern)**:
  - `AbstractUserRegistrationTemplate.java`: Defines the invariant user registration algorithm via `final register(RegisterRequest)` with primitive steps (`validateRequest`, `normalizeEmail`, `checkDuplicateAccount`, `buildUserEntity`, `encodePassword`, `persistUser`) and virtual extension hook (`postRegistrationHook`).
  - `PassengerRegistrationProcessor.java`: Concrete processor enforcing passenger validation, role assignment (`ROLE_PASSENGER`), BCrypt encoding, and persistence.
  - Integrated directly into `AuthService.java` for all passenger signups.
  - `PassengerRegistrationTemplateTest.java`: Verified invariant step execution, input validation failures, and duplicate prevention.

---

## ⏳ IN PROGRESS (Current Phase)
- Completed all planned phase deliverables.

---

## 📋 TODO (Upcoming Phases)
- [x] Multi-factor authentication (MFA) / SMS verification preference toggles.
- [x] Integration with UC-05 multi-channel alerting engine.
- [x] E-Ticket PDF download integration from booking history.
