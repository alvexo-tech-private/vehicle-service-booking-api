package com.alvexo.bookingapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.Vehicle;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVin(String vin);
    List<Vehicle> findByCreatedBy(User createdBy);
    Page<Vehicle> findByActiveTrue(Pageable pageable);
    boolean existsByVin(String vin);
}
