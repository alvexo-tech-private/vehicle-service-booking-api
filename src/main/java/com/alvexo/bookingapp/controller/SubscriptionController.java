package com.alvexo.bookingapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.SubscriptionPlan;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserSubscription;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.SubscriptionService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlan>>> getPlans(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<SubscriptionPlan> plans = subscriptionService.getPlansForUserRole(user.getRole());
        return ResponseEntity.ok(ApiResponse.success(plans));
    }
    
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<UserSubscription>> subscribe(
            @RequestParam Long planId,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserSubscription subscription = subscriptionService.subscribe(user, planId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscribed successfully", subscription));
    }
    
    @GetMapping("/my-subscription")
    public ResponseEntity<ApiResponse<UserSubscription>> getMySubscription(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Optional<UserSubscription> subscription = subscriptionService.getActiveSubscription(user);
        return ResponseEntity.ok(ApiResponse.success(subscription.orElse(null)));
    }
    
    @PostMapping("/cancel/{subscriptionId}")
    public ResponseEntity<ApiResponse<Void>> cancelSubscription(
            @PathVariable Long subscriptionId,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        subscriptionService.cancelSubscription(subscriptionId, user);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled", null));
    }
}
