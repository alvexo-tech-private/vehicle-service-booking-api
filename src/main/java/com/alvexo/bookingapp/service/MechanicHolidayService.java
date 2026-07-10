package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.MechanicHolidayRequest;
import com.alvexo.bookingapp.dto.response.MechanicHolidayResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.MechanicHoliday;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.MechanicHolidayRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MechanicHolidayService {

    private final MechanicHolidayRepository holidayRepository;

    public MechanicHolidayService(MechanicHolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Transactional
    public MechanicHolidayResponse createHoliday(User mechanic, MechanicHolidayRequest request) {
        validateRole(mechanic);
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot mark a past date as a holiday/pause");
        }

        MechanicHoliday holiday = MechanicHoliday.builder()
                .mechanic(mechanic)
                .date(request.getDate())
                .type(request.getType())
                .reason(request.getReason())
                .build();

        try {
            return toResponse(holidayRepository.save(holiday));
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("A holiday/pause is already set for " + request.getDate());
        }
    }

    @Transactional
    public void deleteHoliday(User mechanic, Long holidayId) {
        validateRole(mechanic);
        MechanicHoliday holiday = holidayRepository.findByIdAndMechanic(holidayId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday/pause not found"));
        holidayRepository.delete(holiday);
    }

    @Transactional(readOnly = true)
    public List<MechanicHolidayResponse> getHolidays(Long mechanicId, LocalDate from, LocalDate to) {
        return holidayRepository.findByMechanicIdAndDateBetweenOrderByDateAsc(mechanicId, from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Used by BookingService to reject new bookings on a holiday/pause date. */
    @Transactional(readOnly = true)
    public Optional<MechanicHoliday> findHoliday(User mechanic, LocalDate date) {
        return holidayRepository.findByMechanicAndDate(mechanic, date);
    }

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can manage holidays/pauses");
        }
    }

    private MechanicHolidayResponse toResponse(MechanicHoliday h) {
        return MechanicHolidayResponse.builder()
                .id(h.getId())
                .date(h.getDate())
                .type(h.getType())
                .reason(h.getReason())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
