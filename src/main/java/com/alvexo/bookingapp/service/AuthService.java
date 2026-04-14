package com.alvexo.bookingapp.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.alvexo.bookingapp.dto.request.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.response.TokenResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.exception.UnauthorizedException;
import com.alvexo.bookingapp.model.RefreshToken;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.RefreshTokenRepository;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.security.JwtTokenProvider;
import com.alvexo.bookingapp.util.MobileNumberUtil;

@Service
public class AuthService {
    
	private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final ReferralService referralService;
    private final OtpService otpService;
    private final NotificationService notificationService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            ReferralService referralService,
            OtpService otpService,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.referralService = referralService;
        this.otpService = otpService;
        this.notificationService = notificationService;
    }
    
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        if (userRepository.existsByMobileNumber(mobile)) {
            throw new BadRequestException("Mobile number already registered");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .mobileNumber(mobile)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .specialization(request.getSpecialization())
                .experienceYears(request.getExperienceYears())
                .hourlyRate(request.getHourlyRate())
                .bio(request.getBio())
                .build();
        
        // Generate referral code for sales representatives
        if (request.getRole() == UserRole.SALES_REPRESENTATIVE) {
            user.setReferralCode(generateUniqueReferralCode());
        }
        
        user = userRepository.save(user);
        
        // Handle referral if provided
        if (request.getReferralCode() != null && !request.getReferralCode().isEmpty()) {
            referralService.processReferral(request.getReferralCode(), user);
        }
        
        return createTokenResponse(user);
    }
    
    @Transactional
    public TokenResponse registerWithMobile(MobileRegisterRequest request) {

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        if (userRepository.existsByMobileNumber(mobile)) {
            throw new BadRequestException("Mobile number already registered");
        }

        User user = User.builder()
                .mobileNumber(mobile)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(request.getRole())
                .active(true)
                .build();

        user = userRepository.save(user);

        return createTokenResponse(user);
    }
    @Transactional
    public TokenResponse loginWithMobile(MobileLoginRequest request) {

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        mobile,
                        request.getPassword()
                )
        );

        User user = userRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.getActive()) {
            throw new UnauthorizedException("Account inactive");
        }

        return createTokenResponse(user, request.getDeviceId(), request.getDeviceType());
    }
    
    @Transactional
    public void sendOtp(String mobileNumber) {

        String mobile = MobileNumberUtil.normalize(mobileNumber);

        User user = userRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = otpService.generateAndSaveOtp(mobile);

        notificationService.sendOtpEmail(mobile, otp);
    }



    @Transactional
    public TokenResponse verifyOtpAndLogin(VerifyOtpRequest request) {

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        otpService.validateOtp(mobile, request.getOtp());

        User user = userRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return createTokenResponse(user, request.getDeviceId(), request.getDeviceType());
    }





    
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // If the username looks like a phone number (no @), normalise it before lookup
        String username = request.getUsername();
        String normalizedUsername = !username.contains("@")
                ? MobileNumberUtil.normalize(username)
                : username;

        User user = userRepository.findByEmail(normalizedUsername)
                .or(() -> userRepository.findByMobileNumber(normalizedUsername))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        
        if (!user.getActive()) {
            throw new UnauthorizedException("Account is inactive");
        }
        
        // Delete old refresh token for this device if exists
        if (request.getDeviceId() != null) {
            refreshTokenRepository.findByUserAndDeviceId(user, request.getDeviceId())
                    .ifPresent(refreshTokenRepository::delete);
        }
        
        return createTokenResponse(user, request.getDeviceId(), request.getDeviceType());
    }
    
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token expired");
        }
        
        User user = refreshToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user.getEmail());
        
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .referralCode(user.getReferralCode())
                .build();
    }
    
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenRepository::delete);
    }
    
    private TokenResponse createTokenResponse(User user) {
        return createTokenResponse(user, null, null);
    }
    
    private TokenResponse createTokenResponse(User user, String deviceId, String deviceType) {
        String accessToken = tokenProvider.generateAccessToken(user.getEmail());
        String refreshTokenValue = tokenProvider.generateRefreshToken(user.getEmail());
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .expiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpiration() / 1000))
                .build();
        
        refreshTokenRepository.save(refreshToken);
        
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .referralCode(user.getReferralCode()) // non-null only for SALES_REPRESENTATIVE
                .build();
    }
    
    /**
     * Returns the list of user types available for registration.
     */
    public java.util.List<UserRole> getUserTypes() {
        return java.util.Arrays.asList(UserRole.VEHICLE_USER, UserRole.MECHANIC);
    }

    /**
     * Registers a vehicle user with name, mobile number, email, city, area (optional), and 4-digit PIN.
     */
    @Transactional
    public TokenResponse registerVehicleUser(VehicleUserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        if (userRepository.existsByMobileNumber(mobile)) {
            throw new BadRequestException("Mobile number already registered");
        }
        
        if(!request.getPin().equals((request.getConfirmPin()))){
        	throw new BadRequestException("PIN and confirm PIN should be same");
        }

        User user = User.builder()
                .firstName(request.getName())
                .lastName("")
                .mobileNumber(mobile)
                .email(request.getEmail())
                .city(request.getCity())
                .area(request.getArea())
                .password(passwordEncoder.encode(String.valueOf(request.getPin())))
                .role(UserRole.VEHICLE_USER)
                .active(true)
                .build();

        user = userRepository.save(user);
        return createTokenResponse(user);
    }

    /**
     * Registers a workshop mechanic with name, mobile number, email, city, area (optional), 4-digit PIN, and workshop name.
     */
    @Transactional
    public TokenResponse registerWorkshopMechanic(WorkshopMechanicRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        if (userRepository.existsByMobileNumber(mobile)) {
            throw new BadRequestException("Mobile number already registered");
        }

        if(!request.getPin().equals((request.getConfirmPin()))){
        	throw new BadRequestException("PIN and confirm PIN should be same");
        }
        User user = User.builder()
                .firstName(request.getName())
                .lastName("")
                .mobileNumber(mobile)
                .email(request.getEmail())
                .city(request.getCity())
                .area(request.getArea())
                .postalCode(request.getPostalCode())
                .password(passwordEncoder.encode(String.valueOf(request.getPin())))
                .workshopName(request.getWorkshopName())
                .role(UserRole.MECHANIC)
                .active(true)
                .build();

        user = userRepository.save(user);
        return createTokenResponse(user);
    }


    /**
     * Admin-only: registers a new Sales Representative.
     * A unique referral code is auto-generated and returned in the response.
     */
    @Transactional
    public TokenResponse registerSalesRepresentative(SalesRepresentativeRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        if (userRepository.existsByMobileNumber(mobile)) {
            throw new BadRequestException("Mobile number already registered");
        }

        User user = User.builder()
                .firstName(request.getName())
                .lastName("")
                .mobileNumber(mobile)
                .email(request.getEmail())
                .city(request.getCity())
                .area(request.getArea())
                .password(passwordEncoder.encode(request.getPin()))
                .role(UserRole.SALES_REPRESENTATIVE)
                .active(true)
                .build();

        // Auto-generate a unique referral code for every sales representative
        user.setReferralCode(generateUniqueReferralCode());

        user = userRepository.save(user);
        return createTokenResponse(user);
    }

    /**
     * Admin-only: registers a new Administrator.
     * Only an existing authenticated ADMINISTRATOR can reach this endpoint.
     */
    @Transactional
    public TokenResponse registerAdministrator(AdministratorRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        if (userRepository.existsByMobileNumber(mobile)) {
            throw new BadRequestException("Mobile number already registered");
        }

        User user = User.builder()
                .firstName(request.getName())
                .lastName("")
                .mobileNumber(mobile)
                .email(request.getEmail())
                .city(request.getCity())
                .area(request.getArea())
                .password(passwordEncoder.encode(request.getPin()))
                .role(UserRole.ADMINISTRATOR)
                .active(true)
                .build();

        user = userRepository.save(user);
        return createTokenResponse(user);
    }


    /**
     * Changes the 4-digit PIN for any authenticated user (all 4 roles).
     *
     * Rules:
     *  1. currentPin must match the stored (encoded) password.
     *  2. newPin and confirmPin must match each other.
     *  3. newPin must be different from currentPin.
     *  4. All existing refresh tokens are invalidated so other devices
     *     are forced to re-authenticate with the new PIN.
     *
     * @param email  the email of the currently authenticated user (from JWT principal)
     * @param request the change-pin request body
     */
    @Transactional
    public void changePin(String email, ChangePinRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // 1. Verify current PIN
        if (!passwordEncoder.matches(request.getCurrentPin(), user.getPassword())) {
            throw new BadRequestException("Current PIN is incorrect");
        }

        // 2. New PIN and confirm PIN must match
        if (!request.getNewPin().equals(request.getConfirmPin())) {
            throw new BadRequestException("New PIN and confirm PIN do not match");
        }

        // 3. New PIN must differ from current PIN
        if (passwordEncoder.matches(request.getNewPin(), user.getPassword())) {
            throw new BadRequestException("New PIN must be different from the current PIN");
        }

        // 4. Save the new encoded PIN
        user.setPassword(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);

        // 5. Invalidate all refresh tokens so other sessions must re-login
        refreshTokenRepository.deleteByUser(user);
    }

    // ── Contact detail change via OTP ─────────────────────────────────────────

    /**
     * Sends an OTP to {@code newEmail} so the user can prove ownership before
     * the address is committed to their account.
     *
     * <p>Checks performed before generating the OTP:
     * <ul>
     *   <li>The caller must exist in the system (looked up by their current email from JWT).</li>
     *   <li>The new email must not already be registered to another account.</li>
     * </ul>
     *
     * @param currentEmail the authenticated user's current email (from JWT principal)
     * @param newEmail     the email address the user wants to change to
     */
    @Transactional
    public void sendEmailChangeOtp(String currentEmail, String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new BadRequestException("This email address is already registered to another account");
        }
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otp = otpService.generateAndSaveOtpForEmail(newEmail,
                user.getMobileNumber() != null ? user.getMobileNumber() : "");
        notificationService.sendEmailChangeOtp(newEmail, otp);
    }

    /**
     * Verifies the OTP the user received at {@code newEmail} and, on success,
     * updates the account's email address.
     *
     * <p>Because the JWT access token encodes the user's email as its subject,
     * all existing refresh tokens are invalidated after the change — the user
     * must re-authenticate on all devices.
     *
     * @param currentEmail the authenticated user's current email (from JWT principal)
     * @param newEmail     the email address being verified
     * @param otp          the code the user received
     */
    @Transactional
    public void verifyEmailChangeOtp(String currentEmail, String newEmail, String otp) {
        // Re-check: another request might have registered newEmail between send and verify
        if (userRepository.existsByEmail(newEmail)) {
            throw new BadRequestException("This email address is already registered to another account");
        }
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        otpService.validateOtpByEmail(newEmail, otp);

        user.setEmail(newEmail);
        userRepository.save(user);

        // JWT subject is the email — existing tokens become invalid, force re-login
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Sends an OTP to the user's <em>current</em> email address so they can
     * authorise a mobile number change.
     *
     * <p>(SMS delivery is not yet implemented; the OTP is delivered via email.)
     *
     * @param currentEmail the authenticated user's current email (from JWT principal)
     * @param newMobile    the mobile number the user wants to change to (raw, will be normalised)
     */
    @Transactional
    public void sendMobileChangeOtp(String currentEmail, String newMobile) {
        String normalizedMobile = MobileNumberUtil.normalize(newMobile);
        if (userRepository.existsByMobileNumber(normalizedMobile)) {
            throw new BadRequestException("This mobile number is already registered to another account");
        }
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otp = otpService.generateAndSaveOtpForMobile(normalizedMobile, user.getEmail());
        notificationService.sendMobileChangeOtp(user.getEmail(), otp);
    }

    /**
     * Verifies the OTP the user received and, on success, updates the account's
     * mobile number.
     *
     * @param currentEmail the authenticated user's current email (from JWT principal)
     * @param newMobile    the mobile number being verified (raw, will be normalised)
     * @param otp          the code the user received
     */
    @Transactional
    public void verifyMobileChangeOtp(String currentEmail, String newMobile, String otp) {
        String normalizedMobile = MobileNumberUtil.normalize(newMobile);
        // Re-check: another request might have registered newMobile between send and verify
        if (userRepository.existsByMobileNumber(normalizedMobile)) {
            throw new BadRequestException("This mobile number is already registered to another account");
        }
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        otpService.validateOtpByMobile(normalizedMobile, otp);

        user.setMobileNumber(normalizedMobile);
        userRepository.save(user);
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.findByReferralCode(code).isPresent());
        return code;
    }
}
