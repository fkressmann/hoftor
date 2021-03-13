package controller;

import clients.MqttClient;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MyServletContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		GpioHandler.gpio.unexportAll();
		GpioHandler.gpio.shutdown();
		Control.tempthread.interrupt();
		Logger.closeCon();
		MqttClient.disconnectMqtt();
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// do all the tasks that you need to perform just after the server
		// starts
		Control.init();
		// Notification that the web application initialization process is
		// starting
	}

}