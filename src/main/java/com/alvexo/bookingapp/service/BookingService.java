package com.alvexo.bookingapp.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.BookingRequest;
import com.alvexo.bookingapp.dto.response.BookingResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.NotificationType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.model.Vehicle;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.repository.VehicleRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.notificationService = notificationService;
    }
    
    @Transactional
    public BookingResponse createBooking(BookingRequest request, User vehicleUser) {
        User mechanic = userRepository.findById(request.getMechanicId())
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found"));
        
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Selected user is not a mechanic");
        }
        
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        
        // Check for conflicts
        LocalDateTime endTime = request.getScheduledDateTime()
                .plusMinutes(request.getEstimatedDurationMinutes() != null ? request.getEstimatedDurationMinutes() : 60);
        
        long conflicts = bookingRepository.countConflictingBookings(
                mechanic, request.getScheduledDateTime(), endTime);
        
        if (conflicts > 0) {
            throw new BadRequestException("Mechanic is not available at the selected time");
        }
        
        Booking booking = Booking.builder()
                .vehicleUser(vehicleUser)
                .mechanic(mechanic)
                .vehicle(vehicle)
                .bookingNumber(generateBookingNumber())
                .scheduledDateTime(request.getScheduledDateTime())
                .status(BookingStatus.PENDING)
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .estimatedCost(request.getEstimatedCost())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .customerNotes(request.getCustomerNotes())
                .build();
        
        booking = bookingRepository.save(booking);
        
        // Send notification to mechanic
        notificationService.createNotification(
                mechanic,
                "New Booking Request",
                "You have a new booking request from " + vehicleUser.getFirstName(),
                NotificationType.BOOKING_CREATED,
                "Booking",
                booking.getId()
        );
        
        return convertToResponse(booking);
    }
    
    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, BookingStatus newStatus, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        // Validate user can update this booking
        if (newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.REJECTED) {
            if (!booking.getMechanic().getId().equals(user.getId())) {
                throw new BadRequestException("Only the assigned mechanic can confirm/reject bookings");
            }
        }
        
        booking.setStatus(newStatus);
        
        if (newStatus == BookingStatus.COMPLETED) {
            booking.setCompletedAt(LocalDateTime.now());
            // Update mechanic stats
            User mechanic = booking.getMechanic();
            mechanic.setTotalBookingsCompleted(mechanic.getTotalBookingsCompleted() + 1);
            userRepository.save(mechanic);
        } else if (newStatus == BookingStatus.CANCELLED) {
            booking.setCancelledAt(LocalDateTime.now());
            booking.setCancelledBy(user);
        }
        
        booking = bookingRepository.save(booking);
        
        // Send notification
        User recipient = booking.getMechanic().getId().equals(user.getId()) 
                ? booking.getVehicleUser() : booking.getMechanic();
        
        notificationService.createNotification(
                recipient,
                "Booking Status Updated",
                "Booking #" + booking.getBookingNumber() + " status: " + newStatus,
                NotificationType.valueOf("BOOKING_" + newStatus),
                "Booking",
                booking.getId()
        );
        
        return convertToResponse(booking);
    }
    
    public Page<BookingResponse> getUserBookings(User user, Pageable pageable) {
        if (user.getRole() == UserRole.MECHANIC) {
            return bookingRepository.findByMechanic(user, pageable)
                    .map(this::convertToResponse);
        } else {
            return bookingRepository.findByVehicleUser(user, pageable)
                    .map(this::convertToResponse);
        }
    }
    
    public BookingResponse getBookingById(Long id, User user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        // Verify user has access to this booking
        if (!booking.getVehicleUser().getId().equals(user.getId()) 
                && !booking.getMechanic().getId().equals(user.getId())
                && user.getRole() != UserRole.ADMINISTRATOR) {
            throw new BadRequestException("You don't have access to this booking");
        }
        
        return convertToResponse(booking);
    }
    
    private BookingResponse convertToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .vehicleUserId(booking.getVehicleUser().getId())
                .vehicleUserName(booking.getVehicleUser().getFirstName() + " " + booking.getVehicleUser().getLastName())
                .mechanicId(booking.getMechanic().getId())
                .mechanicName(booking.getMechanic().getFirstName() + " " + booking.getMechanic().getLastName())
                .vehicleId(booking.getVehicle().getId())
                .vehicleInfo(booking.getVehicle().getMake() + " " + booking.getVehicle().getModel() + " " + booking.getVehicle().getYear())
                .scheduledDateTime(booking.getScheduledDateTime())
                .status(booking.getStatus())
                .serviceType(booking.getServiceType())
                .description(booking.getDescription())
                .estimatedCost(booking.getEstimatedCost())
                .actualCost(booking.getActualCost())
                .estimatedDurationMinutes(booking.getEstimatedDurationMinutes())
                .actualDurationMinutes(booking.getActualDurationMinutes())
                .mechanicNotes(booking.getMechanicNotes())
                .customerNotes(booking.getCustomerNotes())
                .cancellationReason(booking.getCancellationReason())
                .completedAt(booking.getCompletedAt())
                .createdAt(booking.getCreatedAt())
                .build();
    }
    
    private String generateBookingNumber() {
        return "BKG" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
