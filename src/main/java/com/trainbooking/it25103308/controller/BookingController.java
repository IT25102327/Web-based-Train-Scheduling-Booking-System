package com.trainbooking.it25103308.controller;

import com.trainbooking.exception.ResourceNotFoundException;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25101520.service.PassengerService;
import com.trainbooking.it25102327.model.Schedule;
import com.trainbooking.it25102327.repository.ScheduleRepository;
import com.trainbooking.it25103308.dto.BookingRequest;
import com.trainbooking.it25103308.dto.CoachDto;
import com.trainbooking.it25103308.dto.SeatStatusResponseDto;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ScheduleRepository scheduleRepository;
    private final PassengerService passengerService;

    /**
     * Displays seat selection view for a chosen schedule and travel date.
     *
     * @param scheduleId schedule ID
     * @param date date of travel
     * @param model UI model
     * @return 'it25103308/seat-select' template name
     */
    @GetMapping("/booking/seats")
    public String showSeatSelection(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        log.debug("Showing seat selection for schedule ID: {}, date: {}", scheduleId, date);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        int availFirst = bookingService.getAvailableSeats(scheduleId, date, "FIRST");
        int availSecond = bookingService.getAvailableSeats(scheduleId, date, "SECOND");

        Map<String, Object> schedView = new HashMap<>();
        schedView.put("id", schedule.getId());
        schedView.put("trainName", schedule.getTrain() != null ? schedule.getTrain().getTrainName() : "Express Train");
        schedView.put("trainType", "Express Intercity");
        schedView.put("origin", schedule.getRoute() != null ? schedule.getRoute().getOrigin() : "Colombo Fort");
        schedView.put("destination", schedule.getRoute() != null ? schedule.getRoute().getDestination() : "Kandy");
        schedView.put("departureTime", schedule.getDepartureTime());
        schedView.put("arrivalTime", schedule.getArrivalTime());
        schedView.put("firstClassFare", schedule.getFirstClassFare());
        schedView.put("secondClassFare", schedule.getSecondClassFare());
        schedView.put("availableFirstClassSeats", availFirst);
        schedView.put("availableSecondClassSeats", availSecond);

        List<CoachDto> coaches = bookingService.generateTrainCoaches(schedule, date);
        List<CoachDto> firstClassCoaches = coaches.stream()
                .filter(c -> "FIRST".equalsIgnoreCase(c.getCoachClass()))
                .toList();
        List<CoachDto> secondClassCoaches = coaches.stream()
                .filter(c -> "SECOND".equalsIgnoreCase(c.getCoachClass()))
                .toList();
        List<String> allBookedSeats = bookingService.getBookedSeatNumbers(scheduleId, date, "ALL");

        model.addAttribute("schedule", schedView);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("date", date);
        model.addAttribute("coaches", coaches);
        model.addAttribute("firstClassCoaches", firstClassCoaches);
        model.addAttribute("secondClassCoaches", secondClassCoaches);
        model.addAttribute("allBookedSeats", allBookedSeats);
        model.addAttribute("firstClassAvailable", availFirst);
        model.addAttribute("secondClassAvailable", availSecond);
        model.addAttribute("bookedFirstSeats", bookingService.getBookedSeatNumbers(scheduleId, date, "FIRST"));
        model.addAttribute("bookedSecondSeats", bookingService.getBookedSeatNumbers(scheduleId, date, "SECOND"));
        model.addAttribute("bookingRequest", new BookingRequest(scheduleId, date, "SECOND", 1));

        return "it25103308/seat-select";
    }

    /**
     * REST API endpoint returning real-time seat availability, coach occupancy, and booked seats.
     * Invoked periodically by the client for live interactive layout synchronization.
     *
     * @param scheduleId schedule ID
     * @param date travel date
     * @return {@link ResponseEntity} containing {@link SeatStatusResponseDto}
     */
    @GetMapping("/api/booking/seats/status")
    @ResponseBody
    public ResponseEntity<SeatStatusResponseDto> getRealTimeSeatStatus(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.debug("Polling live seat status for schedule ID: {}, date: {}", scheduleId, date);
        SeatStatusResponseDto status = bookingService.getSeatStatus(scheduleId, date);
        return ResponseEntity.ok(status);
    }

    /**
     * Handles new booking submission and redirects to checkout/payment page.
     *
     * @param principal authenticated user
     * @param bookingRequest booking form payload
     * @param redirectAttributes flash attributes for error messages
     * @return redirect to payment view
     */
    @PostMapping("/booking")
    public String createBooking(
            Principal principal,
            @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Processing booking submission for schedule ID: {}", bookingRequest.getScheduleId());
        Long userId = 1L;
        if (principal != null) {
            try {
                User user = passengerService.getUserByEmail(principal.getName());
                userId = user.getId();
            } catch (Exception ignored) {
            }
        }
        try {
            Booking booking = bookingService.createBooking(bookingRequest, userId);
            return "redirect:/booking/" + booking.getId() + "/payment";
        } catch (Exception ex) {
            log.warn("Failed to create booking: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/booking/seats?scheduleId=" + bookingRequest.getScheduleId() + "&date=" + bookingRequest.getTravelDate();
        }
    }

    /**
     * Displays payment view for a specific booking.
     *
     * @param id booking ID
     * @param model UI model
     * @return 'it25100977/payment' template name
     */
    @GetMapping({"/booking/{id}/payment", "/payment/checkout/{id}"})
    public String showPaymentPage(@PathVariable("id") Long id, Model model) {
        log.debug("Displaying payment page for booking ID: {}", id);
        Booking booking = bookingService.getBookingById(id);
        model.addAttribute("booking", booking);
        return "it25100977/payment";
    }

    /**
     * Handles explicit seat lock cancellation or redirection to refund flow for confirmed bookings.
     *
     * @param id booking ID
     * @param bookingId optional query param ID
     * @return redirect
     */
    @PostMapping({"/booking/{id}/cancel", "/booking/release"})
    public String cancelBooking(@PathVariable(value = "id", required = false) Long id,
                                @RequestParam(value = "bookingId", required = false) Long bookingId) {
        Long targetId = (id != null) ? id : bookingId;
        if (targetId != null) {
            log.info("Passenger requested explicit cancellation for booking #{}", targetId);
            Booking booking = bookingService.getBookingById(targetId);
            if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
                return "redirect:/passenger/bookings/" + targetId + "/cancel";
            } else {
                bookingService.releaseLock(targetId);
            }
        }
        return "redirect:/trains/search?cancelled=true";
    }

    /**
     * REST API endpoint to retrieve cancellation and refund summary quote.
     *
     * @param id booking ID
     * @return JSON response with refund breakdown
     */
    @GetMapping("/api/bookings/{id}/cancellation-summary")
    @ResponseBody
    public org.springframework.http.ResponseEntity<com.trainbooking.it25103308.dto.CancellationSummaryDto> getCancellationSummaryApi(@PathVariable("id") Long id) {
        com.trainbooking.it25103308.dto.CancellationSummaryDto summary = bookingService.getCancellationSummary(id, null);
        return org.springframework.http.ResponseEntity.ok(summary);
    }
}
