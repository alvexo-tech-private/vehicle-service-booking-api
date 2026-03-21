package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MyUserService;
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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserResponse response = myUserService.convertToResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
