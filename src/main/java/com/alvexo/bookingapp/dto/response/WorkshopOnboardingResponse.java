package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.OnboardingDecision;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopOnboardingResponse {
    private List<ChecklistItem> checklist;
    private BigDecimal feeAmount;
    private Boolean feePaid;
    private String referenceId;
    private OnboardingDecision decision;
    private String decisionReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistItem {
        private String section;
        private boolean complete;
    }
}
