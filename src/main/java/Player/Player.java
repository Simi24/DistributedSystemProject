package Player;

import MQTTHandler.MqttCallbackHandler;
import Utils.Coordinate;
import Utils.GameInfo;
import com.sun.jersey.api.client.ClientResponse;
import gRPC.GreetingsServiceImp;
import io.grpc.ServerBuilder;
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
    private static List<AdministratorServer.beans.Player> players = new ArrayList<>();
    private static HashMap<AdministratorServer.beans.Player, Coordinate> playerCoordinateMap = new HashMap<>();
    private static Coordinate coordinate;
    private static final String BASE_URL = "http://localhost:1337/";
    private final String address = "localhost";
    private static AdminServerModule adminServerModule;
    private String id;
    private static String port;
    private static AdministratorServer.beans.Player beanPlayer;

    private static Boolean isSeeker = false;

    public static void main(String[] args) throws InterruptedException {
        Player player = new Player();
        NetworkTopologyModule networkTopologyModule = NetworkTopologyModule.getInstance();
        adminServerModule = new AdminServerModule();
        Thread inputThread = new Thread(player::handleStandardInput);
        inputThread.start();

        // Wait for the inputThread to finish
        try {
            inputThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //TODO: Start sending HR data to AdminServer
        adminServerModule.sendHRData();

        Thread serverThread = new Thread(() -> {
            try {
                io.grpc.Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(new GreetingsServiceImp()).build();
                server.start();
                System.out.println("Server started for gRPC at port: " + port);
                server.awaitTermination();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        serverThread.start();

        System.out.println("qui arrivo?");

        if(!players.isEmpty()){
            for (AdministratorServer.beans.Player player1 : players) {
                playerCoordinateMap.put(player1, new Coordinate(0, 0)); // replace with actual coordinates
            }
        }

        System.out.println("e qui?");

        networkTopologyModule.setPlayerCoordinateMap(playerCoordinateMap);

        networkTopologyModule.sendPlayerCoordinates(beanPlayer, coordinate);

        Thread mqttThread = new Thread(player::handleMQTTConnection);
        mqttThread.start();

    }

    private void handleStandardInput() {
        Scanner command = new Scanner(System.in);
        System.out.println("\nInsert your ID: ");
        id = command.nextLine();
        System.out.println("Insert your port: ");
        port = command.nextLine();
        beanPlayer = new AdministratorServer.beans.Player(id, address, Integer.parseInt(port));

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
}
