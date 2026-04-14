package com.alvexo.bookingapp.controller;

import java.util.List;

import com.alvexo.bookingapp.dto.request.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.TokenResponse;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
	@PostMapping("/send-otp")
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

	@PostMapping("/verify-otp")
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

	// ── Email change via OTP (authenticated) ──────────────────────────────────

	@Operation(
		summary = "Send OTP to verify new email address",
		description = """
			Sends a one-time password to the **new** email address the user wants to switch to.
			The user must be logged in. The OTP must then be submitted to the verify endpoint
			within 5 minutes to complete the change.

			**Rules:**
			- The new email must not already be registered to another account.
			- After a successful verify, all refresh tokens are invalidated — the user
			  must re-login on all devices because the JWT subject (email) has changed.
			""")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OTP sent to the new email address"),
		@ApiResponse(responseCode = "400", description = "New email already registered"),
		@ApiResponse(responseCode = "401", description = "JWT token missing or invalid")
	})
	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/email-change/send-otp")
	public ResponseEntity<MyApiResponse<Void>> sendEmailChangeOtp(
			@Valid @RequestBody EmailChangeOtpRequest request,
			Authentication authentication) {
		authService.sendEmailChangeOtp(authentication.getName(), request.getNewEmail());
		return ResponseEntity.ok(MyApiResponse.success(
				"OTP sent to " + request.getNewEmail() + ". It is valid for 5 minutes.", null));
	}

	@Operation(
		summary = "Verify OTP and update email address",
		description = """
			Validates the OTP received at the new email address and updates the account.
			Submit the same new email used in the send-otp call together with the received code.

			On success, **all existing refresh tokens are invalidated** — the user must
			re-authenticate on all devices.
			""")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Email updated successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid or expired OTP, or email already registered"),
		@ApiResponse(responseCode = "401", description = "JWT token missing or invalid")
	})
	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/email-change/verify")
	public ResponseEntity<MyApiResponse<Void>> verifyEmailChangeOtp(
			@Valid @RequestBody EmailChangeVerifyRequest request,
			Authentication authentication) {
		authService.verifyEmailChangeOtp(authentication.getName(), request.getNewEmail(), request.getOtp());
		return ResponseEntity.ok(MyApiResponse.success(
				"Email address updated successfully. Please log in again on all devices.", null));
	}

	// ── Mobile change via OTP (authenticated) ─────────────────────────────────

	@Operation(
		summary = "Send OTP to verify new mobile number",
		description = """
			Sends a one-time password to the user's **current email address** to authorise
			a mobile number change. (SMS delivery is not yet available.)
			The user must be logged in. The OTP must be submitted to the verify endpoint
			within 5 minutes to complete the change.

			**Rules:**
			- The new mobile number must not already be registered to another account.
			- The number is normalised before storage (leading country code stripped if present).
			""")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OTP sent to the user's current email"),
		@ApiResponse(responseCode = "400", description = "New mobile number already registered or invalid format"),
		@ApiResponse(responseCode = "401", description = "JWT token missing or invalid")
	})
	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/mobile-change/send-otp")
	public ResponseEntity<MyApiResponse<Void>> sendMobileChangeOtp(
			@Valid @RequestBody MobileChangeOtpRequest request,
			Authentication authentication) {
		authService.sendMobileChangeOtp(authentication.getName(), request.getNewMobile());
		return ResponseEntity.ok(MyApiResponse.success(
				"OTP sent to your registered email address. It is valid for 5 minutes.", null));
	}

	@Operation(
		summary = "Verify OTP and update mobile number",
		description = """
			Validates the OTP received via email and updates the account's mobile number.
			Submit the same new mobile number used in the send-otp call together with the received code.
			""")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Mobile number updated successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid or expired OTP, or mobile number already registered"),
		@ApiResponse(responseCode = "401", description = "JWT token missing or invalid")
	})
	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/mobile-change/verify")
	public ResponseEntity<MyApiResponse<Void>> verifyMobileChangeOtp(
			@Valid @RequestBody MobileChangeVerifyRequest request,
			Authentication authentication) {
		authService.verifyMobileChangeOtp(authentication.getName(), request.getNewMobile(), request.getOtp());
		return ResponseEntity.ok(MyApiResponse.success("Mobile number updated successfully.", null));
	}

}
