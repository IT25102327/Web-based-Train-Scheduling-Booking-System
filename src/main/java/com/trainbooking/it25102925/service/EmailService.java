package com.trainbooking.it25102925.service;
import com.trainbooking.it25102925.model.*;
import com.trainbooking.it25102925.dto.*;
import com.trainbooking.it25102925.repository.*;

import jakarta.mail.MessagingException;
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
        // TODO: Prepare SimpleMailMessage and dispatch using mailSender.send(message)
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
        // TODO: Construct MimeMessage with MimeMessageHelper, attach ByteArrayResource, and send via mailSender
    }
}
