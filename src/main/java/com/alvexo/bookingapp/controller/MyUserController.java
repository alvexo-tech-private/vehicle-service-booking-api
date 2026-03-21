package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MyUserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-users")
public class MyUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MyUserService myUserService;

    @Operation(summary = "Get User Vehicle", description = "Returns paginated list of User Vechicle for the currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserResponse response = myUserService.convertToResponse(user);
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

}
