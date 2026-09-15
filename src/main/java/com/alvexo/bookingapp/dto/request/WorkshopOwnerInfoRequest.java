package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkshopOwnerInfoRequest {

    @NotBlank(message = "ownerName is required")
    @Size(max = 150, message = "ownerName must be at most 150 characters")
    private String ownerName;

    @NotBlank(message = "primaryPhone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "primaryPhone must be a valid phone number")
    private String primaryPhone;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "secondaryPhone must be a valid phone number")
    private String secondaryPhone;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "whatsappNumber must be a valid phone number")
    private String whatsappNumber;

    @Email(message = "email must be a valid email address")
    private String email;
}
