package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.FuelType;
import com.alvexo.bookingapp.model.ServiceCategory;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopServiceEligibility;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopServiceEligibilityRepository extends JpaRepository<WorkshopServiceEligibility, Long> {

    List<WorkshopServiceEligibility> findByMechanic(User mechanic);

    List<WorkshopServiceEligibility> findByMechanicId(Long mechanicId);

    List<WorkshopServiceEligibility> findByMechanicIdAndFuelType(Long mechanicId, FuelType fuelType);

    Optional<WorkshopServiceEligibility> findByMechanicAndFuelTypeAndServiceCategory(
            User mechanic, FuelType fuelType, ServiceCategory serviceCategory);
}
