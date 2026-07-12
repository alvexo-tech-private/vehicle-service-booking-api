package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Payment-partner-agnostic payout setup for the workshop. `partner` is a free-form
 * label supplied by whichever gateway integration links the account — never hard-coded here.
 */
@Entity
@Table(name = "workshop_payment_setups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopPaymentSetup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility", nullable = false, length = 20)
    @Builder.Default
    private PaymentEligibility eligibility = PaymentEligibility.NOT_ELIGIBLE;

    @Column(name = "partner", length = 50)
    private String partner;

    @Column(name = "account_ref", length = 100)
    private String accountRef;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
