package com.trainbooking.features.booking;

import com.trainbooking.features.booking.dto.BookingRequest;
import com.trainbooking.features.booking.model.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Controller managing booking workflows, seat selection, and payment transitions.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Displays seat selection view for a chosen schedule and travel date.
     *
     * @param scheduleId schedule ID
     * @param date date of travel
     * @param model UI model
     * @return 'booking/seat-select' template name
     */
    @GetMapping("/booking/seats")
    public String showSeatSelection(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        log.debug("Showing seat selection for schedule ID: {}, date: {}", scheduleId, date);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("date", date);
        model.addAttribute("firstClassAvailable", bookingService.getAvailableSeats(scheduleId, date, "FIRST"));
        model.addAttribute("secondClassAvailable", bookingService.getAvailableSeats(scheduleId, date, "SECOND"));
        model.addAttribute("bookingRequest", new BookingRequest(scheduleId, date, "SECOND", 1));
        return "booking/seat-select";
    }

    /**
     * Handles new booking submission and redirects to checkout/payment page.
     *
     * @param bookingRequest booking form payload
     * @param model UI model
     * @return redirect to payment view
     */
    @PostMapping("/booking")
    public String createBooking(@ModelAttribute("bookingRequest") BookingRequest bookingRequest, Model model) {
        log.info("Processing booking submission for schedule ID: {}", bookingRequest.getScheduleId());
        // TODO: Obtain authenticated user id and invoke bookingService.createBooking
        Booking booking = bookingService.createBooking(bookingRequest, 1L);
        Long bookingId = booking != null ? booking.getId() : 1L;
        return "redirect:/booking/" + bookingId + "/payment";
    }

    /**
     * Displays payment view for a specific booking.
     *
     * @param id booking ID
     * @param model UI model
     * @return 'booking/payment' template name
     */
    @GetMapping("/booking/{id}/payment")
    public String showPaymentPage(@PathVariable("id") Long id, Model model) {
        log.debug("Displaying payment page for booking ID: {}", id);
        model.addAttribute("booking", bookingService.getBookingById(id));
        return "booking/payment";
    }
}
