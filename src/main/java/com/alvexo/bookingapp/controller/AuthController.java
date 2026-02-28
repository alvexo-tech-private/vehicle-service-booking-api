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
import com.alvexo.bookingapp.dto.request.VerifyOtpRequest;
import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.TokenResponse;
import com.alvexo.bookingapp.service.AuthService;

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

}
