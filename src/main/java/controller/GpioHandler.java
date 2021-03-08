package controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import static controller.Control.gate;

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
		gateOpened.addListener((GpioPinListenerDigital) event -> {
			// display pin state on console
			System.out.println(sdf.format(new Date()) + " --> gateOpen PIN STATE CHANGE: " + event.getPin() + " = "
					+ event.getState());
			if (event.getState().equals(PinState.LOW)) {
				gate.setOpen();
			} else if (event.getState().equals(PinState.HIGH)) {
				gate.setClosing();
			}
		});
		Logger.log("---gateOpened in registered---");

		gateClosed = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_UP);
		gateClosed.setShutdownOptions(true);
		gateClosed.addListener((GpioPinListenerDigital) event -> {
			// display pin state on console
			System.out.println(sdf.format(new Date()) + " --> gateClosed PIN STATE CHANGE: " + event.getPin()
					+ " = " + event.getState());
			if (event.getState().equals(PinState.LOW)) {
				gate.setClosed();
			} else if (event.getState().equals(PinState.HIGH)) {
				gate.setOpening();
			}
		});
		Logger.log("---gateClosed in registered---");

		remote = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_UP);
		remote.setShutdownOptions(true);
		remote.addListener((GpioPinListenerDigital) event -> {
			// display pin state on console
			System.out.println(sdf.format(new Date()) + " --> remote PIN STATE CHANGE: " + event.getPin() + " = "
					+ event.getState());
			if (event.getState().equals(PinState.LOW)) {
				Control.cycleDoor();
			}
		});
		Logger.log("---remote in registered---");

		door = gpio.provisionDigitalInputPin(RaspiPin.GPIO_12, PinPullResistance.PULL_UP);
		door.setShutdownOptions(true);
		door.addListener((GpioPinListenerDigital) event -> {
			// display pin state on console
			System.out.println(sdf.format(new Date()) + " --> door PIN STATE CHANGE: " + event.getPin() + " = "
					+ event.getState());
			if (event.getState().equals(PinState.LOW)) {
				Control.door.setOpen();
			} else if (event.getState().equals(PinState.HIGH)) {
				Control.door.setClosed();
			}
		});
		Logger.log("---door in registered---");

		lb = gpio.provisionDigitalInputPin(RaspiPin.GPIO_13, PinPullResistance.PULL_UP);
		lb.setShutdownOptions(true);
		lb.addListener((GpioPinListenerDigital) event -> {
			if (event.getState().equals(PinState.LOW)) {
				Control.lightbarrier.setOpen();
				System.out.println(sdf.format(new Date()) + " --> lightbarrier PIN STATE CHANGE: " + event.getPin()
						+ " = BAD");
			} else if (event.getState().equals(PinState.HIGH)) {
				Control.lightbarrier.isClosed();
				System.out.println(sdf.format(new Date()) + " --> lightbarrier PIN STATE CHANGE: " + event.getPin()
						+ " = GOOD");
			}
		});
		Logger.log("---lightbarrier in registered---");
	}

	public static void toggleGateGpio() {
		trigger.pulse(400, true);
	}

	public static void activateLbGpio() {
		lbw.high();
	}

	public static void deactivateLbGpio() {
		lbw.low();
	}

}
