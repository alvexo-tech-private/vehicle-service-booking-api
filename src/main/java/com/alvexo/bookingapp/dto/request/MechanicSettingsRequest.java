package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.JobCardClassification;
import com.alvexo.bookingapp.model.JobCardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
@Schema(description = "Mechanic booking settings — job card type, capacity config, reporting times, advance payment, and service catalogue")
public class MechanicSettingsRequest {

    @Schema(description = "Booking validation strategy. TYPE_1=count-based, TYPE_2=per-service capacity, TYPE_3=hybrid allocation, TYPE_4=slots+hybrid",
            example = "TYPE_1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "jobCardType is required")
    private JobCardType jobCardType;

    @Schema(description = "Booking classification mode. AUTO=system-driven, MECHANIC=manual job card creation",
            example = "AUTO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "classification is required")
    private JobCardClassification classification;

    @Schema(description = "Maximum vehicles accepted per day (TYPE_1/2)", example = "20",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "maxVehiclesPerDay is required")
    @Min(value = 1, message = "maxVehiclesPerDay must be at least 1")
    private Integer maxVehiclesPerDay;

    @Schema(description = "Legacy capacity mode toggle. true=hour-slot mode, false=vehicle-count mode",
            example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "reserveCapacity is required")
    private Boolean reserveCapacity;

    @Schema(description = "Total working hours per day. Required for TYPE_3/4 and when reserveCapacity=true",
            example = "36.00", nullable = true)
    @DecimalMin(value = "0.5", message = "fullDayCapacityHours must be at least 0.5")
    private BigDecimal fullDayCapacityHours;

    @Schema(description = "Prefix for job card number generation. Format: {prefix}{YYMMDD}{seq}",
            example = "0101", minLength = 2, maxLength = 10, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "jobCardSerialPrefix is required")
    @Size(min = 2, max = 10, message = "jobCardSerialPrefix must be 2–10 characters")
    private String jobCardSerialPrefix;

    @Schema(description = "Standard vehicle reporting time", example = "08:30",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "serviceReportingTime is required")
    private LocalTime serviceReportingTime;

    @Schema(description = "Express vehicle reporting time. Required for TYPE_3/4. Bookings before this time are EXPRESS",
            example = "11:00", nullable = true)
    private LocalTime expressReportingTime;

    @Schema(description = "Whether advance payment is required at booking confirmation",
            example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "advanceEnabled is required")
    private Boolean advanceEnabled;

    @Schema(description = "Advance amount in INR. Required when advanceEnabled=true",
            example = "100.00", nullable = true)
    @DecimalMin(value = "1.0", message = "advanceAmount must be greater than 0")
    private BigDecimal advanceAmount;

    @Schema(description = "Overall daily hour cap for TYPE_2 (on top of per-service qty limits)",
            example = "24.00", nullable = true)
    @DecimalMin(value = "0.5", message = "totalDailyCapacityHours must be at least 0.5")
    private BigDecimal totalDailyCapacityHours;

    @Schema(description = "Enable auto/manual allocation split for TYPE_3/4", example = "false")
    private Boolean autoAllocationEnabled;

    @Schema(description = "Hours auto-confirmed without mechanic review. Required for TYPE_3/4. Must be ≤ fullDayCapacityHours",
            example = "12.00", nullable = true)
    @DecimalMin(value = "0.5", message = "autoAllocationCapacityHours must be at least 0.5")
    private BigDecimal autoAllocationCapacityHours;

    @Schema(description = "Optional list of service settings. When provided, replaces all existing services")
    @Valid
    private List<MechanicServiceSettingRequest> serviceSettings;
}
