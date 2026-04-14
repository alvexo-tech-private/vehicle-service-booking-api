package com.alvexo.bookingapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Review;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.ReviewService;

@Tag(name = "Reviews", description = "Post and manage reviews for mechanics after completed bookings. Requires JWT token.")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Operation(summary = "Create a review", description = "Vehicle user submits a rating and comment for a mechanic after a completed booking.")
    @PostMapping
    public ResponseEntity<MyApiResponse<Review>> createReview(
            @RequestParam Long bookingId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Review review = reviewService.createReview(bookingId, rating, comment, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Review submitted successfully", review));
    }
    
    @PutMapping("/{reviewId}/response")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Review>> respondToReview(
            @PathVariable Long reviewId,
            @RequestParam String response,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Review review = reviewService.respondToReview(reviewId, response, mechanic);
        return ResponseEntity.ok(MyApiResponse.success("Response added successfully", review));
    }
    
    @Operation(summary = "Get mechanic reviews", description = "Returns paginated reviews for a specific mechanic by their user ID.")
    @GetMapping("/mechanic/{mechanicId}")
    public ResponseEntity<MyApiResponse<Page<Review>>> getMechanicReviews(
            @PathVariable Long mechanicId,
            Pageable pageable) {
        Page<Review> reviews = reviewService.getMechanicReviews(mechanicId, pageable);
        return ResponseEntity.ok(MyApiResponse.success(reviews));
    }
}
