package com.trainbooking.it25102327.controller;

import com.trainbooking.it25102327.dto.RouteDetailDto;
import com.trainbooking.it25102327.dto.ScheduleDto;
import com.trainbooking.it25102327.dto.StationDto;
import com.trainbooking.it25102327.dto.TrainDto;
import com.trainbooking.it25102327.dto.TrainSearchResultDto;
import com.trainbooking.it25102327.model.Route;
import com.trainbooking.it25102327.model.Schedule;
import com.trainbooking.it25102327.service.RailwayStationService;
import com.trainbooking.it25102327.service.ScheduleService;
import com.trainbooking.it25102327.service.TrainService;
import com.trainbooking.it25102925.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller handling train search views, fleet management, and live schedule operations.
 *
 * @author SLIIT Software Engineering Team (IT25102327)
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;
    private final RailwayStationService railwayStationService;

    /**
     * Handles train search queries and displays matching available trains.
     * Serves both the home root URL and /trains/search.
     *
     * @param origin departure station
     * @param destination destination station
     * @param date travel date
     * @param model UI model
     * @return 'it25102327/search' template name
     */
    @GetMapping({"/", "/trains/search"})
    public String searchTrains(
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "destination", required = false) String destination,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        log.info("Handling train search: origin='{}', destination='{}', date={}", origin, destination, date);
        boolean isSearchSubmitted = (origin != null && !origin.isBlank()) || (destination != null && !destination.isBlank());
        if (isSearchSubmitted) {
            List<TrainSearchResultDto> results = trainService.searchTrains(origin, destination, date);
            model.addAttribute("trainResults", results);
            model.addAttribute("results", results);
        }
        model.addAttribute("origin", origin != null ? origin.trim() : "");
        model.addAttribute("destination", destination != null ? destination.trim() : "");
        model.addAttribute("date", date != null ? date : LocalDate.now());
        return "it25102327/search";
    }

    /**
     * Page 1: Displays admin train fleet management dashboard.
     *
     * @param model UI model
     * @return 'it25102327/fleet' template name
     */
    @GetMapping({"/trains/fleet", "/trains/manage"})
    public String fleetDashboard(Model model) {
        log.debug("Accessing train fleet dashboard");
        model.addAttribute("trains", trainService.getAllTrains());
        model.addAttribute("trainDto", new TrainDto());
        return "it25102327/fleet";
    }

    /**
     * Page 3: Displays railway routes and dynamic station pricing simulator dashboard.
     *
     * @param model UI model
     * @return 'it25102327/routes' template name
     */
    @GetMapping("/trains/routes")
    public String routesDashboard(Model model) {
        log.debug("Accessing railway routes and pricing dashboard");
        model.addAttribute("routes", scheduleService.getAllRoutes());
        model.addAttribute("allStations", railwayStationService.getAllStations());
        return "it25102327/routes";
    }

    /**
     * REST endpoint returning intermediate stations, geo-coordinates, and distance for a specific route.
     *
     * @param id route ID
     * @return RouteDetailDto with stations and map polyline coordinates
     */
    @GetMapping("/api/routes/{id}/stations")
    @ResponseBody
    public RouteDetailDto getRouteStations(@PathVariable("id") Long id) {
        log.debug("Fetching route station details for ID: {}", id);
        Route route = scheduleService.getRouteById(id);
        return railwayStationService.getRouteDetails(route);
    }

    /**
     * REST endpoint returning corridor geometry and stations between any origin and destination.
     *
     * @param origin departure station
     * @param destination arrival station
     * @return RouteDetailDto
     */
    @GetMapping("/api/routes/corridor")
    @ResponseBody
    public RouteDetailDto getCorridorStations(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination
    ) {
        log.debug("Resolving corridor stations: {} ➔ {}", origin, destination);
        Route mockRoute = Route.builder()
                .origin(origin)
                .destination(destination)
                .defaultPlatform("Platform 1")
                .build();
        return railwayStationService.getRouteDetails(mockRoute);
    }

    /**
     * REST endpoint returning all registered railway network stations.
     *
     * @return list of StationDto
     */
    @GetMapping("/api/routes/stations")
    @ResponseBody
    public List<StationDto> getAllNetworkStations() {
        return railwayStationService.getAllStations();
    }

    /**
     * REST endpoint autonomously calculating railway track distance between any two stations.
     *
     * @param origin origin station
     * @param destination destination station
     * @return Map with calculated distance in km and metadata
     */
    @GetMapping("/api/routes/calculate-distance")
    @ResponseBody
    public Map<String, Object> calculateRouteDistance(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination
    ) {
        log.debug("Calculating track distance: {} ➔ {}", origin, destination);
        int distanceKm = railwayStationService.calculateTrackDistanceKm(origin, destination);
        List<StationDto> stations = railwayStationService.getStationsAlongRoute(origin, destination);

        Map<String, Object> result = new HashMap<>();
        result.put("origin", origin);
        result.put("destination", destination);
        result.put("distanceKm", distanceKm);
        result.put("stationsCount", stations.size());
        result.put("status", "SUCCESS");
        return result;
    }

    /**
     * Creates a new railway route with automatic track distance computation if omitted.
     *
     * @param origin origin station
     * @param destination destination station
     * @param distanceKm travel distance in km (auto-computed if null or 0)
     * @param defaultPlatform default platform designation
     * @param redirectAttributes flash attributes
     * @return redirect to routes dashboard
     */
    @PostMapping("/trains/routes/add")
    public String createRoute(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam(value = "distanceKm", required = false) Integer distanceKm,
            @RequestParam(value = "defaultPlatform", defaultValue = "Platform 1") String defaultPlatform,
            RedirectAttributes redirectAttributes
    ) {
        if (distanceKm == null || distanceKm <= 0) {
            distanceKm = railwayStationService.calculateTrackDistanceKm(origin, destination);
        }
        log.info("Creating new route: {} ➔ {} ({} km, {})", origin, destination, distanceKm, defaultPlatform);
        scheduleService.createRoute(origin, destination, distanceKm, defaultPlatform);
        redirectAttributes.addFlashAttribute("successMessage",
                "Route '" + origin + " ➔ " + destination + "' (" + distanceKm + " km) added successfully.");
        return "redirect:/trains/routes";
    }

    /**
     * Deletes a railway route and its associated schedules.
     *
     * @param id route ID
     * @param redirectAttributes flash attributes
     * @return redirect to routes dashboard
     */
    @PostMapping("/trains/routes/{id}/delete")
    public String deleteRoute(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.info("Deleting route ID: {}", id);
        scheduleService.deleteRoute(id);
        redirectAttributes.addFlashAttribute("successMessage", "Route and all associated schedules were deleted successfully.");
        return "redirect:/trains/routes";
    }

    /**
     * Creates a new train record.
     *
     * @param trainDto train payload
     * @param redirectAttributes flash attributes
     * @return redirect to fleet dashboard
     */
    @PostMapping({"/trains", "/trains/add"})
    public String createTrain(@ModelAttribute("trainDto") TrainDto trainDto, RedirectAttributes redirectAttributes) {
        log.info("Creating train via form submission: {}", trainDto.getTrainNumber());
        trainService.createTrain(trainDto);
        redirectAttributes.addFlashAttribute("successMessage", "Train " + trainDto.getTrainNumber() + " (" + trainDto.getTrainName() + ") added successfully to the fleet.");
        return "redirect:/trains/fleet";
    }

    /**
     * Displays edit form for a specific train.
     *
     * @param id train ID
     * @param model UI model
     * @return 'it25102327/fleet' template name
     */
    @GetMapping("/trains/{id}/edit")
    public String editTrainForm(@PathVariable("id") Long id, Model model) {
        log.debug("Displaying edit form for train ID: {}", id);
        model.addAttribute("train", trainService.getTrainById(id));
        model.addAttribute("trains", trainService.getAllTrains());
        model.addAttribute("trainDto", new TrainDto());
        return "it25102327/fleet";
    }

    /**
     * Updates an existing train record.
     *
     * @param id train ID
     * @param trainDto updated train details
     * @param redirectAttributes flash attributes
     * @return redirect to fleet dashboard
     */
    @PostMapping("/trains/{id}")
    public String updateTrain(@PathVariable("id") Long id, @ModelAttribute("trainDto") TrainDto trainDto, RedirectAttributes redirectAttributes) {
        log.info("Updating train ID: {}", id);
        trainService.updateTrain(id, trainDto);
        redirectAttributes.addFlashAttribute("successMessage", "Train #" + trainDto.getTrainNumber() + " (" + trainDto.getTrainName() + ") updated successfully.");
        return "redirect:/trains/fleet";
    }

    /**
     * Deletes a train record.
     *
     * @param id train ID
     * @param redirectAttributes flash attributes
     * @return redirect to fleet dashboard
     */
    @PostMapping("/trains/{id}/delete")
    public String deleteTrain(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.info("Deleting train ID: {}", id);
        trainService.deleteTrain(id);
        redirectAttributes.addFlashAttribute("successMessage", "Train and all associated schedules were deleted successfully.");
        return "redirect:/trains/fleet";
    }

    /**
     * Updates live operational status of a train.
     *
     * @param id train ID
     * @param status new status (ON_TIME, DELAYED, CANCELLED, ACTIVE, MAINTENANCE)
     * @param redirectAttributes flash attributes
     * @return redirect to fleet dashboard
     */
    @PostMapping("/trains/{id}/status")
    public String updateStatus(@PathVariable("id") Long id, @RequestParam("status") String status, RedirectAttributes redirectAttributes) {
        log.info("Updating status for train ID {} to {}", id, status);
        trainService.updateTrainStatus(id, status);
        try {
            notificationService.notifyAffectedPassengers(id, status);
        } catch (Exception ex) {
            log.warn("Could not dispatch passenger notifications for train ID {}: {}", id, ex.getMessage());
        }
        redirectAttributes.addFlashAttribute("successMessage", "Train status successfully updated to " + status + ".");
        return "redirect:/trains/fleet";
    }

    /**
     * REST endpoint returning real-time train status, platform assignments, and capacities.
     * Used for auto-refreshing public departure boards and mobile apps.
     *
     * @return list of live train status objects
     */
    @GetMapping("/api/trains/live-status")
    @ResponseBody
    public List<Map<String, Object>> getLiveTrainStatus() {
        return trainService.getAllTrains().stream().map(train -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", train.getId());
            map.put("trainNumber", train.getTrainNumber());
            map.put("trainName", train.getTrainName());
            map.put("trainType", train.getTrainType() != null ? train.getTrainType() : "EXPRESS");
            map.put("status", train.getStatus() != null ? train.getStatus().name() : "ON_TIME");
            map.put("firstClassSeats", train.getFirstClassSeats());
            map.put("secondClassSeats", train.getSecondClassSeats());
            map.put("totalSeats", train.getTotalSeats());

            List<Schedule> schedules = scheduleService.getSchedulesByTrainId(train.getId());
            String platform = "Platform 1";
            String origin = "Colombo Fort";
            String destination = "Kandy";
            if (!schedules.isEmpty()) {
                Schedule s = schedules.get(0);
                if (s.getRoute() != null && s.getRoute().getDefaultPlatform() != null && !s.getRoute().getDefaultPlatform().isBlank()) {
                    platform = s.getRoute().getDefaultPlatform();
                }
                if (s.getRoute() != null) {
                    origin = s.getRoute().getOrigin();
                    destination = s.getRoute().getDestination();
                }
            }
            map.put("platform", platform);
            map.put("origin", origin);
            map.put("destination", destination);
            return map;
        }).toList();
    }
}
