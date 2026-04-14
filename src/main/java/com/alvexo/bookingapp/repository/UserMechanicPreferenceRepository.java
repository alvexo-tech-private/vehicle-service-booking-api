package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserMechanicPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMechanicPreferenceRepository extends JpaRepository<UserMechanicPreference, Long> {

    /**
     * Returns all preferences for a vehicle user whose mechanic is still active,
     * ordered newest-first. Inactive mechanic accounts are excluded from the list.
     */
    @Query("SELECT p FROM UserMechanicPreference p " +
           "WHERE p.vehicleUser = :vehicleUser " +
           "AND p.mechanic.active = true " +
           "ORDER BY p.createdAt DESC")
    List<UserMechanicPreference> findActiveByVehicleUser(@Param("vehicleUser") User vehicleUser);

    /** True when the user has already bookmarked this mechanic. */
    boolean existsByVehicleUserAndMechanic(User vehicleUser, User mechanic);

    /** Total number of preferences the user currently holds (including inactive mechanics). */
    long countByVehicleUser(User vehicleUser);

    /** Used by the remove flow to delete a specific bookmark. */
    Optional<UserMechanicPreference> findByVehicleUserAndMechanic(User vehicleUser, User mechanic);
}
