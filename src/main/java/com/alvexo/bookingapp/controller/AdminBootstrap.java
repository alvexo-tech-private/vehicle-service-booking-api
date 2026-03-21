package com.alvexo.bookingapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.UserRepository;

@Component
public class AdminBootstrap {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
    private PasswordEncoder passwordEncoder;
	
	@Autowired
    private Environment env;



    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createDefaultAdmin() {
        if (userRepository.findByRole(UserRole.ADMINISTRATOR).isEmpty()) {
            String email    = env.getProperty("app.admin.email", "admin@alvexotech.com");
            String mobile   = env.getProperty("app.admin.mobile", "9500095000");
            String pin      = env.getProperty("app.admin.pin",    "4698");

            User admin = User.builder()
                .email(email)
                .mobileNumber(mobile)
                .password(passwordEncoder.encode(pin))
                .firstName("Default")
                .lastName("Admin")
                .role(UserRole.ADMINISTRATOR)
                .city("Chennai")
                .active(true)
                .build();

            userRepository.save(admin);
            System.out.println("Default administrator created. Change credentials immediately.");
        }
    }
}