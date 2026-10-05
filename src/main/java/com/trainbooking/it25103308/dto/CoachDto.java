package com.trainbooking.it25103308.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a passenger coach/carriage on a train.
 * Categorized into 1st Class or 2nd Class with individual seat maps.
 *
 * @author SLIIT Software Engineering Team (IT25103308 - Anfas M.S.)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachDto {

    private String coachId;          // e.g. "COACH-01"
    private String coachNumber;      // e.g. "Coach 01"
    private String coachLabel;       // e.g. "Car A1"
    private String coachClass;       // "FIRST" or "SECOND"
    private String classDisplayName; // "1st Class (AC)" or "2nd Class (Reserved)"
    private int totalSeats;          // total capacity of this coach
    private int availableSeats;      // available seats remaining
    private int bookedSeats;         // currently booked or locked
    private String layoutType;       // "2+1" or "2+2"

    @Builder.Default
    private List<SeatDto> seats = new ArrayList<>();
}
