package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentsResponse {
    private DocRef terms;
    private DocRef privacy;
    private DocRef disclaimer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocRef {
        private String version;
        private String url;
    }
}
