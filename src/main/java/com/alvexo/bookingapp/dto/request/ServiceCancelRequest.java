package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT .../cancel body, shared by the Today (§1.7) and Service Week (§2.5)
 * tabs. The mechanic app is expected to pre-fill this with the default
 * cancellation template and let the mechanic edit it; an empty message is
 * rejected.
 */
@Data
public class ServiceCancelRequest {

    @NotBlank(message = "message is required")
    @Size(max = 500, message = "message must be at most 500 characters")
    private String message;
}
