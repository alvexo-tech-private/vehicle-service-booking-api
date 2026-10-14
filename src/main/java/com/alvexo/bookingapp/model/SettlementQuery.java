package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Raise Settlement Query (WORKSHOP_FINANCE_API_SPEC.md §6). */
@Entity
@Table(name = "settlement_queries", indexes = @Index(name = "idx_settlement_queries_mechanic", columnList = "mechanic_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(name = "reference_id", nullable = false, unique = true, length = 30)
    private String referenceId;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SettlementQueryStatus status = SettlementQueryStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
