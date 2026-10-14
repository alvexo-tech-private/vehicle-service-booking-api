package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementFaqEntryResponse {
    private String question;
    private String answer;
}
