package Utils;

import Simulators.Measurement;
import Simulators.Simulator;

import java.util.ArrayList;
import java.util.List;

public class Buffer implements Simulators.Buffer {
    private List<Measurement> measurements;

    public Buffer() {
        this.measurements = new ArrayList<>();
    }

    @Override
    public synchronized void addMeasurement(Measurement m) {
        measurements.add(m);
        if(measurements.size() == 8){
            notify();
        }
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        while (measurements.size() < 8) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<Measurement> result = new ArrayList<>(measurements);
        measurements.clear();
        return result;
    }
}
