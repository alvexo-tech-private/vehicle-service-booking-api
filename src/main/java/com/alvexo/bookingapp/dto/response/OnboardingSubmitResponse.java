package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSubmitResponse {
    private String referenceId;
    private String status;
}
