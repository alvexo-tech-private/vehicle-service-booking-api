package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.SettlementQueryStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementQueryResponse {

    private String queryReference;
    private SettlementQueryStatus queryStatus;
    private LocalDate submissionDate;
    private LocalTime submissionTime;

    /** Populated on the list endpoint (GET /api/settlements/queries); null on the create response. */
    private LocalDate settlementDate;
    private String description;
}
