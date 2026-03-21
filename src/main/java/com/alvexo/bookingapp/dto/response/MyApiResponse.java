package com.alvexo.bookingapp.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> MyApiResponse<T> success(T data) {
        return MyApiResponse.<T>builder()
                .success(true)
                .message("Operation successful")
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }
    
    public static <T> MyApiResponse<T> success(String message, T data) {
        return MyApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data).timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> MyApiResponse<T> error(String message) {
        return MyApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
}
