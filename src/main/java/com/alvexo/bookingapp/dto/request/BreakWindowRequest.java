package com.alvexo.bookingapp.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Schema(description = "A break window within the mechanic's working day")
@Data
public class BreakWindowRequest {

    @NotNull(message = "Break start time is required")
    @JsonFormat(pattern = "HH:mm")
    @Schema(example = "13:00", description = "Break start time (HH:mm)")
    private LocalTime start;

    @NotNull(message = "Break end time is required")
    @JsonFormat(pattern = "HH:mm")
    @Schema(example = "14:00", description = "Break end time (HH:mm)")
    private LocalTime end;

    @Schema(example = "Lunch", description = "Optional label e.g. Lunch, Prayer break")
    private String label;
}
