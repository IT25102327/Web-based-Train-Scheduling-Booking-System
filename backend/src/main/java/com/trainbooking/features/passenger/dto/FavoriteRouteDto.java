package com.trainbooking.features.passenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for passenger favorite train routes.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRouteDto {

    private Long id;
    private String origin;
    private String destination;
}
