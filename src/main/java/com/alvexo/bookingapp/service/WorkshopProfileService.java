package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.*;
import com.alvexo.bookingapp.dto.response.*;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.BusinessRuleException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.*;
import com.alvexo.bookingapp.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkshopProfileService {

    private final UserRepository userRepository;
    private final WorkshopProfileRepository profileRepository;
    private final WorkshopOwnerInfoRepository ownerInfoRepository;
    private final WorkshopImageRepository imageRepository;
    private final ReferralPartnerCodeRepository referralPartnerCodeRepository;
    private final WorkshopReferralRepository referralRepository;
    private final WorkshopOnboardingRepository onboardingRepository;
    private final WorkshopVerificationRepository verificationRepository;
    private final FileStorageService fileStorageService;
    private final OtpService otpService;
    private final NotificationService notificationService;

    public WorkshopProfileService(UserRepository userRepository,
                                   WorkshopProfileRepository profileRepository,
                                   WorkshopOwnerInfoRepository ownerInfoRepository,
                                   WorkshopImageRepository imageRepository,
                                   ReferralPartnerCodeRepository referralPartnerCodeRepository,
                                   WorkshopReferralRepository referralRepository,
                                   WorkshopOnboardingRepository onboardingRepository,
                                   WorkshopVerificationRepository verificationRepository,
                                   FileStorageService fileStorageService,
                                   OtpService otpService,
                                   NotificationService notificationService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.ownerInfoRepository = ownerInfoRepository;
        this.imageRepository = imageRepository;
        this.referralPartnerCodeRepository = referralPartnerCodeRepository;
        this.referralRepository = referralRepository;
        this.onboardingRepository = onboardingRepository;
        this.verificationRepository = verificationRepository;
        this.fileStorageService = fileStorageService;
        this.otpService = otpService;
        this.notificationService = notificationService;
    }

    // ── Section 0: Profile aggregate ──────────────────────────────────────────

    @Transactional
    public WorkshopProfileAggregateResponse getAggregate(User mechanic) {
        validateRole(mechanic);
        WorkshopProfile profile = getOrCreateProfile(mechanic);
        Map<String, Boolean> completion = computeSectionCompletion(mechanic);

        Map<String, WorkshopProfileAggregateResponse.SectionComplete> sections = completion.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> WorkshopProfileAggregateResponse.SectionComplete.builder().complete(e.getValue()).build()));

        return WorkshopProfileAggregateResponse.builder()
                .id("WS-" + mechanic.getId())
                .name(mechanic.getWorkshopName())
                .jobCardType("Mechanic Job Card")
                .avatarUrl(mechanic.getProfileImageUrl())
                .stats(WorkshopProfileAggregateResponse.Stats.builder()
                        .totalJobs(mechanic.getTotalBookingsCompleted())
                        .rating(mechanic.getRating() != null ? mechanic.getRating().doubleValue() : null)
                        .reviews(mechanic.getTotalReviews())
                        .active(mechanic.getActive())
                        .build())
                .status(profile.getStatus())
                .sections(sections)
                .build();
    }

    /**
     * Completion of every Manage section, keyed by SectionKey. Reused by the
     * aggregate response and the onboarding checklist so both stay in sync.
     */
    private Map<String, Boolean> computeSectionCompletion(User mechanic) {
        Map<String, Boolean> sections = new LinkedHashMap<>();
        sections.put("owner", ownerInfoRepository.findByMechanic(mechanic).isPresent());
        sections.put("workshop", mechanic.getWorkshopName() != null && !mechanic.getWorkshopName().isBlank());
        sections.put("location", isLocationComplete(mechanic));
        sections.put("images", hasAllMandatoryImages(mechanic));
        sections.put("referral", referralRepository.findByMechanic(mechanic).isPresent());
        sections.put("onboarding", onboardingRepository.findByMechanic(mechanic)
                .map(WorkshopOnboarding::getSubmitted).orElse(false));
        sections.put("verification", verificationRepository.findByMechanic(mechanic)
                .map(WorkshopVerification::getVerified).orElse(false));
        return sections;
    }

    private boolean isLocationComplete(User mechanic) {
        return mechanic.getAddressLine1() != null && !mechanic.getAddressLine1().isBlank()
                && mechanic.getCity() != null && !mechanic.getCity().isBlank()
                && mechanic.getState() != null && !mechanic.getState().isBlank()
                && mechanic.getPostalCode() != null && !mechanic.getPostalCode().isBlank();
    }

    private boolean hasAllMandatoryImages(User mechanic) {
        return Arrays.stream(WorkshopImageType.values())
                .allMatch(type -> imageRepository.existsByMechanicAndType(mechanic, type));
    }

    private WorkshopProfile getOrCreateProfile(User mechanic) {
        return profileRepository.findByMechanic(mechanic)
                .orElseGet(() -> profileRepository.save(WorkshopProfile.builder().mechanic(mechanic).build()));
    }

    // ── Section 2: Owner Information ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public WorkshopOwnerInfoResponse getOwnerInfo(User mechanic) {
        validateRole(mechanic);
        WorkshopOwnerInfo info = ownerInfoRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Owner information not set yet"));
        return toOwnerInfoResponse(info);
    }

    @Transactional
    public WorkshopOwnerInfoResponse saveOwnerInfo(User mechanic, WorkshopOwnerInfoRequest request) {
        validateRole(mechanic);
        WorkshopOwnerInfo info = ownerInfoRepository.findByMechanic(mechanic)
                .orElse(WorkshopOwnerInfo.builder().mechanic(mechanic).build());

        boolean whatsappChanged = info.getWhatsappNumber() == null
                || !info.getWhatsappNumber().equals(request.getWhatsappNumber());

        info.setOwnerName(request.getOwnerName());
        info.setPrimaryPhone(request.getPrimaryPhone());
        info.setSecondaryPhone(request.getSecondaryPhone());
        info.setWhatsappNumber(request.getWhatsappNumber());
        info.setEmail(request.getEmail());
        if (whatsappChanged) {
            info.setWhatsappVerified(false);
        }

        return toOwnerInfoResponse(ownerInfoRepository.save(info));
    }

    @Transactional
    public WorkshopOwnerInfoResponse uploadIdProof(User mechanic, MultipartFile file) {
        validateRole(mechanic);
        WorkshopOwnerInfo info = ownerInfoRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new BadRequestException("Save owner information before uploading an ID proof"));

        StoredFile stored = fileStorageService.store(file, mechanic,
                Constants.MAX_IMAGE_SIZE_BYTES, Constants.ALLOWED_IMAGE_CONTENT_TYPES);
        info.setIdProofFile(stored);
        return toOwnerInfoResponse(ownerInfoRepository.save(info));
    }

    @Transactional
    public WhatsappOtpResponse sendWhatsappOtp(User mechanic, String whatsappNumber) {
        validateRole(mechanic);
        String otp = otpService.generateAndSaveOtpForMobile(whatsappNumber, mechanic.getEmail());
        notificationService.sendWhatsappVerificationOtp(mechanic.getEmail(), otp);
        return WhatsappOtpResponse.builder().otpSent(true).expiresInSec(300).build();
    }

    @Transactional
    public WhatsappOtpResponse verifyWhatsappOtp(User mechanic, String whatsappNumber, String otp) {
        validateRole(mechanic);
        try {
            otpService.validateOtpByMobile(whatsappNumber, otp);
        } catch (BadRequestException e) {
            String code = e.getMessage() != null && e.getMessage().toLowerCase().contains("expired")
                    ? "OTP_EXPIRED" : "OTP_INVALID";
            throw new BusinessRuleException(code, e.getMessage());
        }

        WorkshopOwnerInfo info = ownerInfoRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new BadRequestException("Save owner information before verifying WhatsApp"));
        info.setWhatsappNumber(whatsappNumber);
        info.setWhatsappVerified(true);
        ownerInfoRepository.save(info);

        return WhatsappOtpResponse.builder().verified(true).build();
    }

    private WorkshopOwnerInfoResponse toOwnerInfoResponse(WorkshopOwnerInfo info) {
        return WorkshopOwnerInfoResponse.builder()
                .ownerName(info.getOwnerName())
                .primaryPhone(info.getPrimaryPhone())
                .secondaryPhone(info.getSecondaryPhone())
                .whatsappNumber(info.getWhatsappNumber())
                .whatsappVerified(info.getWhatsappVerified())
                .email(info.getEmail())
                .idProofUrl(info.getIdProofFile() != null ? "/api/files/" + info.getIdProofFile().getId() : null)
                .build();
    }

    // ── Section 3: Workshop Information ───────────────────────────────────────

    @Transactional(readOnly = true)
    public WorkshopInfoResponse getWorkshopInfo(User mechanic) {
        validateRole(mechanic);
        return WorkshopInfoResponse.builder().workshopName(mechanic.getWorkshopName()).build();
    }

    @Transactional
    public WorkshopInfoResponse saveWorkshopInfo(User mechanic, WorkshopInfoRequest request) {
        validateRole(mechanic);
        mechanic.setWorkshopName(request.getWorkshopName());
        userRepository.save(mechanic);
        return WorkshopInfoResponse.builder().workshopName(mechanic.getWorkshopName()).build();
    }

    // ── Section 4: Workshop Location ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public WorkshopLocationResponse getLocation(User mechanic) {
        validateRole(mechanic);
        return toLocationResponse(mechanic);
    }

    @Transactional
    public WorkshopLocationResponse saveLocation(User mechanic, WorkshopLocationRequest request) {
        validateRole(mechanic);
        if (!Constants.INDIAN_STATES.contains(request.getState())) {
            throw new BusinessRuleException("VALIDATION_ERROR", "state must be a valid Indian state or union territory");
        }

        mechanic.setAddressLine1(request.getAddressLine1());
        mechanic.setAddressLine2(request.getAddressLine2());
        mechanic.setCity(request.getCity());
        mechanic.setState(request.getState());
        mechanic.setPostalCode(request.getPincode());
        mechanic.setLatitude(request.getLatitude());
        mechanic.setLongitude(request.getLongitude());
        userRepository.save(mechanic);

        return toLocationResponse(mechanic);
    }

    private WorkshopLocationResponse toLocationResponse(User mechanic) {
        return WorkshopLocationResponse.builder()
                .addressLine1(mechanic.getAddressLine1())
                .addressLine2(mechanic.getAddressLine2())
                .city(mechanic.getCity())
                .state(mechanic.getState())
                .pincode(mechanic.getPostalCode())
                .latitude(mechanic.getLatitude())
                .longitude(mechanic.getLongitude())
                .build();
    }

    // ── Section 5: Workshop Images ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WorkshopImageResponse> getImages(User mechanic) {
        validateRole(mechanic);
        return imageRepository.findByMechanicOrderByUploadedAtAsc(mechanic).stream()
                .map(this::toImageResponse).collect(Collectors.toList());
    }

    @Transactional
    public WorkshopImageResponse uploadImage(User mechanic, WorkshopImageType type, MultipartFile file) {
        validateRole(mechanic);
        StoredFile stored = fileStorageService.store(file, mechanic,
                Constants.MAX_IMAGE_SIZE_BYTES, Constants.ALLOWED_IMAGE_CONTENT_TYPES);

        WorkshopImage image = WorkshopImage.builder()
                .mechanic(mechanic)
                .type(type)
                .file(stored)
                .build();
        return toImageResponse(imageRepository.save(image));
    }

    @Transactional
    public void deleteImage(User mechanic, Long imageId) {
        validateRole(mechanic);
        WorkshopImage image = imageRepository.findByIdAndMechanic(imageId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        StoredFile file = image.getFile();
        imageRepository.delete(image);
        fileStorageService.delete(file);
    }

    private WorkshopImageResponse toImageResponse(WorkshopImage image) {
        return WorkshopImageResponse.builder()
                .id(image.getId())
                .type(image.getType())
                .url("/api/files/" + image.getFile().getId())
                .uploadedAt(image.getUploadedAt())
                .sizeBytes(image.getFile().getSizeBytes())
                .build();
    }

    // ── Section 6: Referral Code ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReferralValidateResponse validateReferralCode(String code) {
        ReferralPartnerCode partnerCode = referralPartnerCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessRuleException("REFERRAL_INVALID", "This referral code is not recognised"));

        if (partnerCode.getExpired()) {
            throw new BusinessRuleException("REFERRAL_EXPIRED", "This referral code has expired");
        }
        if (!partnerCode.getActive()) {
            throw new BusinessRuleException("REFERRAL_INACTIVE", "This referral code is no longer active");
        }

        return ReferralValidateResponse.builder()
                .valid(true)
                .partnerName(partnerCode.getPartnerName())
                .benefit(partnerCode.getBenefit())
                .build();
    }

    @Transactional
    public WorkshopReferralResponse applyReferralCode(User mechanic, String code) {
        validateRole(mechanic);
        ReferralValidateResponse validated = validateReferralCode(code);

        WorkshopReferral referral = referralRepository.findByMechanic(mechanic)
                .orElse(WorkshopReferral.builder().mechanic(mechanic).build());
        referral.setCode(code);
        referral.setPartnerName(validated.getPartnerName());
        referral.setBenefit(validated.getBenefit());
        referral = referralRepository.save(referral);

        return WorkshopReferralResponse.builder()
                .code(referral.getCode())
                .partnerName(referral.getPartnerName())
                .benefit(referral.getBenefit())
                .appliedAt(referral.getAppliedAt())
                .build();
    }

    // ── Section 7: Submit Onboarding ───────────────────────────────────────────

    @Transactional
    public WorkshopOnboardingResponse getOnboarding(User mechanic) {
        validateRole(mechanic);
        WorkshopOnboarding onboarding = getOrCreateOnboarding(mechanic);
        return toOnboardingResponse(mechanic, onboarding);
    }

    private WorkshopOnboarding getOrCreateOnboarding(User mechanic) {
        return onboardingRepository.findByMechanic(mechanic)
                .orElseGet(() -> onboardingRepository.save(WorkshopOnboarding.builder()
                        .mechanic(mechanic)
                        .feeAmount(Constants.WORKSHOP_ONBOARDING_FEE)
                        .build()));
    }

    @Transactional
    public OnboardingPayResponse payOnboardingFee(User mechanic, java.math.BigDecimal amount) {
        validateRole(mechanic);
        WorkshopOnboarding onboarding = getOrCreateOnboarding(mechanic);

        if (amount.compareTo(onboarding.getFeeAmount()) != 0) {
            throw new BusinessRuleException("VALIDATION_ERROR",
                    "amount must equal the onboarding fee of " + onboarding.getFeeAmount());
        }

        onboarding.setFeePaid(true);
        onboarding.setPaymentRef("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        onboardingRepository.save(onboarding);

        return OnboardingPayResponse.builder().feePaid(true).paymentRef(onboarding.getPaymentRef()).build();
    }

    @Transactional
    public OnboardingSubmitResponse submitOnboarding(User mechanic) {
        validateRole(mechanic);
        WorkshopOnboarding onboarding = getOrCreateOnboarding(mechanic);

        Map<String, Boolean> completion = computeSectionCompletion(mechanic);
        List<String> incomplete = completion.entrySet().stream()
                .filter(e -> !e.getKey().equals("onboarding") && !e.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!incomplete.isEmpty()) {
            throw new BusinessRuleException("ONBOARDING_INCOMPLETE",
                    "Complete these sections before submitting: " + String.join(", ", incomplete));
        }
        if (!onboarding.getFeePaid()) {
            throw new BusinessRuleException("FEE_UNPAID", "The onboarding fee has not been paid yet");
        }

        onboarding.setSubmitted(true);
        onboarding.setSubmittedAt(LocalDateTime.now());
        onboarding.setReferenceId("ONB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        onboardingRepository.save(onboarding);

        WorkshopProfile profile = getOrCreateProfile(mechanic);
        profile.setStatus(WorkshopStatus.PENDING);
        profileRepository.save(profile);

        return OnboardingSubmitResponse.builder()
                .referenceId(onboarding.getReferenceId())
                .status("pending")
                .build();
    }

    /**
     * Admin-only: records the reviewer's decision on a submitted onboarding and
     * transitions the workshop's overall status accordingly.
     */
    @Transactional
    public WorkshopOnboardingResponse recordOnboardingDecision(Long mechanicId, OnboardingDecision decision, String reason) {
        WorkshopOnboarding onboarding = onboardingRepository.findByMechanicId(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding not found for mechanic " + mechanicId));
        if (!onboarding.getSubmitted()) {
            throw new BadRequestException("Onboarding has not been submitted yet");
        }

        onboarding.setDecision(decision);
        onboarding.setDecisionReason(reason);
        onboarding.setDecidedAt(LocalDateTime.now());
        onboardingRepository.save(onboarding);

        WorkshopProfile profile = profileRepository.findByMechanicId(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop profile not found for mechanic " + mechanicId));
        profile.setStatus(switch (decision) {
            case APPROVED -> WorkshopStatus.VERIFIED;
            case REJECTED -> WorkshopStatus.REJECTED;
            case MORE_INFO -> WorkshopStatus.IN_PROGRESS;
        });
        profileRepository.save(profile);

        return toOnboardingResponse(onboarding.getMechanic(), onboarding);
    }

    private WorkshopOnboardingResponse toOnboardingResponse(User mechanic, WorkshopOnboarding onboarding) {
        List<WorkshopOnboardingResponse.ChecklistItem> checklist = computeSectionCompletion(mechanic).entrySet().stream()
                .map(e -> WorkshopOnboardingResponse.ChecklistItem.builder()
                        .section(e.getKey()).complete(e.getValue()).build())
                .collect(Collectors.toList());

        return WorkshopOnboardingResponse.builder()
                .checklist(checklist)
                .feeAmount(onboarding.getFeeAmount())
                .feePaid(onboarding.getFeePaid())
                .referenceId(onboarding.getReferenceId())
                .decision(onboarding.getDecision())
                .decisionReason(onboarding.getDecisionReason())
                .build();
    }

    // ── Section 8: Platform Verified ───────────────────────────────────────────

    @Transactional
    public WorkshopVerificationResponse getVerification(User mechanic) {
        validateRole(mechanic);
        WorkshopProfile profile = getOrCreateProfile(mechanic);
        WorkshopVerification verification = getOrCreateVerification(mechanic);
        return toVerificationResponse(profile, verification);
    }

    private WorkshopVerification getOrCreateVerification(User mechanic) {
        return verificationRepository.findByMechanic(mechanic)
                .orElseGet(() -> verificationRepository.save(WorkshopVerification.builder().mechanic(mechanic).build()));
    }

    @Transactional
    public WorkshopVerificationResponse submitVerification(User mechanic, Set<VerificationMethod> methods) {
        validateRole(mechanic);
        WorkshopProfile profile = getOrCreateProfile(mechanic);
        if (profile.getStatus() != WorkshopStatus.NEW_TO_APP) {
            throw new BusinessRuleException("VALIDATION_ERROR",
                    "Platform verification is only available for workshops that are new to the app");
        }
        if (methods.size() < Constants.PLATFORM_VERIFICATION_MIN_METHODS) {
            throw new BusinessRuleException("VERIFICATION_INSUFFICIENT_METHODS",
                    "At least " + Constants.PLATFORM_VERIFICATION_MIN_METHODS + " verification methods are required");
        }

        WorkshopVerification verification = getOrCreateVerification(mechanic);
        verification.setCompletedMethods(methods);
        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        return toVerificationResponse(profile, verification);
    }

    private WorkshopVerificationResponse toVerificationResponse(WorkshopProfile profile, WorkshopVerification verification) {
        boolean eligible = profile.getStatus() == WorkshopStatus.NEW_TO_APP;
        List<WorkshopVerificationResponse.MethodStatus> methods = Arrays.stream(VerificationMethod.values())
                .map(m -> WorkshopVerificationResponse.MethodStatus.builder()
                        .key(m)
                        .label(labelFor(m))
                        .done(verification.getCompletedMethods().contains(m))
                        .build())
                .collect(Collectors.toList());

        return WorkshopVerificationResponse.builder()
                .eligible(eligible)
                .methods(methods)
                .verified(verification.getVerified())
                .build();
    }

    private String labelFor(VerificationMethod method) {
        return switch (method) {
            case BUSINESS_LICENSE -> "Business License";
            case GST_CERTIFICATE -> "GST Certificate";
            case BANK_STATEMENT -> "Bank Statement";
        };
    }

    // ── Section 9: Operational Trust ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public OperationalTrustResponse getOperationalTrust(User mechanic) {
        validateRole(mechanic);
        WorkshopProfile profile = profileRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop profile not found"));

        String outcome = profile.getSuspended() ? "suspended"
                : profile.getStatus() == WorkshopStatus.VERIFIED ? "verified"
                : "under_evaluation";

        return OperationalTrustResponse.builder()
                .outcome(outcome)
                .suspended(profile.getSuspended())
                .reason(profile.getSuspensionReason())
                .build();
    }

    // ── Section 13: Account (workshop-specific) ───────────────────────────────

    @Transactional
    public void deactivateAccount(User mechanic, String reason) {
        validateRole(mechanic);
        mechanic.setActive(false);
        userRepository.save(mechanic);

        WorkshopProfile profile = getOrCreateProfile(mechanic);
        profile.setSuspensionReason(reason);
        profileRepository.save(profile);
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only workshops (mechanics) can manage a workshop profile");
        }
    }
}
