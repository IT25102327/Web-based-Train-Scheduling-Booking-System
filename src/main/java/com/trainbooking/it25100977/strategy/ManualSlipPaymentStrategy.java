package com.trainbooking.it25100977.strategy;

import com.trainbooking.it25100977.dto.PaymentRequest;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25100977.repository.PaymentRepository;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Concrete strategy implementing manual bank deposit slip payment processing.
 * Validates the uploaded slip format, stores the proof-of-payment document in file storage,
 * and records banking transaction metadata.
 *
 * @author SLIIT Software Engineering Team (IT25100977)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualSlipPaymentStrategy implements PaymentStrategy {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Override
    public Payment.PaymentMethod getSupportedMethod() {
        return Payment.PaymentMethod.MANUAL_SLIP;
    }

    @Override
    public Payment processPayment(Booking booking, PaymentRequest request, MultipartFile slipFile) {
        log.info("Executing ManualSlipPaymentStrategy for booking ID: {}", booking.getId());

        String savedSlipFileName = null;
        String savedSlipFilePath = null;

        if (slipFile != null && !slipFile.isEmpty()) {
            String originalFilename = slipFile.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }
            if (!ext.matches("^\\.(png|jpg|jpeg|webp|pdf)$")) {
                throw new IllegalArgumentException("Invalid file format: Please upload a valid payment slip image (PNG, JPG, WEBP) or PDF document.");
            }

            try {
                Path uploadDir = Paths.get("uploads", "slips");
                Files.createDirectories(uploadDir);
                String storedFilename = "slip_bkg_" + booking.getId() + "_" + System.currentTimeMillis() + ext;
                Path destination = uploadDir.resolve(storedFilename);
                Files.copy(slipFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
                savedSlipFileName = originalFilename;
                savedSlipFilePath = "/uploads/slips/" + storedFilename;
                log.info("Uploaded payment slip saved at: {}", destination.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to store payment slip file", e);
                throw new RuntimeException("Could not store uploaded payment slip. Please try again.", e);
            }
        } else if (request != null && request.getSlipFileName() != null && !request.getSlipFileName().trim().isEmpty()) {
            savedSlipFileName = request.getSlipFileName();
            savedSlipFilePath = "/uploads/slips/" + request.getSlipFileName();
        } else {
            throw new IllegalArgumentException("Please upload a bank deposit slip or transaction receipt (JPG, PNG, or PDF).");
        }

        String bankName = (request != null && request.getBankName() != null && !request.getBankName().isBlank())
                ? request.getBankName() : "Bank of Ceylon (BOC)";
        String bankRef = (request != null && request.getBankReference() != null && !request.getBankReference().isBlank())
                ? request.getBankReference() : ("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionRef("MANUAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod(Payment.PaymentMethod.MANUAL_SLIP)
                .bankName(bankName)
                .bankReference(bankRef)
                .slipFileName(savedSlipFileName)
                .slipFilePath(savedSlipFilePath)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Manual slip payment recorded for booking ID: {}, txn ref: {}", booking.getId(), savedPayment.getTransactionRef());
        return savedPayment;
    }
}
