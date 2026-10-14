package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.VerificationMethod;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopVerificationResponse {
    private Boolean eligible;
    private List<MethodStatus> methods;
    private Boolean verified;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodStatus {
        private VerificationMethod key;
        private String label;
        private boolean done;
    }
}
