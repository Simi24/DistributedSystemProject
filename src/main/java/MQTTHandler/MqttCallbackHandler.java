package MQTTHandler;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.sql.Timestamp;

public class MqttCallbackHandler implements MqttCallback {

    private String clientId;

    public MqttCallbackHandler(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println(clientId + " Connection lost! Cause: " + cause.getMessage() + " - Thread PID: " + Thread.currentThread().getId());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String time = new Timestamp(System.currentTimeMillis()).toString();
        String receivedMessage = new String(message.getPayload());
        System.out.println(clientId + " Received a Message! - Callback - Thread PID: " + Thread.currentThread().getId() +
                "\n\tTime:    " + time +
                "\n\tTopic:   " + topic +
                "\n\tMessage: " + receivedMessage +
                "\n\tQoS:     " + message.getQos() + "\n");

        System.out.println("\n ***  Press a random key to exit *** \n");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //Not used;
    }
}