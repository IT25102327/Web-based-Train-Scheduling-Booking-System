package com.trainbooking.it25102327.controller;
import com.trainbooking.it25102327.service.*;
import com.trainbooking.it25102327.model.*;
import com.trainbooking.it25102327.dto.*;
import com.trainbooking.it25102327.repository.*;

import com.trainbooking.it25102327.dto.ScheduleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * Controller handling train schedule management endpoints for administrators.
 * Provides views and actions for listing, creating, and deleting schedules.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Displays the list of all train schedules for administrative management.
     *
     * @param model the UI Model to supply attributes to the view template
     * @return the view template path for schedules
     */
    @GetMapping("/schedules")
    public String listSchedules(Model model) {
        log.debug("Fetching all schedules for admin view");
        List<ScheduleDto> schedules = scheduleService.getAllSchedules();
        model.addAttribute("schedules", schedules);
        model.addAttribute("newSchedule", new ScheduleDto());
        return "it25102327/manage";
    }

    /**
     * Handles form submission for creating a new train schedule.
     *
     * @param scheduleDto the schedule details submitted from the form
     * @return redirect URL back to the schedules listing
     */
    @PostMapping("/schedules")
    public String createSchedule(@ModelAttribute("newSchedule") ScheduleDto scheduleDto) {
        log.debug("Creating new schedule: {}", scheduleDto);
        scheduleService.createSchedule(scheduleDto);
        return "redirect:/schedules";
    }

    /**
     * Handles deletion of an existing train schedule by its ID.
     *
     * @param id the unique identifier of the schedule to delete
     * @return redirect URL back to the schedules listing
     */
    @PostMapping("/schedules/{id}/delete")
    public String deleteSchedule(@PathVariable("id") Long id) {
        log.debug("Deleting schedule with ID: {}", id);
        scheduleService.deleteSchedule(id);
        return "redirect:/schedules";
    }
}
