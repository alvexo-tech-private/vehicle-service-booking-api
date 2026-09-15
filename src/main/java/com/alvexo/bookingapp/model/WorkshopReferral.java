package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_referrals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "partner_name", nullable = false, length = 100)
    private String partnerName;

    @Column(name = "benefit", nullable = false, length = 255)
    private String benefit;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;
}
