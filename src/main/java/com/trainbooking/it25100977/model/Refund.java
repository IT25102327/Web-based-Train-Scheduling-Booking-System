package com.trainbooking.it25100977.model;

import com.trainbooking.it25103308.model.Booking;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an electronic refund transaction generated upon ticket cancellation.
 *
 * @author SLIIT Software Engineering Team (IT25100977 - Shehara D.M.D.)
 * @version 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cancellationFee;

    @Column(nullable = false)
    private Integer refundPercentage;

    @Column(nullable = false, unique = true)
    private String refundTransactionRef;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RefundStatus status = RefundStatus.COMPLETED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime refundedAt;

    /**
     * Operational state of the financial refund.
     */
    public enum RefundStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
