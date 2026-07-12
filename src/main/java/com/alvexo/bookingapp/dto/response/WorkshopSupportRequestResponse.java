package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.SupportCategory;
import com.alvexo.bookingapp.model.SupportRequestStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopSupportRequestResponse {
    private String id;
    private SupportCategory category;
    private String subject;
    private String description;
    private String imageUrl;
    private SupportRequestStatus status;
    private LocalDateTime createdAt;
}
