package com.internship.tool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around JavaMailSender.
 *
 * Bug fixes:
 * - Catches MailException specifically (not bare Exception) so non-mail
 *   errors propagate normally.
 * - Validates recipient address before attempting to send.
 * - Logs the full exception class, not just the message, for easier diagnosis.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send a plain-text email.
     *
     * @param to      recipient address — must not be null or blank
     * @param subject email subject
     * @param body    plain-text body
     */
    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            logger.warn("sendEmail called with null/blank recipient — skipping");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Email sent to {}", to);
        } catch (MailException e) {
            // Log but do not rethrow — email failures must not crash the caller
            // (scheduler, service layer). The obligation is still processed.
            logger.error("Failed to send email to {} [{}]: {}", to, e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }
}
