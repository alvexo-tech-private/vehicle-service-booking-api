package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.UserVehicleRequestDTO;
import com.alvexo.bookingapp.dto.response.UserVehicleResponseDto;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserVehicle;
import com.alvexo.bookingapp.model.Vehicle;
import com.alvexo.bookingapp.repository.UserVehicleRepository;
import com.alvexo.bookingapp.repository.VehicleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserVehicleService {

    @Autowired
    private UserVehicleRepository userVehicleRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

   
    @Transactional
    public UserVehicleResponseDto mapVehicleToUser(User user, Long vehicleId, Boolean isPrimary) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (userVehicleRepository.existsByUserAndVehicle(user, vehicle)) {
            throw new BadRequestException("Vehicle already mapped to this user");
        }

        // If this is primary, unset other primary vehicles
        if (isPrimary != null && isPrimary) {
            userVehicleRepository.findByUserAndIsPrimaryTrue(user)
                    .ifPresent(uv -> {
                        uv.setIsPrimary(false);
                        userVehicleRepository.save(uv);
                    });
        }

        UserVehicle userVehicle = UserVehicle.builder()
                .user(user)
                .vehicle(vehicle)
                .isPrimary(isPrimary != null ? isPrimary : false)
                .build();

        return UserVehicleResponseDto.from(userVehicleRepository.save(userVehicle));
    }

    public List<UserVehicleResponseDto> getUserVehicles(User user) {
        return userVehicleRepository.findByUser(user)
                .stream()
                .map(UserVehicleResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeVehicleMapping(Long mappingId, User user) {
        UserVehicle userVehicle = userVehicleRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle mapping not found"));

        if (!userVehicle.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only remove your own vehicle mappings");
        }

        userVehicleRepository.delete(userVehicle);
    }

    public UserVehicleResponseDto from(UserVehicle uv) {
        return UserVehicleResponseDto.builder()
                .id(uv.getId())
                .isPrimary(uv.getIsPrimary())
                // User fields
                .userId(uv.getUser().getId())
                .userEmail(uv.getUser().getEmail())
                .userName(uv.getUser().getFirstName() + uv.getUser().getLastName())
                // Vehicle fields — adjust getters to match your Vehicle model
                .vehicleId(uv.getVehicle().getId())
                .vehicleName(uv.getVehicle().getMake())
                .vehicleType(uv.getVehicle().getVehicleType().name())
                .registraionYear(uv.getRegistraionYear())
                .registrationNumber(uv.getRegistrationNumber())
                .build();
    }

}