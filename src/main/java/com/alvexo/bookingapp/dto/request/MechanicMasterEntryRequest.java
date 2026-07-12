package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MechanicMasterEntryRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "phone is required")
    @Size(max = 20)
    private String phone;

    private Boolean active = true;

    private Integer displayOrder = 0;
}
