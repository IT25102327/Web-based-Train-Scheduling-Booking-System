package com.trainbooking.features.payment.repository;

import com.trainbooking.features.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Payment} entity management.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds payment record associated with a given booking ID.
     *
     * @param bookingId the booking ID
     * @return an {@link Optional} containing the payment if present
     */
    Optional<Payment> findByBookingId(Long bookingId);
}
