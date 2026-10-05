package com.trainbooking.it25102327.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object containing the outcome of autonomous platform assignment and conflict validation.
 *
 * @author SLIIT Software Engineering Team (IT25102327)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAssignmentResult {

    private String stationName;
    private String requestedPlatform;
    private String allocatedPlatform;
    private Boolean isConflict;
    private String conflictDescription;
    private List<String> availablePlatforms;
    private Map<String, String> platformOccupancyMap;
}
