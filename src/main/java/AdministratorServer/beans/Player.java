package AdministratorServer.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Player {
    //Player data
    private String id;

    private String address;

    private int portNumber;

    public Player(){}

    public Player(String id, String address, int portNumber) {
        this.id = id;
        this.address = address;
        this.portNumber = portNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }
}
