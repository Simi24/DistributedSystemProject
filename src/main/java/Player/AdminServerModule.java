package Player;

import AdministratorServer.beans.PlayerBean;
import Utils.GameInfo;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class AdminServerModule {
    private static final String BASE_URL = "http://localhost:1337/";
    private Client client;

    private HRSensorModule hrSensorModule;

    public AdminServerModule() {
        this.client = Client.create();
        hrSensorModule = new HRSensorModule();
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

    public boolean sendHRData() {
        hrSensorModule.startSensor();
        hrSensorModule.getBuffer();
        //TODO: methods to send hr value to AdminServer
        return true;
    }

}