package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountDeactivateRequest {

    @Size(max = 500, message = "reason must be at most 500 characters")
    private String reason;
}
