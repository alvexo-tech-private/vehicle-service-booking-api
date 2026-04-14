package com.alvexo.bookingapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.ReferralRepository;
import com.alvexo.bookingapp.repository.UserRepository;

import java.math.BigDecimal;

@Service
public class ReferralService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ReferralRepository referralRepository;
    
    private static final BigDecimal REFERRAL_BONUS = new BigDecimal("50.00");
    
    @Transactional
    public void processReferral(String referralCode, User referredUser) {
        User salesRep = userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new BadRequestException("Invalid referral code"));
        
        if (salesRep.getRole() != UserRole.SALES_REPRESENTATIVE) {
            throw new BadRequestException("Invalid referral code");
        }
        
        // Only mechanics can be referred by sales reps
        if (referredUser.getRole() != UserRole.MECHANIC) {
            return;
        }
        
        Referral referral = Referral.builder()
                .salesRep(salesRep)
                .referredUser(referredUser)
                .referralCodeUsed(referralCode)
                .status(ReferralStatus.ACTIVE)
                .bonusAmount(REFERRAL_BONUS)
                .build();
        
        referralRepository.save(referral);
        
        // Update sales rep stats
        salesRep.setTotalReferrals(salesRep.getTotalReferrals() + 1);
        userRepository.save(salesRep);
    }
}
