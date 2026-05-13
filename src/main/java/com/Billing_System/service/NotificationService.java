package com.Billing_System.service;

import com.Billing_System.vendor.entity.Vendor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending external notifications (Email/SMS).
 * Runs asynchronously to prevent blocking the main thread.
 */
@Service
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    
    // Configured sender address (update in production)
    private final String fromEmail = "billing.system.noreply@gmail.com";

    @Autowired
    public NotificationService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send an email asynchronously.
     */
    @Async
    public void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            log.warn("Cannot send email to {}. Mail configuration is missing or commented out in application.properties.", to);
            log.info("MOCK EMAIL CONTENT:\nSubject: {}\n{}", subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ── KYC Onboarding Emails ───────────────────────────────────────────────

    public void sendVendorActivatedEmail(Vendor vendor) {
        String subject = "Your Vendor Account is Now ACTIVE!";
        String body = String.format("""
                Dear %s,
                
                Congratulations! Your KYC process is complete and your vendor profile has been APPROVED.
                You are now an active vendor in our system. You can log in to your dashboard to view new Purchase Orders.
                
                Vendor Code: %s
                
                Thank you,
                Procurement Team
                """, vendor.getLegalName(), vendor.getVendorCode());
        
        sendEmail(vendor.getPrimaryEmail(), subject, body);
    }

    public void sendVendorRejectedEmail(Vendor vendor, String reason) {
        String subject = "Vendor KYC Rejected - Action Required";
        String body = String.format("""
                Dear %s,
                
                We regret to inform you that your recent KYC submission has been REJECTED.
                
                Reason: %s
                
                Please log in to your dashboard to resolve these issues and re-submit your documents.
                
                Thank you,
                Procurement Team
                """, vendor.getLegalName(), reason != null ? reason : "Please contact support for details.");
        
        sendEmail(vendor.getPrimaryEmail(), subject, body);
    }

    // ── Compliance Emails ───────────────────────────────────────────────────

    public void sendComplianceWarningEmail(Vendor vendor) {
        String subject = "URGENT: Compliance Documents Expiring Soon";
        String body = String.format("""
                Dear %s,
                
                This is a friendly reminder that one or more of your compliance documents will expire in less than 30 days.
                
                To avoid being blocked from receiving new Purchase Orders, please log in to your dashboard and upload the renewed documents as soon as possible.
                
                Thank you,
                Procurement Team
                """, vendor.getLegalName());
        
        sendEmail(vendor.getPrimaryEmail(), subject, body);
    }

    public void sendComplianceBlockedEmail(Vendor vendor) {
        String subject = "ACCOUNT BLOCKED: Expired Compliance Documents";
        String body = String.format("""
                Dear %s,
                
                Your vendor account has been temporarily BLOCKED because your compliance documents have expired.
                
                You will not receive any new Purchase Orders until this is resolved. 
                Please log in immediately and upload your updated compliance documents for review.
                
                Thank you,
                Procurement Team
                """, vendor.getLegalName());
        
        sendEmail(vendor.getPrimaryEmail(), subject, body);
    }
}
