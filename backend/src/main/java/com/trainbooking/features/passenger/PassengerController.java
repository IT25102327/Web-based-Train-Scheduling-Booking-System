package com.trainbooking.features.passenger;

import com.trainbooking.features.passenger.dto.FavoriteRouteDto;
import com.trainbooking.features.passenger.dto.UserProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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

    /**
     * Displays passenger profile page.
     *
     * @param model UI model
     * @return 'passenger/profile' template name
     */
    @GetMapping("/profile")
    public String showProfile(Model model) {
        log.debug("Displaying passenger profile page");
        // TODO: Retrieve authenticated user details and add to model
        model.addAttribute("userProfile", new UserProfileDto());
        return "passenger/profile";
    }

    /**
     * Handles passenger profile update form submission.
     *
     * @param profileDto updated profile details
     * @return redirect to profile page
     */
    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("userProfile") UserProfileDto profileDto) {
        log.info("Updating profile submission for email: {}", profileDto.getEmail());
        // TODO: Get authenticated user id and invoke passengerService.updateProfile(userId, profileDto)
        return "redirect:/profile?success=true";
    }

    /**
     * Displays saved favorite routes for the passenger.
     *
     * @param model UI model
     * @return 'passenger/favorites' template name
     */
    @GetMapping("/favorites")
    public String showFavorites(Model model) {
        log.debug("Displaying passenger favorite routes page");
        // TODO: Load user's favorite routes and add to model
        model.addAttribute("favorites", passengerService.getFavoriteRoutes(1L));
        model.addAttribute("newFavorite", new FavoriteRouteDto());
        return "passenger/favorites";
    }

    /**
     * Adds a new route to passenger favorites.
     *
     * @param favoriteRouteDto favorite route details
     * @return redirect to favorites page
     */
    @PostMapping("/favorites")
    public String addFavorite(@ModelAttribute("newFavorite") FavoriteRouteDto favoriteRouteDto) {
        log.info("Adding favorite route: {} -> {}", favoriteRouteDto.getOrigin(), favoriteRouteDto.getDestination());
        // TODO: Invoke passengerService.addFavoriteRoute(userId, favoriteRouteDto)
        passengerService.addFavoriteRoute(1L, favoriteRouteDto);
        return "redirect:/favorites";
    }

    /**
     * Removes a route from passenger favorites via DELETE HTTP method.
     *
     * @param id the favorite route ID
     * @return redirect to favorites page
     */
    @DeleteMapping("/favorites/{id}")
    public String deleteFavorite(@PathVariable("id") Long id) {
        log.info("Deleting favorite route ID: {}", id);
        // TODO: Obtain authenticated user id and invoke passengerService.removeFavoriteRoute(id, userId)
        passengerService.removeFavoriteRoute(id, 1L);
        return "redirect:/favorites";
    }

    /**
     * Alternate POST endpoint for removing favorite route from HTML form submissions.
     *
     * @param id the favorite route ID
     * @return redirect to favorites page
     */
    @PostMapping("/favorites/{id}/delete")
    public String deleteFavoritePost(@PathVariable("id") Long id) {
        return deleteFavorite(id);
    }

    /**
     * Displays passenger booking history.
     *
     * @param model UI model
     * @return 'passenger/booking-history' template name
     */
    @GetMapping("/booking-history")
    public String showBookingHistory(Model model) {
        log.debug("Displaying passenger booking history page");
        // TODO: Fetch bookings for current authenticated user and populate model
        return "passenger/booking-history";
    }
}
