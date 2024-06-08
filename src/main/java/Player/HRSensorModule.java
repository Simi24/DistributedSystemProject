package Player;

import Simulators.HRSimulator;
import Utils.Buffer;

public class HRSensorModule {
    private HRSimulator hrSimulator;
    private Buffer buffer;
    private MeasurementProcessor measurementProcessor;
    AdminServerModule adminServerModule;
    Player player;

    public HRSensorModule(AdminServerModule adminServerModule){
        this.adminServerModule = adminServerModule;
        this.buffer = new Buffer();
        this.hrSimulator = new HRSimulator(buffer);
        this.measurementProcessor = new MeasurementProcessor(adminServerModule, this, buffer);
    }

    public void setPlayer(Player player) {
        this.player = player;
        measurementProcessor.setPlayer(player);
    }

    public void startSensor() {
        if (!hrSimulator.isAlive()) {
            hrSimulator.start();
            new Thread(measurementProcessor::processMeasurements).start();
        }
    }

    public void stopSensor() {
        hrSimulator.stopMeGently();
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public boolean isSenorAlive() {
        return hrSimulator.isAlive();
    }
}
