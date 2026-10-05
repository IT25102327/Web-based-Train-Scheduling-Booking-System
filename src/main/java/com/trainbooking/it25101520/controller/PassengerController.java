package com.trainbooking.it25101520.controller;

import com.trainbooking.it25101520.dto.FavoriteRouteDto;
import com.trainbooking.it25101520.dto.UserProfileDto;
import com.trainbooking.it25101520.model.FavoriteRoute;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25101520.service.PassengerService;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller managing passenger profile, favorites, and booking history views.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;
    private final BookingRepository bookingRepository;
    private final com.trainbooking.it25103308.service.BookingService bookingService;
    private final com.trainbooking.it25100977.repository.RefundRepository refundRepository;

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            try {
                return passengerService.getUserByEmail("passenger@trainbooking.lk");
            } catch (Exception e) {
                return null;
            }
        }
        try {
            return passengerService.getUserByEmail(principal.getName());
        } catch (Exception e) {
            log.warn("User with email {} not found in database", principal.getName());
            return null;
        }
    }

    /**
     * Displays passenger profile page.
     *
     * @param principal authenticated user
     * @param model UI model
     * @return 'it25101520/profile' template name
     */
    @GetMapping({"/profile", "/passenger/profile"})
    public String showProfile(Principal principal, Model model) {
        log.debug("Displaying passenger profile page");
        User currentUser = getAuthenticatedUser(principal);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Map<String, Object> userView = new HashMap<>();
        userView.put("id", currentUser.getId());
        userView.put("firstName", currentUser.getFirstName());
        userView.put("lastName", currentUser.getLastName());
        userView.put("fullName", currentUser.getFirstName() + " " + currentUser.getLastName());
        userView.put("email", currentUser.getEmail());
        userView.put("phone", currentUser.getPhone());
        userView.put("phoneNumber", currentUser.getPhone());
        userView.put("nic", "199623849102");
        userView.put("notifyByEmail", currentUser.getNotifyByEmail() != null ? currentUser.getNotifyByEmail() : true);
        userView.put("notifyBySms", currentUser.getNotifyBySms() != null ? currentUser.getNotifyBySms() : true);
        userView.put("delayAlertThresholdMinutes", currentUser.getDelayAlertThresholdMinutes() != null ? currentUser.getDelayAlertThresholdMinutes() : 15);
        userView.put("memberSince", currentUser.getCreatedAt() != null ?
                currentUser.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy")) : "August 2026");

        List<Booking> userBookings = bookingRepository.findByPassengerId(currentUser.getId());
        userView.put("totalBookings", userBookings.size());

        model.addAttribute("user", userView);
        model.addAttribute("favoriteRoutes", passengerService.getFavoriteRoutes(currentUser.getId()));
        return "it25101520/profile";
    }

    /**
     * Handles passenger profile update form submission.
     *
     * @param principal authenticated user
     * @param profileDto updated profile details
     * @return redirect to profile page
     */
    @PostMapping({"/profile", "/passenger/profile"})
    public String updateProfile(Principal principal, @ModelAttribute("userProfile") UserProfileDto profileDto) {
        log.info("Updating profile submission for email: {}", profileDto.getEmail());
        User currentUser = getAuthenticatedUser(principal);
        if (currentUser != null) {
            passengerService.updateProfile(currentUser.getId(), profileDto);
        }
        return "redirect:/profile?success=true";
    }

    /**
     * Displays saved favorite routes for the passenger.
     *
     * @param principal authenticated user
     * @param model UI model
     * @return 'it25101520/profile' template name
     */
    @GetMapping({"/favorites", "/passenger/favorites"})
    public String showFavorites(Principal principal, Model model) {
        return showProfile(principal, model);
    }

    /**
     * Adds a new route to passenger favorites.
     *
     * @param principal authenticated user
     * @param favoriteRouteDto favorite route details
     * @return redirect to profile page
     */
    @PostMapping({"/favorites", "/passenger/favorites/add"})
    public String addFavorite(Principal principal, @ModelAttribute("newFavorite") FavoriteRouteDto favoriteRouteDto) {
        log.info("Adding favorite route: {} -> {}", favoriteRouteDto.getOrigin(), favoriteRouteDto.getDestination());
        User currentUser = getAuthenticatedUser(principal);
        if (currentUser != null) {
            passengerService.addFavoriteRoute(currentUser.getId(), favoriteRouteDto);
        }
        return "redirect:/profile";
    }

    /**
     * Removes a route from passenger favorites.
     *
     * @param principal authenticated user
     * @param id the favorite route ID
     * @return redirect to profile page
     */
    @PostMapping({"/favorites/{id}/delete", "/passenger/favorites/{id}/delete"})
    public String deleteFavorite(Principal principal, @PathVariable("id") Long id) {
        log.info("Deleting favorite route ID: {}", id);
        User currentUser = getAuthenticatedUser(principal);
        if (currentUser != null) {
            passengerService.removeFavoriteRoute(id, currentUser.getId());
        }
        return "redirect:/profile";
    }

    /**
     * Displays passenger booking history.
     *
     * @param principal authenticated user
     * @param model UI model
     * @return 'it25101520/booking-history' template name
     */
    @GetMapping({"/booking-history", "/passenger/bookings"})
    public String showBookingHistory(
            @RequestParam(value = "filter", required = false) String filter,
            Principal principal,
            Model model) {
        log.debug("Displaying passenger booking history page with filter: {}", filter);
        User currentUser = getAuthenticatedUser(principal);
        if (currentUser == null) {
            return "redirect:/login";
        }
        List<Booking> bookings = bookingRepository.findByPassengerId(currentUser.getId());
        if (filter != null && !filter.isBlank()) {
            String f = filter.trim().toUpperCase();
            java.time.LocalDate today = java.time.LocalDate.now();
            if ("UPCOMING".equals(f)) {
                bookings = bookings.stream()
                        .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED
                                && (b.getTravelDate() == null || !b.getTravelDate().isBefore(today)))
                        .toList();
            } else if ("COMPLETED".equals(f)) {
                bookings = bookings.stream()
                        .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED
                                && b.getTravelDate() != null && b.getTravelDate().isBefore(today))
                        .toList();
            } else if ("CANCELLED".equals(f)) {
                bookings = bookings.stream()
                        .filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED)
                        .toList();
            }
        }

        Map<Long, com.trainbooking.it25100977.model.Refund> refundMap = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getStatus() == Booking.BookingStatus.CANCELLED) {
                refundRepository.findByBookingId(b.getId()).ifPresent(r -> refundMap.put(b.getId(), r));
            }
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("refundMap", refundMap);
        return "it25101520/booking-history";
    }

    /**
     * Displays booking cancellation review and refund estimate preview page.
     *
     * @param id booking ID
     * @param principal authenticated user
     * @param model UI model
     * @param redirectAttributes flash attributes for error messages
     * @return cancellation confirmation view
     */
    @GetMapping("/passenger/bookings/{id}/cancel")
    public String showCancellationConfirmation(
            @PathVariable("id") Long id,
            Principal principal,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        log.info("Displaying cancellation review for booking #{}", id);
        User currentUser = getAuthenticatedUser(principal);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            com.trainbooking.it25103308.dto.CancellationSummaryDto summary = bookingService.getCancellationSummary(id, currentUser.getId());
            model.addAttribute("summary", summary);
            model.addAttribute("pageTitle", "Confirm Cancellation & Refund");
            return "it25101520/cancel-booking";
        } catch (Exception e) {
            log.warn("Cannot show cancellation quote for booking #{}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/passenger/bookings";
        }
    }

    /**
     * Executes booking cancellation, processes refund, releases seats, and redirects with flash notice.
     *
     * @param id booking ID
     * @param reason cancellation reason
     * @param principal authenticated user
     * @param redirectAttributes flash feedback
     * @return redirect to booking history
     */
    @PostMapping("/passenger/bookings/{id}/cancel")
    public String executeCancellation(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            Principal principal,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        log.info("Executing cancellation submission for booking #{}, reason: {}", id, reason);
        User currentUser = getAuthenticatedUser(principal);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            com.trainbooking.it25100977.model.Refund refund = bookingService.cancelBooking(id, currentUser.getId(), reason);
            if (refund != null) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Booking #" + id + " has been cancelled. A refund of LKR " +
                        String.format("%,.2f", refund.getRefundAmount()) + " (" + refund.getRefundPercentage() + "%) " +
                        "was issued to your original payment method (Ref: " + refund.getRefundTransactionRef() + ").");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " has been cancelled and seats released.");
            }
        } catch (Exception e) {
            log.error("Failed to cancel booking #{}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cancellation failed: " + e.getMessage());
        }
        return "redirect:/passenger/bookings";
    }
}
