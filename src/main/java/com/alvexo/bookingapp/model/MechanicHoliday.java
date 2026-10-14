package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Holiday / Pause for a single date (spec §7 Home "Holiday / Pause"). Blocks
 * new bookings for that date. No automatic redistribution of existing
 * bookings — that requires a product decision and is intentionally out of
 * scope; existing bookings on a newly-marked date are left untouched.
 */
@Entity
@Table(name = "mechanic_holidays",
        uniqueConstraints = @UniqueConstraint(name = "uq_mechanic_holidays_mechanic_date",
                                               columnNames = {"mechanic_id", "date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HolidayType type;

    @Column(length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
