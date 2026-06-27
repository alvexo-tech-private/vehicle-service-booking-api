package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.JobCardClassification;
import com.alvexo.bookingapp.model.JobCardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mechanic's full settings profile including job card type, capacity config, services, and technicians")
public class MechanicSettingsResponse {

    @Schema(description = "Settings ID", example = "1")
    private Long id;

    @Schema(description = "Mechanic user ID", example = "5")
    private Long mechanicId;

    @Schema(description = "Mechanic display name", example = "Suresh Mech")
    private String mechanicName;

    @Schema(description = "Active job card type", example = "TYPE_1")
    private JobCardType jobCardType;

    @Schema(description = "Classification mode", example = "AUTO")
    private JobCardClassification classification;

    @Schema(description = "Max vehicles per day (TYPE_1/2)", example = "20")
    private Integer maxVehiclesPerDay;

    @Schema(description = "Legacy capacity mode flag", example = "false")
    private Boolean reserveCapacity;

    @Schema(description = "Total working hours per day (TYPE_3/4 fallback)", example = "36.00")
    private BigDecimal fullDayCapacityHours;

    @Schema(description = "Job card serial prefix", example = "0101")
    private String jobCardSerialPrefix;

    @Schema(description = "Standard reporting time", example = "08:30")
    private LocalTime serviceReportingTime;

    @Schema(description = "Express reporting time", example = "11:00")
    private LocalTime expressReportingTime;

    @Schema(description = "Advance payment enabled", example = "false")
    private Boolean advanceEnabled;

    @Schema(description = "Advance amount in INR", example = "100.00")
    private BigDecimal advanceAmount;

    @Schema(description = "TYPE_2 overall daily hour cap", example = "24.00")
    private BigDecimal totalDailyCapacityHours;

    @Schema(description = "Auto/manual allocation split enabled", example = "false")
    private Boolean autoAllocationEnabled;

    @Schema(description = "Auto-confirm threshold in hours", example = "12.00")
    private BigDecimal autoAllocationCapacityHours;

    @Schema(description = "Computed: effective capacity from technicians or fallback to fullDayCapacityHours",
            example = "36.00", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal effectiveCapacityHours;

    @Schema(description = "Computed: effectiveCapacityHours - autoAllocationCapacityHours",
            example = "24.00", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal reservedCapacityHours;

    @Schema(description = "Service catalogue offered by this mechanic")
    private List<MechanicServiceSettingResponse> serviceSettings;

    @Schema(description = "Workshop technician capacity allocations")
    private List<MechanicTechnicianCapacityResponse> technicianCapacities;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
