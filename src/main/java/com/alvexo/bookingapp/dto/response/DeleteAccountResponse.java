package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountResponse {
    private String requestId;
    private String status;
}
