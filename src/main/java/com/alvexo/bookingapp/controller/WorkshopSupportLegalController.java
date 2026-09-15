package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.LegalConsentRequest;
import com.alvexo.bookingapp.dto.request.WorkshopSupportRequestCreateRequest;
import com.alvexo.bookingapp.dto.response.LegalDocumentsResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.WorkshopLegalConsentResponse;
import com.alvexo.bookingapp.dto.response.WorkshopSupportRequestResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.WorkshopSupportLegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workshop Support & Legal",
     description = "Feedback & support tickets and legal documents/consents (Menus 14-15 of the Workshop Profile).")
@RestController
@RequestMapping("/api/workshop-profile")
public class WorkshopSupportLegalController {

    private final WorkshopSupportLegalService service;
    private final UserRepository userRepository;

    public WorkshopSupportLegalController(WorkshopSupportLegalService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    // ── Section 14: Feedback & Support ────────────────────────────────────────

    @Operation(summary = "List my support requests")
    @GetMapping("/support/requests")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<List<WorkshopSupportRequestResponse>>> getSupportRequests(
            Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(service.getSupportRequests(resolveUser(authentication))));
    }

    @Operation(summary = "Raise a support request",
               description = "multipart/form-data with fields `category`, `subject`, `description` (<=200 words), optional `image` (<=2MB).")
    @PostMapping(value = "/support/requests", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<WorkshopSupportRequestResponse>> createSupportRequest(
            @Valid @ModelAttribute WorkshopSupportRequestCreateRequest request, Authentication authentication) {
        WorkshopSupportRequestResponse response = service.createSupportRequest(resolveUser(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MyApiResponse.success("Support request submitted", response));
    }

    // ── Section 15: Legal ──────────────────────────────────────────────────────

    @Operation(summary = "Get legal documents", description = "Terms, Privacy, and Disclaimer with current version and URL. Available to any authenticated user.")
    @GetMapping("/legal/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<LegalDocumentsResponse>> getLegalDocuments() {
        return ResponseEntity.ok(MyApiResponse.success(service.getLegalDocuments()));
    }

    @Operation(summary = "Get my consent history")
    @GetMapping("/legal/consents")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<List<WorkshopLegalConsentResponse>>> getConsents(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(service.getConsents(resolveUser(authentication))));
    }

    @Operation(summary = "Record a legal consent")
    @PostMapping("/legal/consents")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<WorkshopLegalConsentResponse>> recordConsent(
            @Valid @RequestBody LegalConsentRequest request, Authentication authentication) {
        WorkshopLegalConsentResponse response = service.recordConsent(resolveUser(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MyApiResponse.success("Consent recorded", response));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
