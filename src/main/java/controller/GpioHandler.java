package controller;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GpioHandler {

	static GpioController gpio;
	static GpioPinDigitalInput gateOpened;
	static GpioPinDigitalInput gateClosed;
	static GpioPinDigitalInput remote;
	static GpioPinDigitalInput door;
	static GpioPinDigitalInput lb;
	static GpioPinDigitalOutput trigger;
	public static GpioPinDigitalOutput lbw;
	static GpioPinDigitalOutput beeper;
	static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public static void initializeGpio() {
		Control.tempthread = new TempThread();
		Control.tempthread.setName("Temperature reader");
		Control.tempthread.setDaemon(true);
		Control.tempthread.start();

		gpio = GpioFactory.getInstance();
		trigger = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "Trigger", PinState.LOW);
		Logger.log("---trigger out registered---");
		trigger.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);

		lbw = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_14, "Write Lightbarrier", PinState.LOW);
		Logger.log("---write lightbarrier registered---");
		lbw.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);

		beeper = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "Beeper", PinState.LOW);
		Logger.log("---beeper registered---");
		beeper.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);

		gateOpened = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.PULL_UP);
		gateOpened.setShutdownOptions(true);
		gateOpened.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				// display pin state on console
				System.out.println(sdf.format(new Date()) + " --> gateOpen PIN STATE CHANGE: " + event.getPin() + " = "
						+ event.getState());
				if (event.getState().equals(PinState.LOW)) {
					Control.state[Control.GATE] = 1;
				} else if (event.getState().equals(PinState.HIGH)) {
					Control.state[Control.GATE] = 3;
				}
			}

		});
		Logger.log("---gateOpened in registered---");

		gateClosed = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_UP);
		gateClosed.setShutdownOptions(true);
		gateClosed.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				// display pin state on console
				System.out.println(sdf.format(new Date()) + " --> gateClosed PIN STATE CHANGE: " + event.getPin()
						+ " = " + event.getState());
				if (event.getState().equals(PinState.LOW)) {
					Control.state[Control.GATE] = 0;
					Logger.sendMqtt("gate", "CLOSED");
				} else if (event.getState().equals(PinState.HIGH)) {
					Control.state[Control.GATE] = 2;
					Logger.sendMqtt("gate", "OPEN");
				}
			}

		});
		Logger.log("---gateClosed in registered---");

		remote = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_UP);
		remote.setShutdownOptions(true);
		remote.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				// display pin state on console
				System.out.println(sdf.format(new Date()) + " --> remote PIN STATE CHANGE: " + event.getPin() + " = "
						+ event.getState());
				if (event.getState().equals(PinState.LOW)) {
					Control.cycleDoor();
				} else if (event.getState().equals(PinState.HIGH)) {
				}
			}

		});
		Logger.log("---remote in registered---");

		door = gpio.provisionDigitalInputPin(RaspiPin.GPIO_12, PinPullResistance.PULL_UP);
		door.setShutdownOptions(true);
		door.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				// display pin state on console
				System.out.println(sdf.format(new Date()) + " --> door PIN STATE CHANGE: " + event.getPin() + " = "
						+ event.getState());
				if (event.getState().equals(PinState.LOW)) {
					Control.state[Control.DOOR] = 1;
					Logger.sendMqtt("door", "OPEN");
				} else if (event.getState().equals(PinState.HIGH)) {
					Control.state[Control.DOOR] = 0;
					Logger.sendMqtt("door", "CLOSED");
				}
			}

		});
		Logger.log("---door in registered---");

		lb = gpio.provisionDigitalInputPin(RaspiPin.GPIO_13, PinPullResistance.PULL_UP);
		lb.setShutdownOptions(true);
		lb.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				if (event.getState().equals(PinState.LOW)) {
					Control.state[Control.LB] = 1;
					System.out.println(sdf.format(new Date()) + " --> lightbarrier PIN STATE CHANGE: " + event.getPin()
							+ " = BAD");
					Logger.sendMqtt("lb", "OPEN");
				} else if (event.getState().equals(PinState.HIGH)) {
					Control.state[Control.LB] = 0;
					System.out.println(sdf.format(new Date()) + " --> lightbarrier PIN STATE CHANGE: " + event.getPin()
							+ " = GOOD");
					Logger.sendMqtt("lb", "CLOSED");
				}
			}

		});
		Logger.log("---lightbarrier in registered---");
	}

	public static boolean toggleGateGpio() {
		trigger.pulse(400, true);
		return true;
	}

	public static boolean activateLbGpio() {
		lbw.high();
		return true;
	}

	public static boolean deactivateLbGpio() {
		lbw.low();
		return true;
	}

}
