package Player;

import MQTTHandler.MqttCallbackHandler;
import Utils.Coordinate;
import Utils.GameInfo;
import com.sun.jersey.api.client.ClientResponse;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Player {
    private List<AdministratorServer.beans.Player> players = new ArrayList<>();
    private Coordinate coordinate;
    private static final String BASE_URL = "http://localhost:1337/";
    private final String address = "localhost";
    private AdminServerModule adminServerModule = new AdminServerModule();

    public static void main(String[] args) {
        Player player = new Player();
        Thread inputThread = new Thread(player::handleStandardInput);
        inputThread.start();

        //Thread mqttThread = new Thread(player::handleMQTTConnection);
        //mqttThread.start();

    }

    private void handleStandardInput() {
        Scanner command = new Scanner(System.in);
        System.out.println("\nInsert your ID: ");
        String id = command.nextLine();
        System.out.println("Insert your port: ");
        String port = command.nextLine();
        AdministratorServer.beans.Player beanPlayer = new AdministratorServer.beans.Player(id, address, Integer.parseInt(port));

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
