package com.trainbooking.it25102925;

import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.TrainRepository;
import com.trainbooking.it25102925.service.GpsSimulationService;
import com.trainbooking.it25102925.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for automated GPS simulation and milestone tracking engine.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class GpsSimulationTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GpsSimulationService gpsSimulationService;

    private Train sampleTrain;

    @BeforeEach
    void setUp() {
        sampleTrain = Train.builder()
                .id(1L)
                .trainNumber("1005")
                .trainName("Podi Menike")
                .status(Train.TrainStatus.ON_TIME)
                .build();
    }

    @Test
    @DisplayName("Should detect deviation and trigger notifications when simulateDeviation is called")
    void testSimulateDeviation_Success() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(sampleTrain));

        gpsSimulationService.simulateDeviation(1L, 25, "Rockfall clearance near Rambukkana");

        assertEquals(Train.TrainStatus.DELAYED, sampleTrain.getStatus());
        verify(trainRepository, times(1)).save(sampleTrain);
        verify(notificationService, times(1)).notifyAffectedPassengers(
                eq(1L),
                contains("DELAYED by 25 mins")
        );
    }

    @Test
    @DisplayName("Should respect simulation active flag toggle")
    void testToggleSimulation() {
        gpsSimulationService.setSimulationActive(false);
        assertFalse(gpsSimulationService.isSimulationActive());

        gpsSimulationService.runGpsTrackingCycle();
        verify(trainRepository, never()).findAll();

        gpsSimulationService.setSimulationActive(true);
        assertTrue(gpsSimulationService.isSimulationActive());
    }
}
