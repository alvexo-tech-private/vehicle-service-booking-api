package com.alvexo.bookingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class VehicleServiceBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(VehicleServiceBookingApplication.class, args);
    }
}
