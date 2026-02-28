package com.alvexo.bookingapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.SubscriptionPlanRepository;
import com.alvexo.bookingapp.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {
    
    @Autowired
    private SubscriptionPlanRepository planRepository;
    
    @Autowired
    private UserSubscriptionRepository subscriptionRepository;
    
    public List<SubscriptionPlan> getPlansForUserRole(UserRole role) {
        return planRepository.findByUserRoleAndActiveTrue(role);
    }
    
    @Transactional
    public UserSubscription subscribe(User user, Long planId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));
        
        if (plan.getUserRole() != user.getRole()) {
            throw new BadRequestException("This plan is not available for your user role");
        }
        
        // Check if user already has an active subscription
        Optional<UserSubscription> existingSub = subscriptionRepository
                .findActiveSubscription(user, LocalDateTime.now());
        
        if (existingSub.isPresent()) {
            throw new BadRequestException("You already have an active subscription");
        }
        
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(plan.getDurationDays());
        
        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .subscriptionPlan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .autoRenew(true)
                .build();
        
        return subscriptionRepository.save(subscription);
    }
    
    public Optional<UserSubscription> getActiveSubscription(User user) {
        return subscriptionRepository.findActiveSubscription(user, LocalDateTime.now());
    }
    
    @Transactional
    public void cancelSubscription(Long subscriptionId, User user) {
        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        
        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only cancel your own subscription");
        }
        
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setAutoRenew(false);
        
        subscriptionRepository.save(subscription);
    }
}
