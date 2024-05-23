package AdministratorServer.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ClientMesuramentAverage {
    private String clientID;
    private List<Float> mesuraments;

    private long timestamp;

    ClientMesuramentAverage(){}

    public ClientMesuramentAverage(String clientID, List<Float> mesuraments, long timestamp) {
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

    public List<Float> getMesuraments() {
        return mesuraments;
    }

    public void setMesuraments(List<Float> mesuraments) {
        this.mesuraments = mesuraments;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
