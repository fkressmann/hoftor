package controller;

public class Control {
	public static final int GATE = 0;
	// 0=zu 1=auf 2=öffnet 3=schließt
	public static final int DOOR = 1;
	public static final int LB = 2;
	public static final int MOVING = 3;
	public static final int AUTO = 4;
	public static final int LOCKED = 5;
	// [tor, tür, lichtschranke, gate moving, auto active, locked]
	public static int[] state = { 0, 0, 0, 0, 0, 0 };
	public static double temp = 0;
	public static ActionThread actionthread;
	public static TempThread tempthread;

	public static void init() {
		Logger.log("Initialisiere");
		GpioHandler.initializeGpio();
		actionthread = new ActionThread(99, false);
		actionthread.start();
		initGpioRead();
		Logger.log("Initialisierung abgeschlossen!");
	}

	public static void initGpioRead() {
		GpioHandler.deactivateLbGpio();
		if (GpioHandler.gateClosed.isLow()) {
			state[GATE] = 0;
		} else {
			state[GATE] = 1;
		}
		if (GpioHandler.lb.isHigh()) {
			state[LB] = 0;
		} else {
			state[LB] = 1;
		}
		if (GpioHandler.door.isHigh()) {
			state[DOOR] = 0;
		} else {
			state[DOOR] = 1;
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
		String action = null;
		if ((state[GATE] == 0) && state[MOVING] == 0 && !actionthread.isAlive()) {
			openDoor(user, true);
		} else if ((state[GATE] == 1 || state[GATE] == 2 || state[GATE] == 3) && state[MOVING] == 0
				&& !actionthread.isAlive()) {
			if (state[GATE] == 3) {
				Logger.log("Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
				Logger.logAccessSQL(new String[] { "SYSTEM", null },
						"WARNING: Torflügel ist wahrscheinlich von selber zu gegangen. Versuche trotzdem zu schließen");
			}
			closeDoor(user, true);
		} else if (state[MOVING] == 1 || actionthread.isAlive()) {
			stopDoor(user, true);
		} else {
			action = "--ERROR: States out of bounds: (Gate/Moving/AT alive) " + state[0] + state[3]
					+ actionthread.isAlive();
			Logger.logAccessSQL(user, action);
			Logger.log(action);

		}
	}

	public static boolean openDoor(String[] user, boolean fb) {
		if (state[LOCKED] == 1) {
			Logger.logAccessSQL(user, "openDoor: fail - gate locked");
			return false;
		}
		if ((state[GATE] == 0 || state[GATE] == 3) && state[MOVING] == 0) {
			if (actionthread.isAlive()) {
				Logger.log("ActionThread aktiv, openDoor konnte nicht ausgelöst werden");
				Logger.logAccessSQL(user, "openDoor: fail - ActionThread alive");
				return false;
			} else {
				actionthread = new ActionThread(ActionThread.OPEN, fb);
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
		if (state[LOCKED] == 1) {
			Logger.logAccessSQL(user, "openDoor: fail - gate locked");
			return false;
		}
		if (state[LB] == 1 && !user[0].equals("felix")) {
			Logger.log("Kein Signal von Lichtschranke, Automatik kann nicht gestartet werden.");
			Logger.logAccessSQL(user, "openAutoClose: No LB signal, not invoked");
			return false;
		}
		if ((state[GATE] == 0 || state[GATE] == 3) && state[MOVING] == 0) {
			if (actionthread.isAlive()) {
				Logger.log("ActionThread active, openAutoCloseDoor could not be invoked");
				Logger.logAccessSQL(user, "openAutoCloseDoor: fail - ActionThread alive");
				return false;
			} else {
				state[AUTO] = 1;
				actionthread = new ActionThread(ActionThread.AUTOCLOSE, user[0]);
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
		if ((state[GATE] == 1 || state[GATE] == 2 || state[GATE] == 3) && state[MOVING] == 0) {
			if (actionthread.isAlive()) {
				System.out.println("ActionThread active, closeDoor could not be invoked");
				Logger.logAccessSQL(user, "closeDoor: fail - ActionThread alive");
				return false;
			} else {
				actionthread = new ActionThread(ActionThread.CLOSE, fb);
				actionthread.start();
				Logger.logAccessSQL(user, "closeDoor: ok");
				if (state[GATE] == 3) {
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
		if (state[MOVING] == 1) {
			if (state[GATE] == 3) {
				if (actionthread.isAlive()) {
					actionthread.interrupt();
				}
				state[MOVING] = 0;
				openDoor(user, false);
				return true;

			} else {
				if (actionthread.isAlive()) {
					actionthread.interrupt();
					GpioHandler.deactivateLbGpio();
					System.out.println("ActionThread interrupted");
				}
				actionthread = new ActionThread(ActionThread.STOP, fb);
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
			state[AUTO] = 0;
			Logger.log("Automatik abgebrochen");
			Logger.logAccessSQL(user, "AutoClose cancled");
			return true;
		} else {
			Logger.log("Automatik nicht aktiv");
			return false;
		}
	}

}
