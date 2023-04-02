package controller;

import model.Gate;
import model.Sensor;
import model.User;

import static clients.MqttClient.initMqtt;

public class Control {
	public static double temp = 0;
	public static ActionThread actionthread;
	public static TempThread tempthread;
	public static Gate gate = new Gate("gate");
	public static Sensor door = new Sensor("door");
	public static Sensor lightbarrier = new Sensor("lb");
	public static boolean automaticActive = false;
	public static boolean passiveMode = false;

	public static void init() {
		Logger.log("Initialisiere");
		GpioHandler.initializeGpio();
		tempthread = new TempThread();
		tempthread.setName("Temperature reader");
		tempthread.setDaemon(true);
		tempthread.start();
		actionthread = new ActionThread(ActionThread.Action.START, false);
		actionthread.start();
		initGpioRead();
		initMqtt();
		Logger.log("Initialisierung abgeschlossen!");
	}

	public static void initGpioRead() {
		GpioHandler.deactivateLbGpio();
		if (GpioHandler.gateClosed.isLow()) {
			gate.setClosed();
		} else if (GpioHandler.gateOpened.isLow()) {
			gate.setOpen();
		}
		if (GpioHandler.lb.isHigh()) {
			lightbarrier.setClosed();
		} else {
			lightbarrier.setOpen();
		}
		if (GpioHandler.door.isHigh()) {
			door.setClosed();
		} else {
			door.setOpen();
		}
	}

	// Wird aufgerufen, wenn Fernbedienung gedrückt wird
	public static void onRemoteTriggered() {
		User user = User.remote();
		String action;
		if (door.isOpen() || lightbarrier.isOpen()) {
			Logger.log("Türchen oder Lichtschranke offen, ignoriere Fernbedienung");
		} else if (gate.isClosed() && !actionthread.isAlive()) {
			openDoor(user);
		} else if (!gate.isClosed() && !actionthread.isAlive()) {
			if (gate.isClosing()) {
				Logger.log("Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
			}
			closeDoor(user);
		} else if (gate.isMoving() && actionthread.isAlive()) {
			stopDoor(user);
		} else if (gate.isStill() && actionthread.isAlive()) {
			// Ignore on purpose as gate might just not have reacted to remote control,
			// thus remoted is triggered a second time
			Logger.log("Fernbedienung wiederholt registriert, warte ab...");
		} else {
			action = String.format("--ERROR: States out of bounds: status=%s, moving=%s, threadalive=%s",
					gate.state, gate.isMoving(), actionthread.isAlive());
			Logger.logAccessSQL(user, action);
			Logger.log(action);

		}
	}

	public static void openDoor(User user) {
		if (gate.isLocked()) {
			Logger.logAccessSQL(user, "openDoor: fail - gate locked");
			return;
		}
		if ((gate.isClosed() || gate.isClosing()) && !gate.isMoving()) {
			if (actionthread.isAlive()) {
				Logger.log("ActionThread aktiv, openDoor konnte nicht ausgelöst werden");
				Logger.logAccessSQL(user, "openDoor: fail - ActionThread alive");
			} else {
				actionthread = new ActionThread(ActionThread.Action.OPEN, user.isRemote());
				actionthread.start();
				Logger.logAccessSQL(user, "openDoor: ok");
			}
		} else {
			Logger.logAccessSQL(user, "openDoor: fail - door already open");
		}
	}

	public static void openAutoCloseDoor(User user) {
		if (gate.isLocked()) {
			Logger.logAccessSQL(user, "openDoor: fail - gate locked");
			return;
		}
		if (lightbarrier.isOpen()) {
			Logger.log("Kein Signal von Lichtschranke, Automatik kann nicht gestartet werden.");
			Logger.logAccessSQL(user, "openAutoClose: No LB signal, not invoked");
			return;
		}
		if ((gate.isClosed() || gate.isClosing()) && !gate.isMoving()) {
			if (actionthread.isAlive()) {
				Logger.log("ActionThread active, openAutoCloseDoor could not be invoked");
				Logger.logAccessSQL(user, "openAutoCloseDoor: fail - ActionThread alive");
			} else {
				automaticActive = true;
				actionthread = new ActionThread(ActionThread.Action.AUTOCLOSE, user.getName());
				actionthread.start();
				Logger.logAccessSQL(user, "openAutoCloseDoor: ok");
			}
		} else {
			Logger.logAccessSQL(user, "openAutoCloseDoor: fail - door already open");
		}
	}

	public static void closeDoor(User user) {
		if (!gate.isClosed() && !gate.isMoving()) {
			if (actionthread.isAlive()) {
				System.out.println("ActionThread active, closeDoor could not be invoked");
				Logger.logAccessSQL(user, "closeDoor: fail - ActionThread alive");
			} else {
				actionthread = new ActionThread(ActionThread.Action.CLOSE, user.isRemote());
				actionthread.start();
				Logger.logAccessSQL(user, "closeDoor: ok");
				if (gate.isClosing()) {
					Logger.log("Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
					Logger.logAccessSQL(new User("SYSTEM"),
							"WARNING: Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
				}
			}
		} else {
			Logger.logAccessSQL(user, "closeDoor: fail - door already closed");
		}
	}

	public static void stopDoor(User user) {
		if (actionthread.isAlive()) {
			actionthread.interrupt();
		}
		if (gate.isOpening()) {
			actionthread = new ActionThread(ActionThread.Action.STOP, user.isRemote());
			actionthread.start();
			Logger.logAccessSQL(user, "stopDoor: ok");
		}
	}

	public static void killAuto(User user) {
		if (actionthread.isAlive()) {
			actionthread.interrupt();
			automaticActive = false;
			Logger.log("Automatik abgebrochen");
			Logger.logAccessSQL(user, "AutoClose cancled");
		} else {
			Logger.log("Automatik nicht aktiv");
		}
	}

}
