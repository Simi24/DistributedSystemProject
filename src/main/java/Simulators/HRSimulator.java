package Simulators;

public class HRSimulator extends Simulator {

    private final int mean = 100;
    private final int variance = 20;
    private static int ID = 1;

    public HRSimulator(String id, Buffer buffer){
        super(id, "HR", buffer);
    }

    //Use this constructor to initialize the HR simulator in your project
    public HRSimulator(Buffer buffer){
        this("HR-"+(ID++), buffer);
    }

    @Override
    public void run() {

        double i = rnd.nextInt();
        long waitingTime;

        while(!stopCondition){

            double hr = getHRValue(i);
            addMeasurement(hr);

            waitingTime = 2000;
            sensorSleep(waitingTime);

            i+=0.2;

        }

    }

    private double getHRValue(double t){
        double gaussian = rnd.nextGaussian();
        return mean + Math.sqrt(variance) * gaussian;
    }
}
