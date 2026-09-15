package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.OnboardingDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Admin-only: records the outcome of reviewing a workshop's onboarding submission.
 */
@Data
public class OnboardingDecisionRequest {

    @NotNull(message = "decision is required")
    private OnboardingDecision decision;

    @Size(max = 500, message = "reason must be at most 500 characters")
    private String reason;
}
