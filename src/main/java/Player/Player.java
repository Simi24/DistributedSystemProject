package Player;

import AdministratorServer.beans.PlayerBean;
import MQTTHandler.MqttCallbackHandler;
import Utils.Coordinate;
import Utils.GameInfo;
import gRPC.gRPCPlayerServer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Player {
    private static List<PlayerBean> players = new ArrayList<>();
    private final HashMap<PlayerBean, Coordinate> playerCoordinateMap = new HashMap<>();
    private Coordinate coordinate;
    private static final String BASE_URL = "http://localhost:1337/";
    private final String address = "localhost";
    private static AdminServerModule adminServerModule;
    private String id;
    private String port;
    private PlayerBean beanPlayer;

    private static Boolean isSeeker = false;

    public static void main(String[] args) throws InterruptedException, IOException {
        Player player = new Player();

        adminServerModule = new AdminServerModule();
        Thread inputThread = new Thread(player::handleStandardInput);
        inputThread.start();

        // Wait for the inputThread to finish
        try {
            inputThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start the gRPC server
        gRPCPlayerServer gRPCPlayerServer = new gRPCPlayerServer(Integer.parseInt(player.port));
        gRPCPlayerServer.start();

        //TODO: Start sending HR data to AdminServer
        adminServerModule.sendHRData();

        player.handleNetworkTopologyModule();

        Thread mqttThread = new Thread(player::handleMQTTConnection);
        mqttThread.start();

    }

    private void handleNetworkTopologyModule() throws InterruptedException {
        if(!players.isEmpty()){
            for (PlayerBean player1 : players) {
                playerCoordinateMap.put(player1, new Coordinate(0, 0)); // replace with actual coordinates
            }
        }

        NetworkTopologyModule networkTopologyModule = NetworkTopologyModule.getInstance();
        networkTopologyModule.setPlayerCoordinateMap(playerCoordinateMap);
        networkTopologyModule.setCurrentPlayer(this);
        networkTopologyModule.sendPlayerCoordinates(beanPlayer, coordinate);
    }

    private void handleStandardInput() {
        Scanner command = new Scanner(System.in);
        System.out.println("\nInsert your ID: ");
        id = command.nextLine();
        System.out.println("Insert your port: ");
        port = command.nextLine();
        beanPlayer = new PlayerBean(id, address, Integer.parseInt(port));

        GameInfo responseBody = adminServerModule.addPlayer(beanPlayer);
        players = responseBody.getPlayers();
        coordinate = responseBody.getCoordinate();
    }

    private void handleMQTTConnection(){
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        String topic = "WatchOut/#";
        int qos = 2;
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            // Connect the client
            System.out.println(clientId + " Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + " Connected - Thread PID: " + Thread.currentThread().getId());

            // Set callback
            MqttCallbackHandler callbackHandler = new MqttCallbackHandler(clientId);
            client.setCallback(callbackHandler);

            // Subscribe
            System.out.println(clientId + " Subscribing ... - Thread PID: " + Thread.currentThread().getId());
            client.subscribe(topic, qos);
            System.out.println(clientId + " Subscribed to topics : " + topic);

            System.out.println("\n ***  Press a random key to exit *** \n");
            Scanner command = new Scanner(System.in);
            command.nextLine();
            client.disconnect();

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    //region getters

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public String getAddress() {
        return address;
    }

    public String getId() {
        return id;
    }

    public String getPort() {
        return port;
    }

    public static Boolean getIsSeeker() {
        return isSeeker;
    }

    //endregion

    //region setters

    public static void setIsSeeker(Boolean isSeeker) {
        Player.isSeeker = isSeeker;
    }

    //endregion
}
