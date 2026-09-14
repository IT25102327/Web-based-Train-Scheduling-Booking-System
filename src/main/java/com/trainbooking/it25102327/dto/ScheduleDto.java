package com.trainbooking.it25102327.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing train schedule and fare information.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDto {

    private Long id;
    private Long trainId;
    private Long routeId;
    private String departureTime;
    private String arrivalTime;
    private String dayOfWeek;
    private BigDecimal firstClassFare;
    private BigDecimal secondClassFare;
    private Boolean isActive;
}
