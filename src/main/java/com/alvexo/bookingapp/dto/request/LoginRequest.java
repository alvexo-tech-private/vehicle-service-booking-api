package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username (email or mobile) is required")
    private String username; // Can be email or mobile number
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String deviceId;
    private String deviceType;
}
