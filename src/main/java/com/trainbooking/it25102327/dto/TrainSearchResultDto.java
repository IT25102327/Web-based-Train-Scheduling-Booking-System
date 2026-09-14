package com.trainbooking.it25102327.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
}
