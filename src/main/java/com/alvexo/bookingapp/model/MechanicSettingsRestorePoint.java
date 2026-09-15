package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

import com.alvexo.bookingapp.dto.request.MechanicConfigurationSettingsRequest;
import com.alvexo.bookingapp.dto.request.MechanicSettingsRequest;

/**
 * A named snapshot of a workshop's capacity + configuration settings
 * (WORKSHOP_API_AUDIT_AND_BACKEND_SPECIFICATION.md §1.2 "Settings Restore Point"),
 * so a mechanic can roll back an accidental change.
 */
@Entity
@Table(name = "mechanic_settings_restore_points")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicSettingsRestorePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "settings_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private MechanicSettingsRequest settingsSnapshot;

    @Column(name = "configuration_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private MechanicConfigurationSettingsRequest configurationSnapshot;

    @CreationTimestamp
    @Column(name = "saved_at", updatable = false)
    private LocalDateTime savedAt;
}
