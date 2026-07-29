package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.HolidayType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MechanicHolidayRequest {

    @NotNull(message = "date is required")
    private LocalDate date;

    @NotNull(message = "type is required")
    private HolidayType type;

    @Size(max = 255)
    private String reason;
}
