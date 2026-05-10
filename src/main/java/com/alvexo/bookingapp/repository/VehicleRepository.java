package com.alvexo.bookingapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.FuelType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.Vehicle;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCreatedBy(User createdBy);
    Page<Vehicle> findByActiveTrue(Pageable pageable);
    Optional<Vehicle> findById(Long id);

    // Distinct manufacturers filtered by fuel type
    @Query("SELECT DISTINCT v.make FROM Vehicle v WHERE v.active = true AND v.fuelType = :fuelType ORDER BY v.make")
    List<String> findDistinctManufacturersByFuelType(@Param("fuelType") FuelType fuelType);

    // Vehicles filtered by fuel type and manufacturer
    Page<Vehicle> findByActiveTrueAndFuelTypeAndMakeIgnoreCase(FuelType fuelType, String make, Pageable pageable);

    // Vehicles filtered by fuel type only
    Page<Vehicle> findByActiveTrueAndFuelType(FuelType fuelType, Pageable pageable);
}
