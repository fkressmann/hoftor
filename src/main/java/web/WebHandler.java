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
import model.User;

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
            throws IOException {

        switch (request.getParameter("action")) {
            case "openDoor":
                Control.openDoor(getAuth(request));
                break;
            case "closeDoor":
                Control.closeDoor(getAuth(request));
                break;
            case "stopDoor":
                Control.stopDoor(getAuth(request));
                break;
            case "openAutoCloseDoor":
                Control.openAutoCloseDoor(getAuth(request));
                break;
            case "test":
                Logger.log("Test. Message: " + request.getParameter("message"));
                break;
            case "lock":
                if (request.getParameter("state").equals("true")) {
                    Control.gate.lock();
                } else if (request.getParameter("state").equals("false")) {
                    Control.gate.unlock();
                }
                break;
            case "kill-auto":
                Control.killAuto(getAuth(request));
                break;
        }

        StringBuilder info = new StringBuilder("<state>\n"
                + "<gate>" + Control.gate.state + "</gate>\n"
                + "<door>" + Control.door.state + "</door>\n"
                + "<lb>" + Control.lightbarrier.state + "</lb>\n"
                + "<moving>" + Control.gate.isMoving() + "</moving>\n"
                + "<auto>" + Control.automaticActive + "</auto>\n"
                + "<locked>" + Control.gate.locked + "</locked>\n"
                + "<temp>" + Control.temp + "</temp>\n"
                + "<status><![CDATA[");

        for (int i = Logger.loglist.size() - 1; i >= 0; i--) {
            info.append(Logger.loglist.get(i));
            info.append("</br>");
        }
        info.append("]]></status>\n</state>\n");
        response.setContentType("text/xml;charset=UTF-8");
        response.getWriter().println(info.toString());

    }

    protected User getAuth(HttpServletRequest request) {
        User user = new User();
        String authorization = request.getHeader("X-Remote-User");
        String xforwardedfor = request.getHeader("X-Forwarded-For");
        String sourceip = request.getRemoteAddr();

        return new User(authorization != null ? authorization : "unknown",
                xforwardedfor != null ? xforwardedfor : sourceip);
    }

}
