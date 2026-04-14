package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column(name = "mobile_verified")
    @Builder.Default
    private Boolean mobileVerified = false;
    
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;
    
    // Address fields
    @Column(name = "address_line1")
    private String addressLine1;
    
    @Column(name = "address_line2")
    private String addressLine2;
    
    private String city;
    private String state;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    private String country;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    // Workshop mechanic-specific fields
    @Column(name = "workshop_name")
    private String workshopName;

    // Area within city (optional)
    private String area;

    // Mechanic-specific fields
    private String specialization;
    
    @Column(name = "experience_years")
    private Integer experienceYears;
    
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Column(precision = 3, scale = 2)
    private BigDecimal rating;
    
    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;
    
    @Column(name = "total_bookings_completed")
    @Builder.Default
    private Integer totalBookingsCompleted = 0;
    
    @Column(name = "certification_details", columnDefinition = "TEXT")
    private String certificationDetails;
    
    // Sales Representative fields
    @Column(name = "referral_code", unique = true, length = 50)
    private String referralCode;
    
    @Column(name = "total_referrals")
    @Builder.Default
    private Integer totalReferrals = 0;
    
    @Column(name = "total_bonus_earned", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalBonusEarned = BigDecimal.ZERO;
    
    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore  // Add this
    private Set<RefreshToken> refreshTokens = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<UserVehicle> userVehicles = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
