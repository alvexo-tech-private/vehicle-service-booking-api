package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.BookingChannel;
import com.alvexo.bookingapp.model.BookingStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lightweight booking row for the Home dashboard's waiting-list (behind P)
 * and issued-JC (behind I) drill-downs — enough to identify the vehicle and
 * contact the customer without pulling the full BookingResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {

    private Long bookingId;
    private String bookingNumber;
    private String jobCardNumber;

    private String vehicleRegistrationNumber;
    private String vehicleInfo;

    private String customerName;
    private String customerPhone;

    private String serviceSettingName;
    private LocalDateTime scheduledDateTime;
    private BookingStatus status;
    private BookingChannel channel;
}
