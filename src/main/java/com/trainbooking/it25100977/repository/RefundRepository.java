package com.trainbooking.it25100977.repository;

import com.trainbooking.it25100977.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Refund} audit trail and ledger records.
 *
 * @author SLIIT Software Engineering Team (IT25100977 - Shehara D.M.D.)
 * @version 1.0.0
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * Finds refund record for a specific booking ID.
     *
     * @param bookingId booking ID
     * @return {@link Optional} containing refund if found
     */
    Optional<Refund> findByBookingId(Long bookingId);

    /**
     * Finds refund record for a specific payment ID.
     *
     * @param paymentId payment ID
     * @return {@link Optional} containing refund if found
     */
    Optional<Refund> findByPaymentId(Long paymentId);

    /**
     * Deletes refund records referencing a given booking ID.
     *
     * @param bookingId booking ID
     */
    void deleteByBookingId(Long bookingId);
}
