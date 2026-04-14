package com.alvexo.bookingapp.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String profileImageUrl;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String bio;
}
