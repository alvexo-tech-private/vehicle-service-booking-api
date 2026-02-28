package com.alvexo.bookingapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@bookingapp.com}")
    private String fromEmail;
    
    public void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            System.out.println("Email not configured. Would send: " + subject + " to " + to);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
    
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to Vehicle Booking Platform";
        String body = "Dear " + name + ",\n\n" +
                     "Welcome to our Vehicle Booking Platform!\n\n" +
                     "Thank you for registering with us.\n\n" +
                     "Best regards,\n" +
                     "Vehicle Booking Team";
        
        sendEmail(to, subject, body);
    }
    
    public void sendBookingConfirmation(String to, String bookingNumber) {
        String subject = "Booking Confirmation - " + bookingNumber;
        String body = "Your booking " + bookingNumber + " has been confirmed.\n\n" +
                     "You will receive further updates about your booking.\n\n" +
                     "Best regards,\n" +
                     "Vehicle Booking Team";
        
        sendEmail(to, subject, body);
    }
}
