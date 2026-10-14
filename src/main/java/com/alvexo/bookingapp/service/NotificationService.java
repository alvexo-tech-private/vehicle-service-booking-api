package com.alvexo.bookingapp.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.exception.BusinessRuleException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Notification;
import com.alvexo.bookingapp.model.NotificationType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.NotificationRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${app.toemail.address:info@alvexotech.com}")
    private String toEmailAddress;
    
    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${app.from.email}")
    private String fromEmail;
    
    @Async
    @Transactional
    public void createNotification(User user, String title, String message, 
                                          NotificationType type, String entityType, Long entityId) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .build();
        
        notificationRepository.save(notification);
    }
    
    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalse(user);
    }
    
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
    
    public void sendOtpEmailOld(String mobileNumber, String otp) {
        if (mailSender == null) {
            System.err.println("Mail sender not configured. Cannot send OTP email.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmailAddress.split(","));
        message.setSubject("Your OTP Code for "+mobileNumber);
        message.setText("OTP for Code mobile Number: " + mobileNumber + " is: " + otp);

        mailSender.send(message);
    }
    
    public void sendOtpEmail(String mobileNumber, String email, String otp) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("EMAIL_DELIVERY_FAILED",
                    "No email on file for this account; SMS delivery is not yet available");
        }
        sendOtpViaSendGrid(email, "Your OTP Code for " + mobileNumber,
                "OTP for mobile number " + mobileNumber + " is: " + otp);
    }
    
    // ── Contact-detail change OTP delivery ────────────────────────────────────
    // Sent via the SendGrid HTTP API rather than JavaMailSender/SMTP: Render
    // blocks outbound SMTP ports (25/465/587), so a direct SMTP send always
    // times out there regardless of mail config.

    /**
     * Sends an OTP to the user's <em>new</em> email address so they can prove
     * ownership before the address is committed to the account.
     */
    public void sendEmailChangeOtp(String newEmail, String otp) {
        sendOtpViaSendGrid(newEmail, "Verify your new email address",
                "Your OTP to verify your new email address is: " + otp
                        + "\nThis code is valid for 5 minutes. Do not share it with anyone.");
    }

    /**
     * Sends an OTP to verify a workshop owner's WhatsApp number.
     * (SMS/WhatsApp delivery is not yet implemented — delivered via the owner's account email.)
     */
    public void sendWhatsappVerificationOtp(String accountEmail, String otp) {
        sendOtpViaSendGrid(accountEmail, "Verify your WhatsApp number",
                "Your OTP to verify your WhatsApp number is: " + otp
                        + "\nThis code is valid for 5 minutes. Do not share it with anyone.");
    }

    private void sendOtpViaSendGrid(String toAddress, String subject, String body) {
        Email from = new Email(fromEmail);
        Email to = new Email(toAddress);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        try {
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() >= 400) {
                throw new BusinessRuleException("EMAIL_DELIVERY_FAILED",
                        "Failed to send email: SendGrid returned " + response.getStatusCode());
            }
        } catch (IOException e) {
            throw new BusinessRuleException("EMAIL_DELIVERY_FAILED", "Failed to send email");
        }
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
    
    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndIsReadFalse(user);
        unread.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(unread);
    }
}
