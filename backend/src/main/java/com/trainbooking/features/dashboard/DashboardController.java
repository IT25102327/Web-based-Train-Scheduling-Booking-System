package com.trainbooking.features.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller providing admin analytics overview pages and revenue data endpoints for charting.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Renders main administrator dashboard with operational summary KPIs.
     *
     * @param model UI model
     * @return 'dashboard/index' template name
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        log.debug("Rendering admin dashboard overview");
        model.addAttribute("stats", dashboardService.getDashboardStats());
        return "dashboard/index";
    }

    /**
     * Returns JSON formatted revenue timeline data for charting libraries.
     *
     * @param from range start date
     * @param to range end date
     * @return JSON response map containing dates and revenue figures
     */
    @GetMapping("/dashboard/revenue")
    @ResponseBody
    public Map<String, Object> getRevenueChartData(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate startDate = (from != null) ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = (to != null) ? to : LocalDate.now();
        log.info("Fetching revenue chart dataset between {} and {}", startDate, endDate);
        return dashboardService.getRevenueByDate(startDate, endDate);
    }
}
