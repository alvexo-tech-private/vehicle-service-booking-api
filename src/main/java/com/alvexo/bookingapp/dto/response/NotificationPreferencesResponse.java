package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.NotificationChannel;
import com.alvexo.bookingapp.model.NotificationSound;
import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesResponse {
    private BookingToggles booking;
    private PaymentToggle payment;
    private Set<NotificationChannel> channels;
    private NotificationSound sound;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingToggles {
        private Boolean newBooking;
        private Boolean bookingCancelled;
        private Boolean bookingRescheduled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentToggle {
        private Boolean enabled;
        private Boolean locked;
    }
}
