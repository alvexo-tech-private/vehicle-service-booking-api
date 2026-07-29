package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.JobCardNumberFormat;
import com.alvexo.bookingapp.model.ResetFrequency;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalTime;

@Data
public class MechanicConfigurationSettingsRequest {

    @NotBlank(message = "jobCardNumberStartingSequence is required")
    @Size(max = 20)
    private String jobCardNumberStartingSequence;

    @NotNull(message = "jobCardNumberFormat is required")
    private JobCardNumberFormat jobCardNumberFormat;

    @NotNull(message = "jobCardNumberResetFrequency is required")
    private ResetFrequency jobCardNumberResetFrequency;

    @Size(max = 4, message = "jobCardNumberPrefix must be at most 4 characters")
    private String jobCardNumberPrefix;

    @Size(max = 4, message = "jobCardNumberSuffix must be at most 4 characters")
    private String jobCardNumberSuffix;

    @NotNull(message = "rescheduleLimit is required")
    @Min(value = 0, message = "rescheduleLimit must be at least 0")
    private Integer rescheduleLimit;

    @NotNull(message = "rescheduleCutoffTime is required")
    private LocalTime rescheduleCutoffTime;

    @NotNull(message = "pickupDropFacilityEnabled is required")
    private Boolean pickupDropFacilityEnabled;

    @NotNull(message = "autoConfirmOtherState is required")
    private Boolean autoConfirmOtherState;

    @NotNull(message = "repairsRequireAdvance is required")
    private Boolean repairsRequireAdvance;

    @NotNull(message = "serviceDueIntervalDays is required")
    @Min(value = 1, message = "serviceDueIntervalDays must be at least 1")
    private Integer serviceDueIntervalDays;

    @NotNull(message = "secondReminderIntervalDays is required")
    @Min(value = 1, message = "secondReminderIntervalDays must be at least 1")
    private Integer secondReminderIntervalDays;
}
