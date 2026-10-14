package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.LegalConsentRequest;
import com.alvexo.bookingapp.dto.request.WorkshopSupportRequestCreateRequest;
import com.alvexo.bookingapp.dto.response.LegalDocumentsResponse;
import com.alvexo.bookingapp.dto.response.WorkshopLegalConsentResponse;
import com.alvexo.bookingapp.dto.response.WorkshopSupportRequestResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.BusinessRuleException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.LegalDocumentRepository;
import com.alvexo.bookingapp.repository.WorkshopLegalConsentRepository;
import com.alvexo.bookingapp.repository.WorkshopSupportRequestRepository;
import com.alvexo.bookingapp.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkshopSupportLegalService {

    private final WorkshopSupportRequestRepository supportRequestRepository;
    private final LegalDocumentRepository legalDocumentRepository;
    private final WorkshopLegalConsentRepository legalConsentRepository;
    private final FileStorageService fileStorageService;

    public WorkshopSupportLegalService(WorkshopSupportRequestRepository supportRequestRepository,
                                       LegalDocumentRepository legalDocumentRepository,
                                       WorkshopLegalConsentRepository legalConsentRepository,
                                       FileStorageService fileStorageService) {
        this.supportRequestRepository = supportRequestRepository;
        this.legalDocumentRepository = legalDocumentRepository;
        this.legalConsentRepository = legalConsentRepository;
        this.fileStorageService = fileStorageService;
    }

    // ── Section 14: Feedback & Support ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WorkshopSupportRequestResponse> getSupportRequests(User mechanic) {
        validateRole(mechanic);
        return supportRequestRepository.findByMechanicOrderByCreatedAtDesc(mechanic).stream()
                .map(this::toSupportResponse).collect(Collectors.toList());
    }

    @Transactional
    public WorkshopSupportRequestResponse createSupportRequest(User mechanic, WorkshopSupportRequestCreateRequest request) {
        validateRole(mechanic);

        int wordCount = request.getDescription().trim().isEmpty() ? 0
                : request.getDescription().trim().split("\\s+").length;
        if (wordCount > Constants.SUPPORT_DESCRIPTION_MAX_WORDS) {
            throw new BusinessRuleException("VALIDATION_ERROR",
                    "description must be at most " + Constants.SUPPORT_DESCRIPTION_MAX_WORDS + " words");
        }

        StoredFile imageFile = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            imageFile = fileStorageService.store(request.getImage(), mechanic,
                    Constants.MAX_SUPPORT_IMAGE_SIZE_BYTES, Constants.ALLOWED_IMAGE_CONTENT_TYPES);
        }

        WorkshopSupportRequest supportRequest = WorkshopSupportRequest.builder()
                .mechanic(mechanic)
                .referenceId("SR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .category(request.getCategory())
                .subject(request.getSubject())
                .description(request.getDescription())
                .imageFile(imageFile)
                .build();

        return toSupportResponse(supportRequestRepository.save(supportRequest));
    }

    private WorkshopSupportRequestResponse toSupportResponse(WorkshopSupportRequest r) {
        return WorkshopSupportRequestResponse.builder()
                .id(r.getReferenceId())
                .category(r.getCategory())
                .subject(r.getSubject())
                .description(r.getDescription())
                .imageUrl(r.getImageFile() != null ? "/api/files/" + r.getImageFile().getId() : null)
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    // ── Section 15: Legal ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LegalDocumentsResponse getLegalDocuments() {
        return LegalDocumentsResponse.builder()
                .terms(toDocRef(LegalDocumentType.TERMS))
                .privacy(toDocRef(LegalDocumentType.PRIVACY))
                .disclaimer(toDocRef(LegalDocumentType.DISCLAIMER))
                .build();
    }

    private LegalDocumentsResponse.DocRef toDocRef(LegalDocumentType type) {
        LegalDocument doc = legalDocumentRepository.findByDocumentType(type)
                .orElseThrow(() -> new ResourceNotFoundException(type + " document not configured"));
        return LegalDocumentsResponse.DocRef.builder().version(doc.getVersion()).url(doc.getUrl()).build();
    }

    @Transactional(readOnly = true)
    public List<WorkshopLegalConsentResponse> getConsents(User mechanic) {
        validateRole(mechanic);
        return legalConsentRepository.findByMechanicOrderByAcceptedAtDesc(mechanic).stream()
                .map(this::toConsentResponse).collect(Collectors.toList());
    }

    @Transactional
    public WorkshopLegalConsentResponse recordConsent(User mechanic, LegalConsentRequest request) {
        validateRole(mechanic);
        legalDocumentRepository.findByDocumentType(request.getDocument())
                .orElseThrow(() -> new ResourceNotFoundException(request.getDocument() + " document not configured"));

        WorkshopLegalConsent consent = WorkshopLegalConsent.builder()
                .mechanic(mechanic)
                .documentType(request.getDocument())
                .version(request.getVersion())
                .build();
        return toConsentResponse(legalConsentRepository.save(consent));
    }

    private WorkshopLegalConsentResponse toConsentResponse(WorkshopLegalConsent c) {
        return WorkshopLegalConsentResponse.builder()
                .document(c.getDocumentType())
                .version(c.getVersion())
                .acceptedAt(c.getAcceptedAt())
                .build();
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only workshops (mechanics) can manage support requests or legal consents");
        }
    }
}
