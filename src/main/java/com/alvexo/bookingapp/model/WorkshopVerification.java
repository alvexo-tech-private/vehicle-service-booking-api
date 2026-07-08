package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "workshop_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workshop_verification_methods", joinColumns = @JoinColumn(name = "workshop_verification_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 30)
    @Builder.Default
    private Set<VerificationMethod> completedMethods = new HashSet<>();

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
