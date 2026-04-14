package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.BookingType;
import com.alvexo.bookingapp.model.ServiceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingNumber;
    private String jobCardNumber;

    private Long vehicleUserId;
    private String vehicleUserName;
    private Long mechanicId;
    private String mechanicName;
    private Long vehicleId;
    private String vehicleInfo;

    /** Populated when booking is linked to a mechanic service setting */
    private Long serviceSettingId;
    private String serviceSettingName;

    private LocalDateTime scheduledDateTime;
    private BookingStatus status;
    private BookingType bookingType;
    private ServiceType serviceType;
    private String description;

    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private Integer estimatedDurationMinutes;
    private Integer actualDurationMinutes;

    private BigDecimal advancePaid;

    private String mechanicNotes;
    private String customerNotes;
    private String cancellationReason;

    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
