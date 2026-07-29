package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.LegalDocumentType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopLegalConsentResponse {
    private LegalDocumentType document;
    private String version;
    private LocalDateTime acceptedAt;
}
