package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.JobCardType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
public class MechanicSettingsRequest {

//    @NotNull(message = "maxVehiclesPerDay is required")
//    @Min(value = 1, message = "maxVehiclesPerDay must be at least 1")
    private Integer maxVehiclesPerDay = 0;

//    @NotNull(message = "reserveCapacity is required")
    private Boolean reserveCapacity = Boolean.FALSE;

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

    /**
     * Identifies which job card classification this mechanic uses.
     * AUTO_JOB_CARD  = Type 1 or Type 2 (Auto Job Card A)
     * MECHANIC_JOB_CARD = Type 3 or Type 4 (Mechanic Job Card B)
     */
    @NotNull(message = "Job Card Type is required")
    private JobCardType jobCardType = JobCardType.AUTO_JOB_CARD;

    /**
     * Type 3 = false (no slot management).
     * Type 4 = true  (dedicated repair slots enabled).
     * Only relevant when jobCardType = MECHANIC_JOB_CARD.
     */
    private Boolean reserveForSlots;

    /**
     * Whether Auto Allocation quota is active.
     * true  = riders can self-book from AA hours.
     * false = all bookings must be created by mechanic.
     * Only relevant when jobCardType = MECHANIC_JOB_CARD.
     */
    private Boolean autoAllocationEnabled;

    /**
     * Whether Repair Auto Allocation quota is active.
     * true  = riders can self-book from RAC hours.
     * false = all bookings must be created by mechanic.
     * Only relevant when jobCardType = MECHANIC_JOB_CARD.
     */
    private Boolean repairAutoAllocationEnabled;

    /**
     * Auto Allocation Capacity in hours (AA).
     * Riders consume from this bucket only.
     * Reserved Capacity (RC) = fullDayCapacityHours - autoAllocHours.
     * RC is never stored — always computed at runtime.
     * Range: 0 to fullDayCapacityHours.
     */
    @DecimalMin(value = "0.0", message = "autoAllocationHours must be 0 or greater")
    private BigDecimal autoAllocationHours;
}
