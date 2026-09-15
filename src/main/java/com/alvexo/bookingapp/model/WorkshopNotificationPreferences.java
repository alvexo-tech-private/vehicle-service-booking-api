package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "workshop_notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopNotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Column(name = "new_booking_enabled", nullable = false)
    @Builder.Default
    private Boolean newBookingEnabled = true;

    @Column(name = "booking_cancelled_enabled", nullable = false)
    @Builder.Default
    private Boolean bookingCancelledEnabled = true;

    @Column(name = "booking_rescheduled_enabled", nullable = false)
    @Builder.Default
    private Boolean bookingRescheduledEnabled = true;

    /**
     * Payment notifications are always on — not persisted as a toggle,
     * the service always returns enabled=true, locked=true for this category.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workshop_notification_channels", joinColumns = @JoinColumn(name = "preferences_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 10)
    @Builder.Default
    private Set<NotificationChannel> channels = new HashSet<>(Set.of(NotificationChannel.PUSH));

    @Enumerated(EnumType.STRING)
    @Column(name = "sound", nullable = false, length = 10)
    @Builder.Default
    private NotificationSound sound = NotificationSound.DAY;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
