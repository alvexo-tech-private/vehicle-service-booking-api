package com.alvexo.bookingapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.AvailabilityRequest;
import com.alvexo.bookingapp.dto.response.AvailabilityResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.MechanicAvailability;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.MechanicAvailabilityRepository;
import com.alvexo.bookingapp.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MechanicService {
    
    @Autowired
    private MechanicAvailabilityRepository availabilityRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public AvailabilityResponse addAvailability(User mechanic, AvailabilityRequest request) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can add availability");
        }
        
        MechanicAvailability availability = MechanicAvailability.builder()
                .mechanic(mechanic)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .slotDurationMinutes(request.getSlotDurationMinutes() != null ? request.getSlotDurationMinutes() : 60)
                .build();
        
        availability = availabilityRepository.save(availability);
        return convertToResponse(availability);
    }
    
    public List<AvailabilityResponse> getMechanicAvailability(User mechanic) {
        return availabilityRepository.findByMechanic(mechanic).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public AvailabilityResponse updateAvailability(Long id, AvailabilityRequest request, User mechanic) {
        MechanicAvailability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found"));
        
        if (!availability.getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("You can only update your own availability");
        }
        
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        if (request.getIsAvailable() != null) {
            availability.setIsAvailable(request.getIsAvailable());
        }
        if (request.getSlotDurationMinutes() != null) {
            availability.setSlotDurationMinutes(request.getSlotDurationMinutes());
        }
        
        availability = availabilityRepository.save(availability);
        return convertToResponse(availability);
    }
    
    @Transactional
    public void deleteAvailability(Long id, User mechanic) {
        MechanicAvailability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found"));
        
        if (!availability.getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("You can only delete your own availability");
        }
        
        availabilityRepository.delete(availability);
    }
    
    public List<UserResponse> findNearbyMechanics(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        List<User> mechanics = userRepository.findNearbyMechanics(
                UserRole.MECHANIC, latitude, longitude, radiusKm);
        
        return mechanics.stream()
                .map(this::convertUserToResponse)
                .collect(Collectors.toList());
    }
    
    private AvailabilityResponse convertToResponse(MechanicAvailability availability) {
        return AvailabilityResponse.builder()
                .id(availability.getId())
                .dayOfWeek(availability.getDayOfWeek())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .isAvailable(availability.getIsAvailable())
                .slotDurationMinutes(availability.getSlotDurationMinutes())
                .build();
    }
    
    private UserResponse convertUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .specialization(user.getSpecialization())
                .experienceYears(user.getExperienceYears())
                .hourlyRate(user.getHourlyRate())
                .bio(user.getBio())
                .rating(user.getRating())
                .totalReviews(user.getTotalReviews())
                .totalBookingsCompleted(user.getTotalBookingsCompleted())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .city(user.getCity())
                .state(user.getState())
                .build();
    }
}
