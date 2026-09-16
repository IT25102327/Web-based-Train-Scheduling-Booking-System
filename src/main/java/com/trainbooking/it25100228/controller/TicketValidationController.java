package com.trainbooking.it25100228.controller;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100228.service.TicketValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Controller handling station staff ticket QR validation UI and scanning verification endpoint.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TicketValidationController {

    private final TicketValidationService ticketValidationService;

    /**
     * Renders ticket scanner camera page for station gate staff.
     * Accessible via both /validate and /dashboard/validate.
     *
     * @param model UI model
     * @return 'it25100228/validate' template name
     */
    @GetMapping({"/validate", "/dashboard/validate"})
    public String showScannerPage(Model model) {
        log.debug("Opening ticket validation camera scanner interface");
        model.addAttribute("pageTitle", "Station Ticket Inspection & QR Scanner");
        model.addAttribute("recentTickets", ticketValidationService.getRecentTickets());
        return "it25100228/validate";
    }

    /**
     * Verifies scanned ticket barcode/QR code and records passenger boarding via form parameter.
     *
     * @param qrCode scanned QR code data
     * @return JSON {@link ValidationResultDto} validation status and passenger information
     */
    @PostMapping("/validate")
    @ResponseBody
    public ResponseEntity<ValidationResultDto> validateTicket(@RequestParam(value = "qrCode", required = false) String qrCode) {
        log.info("Processing ticket QR code verification request: {}", qrCode);
        ValidationResultDto result = ticketValidationService.validateAndBoardTicket(qrCode);
        return ResponseEntity.ok(result);
    }

    /**
     * REST API endpoint for jsQR scanner script sending JSON payload { "ticketNumber": "..." }.
     *
     * @param payload JSON request map containing ticketNumber or qrCode
     * @return ResponseEntity with validation result
     */
    @PostMapping("/api/tickets/validate")
    @ResponseBody
    public ResponseEntity<ValidationResultDto> validateTicketApi(@RequestBody(required = false) Map<String, String> payload) {
        String code = null;
        if (payload != null) {
            code = payload.get("ticketNumber");
            if (code == null || code.trim().isEmpty()) {
                code = payload.get("qrCode");
            }
        }
        log.info("Processing API ticket validation request for code: {}", code);
        ValidationResultDto result = ticketValidationService.validateAndBoardTicket(code);
        return ResponseEntity.ok(result);
    }

    /**
     * Resets a ticket's boarded state back to unboarded for repeated test-scanning.
     *
     * @param id ticket ID
     * @param redirectAttributes flash attributes
     * @return redirect to /dashboard/validate
     */
    @PostMapping({"/validate/{id}/reset", "/dashboard/validate/{id}/reset"})
    public String resetTicketStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.info("Resetting boarding status for ticket ID {}", id);
        boolean success = ticketValidationService.resetBoardedStatus(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Ticket status reset successfully! You can scan or validate it again.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Ticket not found or could not be reset.");
        }
        return "redirect:/dashboard/validate";
    }
}
