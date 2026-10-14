package com.alvexo.bookingapp.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProposeDateRequest {

    @NotNull(message = "proposedDateTime is required")
    private LocalDateTime proposedDateTime;
}
