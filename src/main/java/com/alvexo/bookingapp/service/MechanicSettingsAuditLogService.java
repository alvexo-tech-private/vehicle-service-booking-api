package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.MechanicSettingsAuditLogResponse;
import com.alvexo.bookingapp.model.MechanicSettingsAuditLog;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.MechanicSettingsAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Records field-level changes to MechanicSettings/MechanicConfigurationSettings
 * (spec §7 Settings Audit). Callers pass the before/after value for each field
 * they own; a row is written only when the value actually changed.
 */
@Service
public class MechanicSettingsAuditLogService {

    private final MechanicSettingsAuditLogRepository auditLogRepository;

    public MechanicSettingsAuditLogService(MechanicSettingsAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logIfChanged(User mechanic, User changedBy, String entityType, String fieldName,
                              Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        auditLogRepository.save(MechanicSettingsAuditLog.builder()
                .mechanic(mechanic)
                .entityType(entityType)
                .fieldName(fieldName)
                .oldValue(oldValue != null ? oldValue.toString() : null)
                .newValue(newValue != null ? newValue.toString() : null)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<MechanicSettingsAuditLogResponse> getRecentChanges(Long mechanicId) {
        return auditLogRepository.findTop50ByMechanicIdOrderByChangedAtDesc(mechanicId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MechanicSettingsAuditLogResponse> getChangesInRange(Long mechanicId,
                                                                     LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByMechanicIdAndChangedAtBetweenOrderByChangedAtDesc(mechanicId, from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private MechanicSettingsAuditLogResponse toResponse(MechanicSettingsAuditLog a) {
        return MechanicSettingsAuditLogResponse.builder()
                .id(a.getId())
                .entityType(a.getEntityType())
                .fieldName(a.getFieldName())
                .oldValue(a.getOldValue())
                .newValue(a.getNewValue())
                .changedByUserId(a.getChangedBy().getId())
                .changedByName(a.getChangedBy().getFirstName() + " " + a.getChangedBy().getLastName())
                .changedAt(a.getChangedAt())
                .build();
    }
}
