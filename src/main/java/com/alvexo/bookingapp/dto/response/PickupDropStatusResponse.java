package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.PickupDropStage;
import com.alvexo.bookingapp.model.PickupDropStageState;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupDropStatusResponse {
    private Boolean facilityEnabled;
    private List<StageStatus> statuses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageStatus {
        private PickupDropStage key;
        private String label;
        private PickupDropStageState state;
    }
}
