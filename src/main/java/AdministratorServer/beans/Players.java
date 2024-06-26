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
import java.util.Random;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Players {

    @XmlElement(name="players")
    private List<PlayerBean> players;

    private static Players instance;

    //region init
    private Players() {
        players = new ArrayList<PlayerBean>();
    }

    public synchronized static Players getInstance(){
        if(instance==null) {
            instance = new Players();
//            instance.initWithFakePlayers();
        }
        return instance;
    }
    //endregion

    //region REST methods

    //Add player and return starting position and list of other players
    public synchronized @Nullable GameInfo add(PlayerBean p){
        System.out.printf("Adding player %s\n", p.getId());
        for (PlayerBean player: players){
            if(player.getId().equals(p.getId())){
                return null;
            }
        }
        System.out.printf("Player %s added\n", p.getId());
        List<PlayerBean> returnList = new ArrayList<>(players);
        players.add(p);
        return new GameInfo(getStartingPosition(), returnList);
    }

    //The list of the players currently in the game.
    public synchronized List<PlayerBean> getPlayers(){
        return players;
    }

    //endregion

    //region utils
    private Coordinate getStartingPosition(){
        Random rand = new Random();
        int edge = rand.nextInt(4);
        int position = rand.nextInt(10);

        switch (edge) {
            case 0: // Top edge
                return new Coordinate(0, position);
            case 1: // Right edge
                return new Coordinate(position, 9);
            case 2: // Bottom edge
                return new Coordinate(9, position);
            case 3: // Left edge
                return new Coordinate(position, 0);
            default:
                return null; // This should never happen
        }
    }

    public synchronized void initWithFakePlayers() {
        players = new ArrayList<>(Arrays.asList(
                new PlayerBean("AAA", "localhost", 8080),
                new PlayerBean("BBB", "localhost", 8081),
                new PlayerBean("CCC", "localhost", 8082),
                new PlayerBean("DDD", "localhost", 8083)
        ));
    }

    //endregion
}
