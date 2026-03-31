package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.AvailabilityRequest;
import com.alvexo.bookingapp.dto.request.WeeklyAvailabilityRequest;
import com.alvexo.bookingapp.dto.response.AvailabilityResponse;
import com.alvexo.bookingapp.dto.response.BreakWindowResponse;
import com.alvexo.bookingapp.dto.response.SlotResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicAvailabilityRepository;
import com.alvexo.bookingapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MechanicService {

    @Autowired private MechanicAvailabilityRepository availabilityRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;

    // ── Single-day availability ───────────────────────────────────────────────

    @Transactional
    public AvailabilityResponse addAvailability(User mechanic, AvailabilityRequest request) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can add availability");
        }
        validateRequest(request);

        MechanicAvailability availability = buildAvailability(mechanic, request);
        return convertToResponse(availabilityRepository.save(availability));
    }

    @Transactional(readOnly = true)
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
        validateRequest(request);

        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        if (request.getIsAvailable() != null) availability.setIsAvailable(request.getIsAvailable());
        if (request.getSlotDurationMinutes() != null) availability.setSlotDurationMinutes(request.getSlotDurationMinutes());
        if (request.getMaxSlotsPerDay() != null) availability.setMaxSlotsPerDay(request.getMaxSlotsPerDay());
        availability.setBreakWindows(toBreakWindows(request.getBreakWindows()));

        return convertToResponse(availabilityRepository.save(availability));
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

    // ── Weekly availability (one call sets all days) ──────────────────────────

    /**
     * Sets or replaces availability for multiple days in a single request.
     *
     * Behaviour per day:
     *  - If an availability rule already exists for that day → it is REPLACED.
     *  - If no rule exists for that day → a new one is CREATED.
     *  - Days not included in the request are LEFT UNCHANGED.
     *
     * This lets a mechanic configure their full week in one API call instead
     * of making 7 separate POST /availability requests.
     */
    @Transactional
    public List<AvailabilityResponse> setWeeklyAvailability(User mechanic,
                                                             WeeklyAvailabilityRequest request) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can set availability");
        }

        // Validate no duplicate days in the request itself
        long distinctDays = request.getDays().stream()
                .map(AvailabilityRequest::getDayOfWeek)
                .distinct().count();
        if (distinctDays != request.getDays().size()) {
            throw new BadRequestException("Duplicate days found in the request. Each day must appear only once.");
        }

        List<AvailabilityResponse> results = new ArrayList<>();

        for (AvailabilityRequest dayRequest : request.getDays()) {
            validateRequest(dayRequest);

            // Check if a rule already exists for this mechanic + day
            List<MechanicAvailability> existing =
                    availabilityRepository.findByMechanicAndDayOfWeek(mechanic, dayRequest.getDayOfWeek());

            MechanicAvailability availability;
            if (!existing.isEmpty()) {
                // Update the existing rule (replace in-place)
                availability = existing.get(0);
                availability.setStartTime(dayRequest.getStartTime());
                availability.setEndTime(dayRequest.getEndTime());
                availability.setIsAvailable(dayRequest.getIsAvailable() != null
                        ? dayRequest.getIsAvailable() : availability.getIsAvailable());
                availability.setSlotDurationMinutes(dayRequest.getSlotDurationMinutes() != null
                        ? dayRequest.getSlotDurationMinutes() : availability.getSlotDurationMinutes());
                availability.setMaxSlotsPerDay(dayRequest.getMaxSlotsPerDay() != null
                        ? dayRequest.getMaxSlotsPerDay() : availability.getMaxSlotsPerDay());
                availability.setBreakWindows(toBreakWindows(dayRequest.getBreakWindows()));
            } else {
                // Create new rule
                availability = buildAvailability(mechanic, dayRequest);
            }

            results.add(convertToResponse(availabilityRepository.save(availability)));
        }

        return results;
    }

    // ── On-demand slot computation ────────────────────────────────────────────

    /**
     * Computes available slots for a mechanic on a given date.
     *
     * - Reads the mechanic's availability template (startTime, endTime, slotDurationMinutes)
     * - Reads already-booked slots from the bookings table
     * - Excludes break windows (lunch, prayer breaks etc.) from the result
     * - Excludes past slots when querying for today
     *
     * No DB writes happen — purely a read + compute operation.
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlots(Long mechanicId, LocalDate date) {
        User mechanic = userRepository.findById(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found"));

        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("User is not a mechanic");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot query slots for past dates");
        }

        DayOfWeek dayOfWeek = DayOfWeek.valueOf(date.getDayOfWeek().name());

        List<MechanicAvailability> templates =
                availabilityRepository.findByMechanicAndDayOfWeek(mechanic, dayOfWeek);

        if (templates.isEmpty()) return List.of();

        // Fetch booked DateTimes for this day from the bookings table
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.atTime(LocalTime.MAX);

        // Extract just LocalTime from already-booked DateTimes for this day
        Set<LocalTime> bookedTimes = bookingRepository
                .findBookedDateTimes(mechanic, dayStart, dayEnd)
                .stream()
                .map(LocalDateTime::toLocalTime)
                .collect(Collectors.toSet());

        LocalTime now = LocalTime.now();
        boolean isToday = date.equals(LocalDate.now());
        List<SlotResponse> result = new ArrayList<>();
        String mechanicName = mechanic.getFirstName() + " " + mechanic.getLastName();

        for (MechanicAvailability template : templates) {
            if (!Boolean.TRUE.equals(template.getIsAvailable())) continue;

            int durationMins = template.getSlotDurationMinutes() != null
                    ? template.getSlotDurationMinutes() : 60;

            LocalTime cursor = template.getStartTime();

            while (!cursor.plusMinutes(durationMins).isAfter(template.getEndTime())) {
                LocalTime slotEnd = cursor.plusMinutes(durationMins);

                boolean alreadyBooked = bookedTimes.contains(cursor);
                boolean inBreak       = template.isInBreak(cursor, slotEnd);
                boolean inPast        = isToday && cursor.isBefore(now);

                if (!alreadyBooked && !inBreak && !inPast) {
                    result.add(SlotResponse.builder()
                            .slotDate(date)
                            .slotStart(cursor)
                            .slotEnd(slotEnd)
                            .scheduledDateTime(LocalDateTime.of(date, cursor))
                            .mechanicId(mechanic.getId())
                            .mechanicName(mechanicName)
                            .slotDurationMinutes(durationMins)
                            .build());
                }
                cursor = slotEnd;
            }
        }
        return result;
    }

    // ── Nearby search ─────────────────────────────────────────────────────────

    public List<UserResponse> findNearbyMechanics(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        return userRepository.findNearbyMechanics(UserRole.MECHANIC, latitude, longitude, radiusKm)
                .stream().map(this::convertUserToResponse).collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private MechanicAvailability buildAvailability(User mechanic, AvailabilityRequest r) {
        return MechanicAvailability.builder()
                .mechanic(mechanic)
                .dayOfWeek(r.getDayOfWeek())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .isAvailable(r.getIsAvailable() != null ? r.getIsAvailable() : true)
                .slotDurationMinutes(r.getSlotDurationMinutes() != null ? r.getSlotDurationMinutes() : 60)
                .maxSlotsPerDay(r.getMaxSlotsPerDay() != null ? r.getMaxSlotsPerDay() : 10)
                .breakWindows(toBreakWindows(r.getBreakWindows()))
                .build();
    }

    private void validateRequest(AvailabilityRequest r) {
        if (!r.getStartTime().isBefore(r.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }
        if (r.getBreakWindows() != null) {
            for (var b : r.getBreakWindows()) {
                if (!b.getStart().isBefore(b.getEnd())) {
                    throw new BadRequestException("Break start must be before break end");
                }
                if (b.getStart().isBefore(r.getStartTime()) || b.getEnd().isAfter(r.getEndTime())) {
                    throw new BadRequestException(
                            "Break window " + b.getStart() + "-" + b.getEnd()
                            + " must be within working hours " + r.getStartTime() + "-" + r.getEndTime());
                }
            }
        }
    }

    private List<BreakWindow> toBreakWindows(
            List<com.alvexo.bookingapp.dto.request.BreakWindowRequest> requests) {
        if (requests == null || requests.isEmpty()) return new ArrayList<>();
        return requests.stream()
                .map(b -> BreakWindow.builder()
                        .start(b.getStart())
                        .end(b.getEnd())
                        .label(b.getLabel())
                        .build())
                .collect(Collectors.toList());
    }

    private AvailabilityResponse convertToResponse(MechanicAvailability a) {
        List<BreakWindowResponse> breaks = (a.getBreakWindows() == null || a.getBreakWindows().isEmpty())
                ? List.of()
                : a.getBreakWindows().stream()
                        .map(b -> BreakWindowResponse.builder()
                                .start(b.getStart()).end(b.getEnd()).label(b.getLabel()).build())
                        .collect(Collectors.toList());

        return AvailabilityResponse.builder()
                .id(a.getId())
                .dayOfWeek(a.getDayOfWeek())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .isAvailable(a.getIsAvailable())
                .slotDurationMinutes(a.getSlotDurationMinutes())
                .maxSlotsPerDay(a.getMaxSlotsPerDay())
                .breakWindows(breaks)
                .build();
    }

    private UserResponse convertUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId()).email(user.getEmail())
                .firstName(user.getFirstName()).lastName(user.getLastName())
                .role(user.getRole()).specialization(user.getSpecialization())
                .experienceYears(user.getExperienceYears()).hourlyRate(user.getHourlyRate())
                .bio(user.getBio()).rating(user.getRating())
                .totalReviews(user.getTotalReviews())
                .totalBookingsCompleted(user.getTotalBookingsCompleted())
                .latitude(user.getLatitude()).longitude(user.getLongitude())
                .city(user.getCity()).state(user.getState())
                .build();
    }
}
