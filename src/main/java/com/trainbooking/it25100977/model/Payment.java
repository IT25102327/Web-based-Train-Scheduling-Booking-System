package com.trainbooking.it25100977.model;

import com.trainbooking.it25103308.model.Booking;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a payment transaction associated with a train booking.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CARD;

    @Column(name = "slip_file_path")
    private String slipFilePath;

    @Column(name = "slip_file_name")
    private String slipFileName;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_reference")
    private String bankReference;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime paidAt;

    /**
     * Supported payment methods.
     */
    public enum PaymentMethod {
        CARD,
        MANUAL_SLIP
    }

    /**
     * Status of the payment transaction.
     */
    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
}
