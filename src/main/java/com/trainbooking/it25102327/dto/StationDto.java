package com.trainbooking.it25102327.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an individual railway station waypoint along a route corridor.
 *
 * @author SLIIT Software Engineering Team (IT25102327)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDto {

    private String name;
    private String code;
    private Double latitude;
    private Double longitude;
    private Integer sequence;
    private Integer distanceKm;
    private Integer platformsCount;
    private String lineName;
}
