package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.VerificationMethod;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class VerificationSubmitRequest {

    @NotEmpty(message = "methods is required")
    private Set<VerificationMethod> methods;
}
