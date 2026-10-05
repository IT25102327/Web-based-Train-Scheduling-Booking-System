package com.trainbooking.it25102925.controller;

import com.trainbooking.it25102925.dto.TrainLocationDto;
import com.trainbooking.it25102925.service.TrainLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller exposing live train telemetry endpoints:
 * - GET /api/trains/location/update: Ingests live telemetry via standard GET API with query parameters
 * - POST /api/trains/location/update: Ingests live telemetry via JSON or form payload
 * - GET /api/trains/locations: Retrieves real-time coordinates and metrics for all trains (used by map view)
 * - GET /api/trains/{trainId}/location: Retrieves real-time position for a single train
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trains")
public class TrainLocationRestController {

    private final TrainLocationService trainLocationService;

    /**
     * Ingestion endpoint via standard GET API with query parameters.
     * Example: /api/trains/location/update?trainId=1&latitude=6.9344&longitude=79.8500&speed=45.5&status=ON_TIME
     */
    @GetMapping("/location/update")
    public ResponseEntity<Map<String, Object>> updateLocationGet(
            @RequestParam("trainId") Long trainId,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "lng", required = false) Double lng,
            @RequestParam(value = "lon", required = false) Double lon,
            @RequestParam(value = "speed", required = false, defaultValue = "0.0") Double speed,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "station", required = false) String station,
            @RequestParam(value = "nextStation", required = false) String nextStation,
            @RequestParam(value = "direction", required = false) String direction,
            @RequestParam(value = "tripDirection", required = false) String tripDirection,
            @RequestParam(value = "route", required = false) String route,
            @RequestParam(value = "progress", required = false) Integer progress
    ) {
        Double effectiveLat = latitude != null ? latitude : lat;
        Double effectiveLng = longitude != null ? longitude : (lng != null ? lng : lon);

        if (effectiveLat == null || effectiveLng == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Missing coordinates: provide latitude/lat and longitude/lng");
            return ResponseEntity.badRequest().body(err);
        }

        String effectiveDirection = direction != null ? direction : tripDirection;

        TrainLocationDto updated = trainLocationService.updateLocation(
                trainId, effectiveLat, effectiveLng, speed, status, station, nextStation,
                effectiveDirection, route, progress
        );

        if (updated == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Train not found with ID: " + trainId);
            return ResponseEntity.badRequest().body(err);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Location telemetry recorded successfully");
        resp.put("data", updated);
        return ResponseEntity.ok(resp);
    }

    /**
     * Ingestion endpoint via POST with JSON body.
     */
    @PostMapping("/location/update")
    public ResponseEntity<Map<String, Object>> updateLocationPost(@RequestBody Map<String, Object> payload) {
        if (payload == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Empty payload");
            return ResponseEntity.badRequest().body(err);
        }

        Long trainId = parseLong(payload.get("trainId"));
        Double lat = parseDouble(payload.get("latitude") != null ? payload.get("latitude") : payload.get("lat"));
        Double lng = parseDouble(payload.get("longitude") != null ? payload.get("longitude") : (payload.get("lng") != null ? payload.get("lng") : payload.get("lon")));
        Double speed = parseDouble(payload.get("speed"));
        String status = (String) payload.get("status");
        String station = (String) payload.get("station");
        String nextStation = (String) payload.get("nextStation");
        String direction = (String) (payload.get("direction") != null ? payload.get("direction") : payload.get("tripDirection"));
        String route = (String) payload.get("route");
        Integer progress = payload.get("progress") instanceof Number n ? n.intValue() : null;

        if (trainId == null || lat == null || lng == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Missing required fields: trainId, latitude, longitude");
            return ResponseEntity.badRequest().body(err);
        }

        TrainLocationDto updated = trainLocationService.updateLocation(
                trainId, lat, lng, speed, status, station, nextStation,
                direction, route, progress
        );

        if (updated == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Train not found with ID: " + trainId);
            return ResponseEntity.badRequest().body(err);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Location telemetry recorded successfully");
        resp.put("data", updated);
        return ResponseEntity.ok(resp);
    }

    /**
     * Retrieves live locations of all active trains for rendering on OpenStreetMap.
     */
    @GetMapping("/locations")
    public ResponseEntity<List<TrainLocationDto>> getAllLocations() {
        return ResponseEntity.ok(trainLocationService.getAllLocations());
    }

    /**
     * Retrieves live location of a specific train.
     */
    @GetMapping("/{trainId}/location")
    public ResponseEntity<TrainLocationDto> getTrainLocation(@PathVariable("trainId") Long trainId) {
        Optional<TrainLocationDto> opt = trainLocationService.getLocation(trainId);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Long parseLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
