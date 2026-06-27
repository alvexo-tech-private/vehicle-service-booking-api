package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service offered by a mechanic with category, duration, and daily cap")
public class MechanicServiceSettingResponse {

    @Schema(description = "Service setting ID", example = "10")
    private Long id;

    @Schema(description = "Service category for slot routing", example = "GENERAL")
    private ServiceCategory category;

    @Schema(description = "Service display name", example = "Oil Change")
    private String serviceName;

    @Schema(description = "Service duration in minutes", example = "60")
    private Integer durationMinutes;

    @Schema(description = "Per-service daily cap. null=no cap", example = "5")
    private Integer maxSlotsPerDay;

    @Schema(description = "Eligible for express booking", example = "false")
    private Boolean isExpressEligible;

    @Schema(description = "Whether service is active and visible", example = "true")
    private Boolean isActive;

    @Schema(description = "UI display order", example = "0")
    private Integer displayOrder;

    private LocalDateTime createdAt;
}
