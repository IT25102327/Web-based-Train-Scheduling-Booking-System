package com.trainbooking.features.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /**
     * Displays all notifications for the authenticated user.
     *
     * @param model UI model
     * @return 'notifications/list' template name
     */
    @GetMapping("/notifications")
    public String showNotifications(Model model) {
        log.debug("Displaying notifications list");
        // TODO: Get authenticated user id and load notifications
        model.addAttribute("notifications", notificationService.getNotificationsForUser(1L));
        return "notifications/list";
    }

    /**
     * Displays status update management view for a specific train.
     *
     * @param id train ID
     * @param model UI model
     * @return 'notifications/status' template name
     */
    @GetMapping("/trains/{id}/status")
    public String showStatusUpdateForm(@PathVariable("id") Long id, Model model) {
        log.debug("Showing status update form for train ID: {}", id);
        model.addAttribute("trainId", id);
        return "notifications/status";
    }

    /**
     * Updates train status and triggers automated notification broadcasts to all booked passengers.
     *
     * @param id train ID
     * @param status new operational status
     * @return redirect back to status update page or manage page
     */
    @PostMapping("/trains/{id}/status")
    public String updateTrainStatusAndNotify(@PathVariable("id") Long id, @RequestParam("status") String status) {
        log.info("Processing train ID {} status update to: {}", id, status);
        notificationService.notifyAffectedPassengers(id, status);
        return "redirect:/trains/" + id + "/status?updated=true";
    }
}
