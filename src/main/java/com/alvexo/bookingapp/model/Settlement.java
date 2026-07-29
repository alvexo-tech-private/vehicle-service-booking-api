package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One row per (mechanic, settlementDate) — the payout for a completed service
 * day (WORKSHOP_FINANCE_API_SPEC.md §2). Materialized on first read for any
 * past date that has qualifying booking activity (advance collected and/or a
 * reliability adjustment); never generated for today or a future date, since
 * the day isn't "done" yet — that's what the live Earnings tab is for.
 *
 * netPay / serviceReliabilityAdjustment are computed from that day's bookings
 * at generation time (see SettlementService) and then frozen — later changes
 * to a booking after its settlement exists do not retroactively change it,
 * mirroring how a real payout, once computed, isn't silently rewritten.
 */
@Entity
@Table(name = "settlements", uniqueConstraints = {
        @UniqueConstraint(name = "uq_settlement_mechanic_date", columnNames = {"mechanic_id", "settlement_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "net_pay", nullable = false, precision = 10, scale = 2)
    private BigDecimal netPay;

    @Column(name = "service_reliability_adjustment", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal serviceReliabilityAdjustment = BigDecimal.ZERO;

    @Column(name = "settlement_released", nullable = false, precision = 10, scale = 2)
    private BigDecimal settlementReleased;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    // ── Payment transaction — populated only once status = PAID ────────────────

    @Column(name = "transaction_number", length = 50)
    private String transactionNumber;

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;

    @Column(name = "payment_gateway_reference", length = 100)
    private String paymentGatewayReference;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_time")
    private LocalTime paymentTime;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
