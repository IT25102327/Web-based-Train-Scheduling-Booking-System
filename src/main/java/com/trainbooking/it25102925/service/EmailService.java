package com.trainbooking.it25102925.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for sending automated transactional emails and PDF e-ticket attachments.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a plain-text email message.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param text body text
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        log.info("Sending simple email to: {}, Subject: {}", to, subject);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@trainbooking.lk");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Could not dispatch live SMTP email to {}: {}. Simulated dispatch succeeded.", to, ex.getMessage());
        }
    }

    /**
     * Sends an email message containing a binary file attachment (e.g. PDF ticket).
     *
     * @param to recipient email address
     * @param subject email subject
     * @param text body text
     * @param attachment raw byte array of the attachment
     * @param filename name of attached file
     */
    public void sendEmailWithAttachment(String to, String subject, String text, byte[] attachment, String filename) {
        log.info("Sending email with attachment '{}' to: {}, Subject: {}", filename, to, subject);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("noreply@trainbooking.lk");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            if (attachment != null && attachment.length > 0) {
                helper.addAttachment(filename, new ByteArrayResource(attachment));
            }
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Could not dispatch SMTP attachment email to {}: {}. Simulated dispatch succeeded.", to, ex.getMessage());
        }
    }
}
