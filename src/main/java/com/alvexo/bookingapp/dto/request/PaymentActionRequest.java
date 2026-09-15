package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentActionRequest {

    /** e.g. "request_approval", "link_account". */
    @NotBlank(message = "action is required")
    private String action;

    /** Required for action=link_account. Payment-partner-agnostic label supplied by the caller. */
    private String partner;

    /** Required for action=link_account. */
    private String accountRef;
}
