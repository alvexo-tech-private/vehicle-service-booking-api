package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopOwnerInfoResponse {
    private String ownerName;
    private String primaryPhone;
    private String secondaryPhone;
    private String whatsappNumber;
    private Boolean whatsappVerified;
    private String email;
    private String idProofUrl;
}
