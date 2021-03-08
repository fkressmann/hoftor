package model;

import static clients.MqttClient.sendMqtt;

public class Sensor {
    private final String prefix;
    public Status state;

    public Sensor(String mqttPrefix) {
        this.state = Status.UNKNOWN;
        this.prefix = mqttPrefix;
    }


    public void setOpen() {
        state = Status.OPEN;
        sendState();
    }

    public void setClosed() {
        state = Status.CLOSED;
        sendState();
    }



    public boolean isClosed() {
        return state == Status.CLOSED;
    }

    public boolean isOpen() {
        return state == Status.OPEN;
    }

    public boolean isUnknown() {
        return state == Status.UNKNOWN;
    }

    void sendState() {
        sendMqtt(prefix + "/state", state.name());
    }
}
