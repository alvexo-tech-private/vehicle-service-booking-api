package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RequestOtpRequest {

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "captchaId is required")
    private String captchaId;

    @NotBlank(message = "captchaResponse is required")
    private String captchaResponse;

    /** e.g. "FORGOT_PIN", "REGISTRATION" — informational only, not yet branched on. */
    private String purpose;
}
