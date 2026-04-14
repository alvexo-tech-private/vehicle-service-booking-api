package com.alvexo.bookingapp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
public class MechanicSettingsRequest {

    @NotNull(message = "maxVehiclesPerDay is required")
    @Min(value = 1, message = "maxVehiclesPerDay must be at least 1")
    private Integer maxVehiclesPerDay;

    @NotNull(message = "reserveCapacity is required")
    private Boolean reserveCapacity;

    /**
     * Required when reserveCapacity = true.
     * Total working hours available per day for bookings.
     */
    @DecimalMin(value = "0.5", message = "fullDayCapacityHours must be at least 0.5")
    private BigDecimal fullDayCapacityHours;

    @NotBlank(message = "jobCardSerialPrefix is required")
    @Size(min = 2, max = 10, message = "jobCardSerialPrefix must be 2–10 characters")
    private String jobCardSerialPrefix;

    /**
     * Time by which standard vehicles must report (e.g. 08:30).
     */
    @NotNull(message = "serviceReportingTime is required")
    private LocalTime serviceReportingTime;

    /**
     * Required when reserveCapacity = true.
     * Bookings scheduled before this time are flagged as EXPRESS.
     */
    private LocalTime expressReportingTime;

    @NotNull(message = "advanceEnabled is required")
    private Boolean advanceEnabled;

    /**
     * Required when advanceEnabled = true.
     */
    @DecimalMin(value = "1.0", message = "advanceAmount must be greater than 0")
    private BigDecimal advanceAmount;

    @Valid
    private List<MechanicServiceSettingRequest> serviceSettings;
}
