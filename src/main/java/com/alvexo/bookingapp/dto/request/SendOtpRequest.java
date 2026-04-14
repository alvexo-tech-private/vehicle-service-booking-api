package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {

    @Pattern(regexp = "^[0-9]{10,15}$")
    private String mobileNumber;
}
