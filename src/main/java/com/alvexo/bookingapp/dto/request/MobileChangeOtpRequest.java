package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MobileChangeOtpRequest {

    @NotBlank(message = "New mobile number must not be blank")
    @Pattern(regexp = "^[+0-9]{10,15}$", message = "New mobile number must be 10–15 digits")
    private String newMobile;
}
