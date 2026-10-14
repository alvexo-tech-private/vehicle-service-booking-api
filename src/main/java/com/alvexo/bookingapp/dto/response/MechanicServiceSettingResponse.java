package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.ServiceCategory;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicServiceSettingResponse {

    private Long id;
    private String serviceName;
    private ServiceCategory category;
    private Integer durationMinutes;
    private Integer maxSlotsPerDay;
    private Boolean isExpressEligible;
    private Boolean isActive;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
