package AdministratorServer.beans;

import Utils.Coordinate;
import Utils.GameInfo;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Players {

    @XmlElement(name="players")
    private List<Player> players;

    private static Players instance;

    //region init
    private Players() {
        players = new ArrayList<Player>();
    }

    public synchronized static Players getInstance(){
        if(instance==null) {
            instance = new Players();
            instance.initWithFakePlayers();
        }
        return instance;
    }
    //endregion

    //region REST methods

    //Add player and return starting position and list of other players
    public synchronized @Nullable GameInfo add(Player p){
        System.out.printf("Adding player %s\n", p.getId());
        for (Player player: players){
            if(player.getId().equals(p.getId())){
                return null;
            }
        }
        System.out.printf("Player %s added\n", p.getId());
        List<Player> returnList = new ArrayList<>(players);
        players.add(p);
        return new GameInfo(getStartingPosition(), returnList);
    }

    //The list of the players currently in the game.
    public synchronized List<Player> getPlayers(){
        return players;
    }

    //endregion

    //region utils
    private Coordinate getStartingPosition(){
        //TODO: implement the starting position
        return new Coordinate(0, 0);
    }

    public synchronized void initWithFakePlayers() {
        players = new ArrayList<>(Arrays.asList(
                new Player("AAA", "localhost", 8080),
                new Player("BBB", "localhost", 8081),
                new Player("CCC", "localhost", 8082),
                new Player("DDD", "localhost", 8083)
        ));
    }

    //endregion
}
