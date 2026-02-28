package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserVehicle;
import com.alvexo.bookingapp.model.Vehicle;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVehicleRepository extends JpaRepository<UserVehicle, Long> {
    List<UserVehicle> findByUser(User user);
    List<UserVehicle> findByVehicle(Vehicle vehicle);
    Optional<UserVehicle> findByUserAndVehicle(User user, Vehicle vehicle);
    Optional<UserVehicle> findByUserAndIsPrimaryTrue(User user);
    boolean existsByUserAndVehicle(User user, Vehicle vehicle);
}
