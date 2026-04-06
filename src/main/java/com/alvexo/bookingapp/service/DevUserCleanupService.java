package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.UserSummaryDto;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicAvailabilityRepository;
import com.alvexo.bookingapp.repository.NotificationRepository;
import com.alvexo.bookingapp.repository.OtpVerificationRepository;
import com.alvexo.bookingapp.repository.PaymentRepository;
import com.alvexo.bookingapp.repository.ReferralRepository;
import com.alvexo.bookingapp.repository.RefreshTokenRepository;
import com.alvexo.bookingapp.repository.ReviewRepository;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.repository.UserSubscriptionRepository;
import com.alvexo.bookingapp.repository.UserVehicleRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DEV / TEST ONLY — not loaded when the "prod" Spring profile is active.
 *
 * Provides user-deletion helpers used by {@link DevUserCleanupController}.
 * Deletes all FK-dependent child rows first so the users row can be removed
 * cleanly without a constraint violation.
 */
@Service
@Profile("!prod")
public class DevUserCleanupService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final ReferralRepository referralRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final MechanicAvailabilityRepository mechanicAvailabilityRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PaymentRepository paymentRepository;

    public DevUserCleanupService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserVehicleRepository userVehicleRepository,
            BookingRepository bookingRepository,
            ReviewRepository reviewRepository,
            NotificationRepository notificationRepository,
            ReferralRepository referralRepository,
            UserSubscriptionRepository userSubscriptionRepository,
            MechanicAvailabilityRepository mechanicAvailabilityRepository,
            OtpVerificationRepository otpVerificationRepository,
            PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userVehicleRepository = userVehicleRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
        this.referralRepository = referralRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.mechanicAvailabilityRepository = mechanicAvailabilityRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.paymentRepository = paymentRepository;
    }

    // -------------------------------------------------------------------------
    // List helpers
    // -------------------------------------------------------------------------

    /** Returns a summary of every user in the table (id, email, mobile, role). */
    public List<UserSummaryDto> listAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    /** Returns users filtered by role. */
    public List<UserSummaryDto> listUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(this::toSummary)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Single-user deletion
    // -------------------------------------------------------------------------

    /**
     * Deletes a single user by ID, removing all FK-dependent child records first.
     *
     * @return summary of the deleted user
     * @throws ResourceNotFoundException if no user exists with the given id
     */
    @Transactional
    public UserSummaryDto deleteUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        UserSummaryDto summary = toSummary(user);
        deleteAllChildRecords(user);
        userRepository.delete(user);
        return summary;
    }

    /**
     * Deletes a single user by email, removing all FK-dependent child records first.
     *
     * @return summary of the deleted user
     * @throws ResourceNotFoundException if no user exists with the given email
     */
    @Transactional
    public UserSummaryDto deleteUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        UserSummaryDto summary = toSummary(user);
        deleteAllChildRecords(user);
        userRepository.delete(user);
        return summary;
    }

    /**
     * Deletes a single user by mobile number (normalisation is done by the caller),
     * removing all FK-dependent child records first.
     *
     * @return summary of the deleted user
     * @throws ResourceNotFoundException if no user exists with the given mobile
     */
    @Transactional
    public UserSummaryDto deleteUserByMobile(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile: " + mobileNumber));
        UserSummaryDto summary = toSummary(user);
        deleteAllChildRecords(user);
        userRepository.delete(user);
        return summary;
    }

    // -------------------------------------------------------------------------
    // Bulk deletion by role
    // -------------------------------------------------------------------------

    /**
     * Deletes all users that have the given role.
     *
     * @return number of users deleted
     */
    @Transactional
    public int deleteAllUsersByRole(UserRole role) {
        List<User> users = userRepository.findByRole(role);
        for (User user : users) {
            deleteAllChildRecords(user);
            userRepository.delete(user);
        }
        return users.size();
    }

    // -------------------------------------------------------------------------
    // Nuke everything
    // -------------------------------------------------------------------------

    /**
     * Deletes ALL rows from the users table and every dependent table.
     * Requires explicit confirmation header — enforced at the controller level.
     *
     * @return number of users deleted
     */
    @Transactional
    public int deleteAllUsers() {
        List<User> all = userRepository.findAll();
        for (User user : all) {
            deleteAllChildRecords(user);
            userRepository.delete(user);
        }
        return all.size();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Deletes all child rows that have a FK pointing at {@code user},
     * in an order that respects FK constraints.
     */
    private void deleteAllChildRecords(User user) {
        Long uid = user.getId();

        // 1. Refresh tokens & OTPs — no further dependencies
        refreshTokenRepository.deleteAll(refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(uid))
                .toList());

        otpVerificationRepository.deleteAll(otpVerificationRepository.findAll().stream()
                .filter(o -> o.getMobileNumber().equals(user.getMobileNumber()))
                .toList());

        // 2. Notifications
        notificationRepository.deleteAll(notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(uid))
                .toList());

        // 3. Reviews (as mechanic or as vehicle user)
        reviewRepository.deleteAll(reviewRepository.findAll().stream()
                .filter(r -> r.getMechanic().getId().equals(uid)
                          || r.getVehicleUser().getId().equals(uid))
                .toList());

        // 4. Payments — must come before bookings (Payment has FK → Booking)
        paymentRepository.deleteAll(paymentRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(uid))
                .toList());

        // 5. Bookings (as vehicle user, as mechanic, or as the one who cancelled)
        bookingRepository.deleteAll(bookingRepository.findAll().stream()
                .filter(b -> b.getVehicleUser().getId().equals(uid)
                          || b.getMechanic().getId().equals(uid)
                          || (b.getCancelledBy() != null && b.getCancelledBy().getId().equals(uid)))
                .toList());

        // 6. Subscriptions & mechanic availability
        userSubscriptionRepository.deleteAll(userSubscriptionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(uid))
                .toList());

        mechanicAvailabilityRepository.deleteAll(mechanicAvailabilityRepository.findAll().stream()
                .filter(a -> a.getMechanic().getId().equals(uid))
                .toList()); 

        // 7. Referrals (as the sales rep who referred, or as the referred user)
        referralRepository.deleteAll(referralRepository.findAll().stream()
                .filter(r -> r.getSalesRep().getId().equals(uid)
                          || r.getReferredUser().getId().equals(uid))
                .toList());

        // 8. User vehicles — last, no downstream FKs
        userVehicleRepository.deleteAll(userVehicleRepository.findAll().stream()
                .filter(v -> v.getUser().getId().equals(uid))
                .toList());
    }

    private UserSummaryDto toSummary(User user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
