package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.JobCardType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
public class MechanicSettingsRequest {

    /**
     * Internal mode behind the mechanic-facing Workshop Capacity Level (1-4).
     * AUTO -> Level 1/2 (distinguished by reserveCapacity).
     * MECHANIC -> Level 3/4 (distinguished by reserveForSlots).
     */
    @NotNull(message = "jobCardType is required")
    private JobCardType jobCardType;

    @NotNull(message = "maxVehiclesPerDay is required")
    @Min(value = 1, message = "maxVehiclesPerDay must be at least 1")
    private Integer maxVehiclesPerDay;

    @NotNull(message = "reserveCapacity is required")
    private Boolean reserveCapacity;

    /**
     * Only meaningful when jobCardType = MECHANIC.
     * false -> Level 3 (Mechanic Day), true -> Level 4 (Mechanic Slot).
     */
    @NotNull(message = "reserveForSlots is required")
    private Boolean reserveForSlots;

    /**
     * Backend validates this for every Level (min 0.5), even when the active
     * Level doesn't use it as its primary capacity basis.
     */
    @NotNull(message = "fullDayCapacityHours is required")
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

    @NotNull(message = "autoAllocationEnabled is required")
    private Boolean autoAllocationEnabled;

    /**
     * Backend validates this for every Level (min 0.5), even when
     * autoAllocationEnabled = false.
     */
    @NotNull(message = "autoAllocationCapacityHours is required")
    @DecimalMin(value = "0.5", message = "autoAllocationCapacityHours must be at least 0.5")
    private BigDecimal autoAllocationCapacityHours;

    @Valid
    private List<MechanicServiceSettingRequest> serviceSettings;
}
