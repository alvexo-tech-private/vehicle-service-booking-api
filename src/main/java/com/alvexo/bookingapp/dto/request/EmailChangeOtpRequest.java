package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailChangeOtpRequest {

    @NotBlank(message = "New email must not be blank")
    @Email(message = "New email must be a valid email address")
    private String newEmail;
}
