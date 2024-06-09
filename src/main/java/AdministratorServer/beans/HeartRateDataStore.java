package AdministratorServer.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class HeartRateDataStore {

    @XmlElement(name="heartRate")
    private final List<ClientMesuramentAverage> clientAverages;

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

    public boolean addClientMeasurementAverage(ClientMesuramentAverage clientAverage) {
        synchronized (clientAverages) {
            clientAverages.add(clientAverage);
        }
        return true;
    }

    public double calculateAverageBetweenTimestamps(double t1, double t2) {
        List<ClientMesuramentAverage> elementsToCompute;
        synchronized (clientAverages) {
            elementsToCompute = clientAverages.stream()
                    .filter(clientAverage -> clientAverage.getTimestamp() >= t1 && clientAverage.getTimestamp() <= t2)
                    .collect(Collectors.toList());
        }

        double sum = 0;
        int count = 0;
        for (ClientMesuramentAverage clientAverage : elementsToCompute) {
            sum += calculateAverage(clientAverage.getMesuraments());
            count++;
        }
        return count > 0 ? sum / count : 0 ; // Return 0 if no measurements were found and avoid division by 0
    }

    public synchronized double getAverageHeartRate(String playerId, int n) {
        List<ClientMesuramentAverage> playerMeasurements;
        synchronized (clientAverages) {
            playerMeasurements = clientAverages.stream()
                    .filter(m -> m.getClientID().equals(playerId))
                    .sorted(Comparator.comparingLong(ClientMesuramentAverage::getTimestamp).reversed())
                    .limit(n)
                    .collect(Collectors.toList());
        }

        double sum = 0;
        for (ClientMesuramentAverage measurement : playerMeasurements) {
            sum += calculateAverage(measurement.getMesuraments());
        }
        return playerMeasurements.isEmpty() ? 0 : sum / playerMeasurements.size();
    }

    //endregion

    //region utils
    private double calculateAverage(ArrayList<Double> measurements) {
        if (measurements.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (double measurement : measurements) {
            sum += measurement;
        }
        return sum / measurements.size();
    }

    public static synchronized void initWithTestData(List<ClientMesuramentAverage> testData) {
        for (ClientMesuramentAverage clientAverage : testData) {
            getInstance().addClientMeasurementAverage(clientAverage);
        }
    }

    //endregion

}
