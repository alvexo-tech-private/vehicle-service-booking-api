package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MobileLoginRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{10,15}$")
    private String mobileNumber;

    @NotBlank
    private String password;

    private String deviceId;
    private String deviceType;
}
