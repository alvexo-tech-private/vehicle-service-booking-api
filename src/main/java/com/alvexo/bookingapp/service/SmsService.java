package com.alvexo.bookingapp.service;

import org.springframework.stereotype.Service;

/**
 * SMS Service interface for sending OTPs
 * Implement this with your preferred SMS provider (Twilio, AWS SNS, etc.)
 */
public interface SmsService {
    
    /**
     * Send SMS to mobile number
     * @param mobileNumber Recipient mobile number
     * @param message SMS message content
     */
    void sendSms(String mobileNumber, String message);
}

/**
 * Default implementation that logs SMS (for development/testing)
 * Replace with actual SMS provider implementation in production
 */
@Service
class SmsServiceImpl implements SmsService {
    
    @Override
    public void sendSms(String mobileNumber, String message) {
        // In development, just log it
        System.out.println("=================================");
        System.out.println("SMS to: " + mobileNumber);
        System.out.println("Message: " + message);
        System.out.println("=================================");
        
        // TODO: Implement actual SMS sending
        // For Twilio:
        // Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        // Message.creator(
        //     new PhoneNumber(mobileNumber),
        //     new PhoneNumber(YOUR_TWILIO_NUMBER),
        //     message
        // ).create();
        
        // For AWS SNS:
        // AmazonSNS snsClient = AmazonSNSClientBuilder.standard().build();
        // PublishRequest publishRequest = new PublishRequest()
        //     .withPhoneNumber(mobileNumber)
        //     .withMessage(message);
        // snsClient.publish(publishRequest);
    }
}