package com.alvexo.bookingapp.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.PaymentResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.PaymentType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    private final UserRepository userRepository;
    
    
    
    public PaymentController(PaymentService paymentService, UserRepository userRepository) {
		super();
		this.paymentService = paymentService;
		this.userRepository = userRepository;
	}

	@PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestParam PaymentType paymentType,
            @RequestParam Long relatedEntityId,
            @RequestParam BigDecimal amount,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        PaymentResponse response = paymentService.createPayment(user, paymentType, relatedEntityId, amount);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated", response));
    }
    
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @RequestParam String transactionId,
            @RequestParam String stripePaymentIntentId) {
        PaymentResponse response = paymentService.confirmPayment(transactionId, stripePaymentIntentId);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", response));
    }
    
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentHistory(
            Pageable pageable,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Page<PaymentResponse> payments = paymentService.getUserPayments(user, pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}
