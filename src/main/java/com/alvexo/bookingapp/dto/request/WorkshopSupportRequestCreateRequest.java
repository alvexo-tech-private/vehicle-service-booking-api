package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.SupportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bound from a multipart/form-data request (category, subject, description, optional image).
 * Word-count limit on description is enforced in the service — length alone can't express it.
 */
@Data
public class WorkshopSupportRequestCreateRequest {

    @NotNull(message = "category is required")
    private SupportCategory category;

    @NotBlank(message = "subject is required")
    @Size(max = 150, message = "subject must be at most 150 characters")
    private String subject;

    @NotBlank(message = "description is required")
    private String description;

    private MultipartFile image;
}
