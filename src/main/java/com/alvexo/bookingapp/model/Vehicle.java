package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String make;
    
    @Column(nullable = false, length = 100)
    private String model;
    
    @Column(nullable = false)
    private Integer year;
    
    @Column(unique = true, length = 50)
    private String vin;
    
    @Column(name = "license_plate", length = 50)
    private String licensePlate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 50)
    private VehicleType vehicleType;
    
    @Column(length = 50)
    private String color;
    
    private Integer mileage;
    
    @Column(name = "engine_type", length = 50)
    private String engineType;
    
    @Column(name = "transmission_type", length = 50)
    private String transmissionType;
    
    @Column(name = "fuel_type", length = 50)
    private String fuelType;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
