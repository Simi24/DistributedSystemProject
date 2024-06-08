package Player;

import AdministratorServer.beans.PlayerBean;
import Utils.Coordinate;
import Utils.GameInfo;
import gRPC.gRPCPlayerServer;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.sql.Timestamp;
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
    private final NetworkTopologyModule networkTopologyModule = NetworkTopologyModule.getInstance();

    private static Boolean isSeeker = false;

    public final Object lock = new Object();

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
        player.handleHRValues();

        player.handleNetworkTopologyModule();

        Thread mqttThread = new Thread(player::handleMQTTConnection);
        mqttThread.start();

        player.handleAccessToBase();

    }

    private void handleHRValues(){
        adminServerModule.setPlayer(this);
        adminServerModule.startSensor();
    }

    private void handleNetworkTopologyModule() throws InterruptedException {
        playerCoordinateMap.put(beanPlayer, coordinate);
        if(!players.isEmpty()){
            for (PlayerBean player1 : players) {
                playerCoordinateMap.put(player1, new Coordinate(0, 0));
            }
        }

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
        System.out.println("My DISTANCE from the base is: " + networkTopologyModule.calculateDistanceToBase(coordinate));
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
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println(clientId + " Connection lost! Cause: " + throwable.getMessage() + " - Thread PID: " + Thread.currentThread().getId());
                    throwable.printStackTrace();
                }

                @Override
                public void messageArrived(String s, MqttMessage message) throws Exception {
                    String time = new Timestamp(System.currentTimeMillis()).toString();
                    String receivedMessage = new String(message.getPayload());

                    if(receivedMessage.equals("start")){
                        networkTopologyModule.startElection(beanPlayer, coordinate);
                    }

                    System.out.println(clientId + " Received a Message! - Callback - Thread PID: " + Thread.currentThread().getId() +
                            "\n\tTime:    " + time +
                            "\n\tTopic:   " + topic +
                            "\n\tMessage: " + receivedMessage +
                            "\n\tQoS:     " + message.getQos() + "\n");

                    System.out.println("\n ***  Press a random key to exit *** \n");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });

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

    private void handleAccessToBase() throws InterruptedException {
        synchronized (lock) {
            try {
                lock.wait();
                if(!isSeeker){
                    networkTopologyModule.askForAccessToBase(beanPlayer);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
