package com.alvexo.bookingapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a vehicle user's preferred (bookmarked) mechanic.
 *
 * <p>The unique constraint on {@code (vehicle_user_id, mechanic_id)} is enforced at
 * both the DB level (see changeset 020) and in {@code PreferenceService}, so concurrent
 * duplicate adds are safe regardless of which layer catches them first.
 *
 * <p>The application-level cap ({@link com.alvexo.bookingapp.util.Constants#MAX_MECHANIC_PREFERENCES})
 * is checked in the service before insert.
 */
@Entity
@Table(
    name = "user_mechanic_preferences",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_preference_user_mechanic",
        columnNames = {"vehicle_user_id", "mechanic_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMechanicPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The vehicle user who bookmarked the mechanic. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_user_id", nullable = false)
    private User vehicleUser;

    /** The mechanic who was bookmarked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
