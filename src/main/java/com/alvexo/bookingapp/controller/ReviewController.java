package com.alvexo.bookingapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Review;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Review>> createReview(
            @RequestParam Long bookingId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Review review = reviewService.createReview(bookingId, rating, comment, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", review));
    }
    
    @PutMapping("/{reviewId}/response")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<ApiResponse<Review>> respondToReview(
            @PathVariable Long reviewId,
            @RequestParam String response,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Review review = reviewService.respondToReview(reviewId, response, mechanic);
        return ResponseEntity.ok(ApiResponse.success("Response added successfully", review));
    }
    
    @GetMapping("/mechanic/{mechanicId}")
    public ResponseEntity<ApiResponse<Page<Review>>> getMechanicReviews(
            @PathVariable Long mechanicId,
            Pageable pageable) {
        Page<Review> reviews = reviewService.getMechanicReviews(mechanicId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
}
