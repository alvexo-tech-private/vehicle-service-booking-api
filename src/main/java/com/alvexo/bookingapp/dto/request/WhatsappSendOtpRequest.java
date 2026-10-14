package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WhatsappSendOtpRequest {

    @NotBlank(message = "whatsappNumber is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "whatsappNumber must be a valid phone number")
    private String whatsappNumber;
}
