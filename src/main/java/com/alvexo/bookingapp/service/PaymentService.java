package com.alvexo.bookingapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.response.PaymentIntentResponse;
import com.alvexo.bookingapp.dto.response.PaymentResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import com.alvexo.bookingapp.repository.PaymentRepository;
import com.alvexo.bookingapp.repository.UserSubscriptionRepository;
import com.alvexo.bookingapp.util.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserSubscriptionRepository subscriptionRepository;

    @Autowired
    private MechanicSettingsRepository mechanicSettingsRepository;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;
    
    @Transactional
    public PaymentResponse createPayment(User user, PaymentType paymentType, Long relatedEntityId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .user(user)
                .paymentType(paymentType)
                .amount(amount)
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .transactionId(generateTransactionId())
                .build();
        
        if (paymentType == PaymentType.BOOKING_PAYMENT) {
            Booking booking = bookingRepository.findById(relatedEntityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
            payment.setBooking(booking);
        } else if (paymentType == PaymentType.SUBSCRIPTION_PAYMENT) {
            UserSubscription subscription = subscriptionRepository.findById(relatedEntityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
            payment.setSubscription(subscription);
        }
        
        payment = paymentRepository.save(payment);
        return convertToResponse(payment);
    }
    
    @Transactional
    public PaymentResponse confirmPayment(String transactionId, String stripePaymentIntentId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setStripePaymentIntentId(stripePaymentIntentId);
        payment.setPaymentDate(LocalDateTime.now());
        
        payment = paymentRepository.save(payment);
        return convertToResponse(payment);
    }
    
    /**
     * Rider payment contract (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §6) — splits what
     * a booking currently owes into the workshop advance and the rider platform fee, and records
     * each as its own pending Payment row so the platform fee is never mistaken for mechanic
     * earnings. Returns zeros (no rows created) when nothing is currently owed.
     */
    @Transactional
    public PaymentIntentResponse createBookingPaymentIntent(User rider, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getVehicleUser().getId().equals(rider.getId())) {
            throw new BadRequestException("You don't have access to this booking");
        }

        BigDecimal advanceAmount = resolveOwedAdvance(booking);
        BigDecimal platformFee = advanceAmount.signum() > 0 ? Constants.RIDER_PLATFORM_FEE : BigDecimal.ZERO;
        BigDecimal totalPayable = advanceAmount.add(platformFee);

        Payment advancePayment = null;
        if (advanceAmount.signum() > 0) {
            advancePayment = paymentRepository.save(Payment.builder()
                    .user(rider).booking(booking).paymentType(PaymentType.BOOKING_PAYMENT)
                    .amount(advanceAmount).currency("INR").status(PaymentStatus.PENDING)
                    .transactionId(generateTransactionId()).build());
        }
        if (platformFee.signum() > 0) {
            paymentRepository.save(Payment.builder()
                    .user(rider).booking(booking).paymentType(PaymentType.PLATFORM_FEE)
                    .amount(platformFee).currency("INR").status(PaymentStatus.PENDING)
                    .transactionId(generateTransactionId()).build());
        }

        String paymentIntentId = advancePayment != null ? advancePayment.getTransactionId() : generateTransactionId();
        return new PaymentIntentResponse(booking.getId(), advanceAmount, platformFee, totalPayable, "INR", paymentIntentId);
    }

    private BigDecimal resolveOwedAdvance(Booking booking) {
        if (booking.getRequiredAdvanceAmount() != null) {
            // Today Approval flow (§5) — the workshop already fixed this figure at accept time.
            BigDecimal paid = booking.getAdvancePaid() != null ? booking.getAdvancePaid() : BigDecimal.ZERO;
            return booking.getRequiredAdvanceAmount().subtract(paid).max(BigDecimal.ZERO);
        }
        if (Boolean.TRUE.equals(booking.getPickupRequired())) {
            return BigDecimal.ZERO;
        }
        return mechanicSettingsRepository.findByMechanic(booking.getMechanic())
                .filter(MechanicSettings::getAdvanceEnabled)
                .map(MechanicSettings::getAdvanceAmount)
                .orElse(BigDecimal.ZERO);
    }

    public Page<PaymentResponse> getUserPayments(User user, Pageable pageable) {
        return paymentRepository.findByUser(user, pageable)
                .map(this::convertToResponse);
    }
    
    private PaymentResponse convertToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentType(payment.getPaymentType())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paymentDate(payment.getPaymentDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }
    
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
