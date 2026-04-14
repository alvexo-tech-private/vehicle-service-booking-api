package com.alvexo.bookingapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.response.PaymentResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.PaymentRepository;
import com.alvexo.bookingapp.repository.UserSubscriptionRepository;

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
