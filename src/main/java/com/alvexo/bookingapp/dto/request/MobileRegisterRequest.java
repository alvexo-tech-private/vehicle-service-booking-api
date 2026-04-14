package com.alvexo.bookingapp.dto.request;


import com.alvexo.bookingapp.model.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MobileRegisterRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{10,15}$")
    private String mobileNumber;

    @NotBlank
    private String password;
    
    @NotBlank
    private String email;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotNull
    private UserRole role;
}
