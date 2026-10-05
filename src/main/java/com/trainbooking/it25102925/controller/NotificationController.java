package com.trainbooking.it25102925.controller;

import com.trainbooking.it25102327.service.TrainService;
import com.trainbooking.it25102925.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller handling user notification list views and train status broadcast updates.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final TrainService trainService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.trainbooking.it25101520.repository.UserRepository userRepository;

    /**
     * Displays all notifications for the authenticated user.
     *
     * @param authentication current user authentication
     * @param model UI model
     * @return 'it25102925/list' template name
     */
    @GetMapping("/notifications")
    public String showNotifications(
            Authentication authentication,
            Model model
    ) {
        log.debug("Displaying notifications list");
        Long userId = resolveUserId(authentication);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(userId));
        return "it25102925/list";
    }

    /**
     * Marks all notifications for the user as read.
     *
     * @param authentication current user authentication
     * @return redirect to notifications list
     */
    @PostMapping("/notifications/mark-all-read")
    public String markAllAsRead(Authentication authentication) {
        log.info("Processing mark-all-read request");
        Long userId = resolveUserId(authentication);
        notificationService.markAllAsRead(userId);
        return "redirect:/notifications";
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && userRepository != null) {
            return userRepository.findByEmail(authentication.getName())
                    .map(com.trainbooking.it25101520.model.User::getId)
                    .orElse(1L);
        }
        return 1L;
    }

    /**
     * Displays status update management view for trains.
     *
     * @param model UI model
     * @return 'it25102925/status' template name
     */
    @GetMapping({"/admin/trains/status", "/notifications/status"})
    public String showStatusUpdateForm(Model model) {
        log.debug("Showing status update dispatch form");
        model.addAttribute("trains", trainService.getAllTrains());
        return "it25102925/status";
    }

    /**
     * Updates train status and triggers automated notification broadcasts to all booked passengers.
     *
     * @param trainId train ID
     * @param status new operational status
     * @param reason reason for update
     * @return redirect back to status update page with confirmation
     */
    @PostMapping("/admin/trains/status")
    public String updateTrainStatusAndNotify(
            @RequestParam("trainId") Long trainId,
            @RequestParam("status") String status,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        log.info("Processing train ID {} status update to: {} with reason: {}", trainId, status, reason);
        trainService.updateTrainStatus(trainId, status);
        notificationService.notifyAffectedPassengers(trainId, status);
        return "redirect:/admin/trains/status?updated=true";
    }

    /**
     * Public station departure board showing live train operational statuses, routes, and delay tags.
     *
     * @param model UI model
     * @return 'it25102925/departure-board' template name
     */
    @GetMapping("/departure-board")
    public String showDepartureBoard(Model model) {
        log.debug("Displaying public station departure board");
        model.addAttribute("trains", trainService.getAllTrains());
        return "it25102925/departure-board";
    }

    /**
     * Displays real-time interactive OpenStreetMap showing live train positions.
     * Regular users see the public layout without the admin sidebar,
     * while admins and staff see the admin console layout with the sidebar.
     *
     * @param view optional parameter to explicitly select 'admin' or 'passenger' view
     * @param authentication current user authentication
     * @param model UI model
     * @return view template name
     */
    @GetMapping({"/notifications/map", "/notifications/live-map", "/trains/live-map"})
    public String showLiveMap(
            @RequestParam(value = "view", required = false) String view,
            Authentication authentication,
            Model model
    ) {
        log.debug("Displaying real-time live train tracking map");
        model.addAttribute("pageTitle", "Live Train Tracking Map");
        model.addAttribute("trains", trainService.getAllTrains());

        if ("passenger".equalsIgnoreCase(view) || "user".equalsIgnoreCase(view)) {
            return "it25102925/live-map";
        }

        boolean isAdminOrStaff = false;
        if (authentication != null && authentication.isAuthenticated()) {
            isAdminOrStaff = authentication.getAuthorities().stream().anyMatch(a -> {
                String auth = a.getAuthority();
                return "ROLE_ADMIN".equals(auth) ||
                       "ROLE_STATION_STAFF".equals(auth) ||
                       "ROLE_COORDINATOR".equals(auth);
            });
        }

        if (isAdminOrStaff || "admin".equalsIgnoreCase(view)) {
            return "it25102925/admin-live-map";
        }
        return "it25102925/live-map";
    }

    /**
     * Dedicated admin endpoint for the live train tracking map in the Admin Console.
     */
    @GetMapping({"/admin/live-map", "/admin/trains/map"})
    public String showAdminLiveMap(Model model) {
        log.debug("Displaying admin live train tracking map");
        model.addAttribute("pageTitle", "Live Train Tracking Map");
        model.addAttribute("trains", trainService.getAllTrains());
        return "it25102925/admin-live-map";
    }
}
