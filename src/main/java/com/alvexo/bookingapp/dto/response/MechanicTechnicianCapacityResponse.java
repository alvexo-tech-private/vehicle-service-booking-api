package com.alvexo.bookingapp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Technician with individual reserved working hours")
public class MechanicTechnicianCapacityResponse {

    @Schema(description = "Technician capacity ID", example = "1")
    private Long id;

    @Schema(description = "Technician name", example = "John")
    private String technicianName;

    @Schema(description = "Reserved working hours per day", example = "9.00")
    private BigDecimal reservedHours;

    @Schema(description = "Active status. false=absent today", example = "true")
    private Boolean isActive;
}
