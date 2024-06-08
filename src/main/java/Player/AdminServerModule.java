package Player;

import AdministratorServer.beans.ClientMesuramentAverage;
import AdministratorServer.beans.PlayerBean;
import Utils.GameInfo;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.Response;
import java.util.ArrayList;

public class AdminServerModule {
    private static final String BASE_URL = "http://localhost:1337/";
    private Client client;

    private HRSensorModule hrSensorModule;

    private Player player;

    public AdminServerModule() {
        this.client = Client.create();
        hrSensorModule = new HRSensorModule(this);
    }

    public void setPlayer(Player player) {
        this.player = player;
        hrSensorModule.setPlayer(player);
    }

    public GameInfo addPlayer(PlayerBean p) {
        String url = "players/add";
        return handleResponse(postRequest(url, p));
    }

    public GameInfo handleResponse(ClientResponse clientResponse) {
        String json = clientResponse.getEntity(String.class);
        System.out.println(json);

        Gson gson = new Gson();
        GameInfo responseBody = gson.fromJson(json, GameInfo.class);
        System.out.println(responseBody);

        return responseBody;
    }

    public ClientResponse postRequest(String url, PlayerBean p){
        WebResource webResource = client.resource(BASE_URL + url);
        String input = new Gson().toJson(p);
        System.out.println(input);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server not available");
            return null;
        }
    }

    public void sendHRData(String playerId, long timestamp, ArrayList<Double> hrValues) {
        ClientMesuramentAverage clientAverage = new ClientMesuramentAverage(playerId, hrValues, timestamp);
        WebResource webResource = client.resource(BASE_URL + "heartRate/statistics");
        String json = new Gson().toJson(clientAverage);

        ClientResponse response = webResource.type("application/json")
                .post(ClientResponse.class, json);

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {

        } else {
            System.err.println("Failed to send averages: " + response.getStatus());
        }
    }

    public void startSensor() {
        hrSensorModule.startSensor();
    }

}