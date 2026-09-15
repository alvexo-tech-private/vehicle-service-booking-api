package com.alvexo.bookingapp.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicServiceSlotResponse {

    private Long id;
    private Integer slotNumber;
    private LocalTime slotTime;
    private Integer repairQty;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
