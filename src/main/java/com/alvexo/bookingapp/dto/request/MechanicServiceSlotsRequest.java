package com.alvexo.bookingapp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MechanicServiceSlotsRequest {

    @NotEmpty(message = "slots must not be empty")
    @Size(max = 2, message = "at most 2 slots are supported")
    @Valid
    private List<ServiceSlotRequest> slots;
}
