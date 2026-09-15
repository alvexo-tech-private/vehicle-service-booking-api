package com.alvexo.bookingapp.dto.response;

import lombok.*;

import java.time.LocalTime;

/** Type 4 (Mechanic Slot) Home dashboard card — Planned / JC Issued per slot. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSlotSummaryResponse {

    private Integer slotNumber;
    private LocalTime slotTime;
    private Boolean enabled;

    /** Planned vehicle capacity configured for this slot. */
    private Integer plannedCount;

    /** Bookings today scheduled at this slot's time with a job card issued. */
    private Long issuedCount;
}
