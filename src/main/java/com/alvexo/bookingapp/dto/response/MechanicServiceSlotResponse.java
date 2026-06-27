package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service slot configuration with auto/manual allocation split")
public class MechanicServiceSlotResponse {

    @Schema(description = "Slot ID", example = "10")
    private Long id;

    @Schema(description = "Slot sequence number", example = "1")
    private Integer slotNumber;

    @Schema(description = "Slot start time", example = "11:00")
    private LocalTime startTime;

    @Schema(description = "Slot end time", example = "13:00")
    private LocalTime endTime;

    @Schema(description = "Service category this slot is reserved for", example = "REPAIR")
    private ServiceCategory restrictedCategory;

    @Schema(description = "Total vehicle capacity for this slot", example = "5")
    private Integer maxVehicleQty;

    @Schema(description = "Auto-confirmed portion", example = "3")
    private Integer autoAllocationQty;

    @Schema(description = "Computed: maxVehicleQty - autoAllocationQty (manual review portion)",
            example = "2", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer manualReviewQty;

    @Schema(description = "Active weekdays. null=all days", example = "MON,WED,FRI")
    private String applicableDays;

    @Schema(description = "Whether slot is visible to riders", example = "true")
    private Boolean isEnabled;
}
