package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A workshop's configured vehicle-make coverage for one (fuelType, serviceCategory)
 * pair (WORKSHOP_API_AUDIT_AND_BACKEND_SPECIFICATION.md §1.1). Drives the
 * "Does not service {make}" disabled state in rider search results.
 */
@Entity
@Table(name = "workshop_service_eligibility",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_workshop_eligibility_mechanic_fuel_category",
                columnNames = {"mechanic_id", "fuel_type", "service_category"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopServiceEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", nullable = false, length = 20)
    private ServiceCategory serviceCategory;

    /** Comma-separated vehicle make names, e.g. "Hero,Honda,TVS". Empty/null = none configured. */
    @Column(name = "vehicle_makes", columnDefinition = "TEXT")
    private String vehicleMakes;

    @Column(name = "other_makes_allowed", nullable = false)
    @Builder.Default
    private Boolean otherMakesAllowed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
