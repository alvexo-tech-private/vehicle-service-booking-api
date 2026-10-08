package com.alvexo.bookingapp.dto.response;

import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.Notification;
import com.alvexo.bookingapp.model.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** GET /api/notifications response shape — never exposes the entity's User relationship. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean read;
    private LocalDateTime createdAt;
    private String targetEntityType;
    private Long targetEntityId;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getNotificationType())
                .read(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .targetEntityType(n.getRelatedEntityType())
                .targetEntityId(n.getRelatedEntityId())
                .build();
    }
}
