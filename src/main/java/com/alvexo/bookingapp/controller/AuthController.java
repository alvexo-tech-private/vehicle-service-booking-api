package com.alvexo.bookingapp.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.request.LoginRequest;
import com.alvexo.bookingapp.dto.request.MobileLoginRequest;
import com.alvexo.bookingapp.dto.request.MobileRegisterRequest;
import com.alvexo.bookingapp.dto.request.RefreshTokenRequest;
import com.alvexo.bookingapp.dto.request.RegisterRequest;
import com.alvexo.bookingapp.dto.request.SendOtpRequest;
import com.alvexo.bookingapp.dto.request.VehicleUserRegisterRequest;
import com.alvexo.bookingapp.dto.request.WorkshopMechanicRegisterRequest;
import com.alvexo.bookingapp.dto.request.VerifyOtpRequest;
import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.TokenResponse;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.service.AuthService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
		TokenResponse response = authService.register(request);
		return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
		TokenResponse response = authService.login(request);
		return ResponseEntity.ok(ApiResponse.success("Login successful", response));
	}

	@PostMapping("/refresh-token")
	public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		TokenResponse response = authService.refreshToken(request);
		return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request.getRefreshToken());
		return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
	}

	@PostMapping("/mobile/register")
	public ResponseEntity<ApiResponse<TokenResponse>> mobileRegister(
			@Valid @RequestBody MobileRegisterRequest request) {

		TokenResponse response = authService.registerWithMobile(request);
		return ResponseEntity.ok(ApiResponse.success("Mobile registration successful", response));
	}

	@PostMapping("/mobile/login")
	public ResponseEntity<ApiResponse<TokenResponse>> mobileLogin(@Valid @RequestBody MobileLoginRequest request) {

		TokenResponse response = authService.loginWithMobile(request);
		return ResponseEntity.ok(ApiResponse.success("Mobile login successful", response));
	}

	@PostMapping("/mobile/send-otp")
	public ResponseEntity<ApiResponse<String>> sendOtp(@RequestBody SendOtpRequest request) {

		authService.sendOtp(request.getMobileNumber());

		return ResponseEntity.ok(ApiResponse.success("OTP sent", "SUCCESS"));
	}

	@PostMapping("/mobile/verify-otp")
	public ResponseEntity<ApiResponse<TokenResponse>> verifyOtp(@RequestBody VerifyOtpRequest request) {

		TokenResponse token = authService.verifyOtpAndLogin(request);

		return ResponseEntity.ok(ApiResponse.success("Login successful", token));
	}

	/**
	 * Endpoint 1: Returns the list of available user types (VEHICLE_USER, MECHANIC).
	 */
	@GetMapping("/user-types")
	public ResponseEntity<ApiResponse<List<UserRole>>> getUserTypes() {
		List<UserRole> userTypes = authService.getUserTypes();
		return ResponseEntity.ok(ApiResponse.success("User types retrieved successfully", userTypes));
	}

	/**
	 * Endpoint 2: Register a new vehicle user.
	 * Fields: name, mobileNumber, email, city, area (optional), pin (4-digit).
	 */
	@PostMapping("/register/vehicle-user")
	public ResponseEntity<ApiResponse<TokenResponse>> registerVehicleUser(
			@Valid @RequestBody VehicleUserRegisterRequest request) {
		TokenResponse response = authService.registerVehicleUser(request);
		return ResponseEntity.ok(ApiResponse.success("Vehicle user registered successfully", response));
	}

	/**
	 * Endpoint 3: Register a new workshop mechanic.
	 * Fields: name, mobileNumber, email, city, area (optional), pin (4-digit), workshopName.
	 */
	@PostMapping("/register/workshop-mechanic")
	public ResponseEntity<ApiResponse<TokenResponse>> registerWorkshopMechanic(
			@Valid @RequestBody WorkshopMechanicRegisterRequest request) {
		TokenResponse response = authService.registerWorkshopMechanic(request);
		return ResponseEntity.ok(ApiResponse.success("Workshop mechanic registered successfully", response));
	}

}
