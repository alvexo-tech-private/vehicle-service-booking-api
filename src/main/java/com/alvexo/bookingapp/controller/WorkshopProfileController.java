package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.*;
import com.alvexo.bookingapp.dto.response.*;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopImageType;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.WorkshopOperationsService;
import com.alvexo.bookingapp.service.WorkshopProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Workshop Profile",
     description = "The 'My Profile' menu: owner info, workshop info/location, images, referral, onboarding, "
             + "platform verification, operational trust, pickup/drop status, payment setup, notification "
             + "preferences, and account lifecycle. All reads/writes are scoped to the authenticated workshop.")
@RestController
@RequestMapping("/api/workshop-profile")
@PreAuthorize("hasRole('MECHANIC')")
public class WorkshopProfileController {

    private final WorkshopProfileService profileService;
    private final WorkshopOperationsService operationsService;
    private final UserRepository userRepository;

    public WorkshopProfileController(WorkshopProfileService profileService,
                                      WorkshopOperationsService operationsService,
                                      UserRepository userRepository) {
        this.profileService = profileService;
        this.operationsService = operationsService;
        this.userRepository = userRepository;
    }

    // ── Section 0: Profile aggregate ──────────────────────────────────────────

    @Operation(summary = "Get profile aggregate",
               description = "Header, stats, and Current Status hub (status + per-section completion) in one call.")
    @GetMapping
    public ResponseEntity<MyApiResponse<WorkshopProfileAggregateResponse>> getAggregate(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.getAggregate(resolveUser(authentication))));
    }

    // ── Section 2: Owner Information ──────────────────────────────────────────

    @Operation(summary = "Get owner information")
    @GetMapping("/owner")
    public ResponseEntity<MyApiResponse<WorkshopOwnerInfoResponse>> getOwnerInfo(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.getOwnerInfo(resolveUser(authentication))));
    }

    @Operation(summary = "Save owner information")
    @PutMapping("/owner")
    public ResponseEntity<MyApiResponse<WorkshopOwnerInfoResponse>> saveOwnerInfo(
            @Valid @RequestBody WorkshopOwnerInfoRequest request, Authentication authentication) {
        WorkshopOwnerInfoResponse response = profileService.saveOwnerInfo(resolveUser(authentication), request);
        return ResponseEntity.ok(MyApiResponse.success("Owner information saved", response));
    }

    @Operation(summary = "Upload ID proof", description = "multipart/form-data with field `file`.")
    @PostMapping(value = "/owner/id-proof", consumes = "multipart/form-data")
    public ResponseEntity<MyApiResponse<WorkshopOwnerInfoResponse>> uploadIdProof(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        WorkshopOwnerInfoResponse response = profileService.uploadIdProof(resolveUser(authentication), file);
        return ResponseEntity.ok(MyApiResponse.success("ID proof uploaded", response));
    }

    @Operation(summary = "Send WhatsApp verification OTP")
    @PostMapping("/owner/whatsapp/send-otp")
    public ResponseEntity<MyApiResponse<WhatsappOtpResponse>> sendWhatsappOtp(
            @Valid @RequestBody WhatsappSendOtpRequest request, Authentication authentication) {
        WhatsappOtpResponse response = profileService.sendWhatsappOtp(resolveUser(authentication), request.getWhatsappNumber());
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

    @Operation(summary = "Verify WhatsApp OTP")
    @PostMapping("/owner/whatsapp/verify-otp")
    public ResponseEntity<MyApiResponse<WhatsappOtpResponse>> verifyWhatsappOtp(
            @Valid @RequestBody WhatsappVerifyOtpRequest request, Authentication authentication) {
        WhatsappOtpResponse response = profileService.verifyWhatsappOtp(
                resolveUser(authentication), request.getWhatsappNumber(), request.getOtp());
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

    // ── Section 3: Workshop Information ───────────────────────────────────────

    @Operation(summary = "Get workshop information")
    @GetMapping("/info")
    public ResponseEntity<MyApiResponse<WorkshopInfoResponse>> getWorkshopInfo(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.getWorkshopInfo(resolveUser(authentication))));
    }

    @Operation(summary = "Save workshop information")
    @PutMapping("/info")
    public ResponseEntity<MyApiResponse<WorkshopInfoResponse>> saveWorkshopInfo(
            @Valid @RequestBody WorkshopInfoRequest request, Authentication authentication) {
        WorkshopInfoResponse response = profileService.saveWorkshopInfo(resolveUser(authentication), request);
        return ResponseEntity.ok(MyApiResponse.success("Workshop information saved", response));
    }

    // ── Section 4: Workshop Location ──────────────────────────────────────────

    @Operation(summary = "Get workshop location")
    @GetMapping("/location")
    public ResponseEntity<MyApiResponse<WorkshopLocationResponse>> getLocation(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.getLocation(resolveUser(authentication))));
    }

    @Operation(summary = "Save workshop location")
    @PutMapping("/location")
    public ResponseEntity<MyApiResponse<WorkshopLocationResponse>> saveLocation(
            @Valid @RequestBody WorkshopLocationRequest request, Authentication authentication) {
        WorkshopLocationResponse response = profileService.saveLocation(resolveUser(authentication), request);
        return ResponseEntity.ok(MyApiResponse.success("Workshop location saved", response));
    }

    // ── Section 5: Workshop Images ─────────────────────────────────────────────

    @Operation(summary = "List workshop images")
    @GetMapping("/images")
    public ResponseEntity<MyApiResponse<List<WorkshopImageResponse>>> getImages(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.getImages(resolveUser(authentication))));
    }

    @Operation(summary = "Upload a workshop image", description = "multipart/form-data with fields `file` and `type` (shopfront|interior|signage).")
    @PostMapping(value = "/images", consumes = "multipart/form-data")
    public ResponseEntity<MyApiResponse<WorkshopImageResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") WorkshopImageType type,
            Authentication authentication) {
        WorkshopImageResponse response = profileService.uploadImage(resolveUser(authentication), type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(MyApiResponse.success("Image uploaded", response));
    }

    @Operation(summary = "Delete a workshop image")
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<MyApiResponse<Void>> deleteImage(@PathVariable Long imageId, Authentication authentication) {
        profileService.deleteImage(resolveUser(authentication), imageId);
        return ResponseEntity.ok(MyApiResponse.success("Image deleted", null));
    }

    // ── Section 6: Referral Code ───────────────────────────────────────────────

    @Operation(summary = "Validate a referral code")
    @PostMapping("/referral/validate")
    public ResponseEntity<MyApiResponse<ReferralValidateResponse>> validateReferral(
            @Valid @RequestBody ReferralCodeRequest request) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.validateReferralCode(request.getCode())));
    }

    @Operation(summary = "Apply a referral code")
    @PutMapping("/referral")
    public ResponseEntity<MyApiResponse<WorkshopReferralResponse>> applyReferral(
            @Valid @RequestBody ReferralCodeRequest request, Authentication authentication) {
        WorkshopReferralResponse response = profileService.applyReferralCode(resolveUser(authentication), request.getCode());
        return ResponseEntity.ok(MyApiResponse.success("Referral code applied", response));
    }

    // ── Section 7: Submit Onboarding ───────────────────────────────────────────

    @Operation(summary = "Get onboarding status", description = "Checklist, fee status, reference ID, and decision.")
    @GetMapping("/onboarding")
    public ResponseEntity<MyApiResponse<WorkshopOnboardingResponse>> getOnboarding(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.getOnboarding(resolveUser(authentication))));
    }

    @Operation(summary = "Pay the onboarding fee")
    @PostMapping("/onboarding/pay")
    public ResponseEntity<MyApiResponse<OnboardingPayResponse>> payOnboarding(
            @Valid @RequestBody OnboardingPayRequest request, Authentication authentication) {
        OnboardingPayResponse response = profileService.payOnboardingFee(resolveUser(authentication), request.getAmount());
        return ResponseEntity.ok(MyApiResponse.success("Onboarding fee paid", response));
    }

    @Operation(summary = "Submit onboarding",
               description = "Fails with ONBOARDING_INCOMPLETE if any required section is incomplete, or FEE_UNPAID if the fee hasn't been paid.")
    @PostMapping("/onboarding/submit")
    public ResponseEntity<MyApiResponse<OnboardingSubmitResponse>> submitOnboarding(Authentication authentication) {
        OnboardingSubmitResponse response = profileService.submitOnboarding(resolveUser(authentication));
        return ResponseEntity.ok(MyApiResponse.success("Onboarding submitted", response));
    }

    // ── Section 8: Platform Verified ───────────────────────────────────────────

    @Operation(summary = "Get platform verification status")
    @GetMapping("/verification")
    public ResponseEntity<MyApiResponse<WorkshopVerificationResponse>> getVerification(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.getVerification(resolveUser(authentication))));
    }

    @Operation(summary = "Submit platform verification", description = "Requires at least 2 of 3 verification methods. Only available while the workshop is new to the app.")
    @PostMapping("/verification/submit")
    public ResponseEntity<MyApiResponse<WorkshopVerificationResponse>> submitVerification(
            @Valid @RequestBody VerificationSubmitRequest request, Authentication authentication) {
        WorkshopVerificationResponse response = profileService.submitVerification(resolveUser(authentication), request.getMethods());
        return ResponseEntity.ok(MyApiResponse.success("Verification submitted", response));
    }

    // ── Section 9: Operational Trust ──────────────────────────────────────────

    @Operation(summary = "Get operational trust outcome", description = "Read-only: under_evaluation | verified | suspended.")
    @GetMapping("/operational-trust")
    public ResponseEntity<MyApiResponse<OperationalTrustResponse>> getOperationalTrust(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(profileService.getOperationalTrust(resolveUser(authentication))));
    }

    // ── Section 10: Pickup & Drop Status ──────────────────────────────────────

    @Operation(summary = "Get pickup & drop status", description = "Read-only view of the 5 pickup/drop stages.")
    @GetMapping("/pickup-drop-status")
    public ResponseEntity<MyApiResponse<PickupDropStatusResponse>> getPickupDropStatus(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(operationsService.getPickupDropStatus(resolveUser(authentication))));
    }

    // ── Section 11: Payment Setup ──────────────────────────────────────────────

    @Operation(summary = "Get payment setup status")
    @GetMapping("/payment")
    public ResponseEntity<MyApiResponse<WorkshopPaymentResponse>> getPaymentSetup(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(operationsService.getPaymentSetup(resolveUser(authentication))));
    }

    @Operation(summary = "Perform a payment setup action", description = "e.g. action=request_approval or action=link_account (with partner + accountRef). Partner-agnostic.")
    @PostMapping("/payment/action")
    public ResponseEntity<MyApiResponse<WorkshopPaymentResponse>> performPaymentAction(
            @Valid @RequestBody PaymentActionRequest request, Authentication authentication) {
        WorkshopPaymentResponse response = operationsService.performPaymentAction(resolveUser(authentication), request);
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

    // ── Section 12: Notifications ─────────────────────────────────────────────

    @Operation(summary = "Get notification preferences")
    @GetMapping("/notifications/preferences")
    public ResponseEntity<MyApiResponse<NotificationPreferencesResponse>> getNotificationPreferences(
            Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(operationsService.getNotificationPreferences(resolveUser(authentication))));
    }

    @Operation(summary = "Save notification preferences", description = "Requires at least one channel. Payment notifications are always on and cannot be disabled.")
    @PutMapping("/notifications/preferences")
    public ResponseEntity<MyApiResponse<NotificationPreferencesResponse>> saveNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesRequest request, Authentication authentication) {
        NotificationPreferencesResponse response =
                operationsService.saveNotificationPreferences(resolveUser(authentication), request);
        return ResponseEntity.ok(MyApiResponse.success("Notification preferences saved", response));
    }

    // ── Section 13: Account (workshop-specific) ───────────────────────────────

    @Operation(summary = "Deactivate the workshop account")
    @PostMapping("/account/deactivate")
    public ResponseEntity<MyApiResponse<Void>> deactivateAccount(
            @Valid @RequestBody AccountDeactivateRequest request, Authentication authentication) {
        profileService.deactivateAccount(resolveUser(authentication), request.getReason());
        return ResponseEntity.ok(MyApiResponse.success("Account deactivated", null));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
