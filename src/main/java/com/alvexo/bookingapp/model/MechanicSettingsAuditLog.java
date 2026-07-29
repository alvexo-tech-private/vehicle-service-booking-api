package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One field-level change to a mechanic's settings or configuration settings.
 * Written alongside the save — see MechanicSettingsService/MechanicConfigurationSettingsService.
 */
@Entity
@Table(name = "mechanic_settings_audit_logs",
        indexes = @Index(name = "idx_msal_mechanic_changed_at", columnList = "mechanic_id, changed_at"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicSettingsAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    /** e.g. "MECHANIC_SETTINGS", "CONFIGURATION_SETTINGS" */
    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "field_name", nullable = false, length = 60)
    private String fieldName;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
