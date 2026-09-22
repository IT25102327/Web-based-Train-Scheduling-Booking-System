package com.trainbooking.it25102327.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Entity representing a scheduled train departure for a specific route, time, and day of week.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private LocalTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(precision = 10, scale = 2)
    private BigDecimal firstClassFare;

    @Column(precision = 10, scale = 2)
    private BigDecimal secondClassFare;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_seasonal")
    private Boolean isSeasonal = false;

    @Column(name = "seasonal_name")
    private String seasonalName;

    @Builder.Default
    @Column(name = "is_maintenance_blocked")
    private Boolean isMaintenanceBlocked = false;

    @Column(name = "maintenance_notes")
    private String maintenanceNotes;
}
