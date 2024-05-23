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
    public void addMeasurement(Measurement m) {
        measurements.add(m);
    }

    @Override
    public List<Measurement> readAllAndClean() {
        List<Measurement> result = new ArrayList<>(measurements);
        measurements.clear();
        return result;
    }
}
