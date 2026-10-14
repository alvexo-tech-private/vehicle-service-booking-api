package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkshopInfoRequest {

    @NotBlank(message = "workshopName is required")
    @Size(min = 2, max = 100, message = "workshopName must be 2-100 characters")
    private String workshopName;
}
