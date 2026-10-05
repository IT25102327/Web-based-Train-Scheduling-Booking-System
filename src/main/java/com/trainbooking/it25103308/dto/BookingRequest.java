package com.trainbooking.it25103308.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Data Transfer Object representing a passenger train reservation request.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    private Long scheduleId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate travelDate;

    private String seatClass;

    private Integer numberOfSeats;

    private Integer seatCount;

    private String passengerName;

    private String passengerNic;

    private String contactPhone;

    private String seatNumbers;

    private String coachNumber;

    public BookingRequest(Long scheduleId, LocalDate travelDate, String seatClass, Integer numberOfSeats) {
        this.scheduleId = scheduleId;
        this.travelDate = travelDate;
        this.seatClass = seatClass;
        this.numberOfSeats = numberOfSeats;
        this.seatCount = numberOfSeats;
    }

    public Integer getNumberOfSeats() {
        if (numberOfSeats != null && numberOfSeats > 0) {
            return numberOfSeats;
        }
        if (seatCount != null && seatCount > 0) {
            return seatCount;
        }
        return 1;
    }
}
