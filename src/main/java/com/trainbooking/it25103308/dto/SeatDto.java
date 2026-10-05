package com.trainbooking.it25103308.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an individual seat within a carriage coach.
 *
 * @author SLIIT Software Engineering Team (IT25103308 - Anfas M.S.)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDto {

    private String seatNumber;       // e.g. "S01", "S02"
    private String fullSeatId;       // e.g. "Coach 01 / S01"
    private String coachNumber;      // e.g. "Coach 01"
    private String seatClass;        // "FIRST" or "SECOND"
    private String seatType;         // "WINDOW", "AISLE", "MIDDLE"
    private int rowNumber;           // 1, 2, 3...
    private String columnLetter;     // "A", "B", "C", "D"
    private boolean isBooked;        // true if confirmed or locked
    private String bookedStatus;     // "AVAILABLE", "BOOKED", "LOCKED"
}
