package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.PickupSummaryResponse;
import com.alvexo.bookingapp.dto.response.PickupVehicleResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.MechanicMasterEntry;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicMasterEntryRepository;
import com.alvexo.bookingapp.repository.UserVehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Backs the Service Desk "Pickup" tab (SERVICE_DESK_API_SPEC.md §3). No history — a single live list per BR-2. */
@Service
public class ServiceDeskPickupService {

    private final BookingRepository bookingRepository;
    private final MechanicMasterEntryRepository mechanicMasterEntryRepository;
    private final UserVehicleRepository userVehicleRepository;

    public ServiceDeskPickupService(BookingRepository bookingRepository,
                                     MechanicMasterEntryRepository mechanicMasterEntryRepository,
                                     UserVehicleRepository userVehicleRepository) {
        this.bookingRepository = bookingRepository;
        this.mechanicMasterEntryRepository = mechanicMasterEntryRepository;
        this.userVehicleRepository = userVehicleRepository;
    }

    @Transactional(readOnly = true)
    public PickupSummaryResponse getPickups(User mechanic, LocalDate date) {
        LocalDate resolvedDate = rejectFutureDate(date);
        List<Booking> bookings = bookingRepository.findPickups(mechanic, resolvedDate);

        List<PickupVehicleResponse> vehicles = bookings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        long assigned = vehicles.stream().filter(v -> v.getAssignedMechanicId() != null).count();
        LocalDateTime lastUpdated = bookings.stream()
                .map(Booking::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return PickupSummaryResponse.builder()
                .total(vehicles.size())
                .assigned(assigned)
                .unassigned(vehicles.size() - assigned)
                .lastUpdated(lastUpdated)
                .vehicles(vehicles)
                .build();
    }

    @Transactional
    public PickupVehicleResponse assign(User mechanic, String bookingId, Long mechanicEntryId) {
        Booking booking = bookingRepository.findByBookingNumberAndMechanic(bookingId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!Boolean.TRUE.equals(booking.getPickupRequired())) {
            throw new BadRequestException("This booking does not require pickup");
        }

        MechanicMasterEntry entry = mechanicMasterEntryRepository.findByIdAndMechanic(mechanicEntryId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found: " + mechanicEntryId));
        if (!Boolean.TRUE.equals(entry.getActive())) {
            throw new BadRequestException("Mechanic '" + entry.getName() + "' is not active");
        }

        booking.setPickupMechanic(entry);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    private PickupVehicleResponse toResponse(Booking booking) {
        String vehicleNumber = userVehicleRepository
                .findByUserAndVehicle(booking.getVehicleUser(), booking.getVehicle())
                .map(uv -> uv.getRegistrationNumber())
                .orElse(null);
        MechanicMasterEntry pickupMechanic = booking.getPickupMechanic();

        return PickupVehicleResponse.builder()
                .bookingId(booking.getBookingNumber())
                .ownerName(booking.getVehicleUser().getFirstName() + " " + booking.getVehicleUser().getLastName())
                .mobile(booking.getVehicleUser().getMobileNumber())
                .vehicleNumber(vehicleNumber)
                .pickupAddress(booking.getPickupAddress())
                .assignedMechanicId(pickupMechanic != null ? pickupMechanic.getId() : null)
                .assignedMechanicName(pickupMechanic != null ? pickupMechanic.getName() : null)
                .build();
    }

    private LocalDate rejectFutureDate(LocalDate date) {
        LocalDate resolved = date != null ? date : LocalDate.now();
        if (resolved.isAfter(LocalDate.now())) {
            throw new BadRequestException("Future dates are not available on this tab");
        }
        return resolved;
    }
}
