package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.ReminderStage;
import lombok.*;

import java.time.LocalDate;

/** One row of the Service Due / Second Reminder card (spec §7 #74/#75). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceReminderResponse {

    private Long vehicleId;
    private String vehicleRegistrationNumber;
    private String vehicleInfo;

    private Long customerId;
    private String customerName;
    private String customerPhone;

    private LocalDate lastServiceDate;
    private Integer daysSinceService;
    private ReminderStage reminderStage;
}
