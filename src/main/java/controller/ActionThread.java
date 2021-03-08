package controller;

import model.Status;

import static controller.Control.*;
import static model.Status.OPEN;

public class ActionThread extends Thread {

	public static final int xCLOSE = 0;
	public static final int xOPEN = 1;
	public static final int xAUTOCLOSE = 5;
	public static final int xSTOP = 2;
	public static final int xSTART = 99;
	public static final int xOBSERVE_OPEN = 51;
	public static final int xOBSERVE_CLOSE = 50;

	Action action;
	String log = null;
	boolean fb;
	boolean stopper = true;
	String user = null;
	ActionThread at;

	// 0=schließen 1=öffnen 2=anhalten, 5=openAutoClose, 10=überwachen,
	// 99=initialisieren
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
				if (gate.isOpening()) {
					gate.setClosing();
				}
				if (!fb) {
					GpioHandler.toggleGateGpio();
				}
				gate.setMoving();
				sleep(200);

				if (observe(Status.CLOSED)) {
					interrupt();
					return;
				}
				Logger.sendMqtt("status", "OFF");
				// GpioHandler.deactivateLbGpio();
				interrupt();

			} else if (action == Action.OPEN) {

				if (fb) {
					Logger.log("Tor wird geöffnet: Fernbedienung");
				} else {
					Logger.log("Tor wird geöffnet");
				}
				gate.setMoving();
				Logger.sendMqtt("status", "ON");
				if (!fb) {
					GpioHandler.toggleGateGpio();
				}
				if (observe(Status.OPEN)) {
					interrupt();
					return;
				}
				interrupt();

			} else if (action == Action.AUTOCLOSE) {

				Logger.log("Tor wird geöffnet (auto close)");
				gate.setMoving();
				Logger.sendMqtt("status", "ON");
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
				// Pipsen wenn Objekt durch Tor gefahren
				GpioHandler.beeper.pulse(700);
				// Erst schließen, wenn observer auch beendet ist
				while (at.isAlive()) {
					sleep(100);
				}
				// Wenn
				Logger.log("Bewegung erkannt, schließe in 10 sek");
				sleep(2000);
				for (int i = 1; i <= 170; i++) {
					if (lightbarrier.isOpen()) {
						i = 1;
						Logger.log("Bewegung erkannt, warte weitere 10sek");
						sleep(3000);
					}
					sleep(50);
				}
				GpioHandler.beeper.pulse(1500);
				Logger.log("Schließe Tor");
				// GpioHandler.activateLbGpio();
				automaticActive = false;
				sleep(200);
				gate.setMoving();
				if (!fb) {
					GpioHandler.toggleGateGpio();
				}
				GpioHandler.beeper.low();
				if (observe(Status.CLOSED)) {
					interrupt();
					return;
				}
				// GpioHandler.deactivateLbGpio();
				Logger.sendMqtt("status", "OFF");
				interrupt();

			} else if (action == Action.STOP) {

				if (!fb) {
					GpioHandler.toggleGateGpio();
					Logger.log("Tor angehalten");
				} else {
					Logger.log("Tor angehalten: Fernbedienung");
				}
				GpioHandler.beeper.low();
				// Wenn Tor am schließen war und angehalten wurde, geht es wieder auf, also wird
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

				if (observe(Status.CLOSED)) {
					interrupt();
				}
			} else if (action == Action.OBSERVE_OPEN) {

				if (observe(Status.OPEN)) {
					interrupt();
				}
			}
			// }
		} catch (InterruptedException e) {
			Logger.log("Thread abgebrochen");
			if (at.isAlive()) {
				at.interrupt();
			}
			GpioHandler.beeper.low();
			GpioHandler.deactivateLbGpio();
			interrupt();
		}
	}

	synchronized private boolean observe(Status shouldBe) throws InterruptedException {
		Logger.log("Observer gestartet, soll Status: " + shouldBe);
		int count = 0;
		int actions = 0;
		while (gate.state != shouldBe) {
			count++;
			sleep(500);
			// Wenn Tor garnicht erst los läuft, nach 10 Sek nochmal auslösen
			if (count >= 20) {
				if (shouldBe == Status.OPEN && !gate.isOpening()) {
					GpioHandler.toggleGateGpio();
					count = 0;
				} else if (shouldBe == Status.CLOSED && !gate.isClosing()) {
					GpioHandler.toggleGateGpio();
					count = 0;
				}
			}
			// Wenn eine Minute rum und Tor ist nicht angekommen
			if (count >= 120) {
				count = 0;
				if (shouldBe == Status.OPEN) {
					Logger.log("Tor ist nicht offen angekommen. Keine Aktion ausgelöst. Aktion Abgebrochen.");
					Logger.logAccessSQL(new String[] { "SYSTEM", null },
							"Door open not arrived. Actual Temp: " + Control.temp);
					gate.setStill();
					return true;
				} else if (actions < 5) {
					actions++;
					Logger.log("Fehler erkannt. Tor nicht angekommen, neu ausgelöst.");
					Logger.logAccessSQL(new String[] { "SYSTEM", null }, "Observer took action beforehand!!");
					GpioHandler.toggleGateGpio();
				} else {
					Logger.log("5 Korrekturen erfolglos, breche ab. Manuelle Korrektur erforderlich.");
					Logger.logAccessSQL(new String[] { "SYSTEM", null }, "Took 5 actions, stopping!!");
					return true;
				}
			}
		}
		System.out.println("gate = " + shouldBe);
		if (shouldBe == Status.CLOSED) {
			count = 500;
		} else {
			count = 200;
		}
		for (int i = 1; i <= count; i++) {
			if (gate.state != shouldBe) {

				GpioHandler.toggleGateGpio();
				sleep(1000);
				GpioHandler.toggleGateGpio();
				Logger.log("Fehler erkannt. Korrektur ausgeführt, starte Rekursion");
				Logger.logAccessSQL(new String[] { "SYSTEM", null }, "Observer took action!!");
				observe(shouldBe);

			}
			sleep(50);
		}

		Logger.log("Alles okay, beende Observer");
		gate.setStill();
		return false;
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
