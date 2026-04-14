package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.MechanicSearchResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserMechanicPreference;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.UserMechanicPreferenceRepository;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PreferenceService {

    @Autowired
    private UserMechanicPreferenceRepository preferenceRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns the list of active preferred mechanics for a vehicle user,
     * ordered newest-first. Inactive mechanic accounts are silently excluded.
     */
    @Transactional(readOnly = true)
    public List<MechanicSearchResponse> getPreferences(User vehicleUser) {
        return preferenceRepository.findActiveByVehicleUser(vehicleUser)
                .stream()
                .map(p -> toSearchResponse(p.getMechanic()))
                .collect(Collectors.toList());
    }

    /**
     * Adds a mechanic to the vehicle user's preferences. Idempotent — if the
     * mechanic is already bookmarked the call returns silently (no error, no
     * duplicate row). Throws if the mechanic does not exist, is not a MECHANIC
     * role user, is inactive, or the user already has the maximum number of
     * preferences.
     */
    @Transactional
    public void addPreference(User vehicleUser, Long mechanicId) {
        User mechanic = userRepository.findById(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found with id: " + mechanicId));

        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("The specified user is not a mechanic");
        }
        if (!Boolean.TRUE.equals(mechanic.getActive())) {
            throw new BadRequestException("Cannot add an inactive mechanic to preferences");
        }

        // Idempotent: silently return if already bookmarked
        if (preferenceRepository.existsByVehicleUserAndMechanic(vehicleUser, mechanic)) {
            return;
        }

        long currentCount = preferenceRepository.countByVehicleUser(vehicleUser);
        if (currentCount >= Constants.MAX_MECHANIC_PREFERENCES) {
            throw new BadRequestException(
                    "You can have at most " + Constants.MAX_MECHANIC_PREFERENCES + " preferred mechanics. "
                    + "Please remove one before adding another.");
        }

        UserMechanicPreference preference = UserMechanicPreference.builder()
                .vehicleUser(vehicleUser)
                .mechanic(mechanic)
                .build();
        preferenceRepository.save(preference);
    }

    /**
     * Removes a mechanic from the vehicle user's preferences.
     * Throws {@link ResourceNotFoundException} if the mechanic is not in their preferences.
     */
    @Transactional
    public void removePreference(User vehicleUser, Long mechanicId) {
        User mechanic = userRepository.findById(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found with id: " + mechanicId));

        Optional<UserMechanicPreference> preference =
                preferenceRepository.findByVehicleUserAndMechanic(vehicleUser, mechanic);

        if (preference.isEmpty()) {
            throw new ResourceNotFoundException("Mechanic is not in your preferences");
        }

        preferenceRepository.delete(preference.get());
    }

    private MechanicSearchResponse toSearchResponse(User user) {
        return new MechanicSearchResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getCity(),
                user.getArea(),
                user.getState(),
                user.getWorkshopName(),
                user.getSpecialization(),
                user.getExperienceYears(),
                user.getHourlyRate(),
                user.getRating(),
                user.getTotalReviews(),
                user.getTotalBookingsCompleted(),
                user.getBio(),
                user.getLatitude(),
                user.getLongitude()
        );
    }
}
