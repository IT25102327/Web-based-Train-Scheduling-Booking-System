package com.trainbooking.it25100977.controller;

import com.trainbooking.it25100977.dto.PaymentRequest;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25100977.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String processPayment(
            @ModelAttribute("paymentRequest") PaymentRequest paymentRequest,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        log.info("Received payment process request for booking ID: {}", paymentRequest.getBookingId());
        try {
            paymentService.processPayment(paymentRequest.getBookingId(), paymentRequest);
            Ticket ticket = paymentService.generateTicket(paymentRequest.getBookingId());
            return "redirect:/payment/ticket/" + ticket.getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Payment failed for booking #{}: {}", paymentRequest.getBookingId(), ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/booking/" + paymentRequest.getBookingId() + "/payment?declined=true";
        }
    }

    /**
     * Displays issued e-ticket details and QR code view.
     * Supports both /payment/ticket/{id} and /booking/ticket/{id}.
     *
     * @param id ticket or booking ID
     * @param model UI model
     * @return 'it25100977/eticket' template name
     */
    @GetMapping({"/payment/ticket/{id}", "/booking/ticket/{id}"})
    public String showTicket(@PathVariable("id") Long id, Model model) {
        log.debug("Displaying ticket view for ID: {}", id);
        Ticket ticket;
        try {
            ticket = paymentService.getTicketById(id);
        } catch (Exception e) {
            ticket = paymentService.generateTicket(id);
        }
        model.addAttribute("ticket", ticket);
        return "it25100977/eticket";
    }

    /**
     * Serves live PNG barcode image for the ticket QR code.
     *
     * @param id ticket ID
     * @return PNG image bytes
     */
    @GetMapping(value = "/api/tickets/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] getTicketQrCode(@PathVariable("id") Long id) {
        return paymentService.generateQrCodeImage(id);
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

    /**
     * Downloads official tax invoice and payment receipt as PDF.
     *
     * @param id ticket ID
     * @return PDF stream
     */
    @GetMapping({"/payment/receipt/{id}/download", "/payment/invoice/{id}/download"})
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable("id") Long id) {
        log.info("Downloading Tax Invoice PDF for ticket ID: {}", id);
        byte[] pdfBytes = paymentService.generateInvoicePdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
