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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.toemail.address:info@alvexotech.com}")
    private String toEmailAddress;
    
    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${app.from.email}")
    private String fromEmail;
    
    @Transactional
    public Notification createNotification(User user, String title, String message, 
                                          NotificationType type, String entityType, Long entityId) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .build();
        
        return notificationRepository.save(notification);
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmailAddress.split(","));
        message.setSubject("Your OTP Code for "+mobileNumber);
        message.setText("OTP for Code mobile Number: " + mobileNumber + " is: " + otp);

        mailSender.send(message);
    }
    
    public void sendOtpEmail(String mobileNumber, String otp)   {

        Email from = new Email(fromEmail);
        Email toEmail = new Email(toEmailAddress);

        Content emailContent = new Content("text/plain", "OTP for mobile " + mobileNumber + " is: " + otp);
        Mail mail = new Mail(from, "Your OTP Code", toEmail, emailContent);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        Response response=null;
        try {
			request.setBody(mail.build());
			response = sg.api(request);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


        System.out.println("Status Code: " + response.getStatusCode());
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
