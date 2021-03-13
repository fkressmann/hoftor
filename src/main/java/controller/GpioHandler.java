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
import static controller.Logger.logToConsole;
import static controller.Logger.sdf;

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

	public static void initializeGpio() {
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
			if (event.getState().isLow()) {
				logPinChange("gateOpen", "OPEN");
				gate.setOpen();
			} else if (event.getState().isHigh()) {
				logPinChange("gateOpen", "NOT OPEN");
				gate.setClosing();
			}
		});
		Logger.log("---gateOpened in registered---");

		gateClosed = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_UP);
		gateClosed.setShutdownOptions(true);
		gateClosed.addListener((GpioPinListenerDigital) event -> {
			if (event.getState().isLow()) {
				logPinChange("gateClosed", "CLOSED");
				gate.setClosed();
			} else if (event.getState().isHigh()) {
				logPinChange("gateClosed", "NOT CLOSED");
				gate.setOpening();
			}
		});
		Logger.log("---gateClosed in registered---");

		remote = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_UP);
		remote.setShutdownOptions(true);
		remote.addListener((GpioPinListenerDigital) event -> {
			if (event.getState().isLow()) {
				logPinChange("remote", "PRESSED");
				if (Control.passiveMode) {
					Logger.log("Mache nichts, passiver Modus ist aktiviert.");
				} else {
					Control.cycleDoor();
				}
			} else if (event.getState().isHigh()) logPinChange("remote", "RELEASED");
		});
		Logger.log("---remote in registered---");

		door = gpio.provisionDigitalInputPin(RaspiPin.GPIO_12, PinPullResistance.PULL_UP);
		door.setShutdownOptions(true);
		door.addListener((GpioPinListenerDigital) event -> {
			if (event.getState().isLow()) {
				logPinChange("door", "OPEN");
				Control.door.setOpen();
			} else if (event.getState().isHigh()) {
				logPinChange("door", "CLOSED");
				Control.door.setClosed();
			}
		});
		Logger.log("---door in registered---");

		lb = gpio.provisionDigitalInputPin(RaspiPin.GPIO_13, PinPullResistance.PULL_UP);
		lb.setShutdownOptions(true);
		lb.addListener((GpioPinListenerDigital) event -> {
			if (event.getState().isLow()) {
				Control.lightbarrier.setOpen();
				logPinChange("lightbarrier", "INTERRUPTED");
			} else if (event.getState().isHigh()) {
				Control.lightbarrier.setClosed();
				logPinChange("lightbarrier", "CLOSED");
			}
		});
		Logger.log("---lightbarrier in registered---");
	}

	private static void logPinChange(String pin, String message) {
		logToConsole(" --> " + pin + ": PIN STATE CHANGE: " + message);
	}

	public static void toggleGateGpio() {
		Logger.logToConsole("Triggered Remote");
		trigger.pulse(400, true);
	}

	public static void beepForMs(int milliseconds) {
		beeper.pulse(milliseconds);
	}

	public static void activateLbGpio() {
		lbw.high();
	}

	public static void deactivateLbGpio() {
		lbw.low();
	}

}
