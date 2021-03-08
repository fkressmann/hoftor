package clients;

import controller.Control;
import controller.Logger;
import model.User;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class MqttClient {
    public static org.eclipse.paho.client.mqttv3.MqttClient client = null;

    public static org.eclipse.paho.client.mqttv3.MqttClient mqttCon() {
        try {
            if (client == null) {
                client = new org.eclipse.paho.client.mqttv3.MqttClient("tcp://debian.fritz.box", "Hoftor");
            }
            if (!client.isConnected()) {
                client.connect();
            }
        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.getMessage();
        }
        return client;
    }

    public static void sendMqtt(String topic, String input) {
        try {
            MqttMessage message = new MqttMessage(input.getBytes());
            mqttCon().publish("stadecken/gate/" + topic, message);
        } catch (MqttException e) {
            Logger.log(topic + " MQTT " + input + " konnte nicht gesendet werden.");
            System.out.println(e.getMessage());
        }

    }

    public static void initMqtt() {
        try {
            mqttCon().subscribe("stadecken/gate/control", ((topic, message) -> {
                User user = new User("HA");
                switch (message.toString()) {
                    case "OPEN":
                        Control.openDoor(user);
                        break;
                    case "CLOSE":
                        Control.closeDoor(user);
                        break;
                    case "STOP":
                        Control.stopDoor(user);
                        break;
                    case "AUTOOPEN":
                        Control.openAutoCloseDoor(user);
                        break;
                }
            }));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void disconnectMqtt() {
        try {
            client.disconnect();
            client.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
