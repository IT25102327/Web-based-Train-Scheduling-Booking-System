package com.trainbooking.features.trains.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a physical train unit with seating capacity and operational status.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trains")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trainNumber;

    @Column(nullable = false)
    private String trainName;

    private Integer totalSeats;

    private Integer firstClassSeats;

    private Integer secondClassSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Train operational status enum.
     */
    public enum TrainStatus {
        ON_TIME,
        DELAYED,
        CANCELLED
    }
}
