package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.EarningEntryResponse;
import com.alvexo.bookingapp.dto.response.EarningsResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.UserVehicleRepository;
import com.alvexo.bookingapp.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily Advance Summary — backs GET /api/service-desk/earnings
 * (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §9). Read-only.
 */
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

        List<Booking> bookings = bookingRepository.findDailyAdvanceSummaryBookings(mechanic, resolvedDate);

        List<EarningEntryResponse> entries = new ArrayList<>();
        BigDecimal totalAdvance = BigDecimal.ZERO;
        int vehiclesWithAdvance = 0;

        for (Booking booking : bookings) {
            String vehicleNumber = userVehicleRepository
                    .findByUserAndVehicle(booking.getVehicleUser(), booking.getVehicle())
                    .map(uv -> uv.getRegistrationNumber())
                    .orElse(null);

            BigDecimal advancePaid = booking.getAdvancePaid() != null ? booking.getAdvancePaid() : BigDecimal.ZERO;
            entries.add(EarningEntryResponse.builder()
                    .bookingId(booking.getBookingNumber())
                    .registrationNumber(vehicleNumber)
                    .advancePaid(advancePaid)
                    .build());

            if (advancePaid.compareTo(BigDecimal.ZERO) > 0) {
                vehiclesWithAdvance++;
                totalAdvance = totalAdvance.add(advancePaid);
            }
        }

        BigDecimal totalFee = Constants.ADVANCE_HANDLING_FEE.multiply(BigDecimal.valueOf(vehiclesWithAdvance));
        BigDecimal net = totalAdvance.subtract(totalFee).max(BigDecimal.ZERO);

        return EarningsResponse.builder()
                .serviceDate(resolvedDate)
                .totalVehicles(entries.size())
                .vehiclesWithAdvance(vehiclesWithAdvance)
                .vehiclesWithoutAdvance(entries.size() - vehiclesWithAdvance)
                .totalAdvance(totalAdvance)
                .feePerAdvanceBooking(Constants.ADVANCE_HANDLING_FEE)
                .totalFee(totalFee)
                .netPaymentToWorkshop(net)
                .bookings(entries)
                .build();
    }
}
