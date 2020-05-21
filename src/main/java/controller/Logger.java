package controller;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class Logger {
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static Connection con = null;
	public static LinkedList<String> loglist = new LinkedList<>();
	public static MqttClient client = null;

	public static Connection createCon() throws SQLException {
		if (con != null && !con.isClosed()) {
			return con;
		}
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://debian.fritz.box/hoftor?user=hoftor&password=8pjh3pxS89MjskxO");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return con;
	}

	public static void closeCon() {
		try {
			if (con != null) {
				con.close();
			}
		} catch (SQLException e) {
			System.out.println("Con schie�en fehlgeschlagen");
			e.printStackTrace();
		}
	}

	public static void logTempSQL(double temp) {
		String time = sdf.format(new Date());
		try {
			createCon();
			PreparedStatement psmt = con.prepareStatement("INSERT INTO temperature (date, temp) VALUES (?, ?)");
			psmt.setString(1, time);
			psmt.setDouble(2, temp);
			psmt.executeUpdate();
			psmt.close();
			closeCon();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void logAccessSQL(String[] user, String action) {
		String time = sdf.format(new Date());
		try {
			createCon();
			PreparedStatement psmt = con
					.prepareStatement("INSERT INTO actions (date, ip, user, action) VALUES (?, ?, ?, ?)");
			psmt.setString(1, time);
			psmt.setString(2, user[1]);
			psmt.setString(3, user[0]);
			psmt.setString(4, action);
			psmt.executeUpdate();
			psmt.close();
			closeCon();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void log(String data) {
		if (loglist.size() >= 10) {
			loglist.removeFirst();
		}
		String output = sdf.format(new Date()) + ": </br>" + data;
		loglist.add(output);
		System.out.println(output);
	}

	public static MqttClient mqttCon() {
		try {
			if (client == null) {
				client = new MqttClient("tcp://debian.fritz.box", "Hoftor");
			}
			if (!client.isConnected()) {
				client.connect();
			}
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.getMessage();
		}
		return client;
	}

	public static void sendMqtt(String topic, String input) {
		try {
			MqttMessage message = new MqttMessage(input.getBytes());
			mqttCon().publish("home/outside/input/gate/" + topic, message);
		} catch (MqttException e) {
			Logger.log(topic + " MQTT " + input + " konnte nicht gesendet werden.");
			System.out.println(e.getMessage());
		}

	}

	public static void disconnectMqtt() {
		try {
			client.disconnect();
			client.close();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
