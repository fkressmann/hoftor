package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import model.User;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class Logger {
	private static final SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static Connection con = null;
	public static LinkedList<String> loglist = new LinkedList<>();

	public static void createCon() throws SQLException {
		if (con != null && !con.isClosed()) {
			return;
		}
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://homeassistant.fritz.box/hoftor?user=hoftor&password=8pjh3pxS89MjskxO");
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	public static void closeCon() {
		try {
			if (con != null) {
				con.close();
			}
		} catch (SQLException e) {
			System.out.println("Con schießen fehlgeschlagen");
			e.printStackTrace();
		}
	}

	public static void logAccessSQL(User user, String action) {
		String time = sdf.format(new Date());
		try {
			createCon();
			@SuppressWarnings("SqlResolve") PreparedStatement psmt = con
					.prepareStatement("INSERT INTO actions (date, ip, user, action) VALUES (?, ?, ?, ?)");
			psmt.setString(1, time);
			psmt.setString(2, user.getIp());
			psmt.setString(3, user.getName());
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

}
