package web;

import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import controller.Control;
import controller.Logger;

@WebServlet("/Webui")
public class WebHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	SimpleDateFormat sdf = new SimpleDateFormat("'['HH:mm:ss']'");

	public WebHandler() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (request.getParameter("action").equals("openDoor")) {
			Control.openDoor(getAuth(request), false);
		} else if (request.getParameter("action").equals("closeDoor")) {
			Control.closeDoor(getAuth(request), false);
		} else if (request.getParameter("action").equals("stopDoor")) {
			Control.stopDoor(getAuth(request), false);
		} else if (request.getParameter("action").equals("openAutoCloseDoor")) {
			Control.openAutoCloseDoor(getAuth(request), false);
		} else if (request.getParameter("action").equals("test")) {
			Logger.log("Test. Message: " + request.getParameter("message"));
		} else if (request.getParameter("action").equals("lock")) {
			if (request.getParameter("state").equals("true")) {
				Control.state[Control.LOCKED] = 1;
			} else if (request.getParameter("state").equals("false")) {
				Control.state[Control.LOCKED] = 0;
			}
		} else if (request.getParameter("action").equals("kill-auto")) {
			Control.killAuto(getAuth(request));
		}

		StringBuilder info = new StringBuilder("<state>\n" + "<gate>" + Control.state[Control.GATE] + "</gate>\n"
				+ "<door>" + Control.state[Control.DOOR] + "</door>\n" + "<lb>" + Control.state[Control.LB] + "</lb>\n"
				+ "<moving>" + Control.state[Control.MOVING] + "</moving>\n" + "<auto>" + Control.state[Control.AUTO]
				+ "</auto>\n" + "<locked>" + Control.state[Control.LOCKED] + "</locked>\n" + "<temp>" + Control.temp
				+ "</temp>\n" + "<status><![CDATA[");

		for (int i = Logger.loglist.size() - 1; i >= 0; i--) {
			// for (String current : Logger.loglist) {
			info.append(Logger.loglist.get(i));
			info.append("</br>");
		}
		info.append("]]></status>\n</state>\n");
		response.setContentType("text/xml;charset=UTF-8");
		response.getWriter().println(info.toString());

	}

	protected String[] getAuth(HttpServletRequest request) {
		String[] ret = new String[2];
		String authorization = request.getHeader("X-Remote-User");
		String xforwardedfor = request.getHeader("X-Forwarded-For");
		String sourceip = request.getRemoteAddr();
		if (authorization != null) {
			ret[0] = authorization;
		} else {
			ret[0] = "unknown";
		}
		if (xforwardedfor != null) {
			ret[1] = xforwardedfor;
		} else {
			ret[1] = sourceip;
		}
		return ret;
	}

}
