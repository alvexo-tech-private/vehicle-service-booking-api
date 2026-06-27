package com.alvexo.bookingapp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One-day emergency capacity override for a specific date")
public class DailyQuotaOverrideResponse {

    @Schema(description = "Override ID", example = "1")
    private Long id;

    @Schema(description = "Override date", example = "2026-07-15")
    private LocalDate overrideDate;

    @Schema(description = "Overridden max vehicles for this date", example = "25")
    private Integer overrideMaxVehicles;

    @Schema(description = "Overridden capacity hours for this date", example = "42.00")
    private BigDecimal overrideCapacityHours;

    @Schema(description = "Reason for the override", example = "Festival rush — extending capacity")
    private String reason;

    @Schema(description = "Whether override is active", example = "true")
    private Boolean isActive;

    private LocalDateTime createdAt;
}
