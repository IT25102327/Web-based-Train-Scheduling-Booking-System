package com.trainbooking.it25103308.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object returned by the real-time seat status REST API endpoint.
 *
 * @author SLIIT Software Engineering Team (IT25103308 - Anfas M.S.)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusResponseDto {

    private Long scheduleId;
    private LocalDate travelDate;
    private int availableFirst;
    private int availableSecond;

    @Builder.Default
    private List<String> bookedSeats = new ArrayList<>();

    @Builder.Default
    private List<CoachDto> coaches = new ArrayList<>();

    private long timestamp;
}
