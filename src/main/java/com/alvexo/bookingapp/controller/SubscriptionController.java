package com.alvexo.bookingapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.SubscriptionPlan;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserSubscription;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.SubscriptionService;

import java.util.List;
import java.util.Optional;

@Tag(name = "Subscriptions", description = "Browse subscription plans and manage active subscriptions. Requires JWT token.")
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Operation(summary = "Get subscription plans", description = "Returns all available subscription plans with pricing and features.")
    @GetMapping("/plans")
    public ResponseEntity<MyApiResponse<List<SubscriptionPlan>>> getPlans(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<SubscriptionPlan> plans = subscriptionService.getPlansForUserRole(user.getRole());
        return ResponseEntity.ok(MyApiResponse.success(plans));
    }
    
    @Operation(summary = "Subscribe to a plan", description = "Creates a new subscription for the authenticated user on the selected plan.")
    @PostMapping("/subscribe")
    public ResponseEntity<MyApiResponse<UserSubscription>> subscribe(
            @RequestParam Long planId,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserSubscription subscription = subscriptionService.subscribe(user, planId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Subscribed successfully", subscription));
    }
    
    @Operation(summary = "Get my subscription", description = "Returns the active subscription of the authenticated user, if any.")
    @GetMapping("/my-subscription")
    public ResponseEntity<MyApiResponse<UserSubscription>> getMySubscription(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Optional<UserSubscription> subscription = subscriptionService.getActiveSubscription(user);
        return ResponseEntity.ok(MyApiResponse.success(subscription.orElse(null)));
    }
    
    @Operation(summary = "Cancel a subscription", description = "Cancels an active subscription by its ID.")
    @PostMapping("/cancel/{subscriptionId}")
    public ResponseEntity<MyApiResponse<Void>> cancelSubscription(
            @PathVariable Long subscriptionId,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        subscriptionService.cancelSubscription(subscriptionId, user);
        return ResponseEntity.ok(MyApiResponse.success("Subscription cancelled", null));
    }
}
