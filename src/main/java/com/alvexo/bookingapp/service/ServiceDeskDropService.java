package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.DropSummaryResponse;
import com.alvexo.bookingapp.dto.response.DropVehicleResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.AssignmentStatus;
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

/** Backs the Service Desk "Drop" tab (SERVICE_DESK_API_SPEC.md §4). No history — a single live list per BR-2. */
@Service
public class ServiceDeskDropService {

    private final BookingRepository bookingRepository;
    private final MechanicMasterEntryRepository mechanicMasterEntryRepository;
    private final UserVehicleRepository userVehicleRepository;

    public ServiceDeskDropService(BookingRepository bookingRepository,
                                   MechanicMasterEntryRepository mechanicMasterEntryRepository,
                                   UserVehicleRepository userVehicleRepository) {
        this.bookingRepository = bookingRepository;
        this.mechanicMasterEntryRepository = mechanicMasterEntryRepository;
        this.userVehicleRepository = userVehicleRepository;
    }

    @Transactional(readOnly = true)
    public DropSummaryResponse getDrops(User mechanic, LocalDate date) {
        LocalDate resolvedDate = rejectFutureDate(date);
        List<Booking> bookings = bookingRepository.findDrops(mechanic, resolvedDate);

        List<DropVehicleResponse> vehicles = bookings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        long assigned = vehicles.stream().filter(v -> v.getAssignedMechanicId() != null).count();
        long delivered = vehicles.stream().filter(v -> v.getDeliveredBy() != null).count();
        LocalDateTime lastUpdated = bookings.stream()
                .map(Booking::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return DropSummaryResponse.builder()
                .total(vehicles.size())
                .assigned(assigned)
                .delivered(delivered)
                .lastUpdated(lastUpdated)
                .vehicles(vehicles)
                .build();
    }

    @Transactional
    public DropVehicleResponse assign(User mechanic, String bookingId, Long mechanicEntryId) {
        Booking booking = resolveDropBooking(mechanic, bookingId);

        MechanicMasterEntry entry = mechanicMasterEntryRepository.findByIdAndMechanic(mechanicEntryId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found: " + mechanicEntryId));
        if (!Boolean.TRUE.equals(entry.getActive())) {
            throw new BadRequestException("Mechanic '" + entry.getName() + "' is not active");
        }

        booking.setDropMechanic(entry);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional
    public DropVehicleResponse deliver(User mechanic, String bookingId, String deliveredBy, LocalDate deliveredOn) {
        Booking booking = resolveDropBooking(mechanic, bookingId);
        if (booking.getDeliveredBy() != null) {
            throw new BadRequestException("This vehicle has already been marked delivered");
        }

        booking.setDeliveredBy(deliveredBy);
        booking.setDeliveredOn(deliveredOn);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    private Booking resolveDropBooking(User mechanic, String bookingId) {
        Booking booking = bookingRepository.findByBookingNumberAndMechanic(bookingId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!Boolean.TRUE.equals(booking.getDropRequired())) {
            throw new BadRequestException("This booking does not require drop");
        }
        return booking;
    }

    private DropVehicleResponse toResponse(Booking booking) {
        String vehicleNumber = userVehicleRepository
                .findByUserAndVehicle(booking.getVehicleUser(), booking.getVehicle())
                .map(uv -> uv.getRegistrationNumber())
                .orElse(null);
        MechanicMasterEntry dropMechanic = booking.getDropMechanic();

        AssignmentStatus assignmentStatus = booking.getDeliveredBy() != null ? AssignmentStatus.DELIVERED
                : dropMechanic != null ? AssignmentStatus.ASSIGNED : AssignmentStatus.UNASSIGNED;

        return DropVehicleResponse.builder()
                .bookingId(booking.getBookingNumber())
                .jobCardNumber(booking.getJobCardNumber())
                .ownerName(booking.getVehicleUser().getFirstName() + " " + booking.getVehicleUser().getLastName())
                .mobile(booking.getVehicleUser().getMobileNumber())
                .vehicleNumber(vehicleNumber)
                .deliveryAddress(booking.getDeliveryAddress())
                .assignedMechanicId(dropMechanic != null ? dropMechanic.getId() : null)
                .assignedMechanicName(dropMechanic != null ? dropMechanic.getName() : null)
                .deliveredBy(booking.getDeliveredBy())
                .deliveredOn(booking.getDeliveredOn())
                .assignmentStatus(assignmentStatus)
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
