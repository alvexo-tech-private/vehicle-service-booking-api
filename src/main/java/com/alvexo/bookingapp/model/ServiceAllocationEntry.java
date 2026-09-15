package com.alvexo.bookingapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of the Service Allocation table (WORKSHOP_API_AUDIT_AND_BACKEND_SPECIFICATION.md §1.2) —
 * whether a service type is handled automatically or assigned to a mechanic.
 * Persisted as a JSON array on {@link MechanicSettings#getServiceAllocations()}; also used
 * directly as the request/response shape since it carries no validation asymmetry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAllocationEntry {
    private String serviceType;
    private Boolean auto;
    private Boolean mechanic;
}
