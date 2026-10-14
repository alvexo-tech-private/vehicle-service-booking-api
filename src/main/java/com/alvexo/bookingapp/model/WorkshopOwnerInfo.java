package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_owner_infos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopOwnerInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Column(name = "owner_name", nullable = false, length = 150)
    private String ownerName;

    @Column(name = "primary_phone", nullable = false, length = 20)
    private String primaryPhone;

    @Column(name = "secondary_phone", length = 20)
    private String secondaryPhone;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "whatsapp_verified", nullable = false)
    @Builder.Default
    private Boolean whatsappVerified = false;

    @Column(name = "email", length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proof_file_id")
    private StoredFile idProofFile;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
