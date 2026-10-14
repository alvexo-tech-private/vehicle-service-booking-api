package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.WorkshopStatus;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopProfileAggregateResponse {
    private String id;
    private String name;
    private String jobCardType;
    private String avatarUrl;
    private Stats stats;
    private WorkshopStatus status;
    private Map<String, SectionComplete> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private Integer totalJobs;
        private Double rating;
        private Integer reviews;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionComplete {
        private boolean complete;
    }
}
