package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReferralCodeRequest {

    @NotBlank(message = "code is required")
    @Size(max = 30, message = "code must be at most 30 characters")
    private String code;
}
