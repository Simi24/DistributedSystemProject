package Player;

import AdministratorServer.beans.PlayerBean;
import Utils.Coordinate;

import java.util.Objects;

public class SeekerModule {
    NetworkTopologyModule networkTopologyModule;

    private SeekerModule instance = null;

    private SeekerModule(){
        networkTopologyModule = NetworkTopologyModule.getInstance();
    }

    public SeekerModule getInstance(){
        if(instance == null){
            instance = new SeekerModule();
        }
        return instance;
    }




}
