package AdministratorServer.services;

import AdministratorServer.beans.HeartRateDataStore;
import AdministratorServer.beans.PlayerBean;
import AdministratorServer.beans.Players;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("adminClientService")
public class AdminClientService {

    //Get players in game
    @Path("getPlayers")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getPlayers(){
        List<PlayerBean> players = Players.getInstance().getPlayers();
        return Response.ok(players).build();
    }

    @Path("/averageBetweenTimestamps/{playerId}/{n}")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getPlayerAverage(@PathParam("playerId") String playerId, @PathParam("n") int n) {
        float average = HeartRateDataStore.getInstance().getAverageHeartRate(playerId, n);
        return Response.ok(average).build();
    }

    @Path("/average/{t1}/{t2}")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getAverageBetweenTimestamps(@PathParam("t1") long t1, @PathParam("t2") long t2) {
        float average = HeartRateDataStore.getInstance().calculateAverageBetweenTimestamps(t1, t2);
        return Response.ok(average).build();
    }
}
