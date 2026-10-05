# Task Progress: IT25102327 — Balawickrama B.R.D.

**Module:** UC-02: Train Route & Schedule Management  
**Role:** Schedule Coordinator (Admin)  
**Priority:** 5  
**Package:** `com.trainbooking.it25102327`  
**Templates:** `src/main/resources/templates/it25102327/`  
**Tests:** `src/test/java/com/trainbooking/it25102327/`  

---

## ✅ DONE
- [x] **Use Case Specification (Lab 03)**: Documented UC-02 master schedules, platform allocations, dynamic pricing, seasonal overrides, and maintenance blocks.
- [x] **Activity Diagram (Lab 04)**: Modeled route input, platform configuration, parallel seasonal/maintenance setup, conflict review, and real-time database push.
- [x] **Entity Models**:
  - `Train.java`: Fleet details (trainNumber, trainName, totalSeats, firstClassSeats, secondClassSeats, status).
  - `Route.java`: Route network (origin, destination, distanceKm, train).
  - `Schedule.java`: Timetable records (train, route, departureTime, arrivalTime, dayOfWeek, firstClassFare, secondClassFare, isActive).
- [x] **Repositories**:
  - `TrainRepository.java`: Custom queries `findByTrainNumber`, `findByStatus`.
  - `RouteRepository.java`: Custom queries `findByOriginAndDestination`.
  - `ScheduleRepository.java`: Flexible search queries with day-of-week and route origin/destination filters.
- [x] **Data Transfer Objects (DTOs)**:
  - `TrainDto.java`, `RouteDto.java`, `ScheduleDto.java`, `TrainSearchResultDto.java` (with duration & formatted fare helpers).
- [x] **Service Layer**:
  - `TrainService.java`: Train creation, capacity update, deletion with cascading cleanup, status updates, and search logic across active schedules.
  - `ScheduleService.java`: Timetable creation, route creation/deletion, route-schedule associations, and schedule CRUD operations with full cascade cleanup.
- [x] **Controller Layer**:
  - `TrainController.java`: Endpoints for `/`, `/trains/search`, `/trains/fleet` (alias `/trains/manage`), `/trains/routes`, `/trains/routes/add`, `/trains/routes/{id}/delete`, `/trains`, `/trains/{id}`, `/trains/{id}/delete`, `/trains/{id}/status`.
  - `ScheduleController.java`: Endpoints for `/trains/schedules` (alias `/schedules`), `/schedules` (create), `/schedules/{id}`, `/schedules/{id}/delete`, `/schedules/seasonal/override`, `/schedules/maintenance/block`.
- [x] **Thymeleaf UI Views (3-Page Modular Admin Architecture)**:
  - `it25102327/fleet.html` (Page 1): Rolling stock registry, seat capacities, edit prefill form, status toggle, and safe delete.
  - `it25102327/schedules.html` (Page 2): Master timetables, departure/arrival schedules, platform assignments, seasonal overrides, maintenance blocks, edit form, and safe delete.
  - `it25102327/routes.html` (Page 3): Railway route corridor management, add/delete route corridors, platform allocations, and interactive real-time dynamic pricing simulator widget.
  - `it25102327/search.html`: Public train search, date picker, station autocomplete, and live availability results display.
- [x] **OpenStreetMap Route & Intermediate Station Visualizer (`/trains/routes`)**:
  - Embedded Leaflet/OpenStreetMap engine visualizing Sri Lankan railway corridors with polylines and station markers.
  - Interactive route corridor selector and custom station-pair trace tool.
  - Intermediate stations sequence timeline breakdown showing stop sequence numbers, cumulative distance (km), and platform counts.
  - Interactive marker zoom and detailed station popup dialogs (code, distance, platforms, line name).
- [x] **Platform Collision Validation & Autonomous Platform Assignment System (`/trains/schedules`)**:
  - Implemented 20-minute safety buffer headway validation across both arrival and departure station movements.
  - Autonomous platform allocation engine that evaluates station platform occupancy across Platforms 1 through 6 on the target day of week, auto-assigning the first conflict-free platform.
  - Real-time collision warning alert box and 1-click "Apply Recommended Platform" in timetable creation and edit forms.
  - Dedicated REST endpoints: `/api/schedules/platform/check`, `/api/schedules/platform/auto-assign`, `/api/routes/{id}/stations`, `/api/routes/corridor`, and `/api/routes/stations`.
- [x] **Unit Testing (JUnit 5 + Mockito)**:
  - `TrainServiceTest.java`: Verified train listing, lookup by ID, creation, status update, cascade deletion, and schedule search mapping.
  - `ScheduleServiceTest.java`: Verified schedule listing, lookup by ID, creation, update, cascade deletion, platform conflict detection, autonomous platform assignment, dynamic pricing calculation, seasonal timetable overrides, and maintenance block conflict alerts.
  - `RailwayStationServiceTest.java`: Verified station sequence extraction along Main Line, Coastal Line, Northern Line, reverse trip geometry, and polyline coordinates.

---

## ⏳ IN PROGRESS (Current Phase)
- All planned tasks, bug fixes, and feature modularization completed.

---

## 📋 TODO (Upcoming Enhancements)
- [x] OpenStreetMap route & intermediate station visualizer.
- [x] Platform collision validation & autonomous platform allocator.
- [x] Coordinator dashboard metrics synchronization with UC-06.
- [x] Real-time fleet live status endpoint `/api/trains/live-status` feeding departure boards.
- [x] Operational status update propagation to UC-05 passenger notification engine.
- [x] Split monolithic train management UI into 3 dedicated pages.
- [x] Resolve Edit button DOM reference error.
- [x] Resolve Delete foreign key constraint violation.
