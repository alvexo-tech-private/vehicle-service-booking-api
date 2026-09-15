package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.HolidayType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicHolidayResponse {

    private Long id;
    private LocalDate date;
    private HolidayType type;
    private String reason;
    private LocalDateTime createdAt;
}
