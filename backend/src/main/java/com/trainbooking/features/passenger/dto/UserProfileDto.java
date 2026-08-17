package com.trainbooking.features.passenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for passenger user profile details.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
