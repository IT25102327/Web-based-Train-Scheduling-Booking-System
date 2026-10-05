package com.trainbooking.it25101520.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a system user (Passenger, Admin, Coordinator, Station Staff).
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String phone;

    @Builder.Default
    private Boolean notifyByEmail = true;

    @Builder.Default
    private Boolean notifyBySms = true;

    @Builder.Default
    private Integer delayAlertThresholdMinutes = 15;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * User roles within the system.
     */
    public enum Role {
        PASSENGER,
        ADMIN,
        COORDINATOR,
        STATION_STAFF
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getPhoneNumber() {
        return phone != null ? phone : "";
    }

    public String getMemberSince() {
        return createdAt != null ? createdAt.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")) : "August 2026";
    }

    public Integer getTotalBookings() {
        return 0;
    }

    public String getNic() {
        return "199623849102";
    }
}
