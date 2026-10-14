package com.alvexo.bookingapp.dto.request;

import java.util.List;

import com.alvexo.bookingapp.model.ReminderNotifyStage;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReminderNotifyRequest {

    @NotNull(message = "stage is required")
    private ReminderNotifyStage stage;

    @NotEmpty(message = "vehicleRegistrationNumbers must not be empty")
    private List<String> vehicleRegistrationNumbers;
}
