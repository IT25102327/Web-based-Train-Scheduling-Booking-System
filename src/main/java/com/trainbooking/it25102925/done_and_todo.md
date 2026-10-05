# Task Progress: IT25102925 — Sovis W.M.A.V.

**Module:** UC-05: Live Status & Automated Notification System  
**Roles:** Schedule Coordinator (Admin), GPS Tracking Engine  
**Secondary Actors:** Passenger (End-User), Email/SMS Gateway  
**Priority:** 4  
**Package:** `com.trainbooking.it25102925`  
**Templates:** `src/main/resources/templates/it25102925/`  
**Tests:** `src/test/java/com/trainbooking/it25102925/`  

---

## ✅ DONE
- [x] **Use Case Specification (Lab 03)**: Documented live status updating, affected passenger queries, delay alerts, and one-click rebooking.
- [x] **Activity Diagram (Lab 04)**: Modeled dual triggers (manual admin update vs automated GPS tracking deviation), station departure board refresh, cancellation checks, rebooking link generation, and multi-channel broadcast.
- [x] **Entity Models**:
  - `Notification.java`: Alert log with recipient relationship, type, subject, message, and read status.
- [x] **Repositories**:
  - `NotificationRepository.java`: Custom query methods `findByRecipientIdOrderBySentAtDesc` and unread counts.
- [x] **Data Transfer Objects (DTOs)**:
  - `NotificationDto.java`: Clean data contract for notification view rendering.
- [x] **Service Layer**:
  - `NotificationService.java`: Querying affected ticket holders on train schedule changes and dispatching alerts.
  - `EmailService.java`: Robust transactional email sending with fallback simulation logging.
- [x] **Controller Layer**:
  - `NotificationController.java`: Mapped `/admin/trains/status`, `/notifications/status`, and `/notifications/list`.
- [x] **Thymeleaf UI Views**:
  - `it25102925/list.html`: Interactive passenger notification center.
  - `it25102925/status.html`: Train status dispatcher with quick preset delay buttons.
- [x] **Unit Testing**:
  - `NotificationServiceTest.java`: Verified delay alert generation, cancellation alert with secure 1-click rebooking token generation, zero-cost schedule transfer, and user notification history queries.
  - `GpsSimulationTest.java`: Verified automated milestone deviation detection and alert triggering.
- [x] **Phase 2 Deliverables**:
  - `RebookingToken.java` & `RebookingTokenRepository.java`: Secure tokens generated whenever trains are cancelled.
  - `RebookingController.java` & `templates/it25102925/rebook.html`: Dedicated 1-Click Complimentary Rebooking portal (`/rebooking/claim/{token}`) transferring passengers to replacement trains for LKR 0.00.
  - `GpsSimulationService.java`: `@Scheduled(fixedRate = 60000)` background engine tracking milestone waypoints, detecting route deviations, auto-updating train statuses to DELAYED, and broadcasting passenger alerts.

- [x] **Phase 3 & Phase 4 Deliverables (Complete System)**:
  - Added `@Async notifyAffectedPassengersAsync()` background worker pool in `NotificationService.java`.
  - Multi-Channel Notification Dispatch engine respecting passenger profile preferences (`notifyByEmail`, `notifyBySms`).
  - Simulated SMS Gateway alert log dispatch (`[SMS GATEWAY SIMULATION] Transmitted SMS alert...`).
  - Public Station Departure Board (`departure-board.html`): Real-time live status auto-polling script refreshing train rows, platforms, and operational delay badges every 10 seconds.
  - Comprehensive unit test `notifyAffectedPassengers_RespectsPreferences()` in `NotificationServiceTest.java`.

- [x] **Phase 5 Deliverables (GoF Design Patterns — Observer Pattern & Factory Method Pattern)**:
  - **Observer Pattern**: `TrainStatusSubject` interface and concrete event publisher `TrainStatusEventPublisher` broadcasting status modifications to registered `TrainStatusObserver` subscribers (`PassengerAlertObserver`), seamlessly decoupling telemetry event generation from notification handlers.
  - **Factory Method & Multi-Channel Dispatch**: `NotificationChannel` interface with concrete channel adapters (`InAppNotificationChannel`, `EmailNotificationChannel`, `SmsNotificationChannel`) instantiated and resolved via `NotificationChannelFactory`.
  - `NotificationPatternTest.java`: 4 unit tests verifying Observer event broadcasting, dynamic subscriber registration, channel factory resolution, in-app persistence, and email dispatch.

---

## ⏳ IN PROGRESS (Current Phase)
- Completed all planned phase deliverables.

---

## 📋 TODO (Upcoming Phases)
- [x] Asynchronous notification delivery queue via `@Async`.
- [x] SMS gateway provider integration simulation.
- [x] Live dynamic departure board polling integration.
