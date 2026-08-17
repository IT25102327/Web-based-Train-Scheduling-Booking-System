package com.trainbooking.features.booking.dto;

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
}
