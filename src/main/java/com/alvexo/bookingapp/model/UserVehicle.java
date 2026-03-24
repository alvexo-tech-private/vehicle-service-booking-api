package com.alvexo.bookingapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vehicles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "vehicle_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVehicle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"refreshTokens", "vehicles", "password"}) // exclude sensitive/circular fields
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(name = "image")
    private String image;

    @Column(name = "insurance_number")
    private String insuranceNumber;
    
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Column(name = "ownership_type", length = 50)
    private String ownershipType;
    
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
