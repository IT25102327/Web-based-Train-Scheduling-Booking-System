package com.trainbooking.it25102327.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Data Transfer Object representing train search results for passenger booking queries.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainSearchResultDto {

    private Long scheduleId;
    private String trainName;
    private String trainNumber;
    private String origin;
    private String destination;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Integer availableFirstClass;
    private Integer availableSecondClass;
    private BigDecimal firstClassFare;
    private BigDecimal secondClassFare;
    private String status;

    public Long getId() {
        return scheduleId;
    }

    public Integer getAvailableFirstClassSeats() {
        return availableFirstClass != null ? availableFirstClass : 0;
    }

    public Integer getAvailableSecondClassSeats() {
        return availableSecondClass != null ? availableSecondClass : 0;
    }

    public String getDuration() {
        if (departureTime == null || arrivalTime == null) {
            return "N/A";
        }
        Duration duration = Duration.between(departureTime, arrivalTime);
        if (duration.isNegative()) {
            duration = duration.plusHours(24);
        }
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return hours + "h " + minutes + "m";
    }

    public String getFormattedFare() {
        BigDecimal fare = (secondClassFare != null) ? secondClassFare : firstClassFare;
        if (fare == null) {
            return "LKR 0.00";
        }
        return "LKR " + String.format("%,.2f", fare);
    }
}
