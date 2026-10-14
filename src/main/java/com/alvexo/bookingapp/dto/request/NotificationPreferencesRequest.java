package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.NotificationChannel;
import com.alvexo.bookingapp.model.NotificationSound;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class NotificationPreferencesRequest {

    @NotNull(message = "newBookingEnabled is required")
    private Boolean newBookingEnabled;

    @NotNull(message = "bookingCancelledEnabled is required")
    private Boolean bookingCancelledEnabled;

    @NotNull(message = "bookingRescheduledEnabled is required")
    private Boolean bookingRescheduledEnabled;

    @NotEmpty(message = "channels is required")
    private Set<NotificationChannel> channels;

    @NotNull(message = "sound is required")
    private NotificationSound sound;
}
