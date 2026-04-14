package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.SubscriptionPlan;
import com.alvexo.bookingapp.model.UserRole;

import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByUserRoleAndActiveTrue(UserRole userRole);
    List<SubscriptionPlan> findByActiveTrue();
}
