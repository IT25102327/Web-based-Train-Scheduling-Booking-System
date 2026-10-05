package com.trainbooking.it25103308.service;

import com.trainbooking.exception.ResourceNotFoundException;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25101520.repository.UserRepository;
import com.trainbooking.it25102327.model.Schedule;
import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.ScheduleRepository;
import com.trainbooking.it25103308.dto.BookingRequest;
import com.trainbooking.it25103308.dto.CoachDto;
import com.trainbooking.it25103308.dto.SeatDto;
import com.trainbooking.it25103308.dto.SeatStatusResponseDto;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class handling booking lifecycle, available seat calculations, and reservation management.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final com.trainbooking.it25100977.service.PaymentService paymentService;
    private final com.trainbooking.it25102925.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.trainbooking.it25103308.strategy.SeatAllocationStrategyFactory seatAllocationStrategyFactory;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.trainbooking.it25103308.state.BookingStateFactory bookingStateFactory;

    /**
     * Calculates the number of available seats remaining for a schedule, date, and seat class.
     *
     * @param scheduleId schedule ID
     * @param date date of travel
     * @param seatClass FIRST or SECOND seat class
     * @return count of available seats
     */
    public int getAvailableSeats(Long scheduleId, LocalDate date, String seatClass) {
        log.debug("Calculating available seats for schedule ID {}, date {}, class {}", scheduleId, date, seatClass);
        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.getTrain() == null) {
            return 0;
        }

        Train train = schedule.getTrain();
        Booking.SeatClass sc;
        try {
            sc = Booking.SeatClass.valueOf(seatClass.toUpperCase());
        } catch (IllegalArgumentException ex) {
            sc = Booking.SeatClass.SECOND;
        }

        int totalCapacity = (sc == Booking.SeatClass.FIRST)
                ? (train.getFirstClassSeats() != null ? train.getFirstClassSeats() : 0)
                : (train.getSecondClassSeats() != null ? train.getSecondClassSeats() : 0);

        Integer bookedCount = bookingRepository.countByScheduleIdAndTravelDateAndSeatClass(scheduleId, date, sc);
        int booked = (bookedCount != null) ? bookedCount : 0;

        int remaining = totalCapacity - booked;
        return Math.max(0, remaining);
    }

    /**
     * Creates a new pending train booking for a passenger with a 10-minute temporary lock.
     *
     * @param request booking details
     * @param userId ID of the passenger making the reservation
     * @return created {@link Booking} entity
     */
    @Transactional
    public synchronized Booking createBooking(BookingRequest request, Long userId) {
        log.info("Creating booking for user ID {} on schedule ID {}", userId, request.getScheduleId());

        User passenger = userRepository.findById(userId)
                .orElseGet(() -> userRepository.findByEmail("passenger@trainbooking.lk")
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId)));

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + request.getScheduleId()));

        Booking.SeatClass seatClass = (request.getSeatClass() != null && request.getSeatClass().equalsIgnoreCase("FIRST"))
                ? Booking.SeatClass.FIRST
                : Booking.SeatClass.SECOND;

        int requestedSeats = request.getNumberOfSeats();
        int availableSeats = getAvailableSeats(schedule.getId(), request.getTravelDate(), seatClass.name());

        if (availableSeats < requestedSeats) {
            throw new IllegalStateException("Not enough seats available. Requested: " + requestedSeats + ", Available: " + availableSeats);
        }

        BigDecimal perSeatFare = (seatClass == Booking.SeatClass.FIRST)
                ? (schedule.getFirstClassFare() != null ? schedule.getFirstClassFare() : BigDecimal.ZERO)
                : (schedule.getSecondClassFare() != null ? schedule.getSecondClassFare() : BigDecimal.ZERO);

        BigDecimal totalAmount = perSeatFare.multiply(BigDecimal.valueOf(requestedSeats));

        List<String> bookedSeats = getBookedSeatNumbers(schedule.getId(), request.getTravelDate(), seatClass.name());
        String allocatedSeats;
        if (seatAllocationStrategyFactory != null) {
            allocatedSeats = seatAllocationStrategyFactory.getStrategy(request.getSeatNumbers())
                    .allocateSeats(seatClass, requestedSeats, request.getSeatNumbers(), availableSeats, bookedSeats);
        } else {
            if (request.getSeatNumbers() != null && !request.getSeatNumbers().trim().isEmpty()) {
                allocatedSeats = request.getSeatNumbers().trim();
                String[] reqParts = allocatedSeats.split(",");
                for (String seat : reqParts) {
                    String cleanSeat = seat.trim();
                    if (!cleanSeat.isEmpty() && isSeatInBookedList(cleanSeat, bookedSeats)) {
                        throw new IllegalStateException("Seat " + cleanSeat + " is already reserved by another passenger. Please select available seats.");
                    }
                }
            } else {
                String coach = (seatClass == Booking.SeatClass.FIRST) ? "Car 01" : "Car 02";
                int seatStartNum = (availableSeats % 50) + 1;
                allocatedSeats = coach + " / S" + seatStartNum + (requestedSeats > 1 ? (" - S" + (seatStartNum + requestedSeats - 1)) : "");
            }
        }

        Booking booking = Booking.builder()
                .passenger(passenger)
                .schedule(schedule)
                .travelDate(request.getTravelDate() != null ? request.getTravelDate() : LocalDate.now())
                .seatClass(seatClass)
                .numberOfSeats(requestedSeats)
                .totalAmount(totalAmount)
                .seatNumbers(allocatedSeats)
                .passengerName(request.getPassengerName() != null && !request.getPassengerName().isBlank()
                        ? request.getPassengerName() : (passenger.getFirstName() + " " + passenger.getLastName()))
                .passengerNic(request.getPassengerNic() != null ? request.getPassengerNic() : "")
                .contactPhone(request.getContactPhone() != null ? request.getContactPhone() : passenger.getPhone())
                .lockExpiresAt(LocalDateTime.now().plusMinutes(10))
                .status(Booking.BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created successfully with ID: {} and total: LKR {}", saved.getId(), saved.getTotalAmount());
        return saved;
    }

    /**
     * Retrieves booking details by ID.
     *
     * @param id booking ID
     * @return {@link Booking} entity
     */
    public Booking getBookingById(Long id) {
        log.info("Fetching booking by ID: {}", id);
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
    }

    /**
     * Explicitly releases a passenger's pending seat lock upon checkout cancellation or abandonment.
     *
     * @param bookingId booking ID
     */
    @Transactional
    public void releaseLock(Long bookingId) {
        log.info("Releasing seat lock for booking ID: {}", bookingId);
        bookingRepository.findById(bookingId).ifPresent(b -> {
            if (b.getStatus() == Booking.BookingStatus.PENDING) {
                b.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(b);
                log.info("Seat lock released back to inventory for booking ID: {}", bookingId);
            }
        });
    }

    /**
     * Confirms an active booking using the GoF State Pattern.
     *
     * @param booking target booking
     */
    @Transactional
    public void confirmBooking(Booking booking) {
        if (booking == null) return;
        if (bookingStateFactory != null) {
            com.trainbooking.it25103308.state.BookingState state = bookingStateFactory.getState(booking);
            state.confirm(booking);
        } else {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
        }
        bookingRepository.save(booking);
        log.info("Booking #{} successfully transitioned to CONFIRMED state.", booking.getId());
    }

    /**
     * Automated background worker that periodically scans for and auto-cancels unconfirmed seat locks
     * that have exceeded their 10-minute temporary reservation window.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30000)
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.PENDING
                        && b.getLockExpiresAt() != null
                        && now.isAfter(b.getLockExpiresAt()))
                .toList();

        if (!expiredBookings.isEmpty()) {
            log.info("Auto-releasing {} expired 10-minute seat lock reservations.", expiredBookings.size());
            for (Booking b : expiredBookings) {
                b.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(b);
                log.info("Auto-released expired lock for booking #{}", b.getId());
            }
        }
    }

    /**
     * Returns a list of occupied seat numbers (e.g. "S01", "S02") for visual seat map rendering.
     * Supports specific class ("FIRST", "SECOND") or all classes ("ALL" or null).
     */
    public List<String> getBookedSeatNumbers(Long scheduleId, LocalDate travelDate, String seatClass) {
        Booking.SeatClass targetClass = null;
        if (seatClass != null && !"ALL".equalsIgnoreCase(seatClass.trim())) {
            try {
                targetClass = Booking.SeatClass.valueOf(seatClass.trim().toUpperCase());
            } catch (Exception ignored) {
                targetClass = Booking.SeatClass.SECOND;
            }
        }
        final Booking.SeatClass filterClass = targetClass;
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getSchedule() != null && b.getSchedule().getId().equals(scheduleId)
                        && travelDate.equals(b.getTravelDate())
                        && (filterClass == null || b.getSeatClass() == filterClass)
                        && (b.getStatus() == Booking.BookingStatus.CONFIRMED || b.getStatus() == Booking.BookingStatus.PENDING))
                .toList();

        List<String> bookedSeats = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getSeatNumbers() != null) {
                for (String part : b.getSeatNumbers().split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        bookedSeats.add(trimmed);
                        if (trimmed.contains(" - ")) {
                            expandSeatRange(trimmed, bookedSeats);
                        }
                    }
                }
            }
        }
        return bookedSeats;
    }

    private void expandSeatRange(String rangeStr, List<String> targetList) {
        try {
            int slashIdx = rangeStr.indexOf('/');
            int dashIdx = rangeStr.indexOf(" - ");
            if (slashIdx != -1 && dashIdx != -1 && dashIdx > slashIdx) {
                String prefix = rangeStr.substring(0, slashIdx).trim();
                String startSeatPart = rangeStr.substring(slashIdx + 1, dashIdx).trim();
                String endSeatPart = rangeStr.substring(dashIdx + 3).trim();

                int startNum = Integer.parseInt(startSeatPart.replaceAll("\\D+", ""));
                int endNum = Integer.parseInt(endSeatPart.replaceAll("\\D+", ""));

                for (int n = startNum; n <= endNum; n++) {
                    String s1 = prefix + " / S" + n;
                    String s2 = prefix + " / S" + String.format("%02d", n);
                    if (!targetList.contains(s1)) targetList.add(s1);
                    if (!targetList.contains(s2)) targetList.add(s2);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Checks if a full seat identifier (e.g. "Coach 01 / S01") is in the list of booked seats.
     */
    public boolean isSeatInBookedList(String fullSeatId, List<String> bookedSeats) {
        if (fullSeatId == null || bookedSeats == null || bookedSeats.isEmpty()) return false;
        String normalizedTarget = normalizeSeatId(fullSeatId);
        for (String booked : bookedSeats) {
            if (fullSeatId.equalsIgnoreCase(booked) || normalizedTarget.equalsIgnoreCase(normalizeSeatId(booked))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalizes a seat identifier for cross-format comparison (e.g. Coach 01 / S01 vs Car 01 / S1).
     */
    public String normalizeSeatId(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\s+", "").toLowerCase()
                .replace("coach", "car")
                .replaceAll("s0+", "s");
    }

    /**
     * Generates all coaches for a train schedule, categorized into 1st Class and 2nd Class,
     * with each coach containing individual seats with real-time occupancy status.
     *
     * @param schedule the train schedule
     * @param travelDate date of travel
     * @return list of categorized {@link CoachDto} carriages
     */
    public List<CoachDto> generateTrainCoaches(Schedule schedule, LocalDate travelDate) {
        List<CoachDto> coachList = new ArrayList<>();
        if (schedule == null || schedule.getTrain() == null) {
            return coachList;
        }

        Train train = schedule.getTrain();
        List<String> allBooked = getBookedSeatNumbers(schedule.getId(), travelDate, "ALL");

        int firstTotal = train.getFirstClassSeats() != null ? train.getFirstClassSeats() : 60;
        int secondTotal = train.getSecondClassSeats() != null ? train.getSecondClassSeats() : 240;

        // 1st Class Coaches (20 seats each, 2+1 layout)
        int firstCoachCount = Math.min(3, Math.max(1, (firstTotal + 19) / 20));
        for (int c = 1; c <= firstCoachCount; c++) {
            String coachNum = String.format("Coach %02d", c);
            String coachLabel = "Car A" + c;
            int seatsInCoach = 20;

            CoachDto coach = CoachDto.builder()
                    .coachId("COACH-" + String.format("%02d", c))
                    .coachNumber(coachNum)
                    .coachLabel(coachLabel)
                    .coachClass("FIRST")
                    .classDisplayName("1st Class (AC)")
                    .totalSeats(seatsInCoach)
                    .layoutType("2+1")
                    .seats(new ArrayList<>())
                    .build();

            int bookedInCoach = 0;
            for (int s = 1; s <= seatsInCoach; s++) {
                String seatNum = String.format("S%02d", s);
                String fullSeatId = coachNum + " / " + seatNum;
                int row = (s - 1) / 3 + 1;
                int colIdx = (s - 1) % 3;
                String colLetter = (colIdx == 0) ? "A" : (colIdx == 1 ? "B" : "C");
                String seatType = (colIdx == 0 || colIdx == 2) ? "WINDOW" : "AISLE";

                boolean isBooked = isSeatInBookedList(fullSeatId, allBooked);
                if (isBooked) bookedInCoach++;

                SeatDto seat = SeatDto.builder()
                        .seatNumber(seatNum)
                        .fullSeatId(fullSeatId)
                        .coachNumber(coachNum)
                        .seatClass("FIRST")
                        .seatType(seatType)
                        .rowNumber(row)
                        .columnLetter(colLetter)
                        .isBooked(isBooked)
                        .bookedStatus(isBooked ? "BOOKED" : "AVAILABLE")
                        .build();
                coach.getSeats().add(seat);
            }
            coach.setBookedSeats(bookedInCoach);
            coach.setAvailableSeats(Math.max(0, seatsInCoach - bookedInCoach));
            coachList.add(coach);
        }

        // 2nd Class Coaches (32 seats each, 2+2 layout)
        int secondCoachCount = Math.min(4, Math.max(2, (secondTotal + 31) / 32));
        for (int c = 1; c <= secondCoachCount; c++) {
            int globalCoachIdx = firstCoachCount + c;
            String coachNum = String.format("Coach %02d", globalCoachIdx);
            String coachLabel = "Car B" + c;
            int seatsInCoach = 32;

            CoachDto coach = CoachDto.builder()
                    .coachId("COACH-" + String.format("%02d", globalCoachIdx))
                    .coachNumber(coachNum)
                    .coachLabel(coachLabel)
                    .coachClass("SECOND")
                    .classDisplayName("2nd Class Reserved")
                    .totalSeats(seatsInCoach)
                    .layoutType("2+2")
                    .seats(new ArrayList<>())
                    .build();

            int bookedInCoach = 0;
            for (int s = 1; s <= seatsInCoach; s++) {
                String seatNum = String.format("S%02d", s);
                String fullSeatId = coachNum + " / " + seatNum;
                int row = (s - 1) / 4 + 1;
                int colIdx = (s - 1) % 4;
                String colLetter = (colIdx == 0) ? "A" : (colIdx == 1 ? "B" : (colIdx == 2 ? "C" : "D"));
                String seatType = (colIdx == 0 || colIdx == 3) ? "WINDOW" : "AISLE";

                boolean isBooked = isSeatInBookedList(fullSeatId, allBooked);
                if (isBooked) bookedInCoach++;

                SeatDto seat = SeatDto.builder()
                        .seatNumber(seatNum)
                        .fullSeatId(fullSeatId)
                        .coachNumber(coachNum)
                        .seatClass("SECOND")
                        .seatType(seatType)
                        .rowNumber(row)
                        .columnLetter(colLetter)
                        .isBooked(isBooked)
                        .bookedStatus(isBooked ? "BOOKED" : "AVAILABLE")
                        .build();
                coach.getSeats().add(seat);
            }
            coach.setBookedSeats(bookedInCoach);
            coach.setAvailableSeats(Math.max(0, seatsInCoach - bookedInCoach));
            coachList.add(coach);
        }

        return coachList;
    }

    /**
     * Retrieves real-time seat availability, booked seat identifiers, and coach layouts for live polling.
     *
     * @param scheduleId schedule ID
     * @param travelDate date of travel
     * @return {@link SeatStatusResponseDto} payload
     */
    public SeatStatusResponseDto getSeatStatus(Long scheduleId, LocalDate travelDate) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        int availFirst = getAvailableSeats(scheduleId, travelDate, "FIRST");
        int availSecond = getAvailableSeats(scheduleId, travelDate, "SECOND");
        List<CoachDto> coaches = generateTrainCoaches(schedule, travelDate);
        List<String> bookedSeats = getBookedSeatNumbers(scheduleId, travelDate, "ALL");

        return SeatStatusResponseDto.builder()
                .scheduleId(scheduleId)
                .travelDate(travelDate)
                .availableFirst(availFirst)
                .availableSecond(availSecond)
                .bookedSeats(bookedSeats)
                .coaches(coaches)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Calculates pre-cancellation refund quote and eligibility summary for a passenger booking.
     *
     * @param bookingId booking ID
     * @param userId authenticated passenger ID (null for unrestricted admin)
     * @return {@link com.trainbooking.it25103308.dto.CancellationSummaryDto} cancellation quote details
     */
    public com.trainbooking.it25103308.dto.CancellationSummaryDto getCancellationSummary(Long bookingId, Long userId) {
        log.info("Calculating cancellation summary for booking ID: {}, User ID: {}", bookingId, userId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (userId != null && booking.getPassenger() != null && !booking.getPassenger().getId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not have permission to view or cancel this booking.");
        }

        com.trainbooking.it25100977.dto.RefundCalculationDto calc = paymentService.calculateRefund(booking);

        boolean alreadyCancelled = (booking.getStatus() == Booking.BookingStatus.CANCELLED);
        boolean canCancel = !alreadyCancelled && (booking.getStatus() == Booking.BookingStatus.CONFIRMED || booking.getStatus() == Booking.BookingStatus.PENDING);

        return com.trainbooking.it25103308.dto.CancellationSummaryDto.builder()
                .bookingId(booking.getId())
                .ticketNumber(booking.getTicketNumber())
                .passengerName(booking.getPassengerName())
                .trainName(booking.getTrainName())
                .origin(booking.getOrigin())
                .destination(booking.getDestination())
                .travelDate(booking.getTravelDate())
                .departureTime(booking.getDepartureTime())
                .seatNumbers(booking.getSeatNumbers())
                .seatClass(booking.getSeatClass() != null ? booking.getSeatClass().name() : "FIRST")
                .numberOfSeats(booking.getNumberOfSeats())
                .originalAmount(calc.getOriginalAmount())
                .refundAmount(calc.getRefundAmount())
                .cancellationFee(calc.getCancellationFee())
                .refundPercentage(calc.getRefundPercentage())
                .policyTierDescription(calc.getPolicyTierDescription())
                .eligibleForRefund(calc.isEligibleForRefund())
                .canCancel(canCancel)
                .build();
    }

    /**
     * Cancels an active or pending train booking, releases allocated seats back to inventory,
     * processes the financial refund via payment gateway, and dispatches automated notifications.
     *
     * @param bookingId booking ID to cancel
     * @param userId authenticated user ID (null for admin/system)
     * @param reason cancellation reason
     * @return processed {@link com.trainbooking.it25100977.model.Refund} entity or null if unpaid hold
     */
    @Transactional
    public com.trainbooking.it25100977.model.Refund cancelBooking(Long bookingId, Long userId, String reason) {
        log.info("Executing booking cancellation for ID: {}, Requested by User ID: {}, Reason: {}", bookingId, userId, reason);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (userId != null && booking.getPassenger() != null && !booking.getPassenger().getId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not have permission to cancel this booking.");
        }

        boolean wasConfirmed = (booking.getStatus() == Booking.BookingStatus.CONFIRMED);

        if (bookingStateFactory != null) {
            com.trainbooking.it25103308.state.BookingState state = bookingStateFactory.getState(booking);
            if (!state.canCancel()) {
                throw new IllegalStateException("Booking #" + bookingId + " has already been cancelled.");
            }
            state.cancel(booking);
        } else {
            if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
                throw new IllegalStateException("Booking #" + bookingId + " has already been cancelled.");
            }
            booking.setStatus(Booking.BookingStatus.CANCELLED);
        }

        bookingRepository.save(booking);
        log.info("Booking #{} status transitioned to CANCELLED. Seats {} restored to inventory.", bookingId, booking.getSeatNumbers());

        com.trainbooking.it25100977.model.Refund refund = null;
        if (wasConfirmed) {
            refund = paymentService.processRefund(bookingId, reason);
            notificationService.sendCancellationNotification(booking, refund);
        }

        return refund;
    }
}
