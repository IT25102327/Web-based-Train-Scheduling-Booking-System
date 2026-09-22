package com.trainbooking.it25102925;

import com.trainbooking.it25102327.model.Route;
import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.RouteRepository;
import com.trainbooking.it25102327.repository.TrainRepository;
import com.trainbooking.it25102925.dto.TrainLocationDto;
import com.trainbooking.it25102925.service.NotificationService;
import com.trainbooking.it25102925.service.TrainLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test suite for {@link TrainLocationService}.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TrainLocationServiceTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TrainLocationService trainLocationService;

    private Train sampleTrain;
    private Route sampleRoute;

    @BeforeEach
    void setUp() {
        sampleTrain = Train.builder()
                .id(1L)
                .trainNumber("1015")
                .trainName("Udarata Menike")
                .status(Train.TrainStatus.ON_TIME)
                .build();

        sampleRoute = Route.builder()
                .id(1L)
                .origin("Colombo Fort")
                .destination("Kandy")
                .train(sampleTrain)
                .build();
    }

    @Test
    @DisplayName("Should successfully ingest and update train GPS coordinates and speed")
    void updateLocation_Success() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(sampleTrain));
        when(routeRepository.findAll()).thenReturn(List.of(sampleRoute));

        TrainLocationDto result = trainLocationService.updateLocation(
                1L, 7.0917, 79.9997, 65.5, "ON_TIME", "Gampaha", "Veyangoda"
        );

        assertNotNull(result);
        assertEquals(1L, result.getTrainId());
        assertEquals("1015", result.getTrainNumber());
        assertEquals("Udarata Menike", result.getTrainName());
        assertEquals(7.0917, result.getLatitude());
        assertEquals(79.9997, result.getLongitude());
        assertEquals(65.5, result.getSpeed());
        assertEquals("ON_TIME", result.getStatus());
        assertEquals("Gampaha", result.getCurrentStation());
        assertEquals("Veyangoda", result.getNextStation());
        assertEquals("Colombo Fort ➔ Kandy", result.getRoute());
    }

    @Test
    @DisplayName("Should update Train status to DELAYED and trigger passenger notifications")
    void updateLocation_StatusChange_TriggersAlert() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(sampleTrain));
        when(routeRepository.findAll()).thenReturn(List.of(sampleRoute));

        TrainLocationDto result = trainLocationService.updateLocation(
                1L, 7.3325, 80.2975, 20.0, "DELAYED", "Polgahawela", "Rambukkana"
        );

        assertNotNull(result);
        assertEquals("DELAYED", result.getStatus());
        assertEquals(Train.TrainStatus.DELAYED, sampleTrain.getStatus());
        verify(trainRepository, times(1)).save(sampleTrain);
        verify(notificationService, times(1)).notifyAffectedPassengers(1L, "DELAYED");
    }

    @Test
    @DisplayName("Should return null if trainId is not found in database")
    void updateLocation_TrainNotFound() {
        when(trainRepository.findById(999L)).thenReturn(Optional.empty());

        TrainLocationDto result = trainLocationService.updateLocation(
                999L, 6.9344, 79.8500, 50.0, "ON_TIME", "Colombo Fort", "Ragama"
        );

        assertNull(result);
    }

    @Test
    @DisplayName("Should return all locations and initialize defaults for registered trains")
    void getAllLocations_Success() {
        when(trainRepository.findAll()).thenReturn(List.of(sampleTrain));
        when(routeRepository.findAll()).thenReturn(List.of(sampleRoute));

        List<TrainLocationDto> locations = trainLocationService.getAllLocations();

        assertNotNull(locations);
        assertFalse(locations.isEmpty());
        assertEquals(1, locations.size());
        assertEquals("1015", locations.get(0).getTrainNumber());
    }

    @Test
    @DisplayName("Should support AT_TERMINAL telemetry status and custom route name")
    void updateLocation_AtTerminal_Success() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(sampleTrain));

        TrainLocationDto result = trainLocationService.updateLocation(
                1L, 7.2906, 80.6337, 0.0, "AT_TERMINAL", "Kandy Terminal", "Peradeniya",
                "RETURN", "Kandy ➔ Colombo Fort (Return)", 100
        );

        assertNotNull(result);
        assertEquals("AT_TERMINAL", result.getStatus());
        assertEquals(0.0, result.getSpeed());
        assertEquals("RETURN", result.getTripDirection());
        assertEquals("Kandy ➔ Colombo Fort (Return)", result.getRoute());
        assertEquals(100, result.getProgressPercentage());
    }
}
