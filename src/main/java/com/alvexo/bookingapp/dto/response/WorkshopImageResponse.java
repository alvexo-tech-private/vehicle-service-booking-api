package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.WorkshopImageType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopImageResponse {
    private Long id;
    private WorkshopImageType type;
    private String url;
    private LocalDateTime uploadedAt;
    private Long sizeBytes;
}
