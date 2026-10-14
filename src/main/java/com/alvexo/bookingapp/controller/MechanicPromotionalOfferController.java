package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.MechanicPromotionalOfferRequest;
import com.alvexo.bookingapp.dto.response.MechanicPromotionalOfferResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicPromotionalOfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Promotional Offers", description = "Mechanic-authored discounts shown on the Home dashboard (spec §7 #76).")
@RestController
@RequestMapping("/api/mechanic-settings/promotional-offers")
public class MechanicPromotionalOfferController {

    private final MechanicPromotionalOfferService offerService;
    private final UserRepository userRepository;

    public MechanicPromotionalOfferController(MechanicPromotionalOfferService offerService,
                                                UserRepository userRepository) {
        this.offerService = offerService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Create a promotional offer")
    @PostMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicPromotionalOfferResponse>> createOffer(
            @Valid @RequestBody MechanicPromotionalOfferRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Offer created successfully",
                        offerService.createOffer(mechanic, request)));
    }

    @Operation(summary = "Update a promotional offer")
    @PutMapping("/{offerId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicPromotionalOfferResponse>> updateOffer(
            @PathVariable Long offerId,
            @Valid @RequestBody MechanicPromotionalOfferRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success("Offer updated successfully",
                offerService.updateOffer(mechanic, offerId, request)));
    }

    @Operation(summary = "Delete a promotional offer")
    @DeleteMapping("/{offerId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteOffer(
            @PathVariable Long offerId,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        offerService.deleteOffer(mechanic, offerId);
        return ResponseEntity.ok(MyApiResponse.success("Offer deleted successfully", null));
    }

    @Operation(summary = "List all offers for a mechanic (mechanic's own management view)")
    @GetMapping("/{mechanicId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<List<MechanicPromotionalOfferResponse>>> getAllOffers(
            @PathVariable Long mechanicId) {
        return ResponseEntity.ok(MyApiResponse.success(offerService.getAllOffers(mechanicId)));
    }

    @Operation(summary = "List currently active offers (customer-facing)")
    @GetMapping("/{mechanicId}/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<List<MechanicPromotionalOfferResponse>>> getActiveOffers(
            @PathVariable Long mechanicId) {
        return ResponseEntity.ok(MyApiResponse.success(offerService.getActiveOffers(mechanicId)));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
