package com.trainbooking.it25102327.controller;

import com.trainbooking.it25102327.dto.PlatformAssignmentResult;
import com.trainbooking.it25102327.dto.ScheduleDto;
import com.trainbooking.it25102327.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.trainbooking.it25102327.service.TrainService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Controller handling train schedule management endpoints for administrators.
 * Provides views and actions for listing, creating, deleting schedules,
 * seasonal overrides, and maintenance blocks.
 *
 * @author SLIIT Software Engineering Team (IT25102327)
 * @version 1.0.0
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TrainService trainService;

    /**
     * Page 2: Displays the list of all train schedules for administrative management.
     *
     * @param model the UI Model to supply attributes to the view template
     * @return the view template path for schedules 'it25102327/schedules'
     */
    @GetMapping({"/trains/schedules", "/schedules"})
    public String listSchedules(Model model) {
        log.debug("Fetching all schedules for admin view");
        List<ScheduleDto> schedules = scheduleService.getAllSchedules();
        model.addAttribute("schedules", schedules);
        model.addAttribute("trains", trainService.getAllTrains());
        model.addAttribute("routes", scheduleService.getAllRoutes());
        model.addAttribute("platforms", ScheduleService.STANDARD_PLATFORMS);
        model.addAttribute("newSchedule", new ScheduleDto());
        model.addAttribute("scheduleDto", new ScheduleDto());
        return "it25102327/schedules";
    }

    /**
     * REST endpoint checking platform collision risks and availability for given station, day, and time.
     *
     * @param station departure station
     * @param day day of week
     * @param departureTime scheduled departure time (HH:mm)
     * @param platform candidate platform
     * @param excludeId optional schedule ID to exclude
     * @return {@link PlatformAssignmentResult}
     */
    @GetMapping("/api/schedules/platform/check")
    @ResponseBody
    public PlatformAssignmentResult checkPlatform(
            @RequestParam("station") String station,
            @RequestParam("day") String day,
            @RequestParam("departureTime") String departureTime,
            @RequestParam(value = "platform", defaultValue = "Platform 1") String platform,
            @RequestParam(value = "excludeId", required = false) Long excludeId
    ) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
        LocalTime time = LocalTime.parse(departureTime);
        return scheduleService.checkPlatformAvailability(station, dayOfWeek, time, platform, excludeId);
    }

    /**
     * REST endpoint autonomously allocating a conflict-free station platform.
     *
     * @param station departure station
     * @param day day of week
     * @param departureTime scheduled departure time (HH:mm)
     * @param excludeId optional schedule ID to exclude
     * @return Map containing allocated platform
     */
    @GetMapping("/api/schedules/platform/auto-assign")
    @ResponseBody
    public Map<String, String> autoAssignPlatform(
            @RequestParam("station") String station,
            @RequestParam("day") String day,
            @RequestParam("departureTime") String departureTime,
            @RequestParam(value = "excludeId", required = false) Long excludeId
    ) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
        LocalTime time = LocalTime.parse(departureTime);
        String allocated = scheduleService.assignAutonomousPlatform(station, dayOfWeek, time, excludeId);
        return Map.of("allocatedPlatform", allocated, "status", "SUCCESS");
    }

    /**
     * Handles form submission for creating a new train schedule.
     *
     * @param scheduleDto the schedule details submitted from the form
     * @param redirectAttributes flash attributes
     * @return redirect URL back to the schedules listing
     */
    @PostMapping("/schedules")
    public String createSchedule(@ModelAttribute("newSchedule") ScheduleDto scheduleDto, RedirectAttributes redirectAttributes) {
        log.debug("Creating new schedule: {}", scheduleDto);
        ScheduleDto created = scheduleService.createSchedule(scheduleDto);
        redirectAttributes.addFlashAttribute("successMessage",
                "Timetable schedule #SCH-" + created.getId() + " created successfully on " + created.getEffectivePlatform() + ".");
        return "redirect:/trains/schedules";
    }

    /**
     * Handles form submission for updating an existing train schedule.
     *
     * @param id schedule ID
     * @param scheduleDto updated details
     * @param redirectAttributes flash attributes
     * @return redirect URL back to the schedules view
     */
    @PostMapping("/schedules/{id}")
    public String updateSchedule(@PathVariable("id") Long id, @ModelAttribute("scheduleDto") ScheduleDto scheduleDto, RedirectAttributes redirectAttributes) {
        log.debug("Updating schedule ID: {} with data: {}", id, scheduleDto);
        scheduleService.updateSchedule(id, scheduleDto);
        redirectAttributes.addFlashAttribute("successMessage", "Timetable schedule #SCH-" + id + " updated successfully.");
        return "redirect:/trains/schedules";
    }

    /**
     * Handles cascading deletion of an existing train schedule by its ID.
     *
     * @param id the unique identifier of the schedule to delete
     * @param redirectAttributes flash attributes
     * @return redirect URL back to the schedules view
     */
    @PostMapping("/schedules/{id}/delete")
    public String deleteSchedule(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.debug("Deleting schedule with ID: {}", id);
        scheduleService.deleteSchedule(id);
        redirectAttributes.addFlashAttribute("successMessage", "Timetable schedule #SCH-" + id + " and all associated bookings deleted successfully.");
        return "redirect:/trains/schedules";
    }

    /**
     * Handles seasonal timetable override toggle.
     *
     * @param scheduleId schedule ID
     * @param isSeasonal true to enable seasonal override
     * @param seasonalName name of seasonal schedule
     * @param redirectAttributes flash attributes
     * @return redirect back to schedules view
     */
    @PostMapping("/schedules/seasonal/override")
    public String setSeasonalOverride(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam(value = "isSeasonal", defaultValue = "false") boolean isSeasonal,
            @RequestParam(value = "seasonalName", required = false) String seasonalName,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Coordinator toggling seasonal override for schedule {}: {} ({})", scheduleId, isSeasonal, seasonalName);
        scheduleService.setSeasonalOverride(scheduleId, isSeasonal, seasonalName);
        redirectAttributes.addFlashAttribute("successMessage", isSeasonal ? "Seasonal override activated for Schedule #SCH-" + scheduleId : "Seasonal override lifted for Schedule #SCH-" + scheduleId);
        return "redirect:/trains/schedules";
    }

    /**
     * Handles maintenance block scheduling.
     *
     * @param scheduleId schedule ID
     * @param blocked true to activate block
     * @param notes maintenance description
     * @param redirectAttributes flash attributes
     * @return redirect back to schedules view
     */
    @PostMapping("/schedules/maintenance/block")
    public String setMaintenanceBlock(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam(value = "blocked", defaultValue = "false") boolean blocked,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Coordinator setting maintenance block for schedule {}: {}", scheduleId, blocked);
        String msg = scheduleService.setMaintenanceBlock(scheduleId, blocked, notes);
        if (msg.contains("WARNING")) {
            redirectAttributes.addFlashAttribute("errorMessage", msg);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", msg);
        }
        return "redirect:/trains/schedules";
    }
}
