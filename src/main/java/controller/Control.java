package controller;

import model.Gate;
import model.Sensor;

public class Control {
	public static double temp = 0;
	public static ActionThread actionthread;
	public static TempThread tempthread;
	public static Gate gate = new Gate();
	public static Sensor door = new Sensor();
	public static Sensor lightbarrier = new Sensor();
	public static boolean automaticActive = false;

	public static void init() {
		Logger.log("Initialisiere");
		GpioHandler.initializeGpio();
		actionthread = new ActionThread(ActionThread.Action.START, false);
		actionthread.start();
		initGpioRead();
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
		String[] user = new String[] { "fernbedienung", null };
		String action;
		if (gate.isClosed() && !gate.isMoving() && !actionthread.isAlive()) {
			openDoor(user, true);
		} else if (!gate.isClosed() && !gate.isMoving() && !actionthread.isAlive()) {
			if (gate.isClosing()) {
				Logger.log("Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
				Logger.logAccessSQL(new String[] { "SYSTEM", null },
						"WARNING: Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
			}
			closeDoor(user, true);
		} else if (gate.isMoving() || actionthread.isAlive()) {
			stopDoor(user, true);
		} else {
			action = "--ERROR: States out of bounds: (Status/Moving) " + gate.state + gate.isMoving();
			Logger.logAccessSQL(user, action);
			Logger.log(action);

		}
	}

	public static boolean openDoor(String[] user, boolean fb) {
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
				actionthread = new ActionThread(ActionThread.Action.OPEN, fb);
				actionthread.start();
				Logger.logAccessSQL(user, "openDoor: ok");
				return true;
			}
		} else {
			Logger.logAccessSQL(user, "openDoor: fail - door already open");
			return false;
		}
	}

	public static boolean openAutoCloseDoor(String[] user, boolean fb) {
		if (gate.isLocked()) {
			Logger.logAccessSQL(user, "openDoor: fail - gate locked");
			return false;
		}
		if (lightbarrier.isOpen() && !user[0].equals("felix")) {
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
				actionthread = new ActionThread(ActionThread.Action.AUTOCLOSE, user[0]);
				actionthread.start();
				Logger.logAccessSQL(user, "openAutoCloseDoor: ok");
				return true;
			}
		} else {
			Logger.logAccessSQL(user, "openAutoCloseDoor: fail - door already open");
			return false;
		}
	}

	public static boolean closeDoor(String[] user, boolean fb) {
		if (!gate.isClosed() && !gate.isMoving()) {
			if (actionthread.isAlive()) {
				System.out.println("ActionThread active, closeDoor could not be invoked");
				Logger.logAccessSQL(user, "closeDoor: fail - ActionThread alive");
				return false;
			} else {
				actionthread = new ActionThread(ActionThread.Action.CLOSE, fb);
				actionthread.start();
				Logger.logAccessSQL(user, "closeDoor: ok");
				if (gate.isClosing()) {
					Logger.log("Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
					Logger.logAccessSQL(new String[] { "SYSTEM", null },
							"WARNING: Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
				}
				return true;
			}
		} else {
			Logger.logAccessSQL(user, "closeDoor: fail - door already closed");
			return false;
		}
	}

	public static boolean stopDoor(String[] user, boolean fb) {
		if (gate.isMoving()) {
			if (gate.isClosing()) {
				if (actionthread.isAlive()) {
					actionthread.interrupt();
				}
				gate.setStill();
				openDoor(user, false);
				return true;

			} else {
				if (actionthread.isAlive()) {
					actionthread.interrupt();
					GpioHandler.deactivateLbGpio();
					System.out.println("ActionThread interrupted");
				}
				actionthread = new ActionThread(ActionThread.Action.STOP, fb);
				actionthread.start();
				Logger.logAccessSQL(user, "stopDoor: ok");
				return true;
			}
		} else {
			Logger.logAccessSQL(user, "stopDoor: fail - door not moving");
			return false;
		}
	}

	public static boolean killAuto(String[] user) {
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
