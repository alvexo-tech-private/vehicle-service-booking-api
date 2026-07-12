package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.JobCardNumberFormat;
import com.alvexo.bookingapp.model.ResetFrequency;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicConfigurationSettingsResponse {

    private Long id;
    private Long mechanicId;

    private String jobCardNumberStartingSequence;
    private JobCardNumberFormat jobCardNumberFormat;
    private ResetFrequency jobCardNumberResetFrequency;
    private String jobCardNumberPrefix;
    private String jobCardNumberSuffix;

    private Integer rescheduleLimit;
    private LocalTime rescheduleCutoffTime;

    private Boolean pickupDropFacilityEnabled;
    private Boolean autoConfirmOtherState;
    private Boolean repairsRequireAdvance;

    private Integer serviceDueIntervalDays;
    private Integer secondReminderIntervalDays;
    private Integer reminderChangesThisYear;
    private Integer reminderChangesRemainingThisYear;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
