package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.LegalDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LegalConsentRequest {

    @NotNull(message = "document is required")
    private LegalDocumentType document;

    @NotBlank(message = "version is required")
    private String version;
}
