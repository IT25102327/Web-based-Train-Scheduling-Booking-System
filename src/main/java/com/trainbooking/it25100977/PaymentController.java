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
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller handling payment submission, electronic ticket rendering, and PDF downloading.
 *
 * @author SLIIT Software Engineering Team (IT25100977)
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Processes mock card payment or manual deposit slip submission and redirects to issued e-ticket view.
     *
     * @param paymentRequest payment form data
     * @param slipFile uploaded bank deposit slip or receipt file (for manual method)
     * @param redirectAttributes flash attributes
     * @return redirect to ticket view
     */
    @PostMapping("/payment/process")
    public String processPayment(
            @ModelAttribute("paymentRequest") PaymentRequest paymentRequest,
            @RequestParam(value = "slipFile", required = false) MultipartFile slipFile,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        log.info("Received payment process request for booking ID: {}, method: {}", paymentRequest.getBookingId(), paymentRequest.getPaymentMethod());
        try {
            paymentService.processPayment(paymentRequest.getBookingId(), paymentRequest, slipFile);
            Ticket ticket = paymentService.generateTicket(paymentRequest.getBookingId());
            return "redirect:/payment/ticket/" + ticket.getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Payment failed for booking #{}: {}", paymentRequest.getBookingId(), ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            String methodParam = (paymentRequest.getPaymentMethod() != null) ? paymentRequest.getPaymentMethod() : "CARD";
            return "redirect:/booking/" + paymentRequest.getBookingId() + "/payment?method=" + methodParam + "&declined=true";
        }
    }

    /**
     * Serves uploaded payment slip file for viewing in browser.
     *
     * @param id payment ID
     * @return file stream response
     */
    @GetMapping("/payment/slip/{id}")
    public ResponseEntity<byte[]> viewPaymentSlip(@PathVariable("id") Long id) {
        log.info("Viewing payment slip for payment ID: {}", id);
        Payment payment = paymentService.getPaymentById(id);
        if (payment.getSlipFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] fileBytes = paymentService.getPaymentSlipBytes(id);
        String contentType = paymentService.getPaymentSlipContentType(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (payment.getSlipFileName() != null ? payment.getSlipFileName() : "slip") + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileBytes);
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
