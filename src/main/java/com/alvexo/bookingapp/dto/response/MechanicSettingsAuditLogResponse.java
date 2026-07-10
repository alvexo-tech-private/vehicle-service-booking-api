package com.alvexo.bookingapp.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicSettingsAuditLogResponse {

    private Long id;
    private String entityType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long changedByUserId;
    private String changedByName;
    private LocalDateTime changedAt;
}
