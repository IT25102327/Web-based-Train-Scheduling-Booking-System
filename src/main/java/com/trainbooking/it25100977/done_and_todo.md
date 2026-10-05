# Task Progress: IT25100977 — Shehara D.M.D.

**Module:** UC-04: Payment Processing & E-Ticket Generation  
**Role:** Passenger (End-User)  
**Secondary Actors:** Payment Gateway (Mock), Email Gateway  
**Priority:** 5  
**Package:** `com.trainbooking.it25100977`  
**Templates:** `src/main/resources/templates/it25100977/`  
**Tests:** `src/test/java/com/trainbooking/it25100977/`  

---

## ✅ DONE
- [x] **Use Case Specification (Lab 03)**: Documented mock payment form, transaction processing, PDF E-ticket generation with embedded QR, and email delivery.
- [x] **Activity Diagram (Lab 04)**: Modeled mock transaction gateway, error retry / release seats logic, parallel PDF and QR generation, and dual delivery (email + instant web download).
- [x] **Entity Models**:
  - `Payment.java`: Transaction records with `booking`, `amount`, `status` (PENDING, COMPLETED, FAILED), `transactionRef`, `paidAt`.
  - `Ticket.java`: E-Ticket details with `ticketNumber`, `booking`, `payment`, `qrCodeData`, `isBoarded`, `issuedAt`, plus convenience template getters for train, passenger, route, times, and fares.
- [x] **Repositories**:
  - `PaymentRepository.java`: Custom queries `findByBookingId`.
  - `TicketRepository.java`: Custom queries `findByTicketNumber`, `findByBookingId`.
- [x] **Data Transfer Objects (DTOs)**:
  - `PaymentRequest.java` (with cardholder name, card number, expiry, CVV).
- [x] **Service Layer**:
  - `PaymentService.java`: Full payment processing updating booking to CONFIRMED, unique ticket generation (`TKT-2026-XXXXX`), cryptographic 2D QR barcode encoding via ZXing, and PDF binary stream generator.
- [x] **Controller Layer**:
  - `PaymentController.java`: Endpoints for `POST /payment/process`, `GET /payment/ticket/{id}` and `GET /booking/ticket/{id}`, `GET /api/tickets/{id}/qr` (live PNG image stream), `GET /payment/ticket/{id}/download`.
- [x] **Thymeleaf UI Views**:
  - `it25100977/payment.html`: Mock payment checkout interface with reservation breakdown and simulated payment gateway inputs.
  - `it25100977/eticket.html`: Full digital boarding pass with embedded live ZXing QR code, print button, and download link.
- [x] **Unit Testing (JUnit 5 + Mockito)**:
  - `PaymentServiceTest.java`: Verified payment completion and transaction reference generation, card decline simulation and retry state preservation, e-ticket creation, QR code PNG generation, and PDF stream generation.
- [x] **Phase 2 Deliverables**:
  - Mock payment decline & retry workflow: card ending in `0000` or CVV `000` triggers gateway decline, preserving `PENDING` booking for retry before 10-minute hold expiry.
  - "Cancel Reservation & Release Seats" button on checkout page allowing instant return of locked seats to inventory.
  - 10-minute visual countdown timer banner on `payment.html`.
  - Automated transactional email dispatch via `EmailService` confirming E-Ticket details, train number, seats, route, and departure times upon successful checkout.

- [x] **Phase 3 & Phase 4 Deliverables (Complete System)**:
  - Official Tax Invoice & Payment Receipt PDF generation (`generateInvoicePdf(Long ticketId)`) in `PaymentService.java`.
  - Added REST endpoint `GET /payment/receipt/{id}/download` and `/payment/invoice/{id}/download` in `PaymentController.java`.
  - Added direct Tax Receipt download and print buttons on `eticket.html`.
  - Comprehensive unit test `testGenerateInvoicePdf()` in `PaymentServiceTest.java`.
  - **High-Definition E-Ticket / Boarding Pass PDF Generation**: Replaced legacy basic text stream with publication-grade iText 8 + ZXing engine generating authentic Sri Lanka Railways boarding pass PDFs containing embedded scannable 2D QR barcodes, origin/destination journey route cards, passenger verification data, allocated car/seat details, fare breakdown, security hashes, and barrier gate instructions. All unit tests passing (6/6 in `PaymentServiceTest`).

---

- [x] **Phase 5 Deliverables (Dual Payment Architecture & Card Validation)**:
  - **Luhn Algorithm (Mod 10) Credit Card Validation**:
    - Rigorous validation of card numbers against length (13-19 digits), numeric-only format, card brand identification (Visa, MasterCard, Amex), expiry date validity (non-expired), and CVV length (3-4 digits).
    - Client-side real-time interactive feedback on `payment.html` with dynamic Luhn validation badges, card brand highlight, auto-formatting, and preset buttons.
  - **Manual Payment Method (Bank Transfer & Slip Upload)**:
    - Displays official Sri Lanka Railways bank account details first (Bank of Ceylon & People's Bank accounts, branch codes, swift codes, 1-click copy buttons, and mandatory booking/NIC reference instructions).
    - Accompanied by a file upload interface for payment slips/receipts supporting PNG, JPG, WEBP, and PDF with live image preview and drag-and-drop support.
    - Stored securely in `uploads/slips/` with unique naming and accessible via `GET /payment/slip/{id}`.
    - Updated `Payment.java` with `PaymentMethod` (CARD, MANUAL_SLIP), slip metadata, and bank reference information.
    - Updated E-Ticket view to reflect payment method and link to the uploaded slip.
  - **GoF Design Patterns (Strategy Pattern & Factory Pattern)**:
    - `PaymentStrategy.java`: Strategy interface defining payment execution contracts across diverse channels.
    - `CardPaymentStrategy.java`: Concrete strategy handling automated card gateway communications, Luhn checksum validation, and decline simulations.
    - `ManualSlipPaymentStrategy.java`: Concrete strategy handling deposit slip verification, file storage, and banking metadata recording.
    - `PaymentStrategyFactory.java`: Factory class utilizing EnumMap to resolve payment strategies based on requested method.
  - **Unit Testing**:
    - `PaymentStrategyTest.java`: 4 unit tests verifying strategy resolution, card execution, card decline simulations, and bank slip upload processing.
    - Expanded `PaymentServiceTest.java` to 23 tests (100% passing) verifying Luhn checksum validation, card expiration checks, non-numeric checks, manual slip upload processing, and error handling for missing/unsupported slips.
