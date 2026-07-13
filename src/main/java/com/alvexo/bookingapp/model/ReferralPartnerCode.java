package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Catalogue of referral codes that can be applied during workshop onboarding.
 * Seeded with demo data; a real partner-management flow would let admins manage this.
 */
@Entity
@Table(name = "referral_partner_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralPartnerCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "partner_name", nullable = false, length = 100)
    private String partnerName;

    @Column(name = "benefit", nullable = false, length = 255)
    private String benefit;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "expired", nullable = false)
    @Builder.Default
    private Boolean expired = false;
}
