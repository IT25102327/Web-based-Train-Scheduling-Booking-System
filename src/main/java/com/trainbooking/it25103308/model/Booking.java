package com.trainbooking.it25103308.model;

import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25102327.model.Schedule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a passenger train reservation / booking record.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(nullable = false)
    private LocalDate travelDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatClass seatClass;

    @Column(nullable = false)
    private Integer numberOfSeats;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private String seatNumbers;

    private String passengerName;

    private String passengerNic;

    private String contactPhone;

    private LocalDateTime lockExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime bookedAt;

    public String getTicketNumber() {
        if (id != null) {
            return String.format("TKT-2026-%05d", id);
        }
        return "TKT-2026-PENDING";
    }

    public String getBookingDate() {
        if (bookedAt != null) {
            return bookedAt.toLocalDate().toString();
        }
        return (travelDate != null) ? travelDate.toString() : "2026-08-16";
    }

    public String getTrainName() {
        return (schedule != null && schedule.getTrain() != null && schedule.getTrain().getTrainName() != null)
                ? schedule.getTrain().getTrainName() : "Udarata Menike";
    }

    public String getOrigin() {
        return (schedule != null && schedule.getRoute() != null && schedule.getRoute().getOrigin() != null)
                ? schedule.getRoute().getOrigin() : "Colombo Fort";
    }

    public String getDestination() {
        return (schedule != null && schedule.getRoute() != null && schedule.getRoute().getDestination() != null)
                ? schedule.getRoute().getDestination() : "Kandy";
    }

    public String getDepartureTime() {
        return (schedule != null && schedule.getDepartureTime() != null)
                ? schedule.getDepartureTime().toString() : "05:55 AM";
    }

    public String getArrivalTime() {
        return (schedule != null && schedule.getArrivalTime() != null)
                ? schedule.getArrivalTime().toString() : "09:10 AM";
    }

    public Integer getSeatCount() {
        return (numberOfSeats != null) ? numberOfSeats : 1;
    }

    /**
     * Train travel cabin/seat class.
     */
    public enum SeatClass {
        FIRST,
        SECOND
    }

    /**
     * Status of the booking transaction.
     */
    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        CANCELLED
    }
}
