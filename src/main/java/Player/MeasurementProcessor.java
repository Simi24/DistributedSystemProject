package Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import Simulators.Measurement;
import Utils.Buffer;

public class MeasurementProcessor {
    private static final int WINDOW_SIZE = 8;
    private static final int OVERLAP = 4;
    private static final int SEND_INTERVAL = 10000; // 10 seconds in milliseconds

    private Buffer buffer;
    private List<Double> averages = new ArrayList<>();
    private AdminServerModule adminServerModule;
    private Player player;
    private HRSensorModule hrSensorModule;

    public void setPlayer(Player player) {
        this.player = player;
    }

    public MeasurementProcessor(AdminServerModule adminServerModule, HRSensorModule hrSensorModule, Buffer buffer) {
        this.adminServerModule = adminServerModule;
        this.buffer = buffer;
        this.hrSensorModule = hrSensorModule;

        // Schedule the task to send averages every 10 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendAverages();
            }
        }, SEND_INTERVAL, SEND_INTERVAL);
    }

    public void processMeasurements() {
        while (hrSensorModule.isSenorAlive()) {
            List<Measurement> measurements = buffer.readAllAndClean();

            if (measurements.size() >= WINDOW_SIZE) {
                double average = measurements.stream().mapToDouble(Measurement::getValue).average().orElse(0.0);
                averages.add(average);

                // Remove the oldest measurements
                for (int i = 0; i < OVERLAP; i++) {
                    measurements.remove(0);
                }
            }
        }
    }

    private void sendAverages() {
        if (!averages.isEmpty()) {
            adminServerModule.sendHRData(player.getId(), System.currentTimeMillis(), new ArrayList<>(averages));
            averages.clear();
        }
    }
}
