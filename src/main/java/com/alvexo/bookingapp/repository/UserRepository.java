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
    Optional<User> findByEmailOrMobileNumber(String mobileNumber, String email);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true " +
           "AND (:latitude IS NULL OR :longitude IS NULL OR " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(u.latitude)) * " +
           "cos(radians(u.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(u.latitude)))) <= :radiusKm)")
    List<User> findNearbyMechanics(@Param("role") UserRole role,
                                    @Param("latitude") BigDecimal latitude,
                                    @Param("longitude") BigDecimal longitude,
                                    @Param("radiusKm") double radiusKm);

    /**
     * Find active mechanics whose area starts with the given prefix (case-insensitive).
     *
     * <p>Uses a prefix LIKE pattern ({@code LOWER(area) LIKE LOWER(:prefix) || '%'}) so
     * the B-tree index on {@code area} is fully utilised. A contains search
     * ({@code '%value%'}) would bypass the index; use a pg_trgm GIN index for that
     * if ever required.
     */
    @Query("SELECT u FROM User u " +
           "WHERE u.role = :role " +
           "AND u.active = true " +
           "AND LOWER(u.area) LIKE LOWER(CONCAT(:area, '%'))")
    List<User> findMechanicsByAreaPrefix(
            @Param("role") UserRole role,
            @Param("area") String area);

    /**
     * Find an active mechanic by exact mobile number.
     * Mobile numbers are unique, so this returns at most one result.
     */
    @Query("SELECT u FROM User u " +
           "WHERE u.role = :role " +
           "AND u.active = true " +
           "AND u.mobileNumber = :mobileNumber")
    List<User> findMechanicsByMobileNumber(
            @Param("role") UserRole role,
            @Param("mobileNumber") String mobileNumber);

    /**
     * Find active mechanics by postal/pin code (exact match).
     */
    @Query("SELECT u FROM User u " +
           "WHERE u.role = :role " +
           "AND u.active = true " +
           "AND u.postalCode = :postalCode")
    List<User> findMechanicsByPostalCode(
            @Param("role") UserRole role,
            @Param("postalCode") String postalCode);
}