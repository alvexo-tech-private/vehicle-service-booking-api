package com.alvexo.bookingapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);
    Optional<User> findByReferralCode(String referralCode);
    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);
    List<User> findByRole(UserRole role);
    Page<User> findByRole(UserRole role, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true " +
           "AND (:latitude IS NULL OR :longitude IS NULL OR " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(u.latitude)) * " +
           "cos(radians(u.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(u.latitude)))) <= :radiusKm)")
    List<User> findNearbyMechanics(@Param("role") UserRole role,
                                    @Param("latitude") BigDecimal latitude,
                                    @Param("longitude") BigDecimal longitude,
                                    @Param("radiusKm") double radiusKm);
}
