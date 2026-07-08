package com.alvexo.bookingapp.util;

public class Constants {
    
    // JWT
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    
    // Roles
    public static final String ROLE_VEHICLE_USER = "ROLE_VEHICLE_USER";
    public static final String ROLE_MECHANIC = "ROLE_MECHANIC";
    public static final String ROLE_SALES_REPRESENTATIVE = "ROLE_SALES_REPRESENTATIVE";
    public static final String ROLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR";
    
    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    // Booking
    public static final int DEFAULT_SLOT_DURATION_MINUTES = 60;
    public static final int MAX_BOOKING_DAYS_AHEAD = 90;
    
    // Search
    public static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;
    public static final double MAX_SEARCH_RADIUS_KM = 100.0;

    // Mechanic preferences (vehicle user favourites)
    // Change this value to adjust the per-user cap without touching any other code.
    public static final int MAX_MECHANIC_PREFERENCES = 5;
    
    // Payment
    public static final String DEFAULT_CURRENCY = "USD";

    // Workshop profile — onboarding
    public static final java.math.BigDecimal WORKSHOP_ONBOARDING_FEE = new java.math.BigDecimal("1000.00");

    // Workshop profile — images / support attachments
    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;   // 5 MB
    public static final long MAX_SUPPORT_IMAGE_SIZE_BYTES = 2L * 1024 * 1024; // 2 MB
    public static final java.util.Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            java.util.Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    public static final int SUPPORT_DESCRIPTION_MAX_WORDS = 200;

    // Workshop profile — platform verification
    public static final int PLATFORM_VERIFICATION_MIN_METHODS = 2;

    // Workshop profile — location
    public static final java.util.Set<String> INDIAN_STATES = java.util.Set.of(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat",
            "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
            "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh",
            "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands", "Chandigarh",
            "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Jammu and Kashmir", "Ladakh",
            "Lakshadweep", "Puducherry"
    );

    private Constants() {
        // Private constructor to prevent instantiation
    }
}
