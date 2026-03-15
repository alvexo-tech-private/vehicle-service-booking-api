package com.alvexo.bookingapp.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.LoginRequest;
import com.alvexo.bookingapp.dto.request.MobileLoginRequest;
import com.alvexo.bookingapp.dto.request.MobileRegisterRequest;
import com.alvexo.bookingapp.dto.request.RefreshTokenRequest;
import com.alvexo.bookingapp.dto.request.RegisterRequest;
import com.alvexo.bookingapp.dto.request.VehicleUserRegisterRequest;
import com.alvexo.bookingapp.dto.request.WorkshopMechanicRegisterRequest;
import com.alvexo.bookingapp.dto.request.VerifyOtpRequest;
import com.alvexo.bookingapp.dto.response.TokenResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
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
    private final OtpServicebkp otpService;
    private final RedisOtpService redisOtpService;
    private final NotificationService notificationService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            ReferralService referralService,
            OtpServicebkp otpService,
            RedisOtpService redisOtpService,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.referralService = referralService;
        this.otpService = otpService;
        this.redisOtpService = redisOtpService;
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
    public void sendOtpRedis(String mobileNumber) {

        String mobile = MobileNumberUtil.normalize(mobileNumber);

        User user = userRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = redisOtpService.generateOtp(mobile);

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
    public TokenResponse verifyOtpAndLoginDB(VerifyOtpRequest request) {

        String mobile = MobileNumberUtil.normalize(request.getMobileNumber());

        redisOtpService.validateOtp(mobile, request.getOtp());

        User user = userRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return createTokenResponse(user);
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

        User user = User.builder()
                .firstName(request.getName())
                .lastName("")
                .mobileNumber(mobile)
                .email(request.getEmail())
                .city(request.getCity())
                .area(request.getArea())
                .password(passwordEncoder.encode(request.getPin()))
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

        User user = User.builder()
                .firstName(request.getName())
                .lastName("")
                .mobileNumber(mobile)
                .email(request.getEmail())
                .city(request.getCity())
                .area(request.getArea())
                .password(passwordEncoder.encode(request.getPin()))
                .workshopName(request.getWorkshopName())
                .role(UserRole.MECHANIC)
                .active(true)
                .build();

        user = userRepository.save(user);
        return createTokenResponse(user);
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.findByReferralCode(code).isPresent());
        return code;
    }
}
