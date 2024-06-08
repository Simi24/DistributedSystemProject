package AdministratorServer.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class ClientMesuramentAverage {
    private String clientID;
    private ArrayList<Double> mesuraments;

    private long timestamp;

    public ClientMesuramentAverage(){}

    public ClientMesuramentAverage(String clientID, ArrayList<Double> mesuraments, long timestamp) {
        this.clientID = clientID;
        this.mesuraments = mesuraments;
        this.timestamp = timestamp;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public ArrayList<Double> getMesuraments() {
        return mesuraments;
    }

    public void setMesuraments(ArrayList<Double> mesuraments) {
        this.mesuraments = mesuraments;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
