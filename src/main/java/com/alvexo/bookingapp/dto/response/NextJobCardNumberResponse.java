package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** GET /api/service-desk/next-job-card-number (§1.6). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextJobCardNumberResponse {
    private String jobCardNumber;
}
