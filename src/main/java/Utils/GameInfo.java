package Utils;

import AdministratorServer.beans.Player;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

public class GameInfo {
    Coordinate coordinate;
    List<Player> players;

    public GameInfo() {
    }

    public GameInfo(Coordinate coordinate, List<Player> players) {
        this.coordinate = coordinate;
        this.players = players;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }
}
