package com.alvexo.bookingapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.request.LoginRequest;
import com.alvexo.bookingapp.dto.request.RefreshTokenRequest;
import com.alvexo.bookingapp.dto.request.SendOtpRequest;
import com.alvexo.bookingapp.dto.request.VehicleUserRegisterRequest;
import com.alvexo.bookingapp.dto.request.VerifyOtpRequest;
import com.alvexo.bookingapp.dto.request.WorkshopMechanicRegisterRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.TokenResponse;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authentication", description = "Public endpoints for registration, login, and OTP. No JWT token required.")
@RestController
@RequestMapping("/api/auth")
@SecurityRequirements   // removes the global bearerAuth lock icon for these public endpoints
public class AuthController {

	@Autowired
	private AuthService authService;

	@Operation(summary = "Get available user types", description = "Returns the two self-registration roles: VEHICLE_USER and MECHANIC.")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "List returned successfully"))
	@GetMapping("/user-types")
	public ResponseEntity<MyApiResponse<List<UserRole>>> getUserTypes() {
		List<UserRole> userTypes = authService.getUserTypes();
		return ResponseEntity.ok(MyApiResponse.success("User types retrieved successfully", userTypes));
	}

	@Operation(summary = "Register a vehicle user",
		description = "Creates a new VEHICLE_USER account. Required: name, mobile, email, city, 4-digit PIN., 4-digit confirm PIN,  Optional: area.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Registered successfully"),
		@ApiResponse(responseCode = "400", description = "Validation failed or email/mobile already registered")
	})
	@PostMapping("/register/vehicle-user")
	public ResponseEntity<MyApiResponse<TokenResponse>> registerVehicleUser(
			@Valid @RequestBody VehicleUserRegisterRequest request) {
		TokenResponse response = authService.registerVehicleUser(request);
		return ResponseEntity.ok(MyApiResponse.success("Vehicle user registered successfully", response));
	}

	@Operation(summary = "Register a workshop mechanic",
		description = "Creates a new MECHANIC account. Required: name, mobile, email, city, 4-digit PIN, workshopName. Optional: area.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Registered successfully"),
		@ApiResponse(responseCode = "400", description = "Validation failed or email/mobile already registered")
	})
	@PostMapping("/register/workshop-mechanic")
	public ResponseEntity<MyApiResponse<TokenResponse>> registerWorkshopMechanic(
			@Valid @RequestBody WorkshopMechanicRegisterRequest request) {
		TokenResponse response = authService.registerWorkshopMechanic(request);
		return ResponseEntity.ok(MyApiResponse.success("Workshop mechanic registered successfully", response));
	}

	@Operation(summary = "Login with email or mobile",
		description = "Authenticate using email or mobile number + PIN. Returns JWT access token and refresh token.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Login successful"),
		@ApiResponse(responseCode = "401", description = "Invalid credentials"),
		@ApiResponse(responseCode = "400", description = "Validation failed")
	})
	@PostMapping("/login")
	public ResponseEntity<MyApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
		TokenResponse response = authService.login(request);
		return ResponseEntity.ok(MyApiResponse.success("Login successful", response));
	}

	@Operation(summary = "Send OTP to mobile number",
		description = "Sends a one-time password to the registered mobile number for OTP-based login.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OTP sent successfully"),
		@ApiResponse(responseCode = "404", description = "Mobile number not found")
	})
	@PostMapping("/mobile/send-otp")
	public ResponseEntity<MyApiResponse<String>> sendOtp(@RequestBody SendOtpRequest request) {
		authService.sendOtp(request.getMobileNumber());
		return ResponseEntity.ok(MyApiResponse.success("OTP sent", "SUCCESS"));
	}

	@Operation(summary = "Verify OTP and login",
		description = "Validates the OTP received on the mobile number and returns JWT tokens on success.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OTP verified, login successful"),
		@ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
	})
	@PostMapping("/mobile/verify-otp")
	public ResponseEntity<MyApiResponse<TokenResponse>> verifyOtp(@RequestBody VerifyOtpRequest request) {
		TokenResponse token = authService.verifyOtpAndLogin(request);
		return ResponseEntity.ok(MyApiResponse.success("Login successful", token));
	}

	@Operation(summary = "Refresh access token",
		description = "Exchange a valid refresh token for a new access token without re-entering credentials.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "New access token returned"),
		@ApiResponse(responseCode = "401", description = "Refresh token expired or invalid")
	})
	@PostMapping("/refresh-token")
	public ResponseEntity<MyApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		TokenResponse response = authService.refreshToken(request);
		return ResponseEntity.ok(MyApiResponse.success("Token refreshed", response));
	}

	@Operation(summary = "Logout",
		description = "Invalidates the refresh token. The access token expires naturally after its TTL.")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "Logged out successfully"))
	@PostMapping("/logout")
	public ResponseEntity<MyApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request.getRefreshToken());
		return ResponseEntity.ok(MyApiResponse.success("Logout successful", null));
	}

}
