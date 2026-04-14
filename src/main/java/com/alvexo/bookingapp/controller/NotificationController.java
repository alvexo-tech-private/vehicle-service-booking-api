package com.alvexo.bookingapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Notification;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notifications", description = "View and manage in-app notifications for the authenticated user. Requires JWT token.")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Operation(summary = "Get notifications", description = "Returns paginated notifications for the authenticated user, newest first.")

    @GetMapping
    public ResponseEntity<MyApiResponse<Page<Notification>>> getNotifications(
            Pageable pageable,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Page<Notification> notifications = notificationService.getUserNotifications(user, pageable);
        return ResponseEntity.ok(MyApiResponse.success(notifications));
    }
    
    @Operation(summary = "Get unread notifications", description = "Returns all unread notifications for the authenticated user.")
    @GetMapping("/unread")
    public ResponseEntity<MyApiResponse<List<Notification>>> getUnreadNotifications(
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<Notification> notifications = notificationService.getUnreadNotifications(user);
        return ResponseEntity.ok(MyApiResponse.success(notifications));
    }
    
    @Operation(summary = "Get unread count", description = "Returns the count of unread notifications as a number.")
    @GetMapping("/unread-count")
    public ResponseEntity<MyApiResponse<Long>> getUnreadCount(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(MyApiResponse.success(count));
    }
    
    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read by ID.")
    @PutMapping("/{id}/read")
    public ResponseEntity<MyApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok(MyApiResponse.success("Notification marked as read", null));
    }
    
    @Operation(summary = "Mark all as read", description = "Marks every unread notification as read for the authenticated user.")
    @PutMapping("/mark-all-read")
    public ResponseEntity<MyApiResponse<Void>> markAllAsRead(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(MyApiResponse.success("All notifications marked as read", null));
    }
}
