package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_card_sequence",
        uniqueConstraints = @UniqueConstraint(
            name = "uq_job_card_seq_mechanic_date",
            columnNames = {"mechanic_settings_id", "sequence_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_settings_id", nullable = false)
    private MechanicSettings mechanicSettings;

    @Column(name = "sequence_date", nullable = false)
    private LocalDate sequenceDate;

    @Column(name = "last_sequence", nullable = false)
    @Builder.Default
    private Integer lastSequence = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
