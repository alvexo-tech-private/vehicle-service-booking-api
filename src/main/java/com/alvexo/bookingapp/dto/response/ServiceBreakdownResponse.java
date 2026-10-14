package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.ServiceCategory;
import lombok.*;

/** One row of the Home dashboard service table — F (booked) / I (issued) / P (pending). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBreakdownResponse {

    private Long serviceId;
    private String serviceName;
    private ServiceCategory category;
    private Integer maxSlotsPerDay;

    /** F — total bookings today for this service (excluding cancelled/rejected). */
    private Long bookedCount;

    /** I — bookings today with a job card already issued. */
    private Long issuedCount;

    /** P — bookings today still pending approval. */
    private Long pendingCount;
}
