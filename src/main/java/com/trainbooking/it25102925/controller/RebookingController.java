package com.trainbooking.it25102925.controller;

import com.trainbooking.it25102327.model.Schedule;
import com.trainbooking.it25102327.repository.ScheduleRepository;
import com.trainbooking.it25102925.model.RebookingToken;
import com.trainbooking.it25102925.repository.RebookingTokenRepository;
import com.trainbooking.it25102925.service.NotificationService;
import com.trainbooking.it25103308.model.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller handling One-Click Complimentary Rebooking for passengers affected by train cancellations.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/rebooking")
public class RebookingController {

    private final RebookingTokenRepository rebookingTokenRepository;
    private final NotificationService notificationService;
    private final ScheduleRepository scheduleRepository;

    /**
     * Displays available alternative trains for one-click compensation transfer.
     *
     * @param token secure rebooking token
     * @param model UI model
     * @return 'it25102925/rebook' template name
     */
    @GetMapping("/claim/{token}")
    public String showRebookingPortal(@PathVariable("token") String token, Model model) {
        log.info("Accessing rebooking portal with token: {}", token);

        RebookingToken rebookToken = rebookingTokenRepository.findByTokenAndIsRedeemedFalse(token).orElse(null);
        if (rebookToken == null || rebookToken.isExpired()) {
            model.addAttribute("errorMessage", "This rebooking link is invalid, expired, or has already been redeemed.");
            return "it25102925/rebook";
        }

        Booking originalBooking = rebookToken.getBooking();
        String origin = (originalBooking.getSchedule() != null && originalBooking.getSchedule().getRoute() != null)
                ? originalBooking.getSchedule().getRoute().getOrigin() : "Colombo Fort";
        String destination = (originalBooking.getSchedule() != null && originalBooking.getSchedule().getRoute() != null)
                ? originalBooking.getSchedule().getRoute().getDestination() : "Kandy";

        List<Schedule> alternatives = scheduleRepository.findMatchingSchedulesAnyDay(origin, destination).stream()
                .filter(s -> !s.getId().equals(originalBooking.getSchedule().getId()))
                .toList();

        model.addAttribute("token", token);
        model.addAttribute("booking", originalBooking);
        model.addAttribute("alternatives", alternatives);
        return "it25102925/rebook";
    }

    /**
     * Confirms one-click rebooking transfer to the chosen replacement schedule for LKR 0.
     *
     * @param token secure rebooking token
     * @param newScheduleId selected replacement schedule
     * @return redirect to updated confirmed ticket view
     */
    @PostMapping("/claim/{token}")
    public String confirmRebooking(
            @PathVariable("token") String token,
            @RequestParam("newScheduleId") Long newScheduleId
    ) {
        log.info("Processing rebooking transfer: token={}, newScheduleId={}", token, newScheduleId);
        Booking updatedBooking = notificationService.claimRebooking(token, newScheduleId);
        return "redirect:/booking/ticket/" + updatedBooking.getId() + "?rebooked=true";
    }
}
