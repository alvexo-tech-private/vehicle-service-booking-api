package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.ServiceWorkspaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** POST /api/service-desk/bookings/bulk-status (§1.8) — per-item outcome, since disallowed transitions are skipped, not failed. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusResultResponse {
    private List<Item> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String bookingId;
        private boolean applied;
        private ServiceWorkspaceStatus resultingStatus;
        private String message;
    }
}
