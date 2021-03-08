package controller;

import clients.MqttClient;
import com.pi4j.component.temperature.TemperatureSensor;
import com.pi4j.io.w1.W1Master;

public class TempThread extends Thread {

    @Override
    synchronized public void run() {
        W1Master w1Master = new W1Master();
        if (w1Master.getDevices(TemperatureSensor.class).isEmpty()) {
            Logger.log("Kein Temperatursensor gefunden!");
        } else {
            System.out.println("Temperature reader started");
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e1) {
            interrupt();
            System.exit(0);
            e1.printStackTrace();
        }
        while (!isInterrupted()) {

            for (TemperatureSensor device : w1Master.getDevices(TemperatureSensor.class)) {
                double temp = device.getTemperature();
                if (temp != Control.temp) {
                    MqttClient.sendMqtt("temp", Double.toString(temp));
                    Control.temp = temp;
                }
            }

            try {
                //noinspection BusyWait
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                interrupt();
            }

        }
    }
}
