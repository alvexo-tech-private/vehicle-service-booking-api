package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.Referral;
import com.alvexo.bookingapp.model.User;

import java.util.List;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findBySalesRep(User salesRep);
    List<Referral> findByReferredUser(User referredUser);
    List<Referral> findBySalesRepAndBonusPaidFalse(User salesRep);
}
