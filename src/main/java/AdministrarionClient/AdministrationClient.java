package AdministrarionClient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Scanner;

public class AdministrationClient {
    private final MqttClient client;
    private final String clientId = MqttClient.generateClientId();
    private int qos = 2;

    private static final String BASE_URL = "http://localhost:1337/adminClientService";

    public AdministrationClient() throws MqttException {
        String broker = "tcp://localhost:1883";
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient(broker, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        System.out.println(clientId + " Connecting Broker " + broker);
        client.connect(connOpts);
        System.out.println(clientId + " Connected");
    }

    public void publishGameStatus(boolean gameStarted) throws JSONException, MqttException {
        publishMessage("WatchOut/status", gameStarted ? "start" : "stop");
    }

    public void publishCustomMessage(String customMessage) throws MqttException {
        publishMessage("WatchOut/customMessage", customMessage);
    }

    public void publishMessageToCleanRetained() throws MqttException {
        publishMessage("WatchOut/status", "");
    }

    private void publishMessage(String topic, String payload) throws MqttException {
        MqttMessage message = payload.isEmpty() ? new MqttMessage(new byte[0]) : new MqttMessage(payload.getBytes());
        message.setQos(qos);
        message.setRetained(payload.equals("start") || payload.isEmpty());
        System.out.println(clientId + " Publishing message: " + payload + " ...");
        client.publish(topic, message);
        System.out.println(clientId + " Message published");
    }

    public void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
            System.out.println("Publisher " + clientId + " disconnected");
        }
    }

    public static ClientResponse getRequest(Client client, String url){
        WebResource webResource = client.resource(url);
        try {
            return webResource.type("application/json").get(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Server not available");
            return null;
        }
    }

    public static void main(String[] args) {
        ClientResponse clientResponse = null;
        String responseBody = "";
        try {
            AdministrationClient adminClient = new AdministrationClient();

            //Call these methods to publish messages
            //adminClient.publishGameStatus(true);
            //adminClient.publishCustomMessage("This is a custom message to all players.");
            //adminClient.disconnect();
            adminClient.publishMessageToCleanRetained();

            Scanner scanner = new Scanner(System.in);
            Client client = Client.create();
            boolean flag = true;
            while (flag) {
                System.out.println("\nSelect a service:");
                System.out.println("1. Get Players");
                System.out.println("2. Get Player Average Heart Rate Between Timestamps");
                System.out.println("3. Get Average Heart Rate Between Timestamps");
                System.out.println("4. Start Game");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");

                int choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        clientResponse = getRequest(client, BASE_URL + "/getPlayers");
                        responseBody = clientResponse.getEntity(String.class);
                        System.out.println(responseBody);
                        break;
                    case 2:
                        System.out.print("Enter Player ID: ");
                        String playerId = scanner.next();
                        System.out.print("Enter N (number of latest heart rates to consider): ");
                        int n = scanner.nextInt();
                        clientResponse = getRequest(client, BASE_URL + "/averageBetweenTimestamps/" + playerId + "/" + String.valueOf(n));
                        responseBody = clientResponse.getEntity(String.class);
                        System.out.println(responseBody);

                        break;
                    case 3:
                        System.out.print("Enter Start Timestamp (t1): ");
                        long t1 = scanner.nextLong();
                        System.out.print("Enter End Timestamp (t2): ");
                        long t2 = scanner.nextLong();
                        clientResponse = getRequest(client, BASE_URL + "/average/" + String.valueOf(t1) + "/" + String.valueOf(t2));
                        responseBody = clientResponse.getEntity(String.class);
                        System.out.println(responseBody);
                        break;
                    case 4:
                        adminClient.publishGameStatus(true);
                        break;

                        case 5:
                            System.out.println("Exiting...");
                            client.destroy();
                            flag = false;
                            break;
                    default:
                        System.out.println("Invalid choice. Please select again.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
