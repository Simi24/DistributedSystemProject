package AdministratorServer.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class HeartRateDataStore {

    @XmlElement(name="heartRate")
    private List<ClientMesuramentAverage> clientAverages;

    static HeartRateDataStore instance = null;

    //region init

    private HeartRateDataStore() {
        clientAverages = new ArrayList<>();
    }

    public static synchronized HeartRateDataStore getInstance() {
        if (instance == null) {
            instance = new HeartRateDataStore();
        }
        return instance;
    }

    //endregion

    //region REST methods

    public synchronized boolean addClientMeasurementAverage(ClientMesuramentAverage clientAverage) {
        try{
            clientAverages.add(clientAverage);
            return true;
        }catch (Exception e){
            System.err.println("Error during insertion: " + e.getMessage());
            return false;
        }
    }

    public synchronized float calculateAverageBetweenTimestamps(long t1, long t2) {
        float sum = 0;
        int count = 0;
        for (ClientMesuramentAverage clientAverage : clientAverages) {
            if (clientAverage.getTimestamp() >= t1 && clientAverage.getTimestamp() <= t2) {
                sum += calculateAverage(clientAverage.getMesuraments());
                count++;
            }
        }
        return count > 0 ? sum / count : 0; // Avoid division by zero
    }

    public synchronized float getAverageHeartRate(String playerId, int n) {
        // Filter measurements for the given playerId
        List<ClientMesuramentAverage> playerMeasurements = clientAverages.stream()
                .filter(m -> m.getClientID().equals(playerId))
                .sorted(Comparator.comparingLong(ClientMesuramentAverage::getTimestamp).reversed())
                .limit(n)
                .collect(Collectors.toList());

        // Calculate the average of these measurements
        float sum = 0;
        for (ClientMesuramentAverage measurement : playerMeasurements) {
            sum += calculateAverage(measurement.getMesuraments());
        }
        return playerMeasurements.isEmpty() ? 0 : sum / playerMeasurements.size();
    }

    //endregion

    //region utils
    private float calculateAverage(List<Float> measurements) {
        if (measurements.isEmpty()) {
            return 0;
        }
        float sum = 0;
        for (float measurement : measurements) {
            sum += measurement;
        }
        return sum / measurements.size();
    }

    public static synchronized void initWithTestData(List<ClientMesuramentAverage> testData) {
        getInstance().clientAverages = new ArrayList<>(testData);
    }

    //endregion

}
