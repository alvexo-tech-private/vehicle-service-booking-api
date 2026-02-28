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
    
    // Payment
    public static final String DEFAULT_CURRENCY = "USD";
    
    private Constants() {
        // Private constructor to prevent instantiation
    }
}
