package controller;

import clients.MqttClient;
import model.Status;
import model.User;

import static controller.Control.*;

public class ActionThread extends Thread {

    Action action;
    boolean fb;
    String user = null;
    ActionThread at;

    public ActionThread(Action action, boolean fb) {
        this.action = action;
        this.fb = fb;

    }

    public ActionThread(Action action, String user) {
        this.action = action;
        this.fb = false;
        this.user = user;

    }

    @Override
    synchronized public void run() {

        try {
            // while (!isInterrupted() && stopper) {
            // stopper = false;

            if (action == Action.CLOSE) {

                Logger.log("Tor wird geschlossen");
                // GpioHandler.activateLbGpio();
                if (!fb) {
                    GpioHandler.toggleGateGpio();
                }

                observe(Status.CLOSED);
                MqttClient.sendMqtt("status", "OFF");
                // GpioHandler.deactivateLbGpio();
                interrupt();

            } else if (action == Action.OPEN) {

                if (fb) {
                    Logger.log("Tor wird geöffnet: Fernbedienung");
                } else {
                    Logger.log("Tor wird geöffnet");
                }
                MqttClient.sendMqtt("status", "ON");
                if (!fb) {
                    GpioHandler.toggleGateGpio();
                }
                observe(Status.OPEN);
                interrupt();

            } else if (action == Action.AUTOCLOSE) {

                Logger.log("Tor wird geöffnet (auto close)");
                MqttClient.sendMqtt("status", "ON");
                if (!fb) {
                    GpioHandler.toggleGateGpio();
                }
                // Observ open ausgelagert um LB Signal abgreifen zu können, während Observer
                // noch läuft
                at = new ActionThread(Action.OBSERVE_OPEN, false);
                at.start();
                while (lightbarrier.isClosed()) {
                    sleep(20);
                }
                Logger.log("Bewegung erkannt");
                // Piepsen, wenn Objekt durch Tor gefahren
                GpioHandler.beepForMs(700);
                // Erst schließen, wenn observer auch beendet ist
                while (at.isAlive()) {
                    sleep(100);
                }
                // Wenn
                Logger.log("schließe in 10 sek");
                sleep(2000);
                for (int i = 1; i <= 170; i++) {
                    if (lightbarrier.isOpen()) {
                        i = 1;
                        Logger.log("Bewegung erkannt, warte weitere 10sek");
                        sleep(3000);
                    }
                    sleep(50);
                }
                GpioHandler.beepForMs(1500);
                Logger.log("Schließe Tor");
                // GpioHandler.activateLbGpio();
                automaticActive = false;
                sleep(200);
                if (!fb) {
                    GpioHandler.toggleGateGpio();
                }
                observe(Status.CLOSED);
                MqttClient.sendMqtt("status", "OFF");
                interrupt();

            } else if (action == Action.STOP) {

                if (!fb) {
                    GpioHandler.toggleGateGpio();
                    Logger.log("Tor angehalten");
                } else {
                    Logger.log("Tor angehalten: Fernbedienung");
                }
                // Wenn Tor am Schließen war und angehalten wurde, geht es wieder auf, also wird
                // MOVING nicht 0 gesetzt
                if (gate.isClosing()) {
                    gate.setOpening();
                } else {
                    gate.setStill();
                }
                interrupt();

            } else if (action == Action.START) {
                System.out.println("ActionThread initialized");
                interrupt();
            } else if (action == Action.OBSERVE_CLOSE) {
                observe(Status.CLOSED);
            } else if (action == Action.OBSERVE_OPEN) {
                observe(Status.OPEN);
            }
        } catch (InterruptedException e) {
            Logger.log("Thread abgebrochen");
            if (at != null && at.isAlive()) {
                at.interrupt();
            }
            GpioHandler.deactivateLbGpio();
            gate.setStill();
            interrupt();
        }
    }

    private boolean secondsElapsedSince(int seconds, long since) {
        long until = since + (seconds * 1000L);
        long current = System.currentTimeMillis();
        return current > until;
    }

    synchronized private void observe(Status shouldBe) throws InterruptedException {
        Logger.log("Observer gestartet, soll Status: " + shouldBe);
        int correctionsExecuted = 0;
        long startTime = System.currentTimeMillis();
        while (gate.state != shouldBe) {
            if (correctionsExecuted >= 5) {
                Logger.log("5 Korrekturen erfolglos, breche ab. Manuelle Korrektur erforderlich.");
                interrupt();
            }
            sleep(500);

            // Wenn 45 sek rum und Tor ist nicht angekommen
            if (secondsElapsedSince(45, startTime)) {
                if (shouldBe == Status.OPEN) {
                    Logger.log(
                        "Tor ist nicht offen angekommen. Keine Aktion ausgelöst. Aktion Abgebrochen.");
                    Logger.logAccessSQL(new User("SYSTEM"),
                        "Door open not arrived. Actual Temp: " + Control.temp);
                    gate.setStill();
                    interrupt();
                } else if (shouldBe == Status.CLOSED) {
                    correctionsExecuted++;
                    Logger.log("Fehler erkannt. Tor nicht angekommen, neu ausgelöst.");
                    Logger.logAccessSQL(new User("SYSTEM"), "Observer took action beforehand!!");
                    GpioHandler.toggleGateGpio();
                    // reset timer
                    startTime = System.currentTimeMillis();
                    continue;
                } else {
                    Logger.log(
                        "5 Korrekturen erfolglos, breche ab. Manuelle Korrektur erforderlich.");
                    Logger.logAccessSQL(new User("SYSTEM"), "Took 5 actions, stopping!!");
                    interrupt();
                }
            }

            // Wenn Tor gar nicht erst losläuft, nach 7 Sek nochmal auslösen
            if (secondsElapsedSince(7, startTime)) {
                if ((shouldBe == Status.CLOSED && gate.isOpen())
                    || (shouldBe == Status.OPEN && gate.isClosed())) {
                    Logger.log("Tor nicht los gelaufen, erneut auslösen");
                    GpioHandler.toggleGateGpio();
                    correctionsExecuted++;
                    // reset timer
                    startTime = System.currentTimeMillis();
                }
            }
        }

        // Tor it jetzt in gewünschter Position, warte beim Schließen ein wenig zur Sicherheit
        if (shouldBe == Status.CLOSED) {
            long waitUntil = System.currentTimeMillis() + 32_000;
            while (System.currentTimeMillis() < waitUntil) {
                if (gate.state != shouldBe) {
                    GpioHandler.toggleGateGpio();
                    sleep(1000);
                    GpioHandler.toggleGateGpio();
                    Logger.log("Fehler erkannt. Korrektur ausgeführt, starte Rekursion");
                    Logger.logAccessSQL(new User("SYSTEM"), "Observer took action!!");
                    observe(shouldBe);
                }
                sleep(50);
            }
        }

        Logger.log("Alles okay, beende Observer");
        gate.setStill();
    }

    synchronized private void sleep(int time) throws InterruptedException {
        Thread.sleep(time);
    }

    public enum Action {
        CLOSE,
        OPEN,
        AUTOCLOSE,
        STOP,
        START,
        OBSERVE_OPEN,
        OBSERVE_CLOSE
    }

}
