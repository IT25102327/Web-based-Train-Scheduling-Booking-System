package com.trainbooking.features.dashboard;

import com.trainbooking.features.dashboard.dto.ValidationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
     *
     * @return 'dashboard/validate' template name
     */
    @GetMapping("/validate")
    public String showScannerPage() {
        log.debug("Opening ticket validation camera scanner interface");
        return "dashboard/validate";
    }

    /**
     * Verifies scanned ticket barcode/QR code and records passenger boarding.
     *
     * @param qrCode scanned QR code data
     * @return JSON {@link ValidationResultDto} validation status and passenger information
     */
    @PostMapping("/validate")
    @ResponseBody
    public ValidationResultDto validateTicket(@RequestParam("qrCode") String qrCode) {
        log.info("Processing ticket QR code verification request");
        return ticketValidationService.validateAndBoardTicket(qrCode);
    }
}
