package com.trainbooking.features.booking.repository;

import com.trainbooking.features.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Booking} entity management.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Finds all bookings for a given passenger ID.
     *
     * @param passengerId the user ID of the passenger
     * @return list of bookings
     */
    List<Booking> findByPassengerId(Long passengerId);

    /**
     * Counts booked seats for a schedule on a specific travel date and seat class.
     *
     * @param scheduleId the schedule ID
     * @param travelDate the date of travel
     * @param seatClass the seat class (FIRST, SECOND)
     * @return total booked seats count
     */
    @Query("SELECT COALESCE(SUM(b.numberOfSeats), 0) FROM Booking b " +
           "WHERE b.schedule.id = :scheduleId " +
           "AND b.travelDate = :travelDate " +
           "AND b.seatClass = :seatClass " +
           "AND b.status <> 'CANCELLED'")
    Integer countByScheduleIdAndTravelDateAndSeatClass(
            @Param("scheduleId") Long scheduleId,
            @Param("travelDate") LocalDate travelDate,
            @Param("seatClass") Booking.SeatClass seatClass
    );
}
