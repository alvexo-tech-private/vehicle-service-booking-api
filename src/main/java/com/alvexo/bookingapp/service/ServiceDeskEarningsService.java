package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.EarningEntryResponse;
import com.alvexo.bookingapp.dto.response.EarningsResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.EarningKind;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.UserVehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Backs the Service Desk "Earnings" tab (SERVICE_DESK_API_SPEC.md §5) — read-only. */
@Service
public class ServiceDeskEarningsService {

    private final BookingRepository bookingRepository;
    private final UserVehicleRepository userVehicleRepository;

    public ServiceDeskEarningsService(BookingRepository bookingRepository, UserVehicleRepository userVehicleRepository) {
        this.bookingRepository = bookingRepository;
        this.userVehicleRepository = userVehicleRepository;
    }

    @Transactional(readOnly = true)
    public EarningsResponse getEarnings(User mechanic, LocalDate date) {
        LocalDate resolvedDate = date != null ? date : LocalDate.now();
        if (resolvedDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Future dates are not available on this tab");
        }

        List<Booking> bookings = bookingRepository.findEarningsBookings(mechanic, resolvedDate);

        List<EarningEntryResponse> entries = new ArrayList<>();
        for (Booking booking : bookings) {
            String vehicleNumber = userVehicleRepository
                    .findByUserAndVehicle(booking.getVehicleUser(), booking.getVehicle())
                    .map(uv -> uv.getRegistrationNumber())
                    .orElse(null);

            if (booking.getAdvancePaid() != null && booking.getAdvancePaid().compareTo(BigDecimal.ZERO) > 0) {
                entries.add(EarningEntryResponse.builder()
                        .bookingId(booking.getBookingNumber())
                        .vehicleNumber(vehicleNumber)
                        .amount(booking.getAdvancePaid())
                        .kind(EarningKind.ADVANCE)
                        .build());
            }
            if (booking.getReliabilityAdjustmentAmount() != null) {
                entries.add(EarningEntryResponse.builder()
                        .bookingId(booking.getBookingNumber())
                        .vehicleNumber(vehicleNumber)
                        .amount(booking.getReliabilityAdjustmentAmount().negate())
                        .kind(EarningKind.CANCELLATION)
                        .build());
            }
        }

        BigDecimal advances = sum(entries, EarningKind.ADVANCE);
        BigDecimal cancellations = sum(entries, EarningKind.CANCELLATION);

        return EarningsResponse.builder()
                .advances(advances)
                .cancellations(cancellations)
                .net(advances.add(cancellations))
                .entries(entries)
                .build();
    }

    private BigDecimal sum(List<EarningEntryResponse> entries, EarningKind kind) {
        return entries.stream()
                .filter(e -> e.getKind() == kind)
                .map(EarningEntryResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
