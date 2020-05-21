package controller;

public class ActionThread extends Thread {

	public static final int CLOSE = 0;
	public static final int OPEN = 1;
	public static final int AUTOCLOSE = 5;
	public static final int STOP = 2;
	public static final int START = 99;
	public static final int OBSERVE_OPEN = 51;
	public static final int OBSERVE_CLOSE = 50;

	int action;
	String log = null;
	boolean fb;
	boolean stopper = true;
	String user = null;
	ActionThread at;

	// 0=schließen 1=öffnen 2=anhalten, 5=openAutoClose, 10=überwachen,
	// 99=initialisieren
	public ActionThread(int action, boolean fb) {
		this.action = action;
		this.fb = fb;

	}

	public ActionThread(int action, String user) {
		this.action = action;
		this.fb = false;
		this.user = user;

	}

	@Override
	synchronized public void run() {

		try {
			// while (!isInterrupted() && stopper) {
			// stopper = false;

			if (action == CLOSE) {

				Logger.log("Tor wird geschlossen");
				// GpioHandler.activateLbGpio();
				if (Control.state[Control.GATE] == 2) {
					Control.state[Control.GATE] = 3;
				}
				if (!fb) {
					GpioHandler.toggleGateGpio();
				}
				Control.state[Control.MOVING] = 1;
				sleep(200);

				if (observe(CLOSE)) {
					interrupt();
					return;
				}
				Logger.sendMqtt("status", "OFF");
				// GpioHandler.deactivateLbGpio();
				interrupt();

			} else if (action == OPEN) {

				if (fb) {
					Logger.log("Tor wird geöffnet: Fernbedienung");
				} else {
					Logger.log("Tor wird geöffnet");
				}
				Control.state[Control.MOVING] = 1;
				Logger.sendMqtt("status", "ON");
				if (!fb) {
					GpioHandler.toggleGateGpio();
				}
				if (observe(OPEN)) {
					interrupt();
					return;
				}
				interrupt();

			} else if (action == AUTOCLOSE) {

				Logger.log("Tor wird geöffnet (auto close)");
				Control.state[Control.MOVING] = 1;
				Logger.sendMqtt("status", "ON");
				if (!fb) {
					GpioHandler.toggleGateGpio();
				}
				// Observ open ausgelagert um LB Signal abgreifen zu können, während Observer
				// noch läuft
				at = new ActionThread(OBSERVE_OPEN, false);
				at.start();
				while (Control.state[Control.LB] != 1) {
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
					if (Control.state[Control.LB] != 0) {
						i = 1;
						Logger.log("Bewegung erkannt, warte weitere 10sek");
						i = i + 60;
						sleep(3000);
					}
					sleep(50);
				}
				GpioHandler.beeper.pulse(1500);
				Logger.log("Schließe Tor");
				// GpioHandler.activateLbGpio();
				Control.state[Control.AUTO] = 0;
				sleep(200);
				Control.state[Control.MOVING] = 1;
				if (!fb) {
					GpioHandler.toggleGateGpio();
				}
				GpioHandler.beeper.low();
				if (observe(CLOSE)) {
					interrupt();
					return;
				}
				// GpioHandler.deactivateLbGpio();
				Logger.sendMqtt("status", "OFF");
				interrupt();

			} else if (action == STOP) {

				if (!fb) {
					GpioHandler.toggleGateGpio();
					Logger.log("Tor angehalten");
				} else {
					Logger.log("Tor angehalten: Fernbedienung");
				}
				GpioHandler.beeper.low();
				// Wenn Tor am schließen war und angehalten wurde, geht es wieder auf, also wird
				// MOVING nicht 0 gesetzt
				if (Control.state[Control.GATE] != 3) {
					Control.state[Control.MOVING] = 0;
				}
				interrupt();

			} else if (action == START) {

				System.out.println("ActionThread initialized");
				interrupt();
			} else if (action == OBSERVE_CLOSE) {

				if (observe(CLOSE)) {
					interrupt();
					return;
				}
			} else if (action == OBSERVE_OPEN) {

				if (observe(OPEN)) {
					interrupt();
					return;
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
			return;

		}
	}

	synchronized private boolean observe(int shouldBe) throws InterruptedException {
		Logger.log("Observer gestartet, soll Status: " + shouldBe);
		int count = 0;
		int actions = 0;
		while (Control.state[Control.GATE] != shouldBe) {
			count++;
			sleep(500);
			if (count >= 240) {
				count = 0;
				if (shouldBe == OPEN) {
					Logger.log("Tor ist nicht offen angekommen. Keine Aktion ausgelöst. Aktion Abgebrochen.");
					Logger.logAccessSQL(new String[] { "SYSTEM", null },
							"Door open not arrived. Actual Temp: " + Control.temp);
					Control.state[Control.MOVING] = 0;
					return true;
				} else if (actions < 5) {
					actions++;
					if (actions == 4) {
						GpioHandler.activateLbGpio();
						Logger.log("Vierter Durchlauf, aktiviere Lichtschranke.");
					}
					Logger.log("Fehler erkannt. Tor nicht angekommen, neu ausgelöst.");
					Logger.logAccessSQL(new String[] { "SYSTEM", null }, "Observer took action beforehand!!");
					GpioHandler.toggleGateGpio();
				} else {
					Logger.log("5 Korrekturen erfolglos, breche ab. Manuelle Korrektur erforderlich.");
					Logger.logAccessSQL(new String[] { "SYSTEM", null }, "Took 5 actions, stopping!!");
					GpioHandler.deactivateLbGpio();
					return true;
				}
			}
		}
		System.out.println("gate = " + shouldBe);
		if (shouldBe == CLOSE) {
			count = 500;
		} else {
			count = 200;
		}
		for (int i = 1; i <= count; i++) {
			if (Control.state[Control.GATE] != shouldBe) {

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
		Control.state[Control.MOVING] = 0;
		return false;
	}

	synchronized private void sleep(int time) throws InterruptedException {

		Thread.sleep(time);

	}

}
