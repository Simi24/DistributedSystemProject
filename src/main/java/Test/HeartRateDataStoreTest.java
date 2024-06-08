package Test;

import AdministratorServer.beans.ClientMesuramentAverage;
import AdministratorServer.beans.HeartRateDataStore;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HeartRateDataStoreTest {

//    private HeartRateDataStore heartRateDataStore;
//
//    @Before
//    public void setUp() {
//        heartRateDataStore = HeartRateDataStore.getInstance();
//
//        List<ClientMesuramentAverage> testData = Arrays.asList(
//                new ClientMesuramentAverage("player1", new ArrayList<>(Arrays.asList(60.0, 70.0, 80.0)), 1000),
//                new ClientMesuramentAverage("player2", new ArrayList<>(Arrays.asList(65.0, 75.0)), 2000),
//                new ClientMesuramentAverage("player3", new ArrayList<Double>(), 3000)
//        );
//
//        HeartRateDataStore.initWithTestData(testData);
//    }
//
//    @Test
//    public void testCalculateAverageBetweenTimestamps_MatchingTimestamps() {
//        float average = heartRateDataStore.calculateAverageBetweenTimestamps(500, 2500);
//        assertEquals(70, average, 0);
//    }
//
//    @Test
//    public void testCalculateAverageBetweenTimestamps_NoMatchingTimestamps() {
//        heartRateDataStore.addClientMeasurementAverage(new ClientMesuramentAverage("player1", Arrays.asList(60f, 70f, 80f), 3000));
//        float average = heartRateDataStore.calculateAverageBetweenTimestamps(100, 200);
//        assertEquals(0, average, 0);
//    }
//
//    @Test
//    public void testGetAverageHeartRate_ValidPlayerID() {
//        heartRateDataStore.addClientMeasurementAverage(new ClientMesuramentAverage("player1", Arrays.asList(60f, 70f, 80f), 1000));
//        float average = heartRateDataStore.getAverageHeartRate("player1", 1);
//        assertEquals(70, average, 0);
//    }
//
//    @Test
//    public void testGetAverageHeartRate_ValidPlayerID_NoMeasurements() {
//        float average = heartRateDataStore.getAverageHeartRate("player3", 1);
//        assertEquals(0, average, 0);
//    }
//
//    @Test
//    public void testGetAverageHeartRate_InvalidPlayerID() {
//        heartRateDataStore.addClientMeasurementAverage(new ClientMesuramentAverage("player1", Arrays.asList(60f, 70f, 80f), 1000));
//        float average = heartRateDataStore.getAverageHeartRate("nonexistent", 1);
//        assertEquals(0, average, 0);
//    }
}