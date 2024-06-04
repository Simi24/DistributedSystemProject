package AdministratorServer.services;

import AdministratorServer.beans.PlayerBean;
import AdministratorServer.beans.Players;
import Utils.GameInfo;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("players")
public class PlayersServices {

    // Enter new player
    @Path("add")
    @POST
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    public Response addPlayer(PlayerBean p){
        GameInfo gameInfo = Players.getInstance().add(p);
        if(gameInfo != null){
            return Response.ok(gameInfo).build();
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }
}
