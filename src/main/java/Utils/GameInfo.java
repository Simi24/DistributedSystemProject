package Utils;

import AdministratorServer.beans.PlayerBean;

import java.util.List;

public class GameInfo {
    Coordinate coordinate;
    List<PlayerBean> players;

    public GameInfo() {
    }

    public GameInfo(Coordinate coordinate, List<PlayerBean> players) {
        this.coordinate = coordinate;
        this.players = players;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public List<PlayerBean> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerBean> players) {
        this.players = players;
    }
}
