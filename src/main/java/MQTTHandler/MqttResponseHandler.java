package MQTTHandler;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MqttResponseHandler {
    void handleConnectionLost(Throwable cause);
    void handleMessageArrived(String topic, MqttMessage message) throws Exception;
    void handleDeliveryComplete(IMqttDeliveryToken token);
}