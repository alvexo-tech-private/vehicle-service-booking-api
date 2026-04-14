package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referrals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_rep_id", nullable = false)
    private User salesRep;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_user_id", nullable = false)
    private User referredUser;
    
    @Column(name = "referral_code_used", nullable = false)
    private String referralCodeUsed;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferralStatus status;
    
    @Column(name = "bonus_amount", precision = 10, scale = 2)
    private BigDecimal bonusAmount;
    
    @Column(name = "bonus_paid")
    @Builder.Default
    private Boolean bonusPaid = false;
    
    @Column(name = "bonus_paid_at")
    private LocalDateTime bonusPaidAt;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
