package com.trainbooking.it25102327.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object representing detailed route information,
 * including intermediate stations sequence and geographic coordinates for map visualization.
 *
 * @author SLIIT Software Engineering Team (IT25102327)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDetailDto {

    private Long routeId;
    private String origin;
    private String destination;
    private Integer distanceKm;
    private String defaultPlatform;
    private String lineName;
    private List<StationDto> stations;
    private List<List<Double>> polylineCoords;
}
