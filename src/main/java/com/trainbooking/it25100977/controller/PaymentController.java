package com.trainbooking.it25100977.controller;
import com.trainbooking.it25100977.service.*;
import com.trainbooking.it25100977.model.*;
import com.trainbooking.it25100977.dto.*;
import com.trainbooking.it25100977.repository.*;

import com.trainbooking.it25100977.dto.PaymentRequest;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25100977.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller handling payment submission, electronic ticket rendering, and PDF downloading.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Processes mock payment and redirects to issued e-ticket view.
     *
     * @param paymentRequest payment form data
     * @return redirect to ticket view
     */
    @PostMapping("/payment/process")
    public String processPayment(@ModelAttribute("paymentRequest") PaymentRequest paymentRequest) {
        log.info("Received payment process request for booking ID: {}", paymentRequest.getBookingId());
        Payment payment = paymentService.processPayment(paymentRequest.getBookingId(), paymentRequest);
        Ticket ticket = paymentService.generateTicket(paymentRequest.getBookingId());
        Long ticketId = (ticket != null) ? ticket.getId() : 1L;
        return "redirect:/payment/ticket/" + ticketId;
    }

    /**
     * Displays issued e-ticket details and QR code view.
     *
     * @param id ticket ID
     * @param model UI model
     * @return 'booking/eticket' template name
     */
    @GetMapping("/payment/ticket/{id}")
    public String showTicket(@PathVariable("id") Long id, Model model) {
        log.debug("Displaying ticket view for ID: {}", id);
        Ticket ticket = paymentService.getTicketById(id);
        model.addAttribute("ticket", ticket);
        return "it25100977/eticket";
    }

    /**
     * Downloads ticket as a generated PDF document.
     *
     * @param id ticket ID
     * @return {@link ResponseEntity} containing binary PDF stream
     */
    @GetMapping("/payment/ticket/{id}/download")
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable("id") Long id) {
        log.info("Downloading PDF for ticket ID: {}", id);
        byte[] pdfBytes = paymentService.generateTicketPdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
