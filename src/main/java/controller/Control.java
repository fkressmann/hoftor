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

	public static void init2() {
		Logger.log("Simuliere init()");
	}

	public static void setTemperature(double newtemp) {
		temp = newtemp;
	}

	// cycleDoor(user, gewünschte Aktion) 0:schließen, 1: öffnen, 2:anhalten
	public static void cycleDoor() {
		User user = User.remote();
		String action;
		if (door.isOpen() || lightbarrier.isOpen()) {
			Logger.log("Türchen oder Lichtschranke offen, ignoriere Fernbedienung");
		} else if (gate.isClosed() && !gate.isMoving() && !actionthread.isAlive()) {
			openDoor(user);
		} else if (!gate.isClosed() && !gate.isMoving() && !actionthread.isAlive()) {
			if (gate.isClosing()) {
				Logger.log("Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
				Logger.logAccessSQL(new User("SYSTEM"),
						"WARNING: Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
			}
			closeDoor(user);
		} else if (gate.isMoving() || actionthread.isAlive()) {
			stopDoor(user);
		} else {
			action = "--ERROR: States out of bounds: (Status/Moving) " + gate.state + gate.isMoving();
			Logger.logAccessSQL(user, action);
			Logger.log(action);

		}
	}

	public static boolean openDoor(User user) {
		if (gate.isLocked()) {
			Logger.logAccessSQL(user, "openDoor: fail - gate locked");
			return false;
		}
		if ((gate.isClosed() || gate.isClosing()) && !gate.isMoving()) {
			if (actionthread.isAlive()) {
				Logger.log("ActionThread aktiv, openDoor konnte nicht ausgelöst werden");
				Logger.logAccessSQL(user, "openDoor: fail - ActionThread alive");
				return false;
			} else {
				actionthread = new ActionThread(ActionThread.Action.OPEN, user.isRemote());
				actionthread.start();
				Logger.logAccessSQL(user, "openDoor: ok");
				return true;
			}
		} else {
			Logger.logAccessSQL(user, "openDoor: fail - door already open");
			return false;
		}
	}

	public static boolean openAutoCloseDoor(User user) {
		if (gate.isLocked()) {
			Logger.logAccessSQL(user, "openDoor: fail - gate locked");
			return false;
		}
		if (lightbarrier.isOpen()) {
			Logger.log("Kein Signal von Lichtschranke, Automatik kann nicht gestartet werden.");
			Logger.logAccessSQL(user, "openAutoClose: No LB signal, not invoked");
			return false;
		}
		if ((gate.isClosed() || gate.isClosing()) && !gate.isMoving()) {
			if (actionthread.isAlive()) {
				Logger.log("ActionThread active, openAutoCloseDoor could not be invoked");
				Logger.logAccessSQL(user, "openAutoCloseDoor: fail - ActionThread alive");
				return false;
			} else {
				automaticActive = true;
				actionthread = new ActionThread(ActionThread.Action.AUTOCLOSE, user.getName());
				actionthread.start();
				Logger.logAccessSQL(user, "openAutoCloseDoor: ok");
				return true;
			}
		} else {
			Logger.logAccessSQL(user, "openAutoCloseDoor: fail - door already open");
			return false;
		}
	}

	public static boolean closeDoor(User user) {
		if (!gate.isClosed() && !gate.isMoving()) {
			if (actionthread.isAlive()) {
				System.out.println("ActionThread active, closeDoor could not be invoked");
				Logger.logAccessSQL(user, "closeDoor: fail - ActionThread alive");
				return false;
			} else {
				actionthread = new ActionThread(ActionThread.Action.CLOSE, user.isRemote());
				actionthread.start();
				Logger.logAccessSQL(user, "closeDoor: ok");
				if (gate.isClosing()) {
					Logger.log("Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
					Logger.logAccessSQL(new User("SYSTEM"),
							"WARNING: Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
				}
				return true;
			}
		} else {
			Logger.logAccessSQL(user, "closeDoor: fail - door already closed");
			return false;
		}
	}

	public static boolean stopDoor(User user) {
		if (gate.isMoving()) {
			if (gate.isClosing()) {
				if (actionthread.isAlive()) {
					actionthread.interrupt();
				}
				gate.setStill();
				openDoor(user);
				return true;

			} else {
				if (actionthread.isAlive()) {
					actionthread.interrupt();
					GpioHandler.deactivateLbGpio();
					System.out.println("ActionThread interrupted");
				}
				actionthread = new ActionThread(ActionThread.Action.STOP, user.isRemote());
				actionthread.start();
				Logger.logAccessSQL(user, "stopDoor: ok");
				return true;
			}
		} else {
			Logger.logAccessSQL(user, "stopDoor: fail - door not moving");
			return false;
		}
	}

	public static boolean killAuto(User user) {
		if (actionthread.isAlive()) {
			actionthread.interrupt();
			automaticActive = false;
			Logger.log("Automatik abgebrochen");
			Logger.logAccessSQL(user, "AutoClose cancled");
			return true;
		} else {
			Logger.log("Automatik nicht aktiv");
			return false;
		}
	}

}
