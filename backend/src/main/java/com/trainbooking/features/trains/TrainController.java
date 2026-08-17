package com.trainbooking.features.trains;

import com.trainbooking.features.trains.dto.TrainDto;
import com.trainbooking.features.trains.dto.TrainSearchResultDto;
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
import java.util.List;

/**
 * Controller handling train search views, fleet management, and live schedule operations.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    /**
     * Handles train search queries and displays matching available trains.
     *
     * @param origin departure station
     * @param destination destination station
     * @param date travel date
     * @param model UI model
     * @return 'trains/search' template name
     */
    @GetMapping("/trains/search")
    public String searchTrains(
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "destination", required = false) String destination,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        log.debug("Train search request - origin: {}, destination: {}, date: {}", origin, destination, date);
        if (origin != null && destination != null && date != null) {
            List<TrainSearchResultDto> results = trainService.searchTrains(origin, destination, date);
            model.addAttribute("results", results);
        }
        model.addAttribute("origin", origin);
        model.addAttribute("destination", destination);
        model.addAttribute("date", date);
        return "trains/search";
    }

    /**
     * Displays admin train fleet management dashboard.
     *
     * @param model UI model
     * @return 'trains/manage' template name
     */
    @GetMapping("/trains/manage")
    public String manageTrains(Model model) {
        log.debug("Accessing train management view");
        model.addAttribute("trains", trainService.getAllTrains());
        model.addAttribute("trainDto", new TrainDto());
        return "trains/manage";
    }

    /**
     * Creates a new train record.
     *
     * @param trainDto train payload
     * @return redirect to manage trains view
     */
    @PostMapping("/trains")
    public String createTrain(@ModelAttribute("trainDto") TrainDto trainDto) {
        log.info("Creating train via form submission: {}", trainDto.getTrainNumber());
        trainService.createTrain(trainDto);
        return "redirect:/trains/manage";
    }

    /**
     * Displays edit form for a specific train.
     *
     * @param id train ID
     * @param model UI model
     * @return 'trains/edit' template name
     */
    @GetMapping("/trains/{id}/edit")
    public String editTrainForm(@PathVariable("id") Long id, Model model) {
        log.debug("Displaying edit form for train ID: {}", id);
        model.addAttribute("train", trainService.getTrainById(id));
        return "trains/edit";
    }

    /**
     * Updates an existing train record.
     *
     * @param id train ID
     * @param trainDto updated train details
     * @return redirect to manage trains view
     */
    @PostMapping("/trains/{id}")
    public String updateTrain(@PathVariable("id") Long id, @ModelAttribute("trainDto") TrainDto trainDto) {
        log.info("Updating train ID: {}", id);
        trainService.updateTrain(id, trainDto);
        return "redirect:/trains/manage";
    }

    /**
     * Deletes a train record.
     *
     * @param id train ID
     * @return redirect to manage trains view
     */
    @PostMapping("/trains/{id}/delete")
    public String deleteTrain(@PathVariable("id") Long id) {
        log.info("Deleting train ID: {}", id);
        trainService.deleteTrain(id);
        return "redirect:/trains/manage";
    }

    /**
     * Updates live operational status of a train.
     *
     * @param id train ID
     * @param status new status (ON_TIME, DELAYED, CANCELLED)
     * @return redirect to manage trains view
     */
    @PostMapping("/trains/{id}/status")
    public String updateStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        log.info("Updating status for train ID {} to {}", id, status);
        trainService.updateTrainStatus(id, status);
        return "redirect:/trains/manage";
    }
}
