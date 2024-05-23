package Player;

import Simulators.HRSimulator;
import Utils.Buffer;

public class HRSensorModule {
    private HRSimulator hrSimulator;
    private Buffer buffer;

    public HRSensorModule() {
        this.buffer = new Buffer();
        this.hrSimulator = new HRSimulator(buffer);
    }

    public void startSensor() {
        if (!hrSimulator.isAlive()) {
            hrSimulator.start();
        }
    }

    public void stopSensor() {
        hrSimulator.stopMeGently();
    }

    public Buffer getBuffer() {
        return buffer;
    }
}
