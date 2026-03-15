package com.softwarearchi.archi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "notification.user-registered")
    public void processUserRegistered(Map<String, Object> event) {
        logger.info("[NOTIFICATION] Received UserRegistered event: {}", event.get("eventId"));

        try {
            String email = (String) event.get("email");
            String tokenId = (String) event.get("tokenId");
            String tokenClear = (String) event.get("tokenClear");

            // Generate Verification Link pointing to Nginx (port 80)
            String verifyLink = String.format("http://localhost/api/auth/verify?tokenId=%s&t=%s", tokenId,
                    tokenClear);

            // Send Email via MailHog
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@softwarearchi.com");
            message.setTo(email);
            message.setSubject("Please verify your email address");
            message.setText("Welcome! Please click the link below to verify your email address:\n\n" + verifyLink);

            mailSender.send(message);
            logger.info("[NOTIFICATION] Verification email sent successfully to: {}", email);

        } catch (Exception e) {
            logger.error("[NOTIFICATION] Failed to process event and send email: {}", e.getMessage(), e);
            throw new RuntimeException("Email sending failed, message will be re-queued or sent to DLQ", e);
        }
    }
}
