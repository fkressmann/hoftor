package web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import controller.Control;
import controller.GpioHandler;
import model.Status;
import model.User;

@WebServlet("/service")
public class ServiceMenu extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ServiceMenu() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String message = null;

        if (request.getParameter("action") != null) {
            switch (request.getParameter("action")) {
                case "sendSignal":
                    GpioHandler.toggleGateGpio();
                    message = "Status GPIO toggled.";
                    break;
                case "cancelActionThread":
                    Control.killAuto(new User("Service"));
                    break;
                case "activateLB":
                    GpioHandler.activateLbGpio();
                    message = "LB GPIO activated.";
                    break;
                case "deactivateLB":
                    GpioHandler.deactivateLbGpio();
                    message = "LB GPIO deactivated.";
                    break;
                case "reinitialzie":
                    Control.initGpioRead();
                    message = "Init GPIO read triggered.";
                    break;
            }
        }

        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().println(generateHTML(message));

    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        Control.gate.state = Status.parse(req.getParameter("gate"));
        Control.gate.moving = Boolean.parseBoolean(req.getParameter("moving"));
        Control.door.state = Status.parse(req.getParameter("door"));
        Control.lightbarrier.state = Status.parse(req.getParameter("lb"));
        Control.passiveMode = Boolean.parseBoolean(req.getParameter("passiveMode"));
        if (req.getParameter("lbW").equals("1")) {
            GpioHandler.activateLbGpio();
        } else {
            GpioHandler.deactivateLbGpio();
        }
        String message = "Set \r\nGATE to " + Control.gate.state + "\r\n MOVING to "
                + Control.gate.moving + "\r\n DOOR to " + Control.door.state + "\r\n LB to "
                + Control.lightbarrier.state + "\r\n Passive Mode to "
                + Control.passiveMode;

        res.setContentType("text/html;charset=UTF-8");
        res.getWriter().println(generateHTML(message));

    }

    protected String generateHTML(String message) {
        if (message != null) {
            message = "<div class=\"alert alert-info\">\r\n" + message + "</div>\r\n";
        } else {
            message = "";
        }
        int actionthread = 0;
        if (Control.actionthread.isAlive()) {
            actionthread = 1;
        }

        int lbw;
        if (GpioHandler.lbw.isHigh()) {
            lbw = 1;
        } else {
            lbw = 0;
        }

        return "<html>\r\n" + "<head>\r\n" + "<title>Service-Menu</title>\r\n"
                + "<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">\r\n"
                + "<meta charset=\"utf-8\"> \r\n" + "</head>\r\n" + "<body>\r\n" + "<div class=\"container\">\r\n"
                + "  <h1>Hoftor Service Menu</h1>\r\n" + message
                + "  <a href=\"service?action=sendSignal\" class=\"btn btn-info\" role=\"button\">Sende Signal manuell</a>\r\n"
                + "  <a href=\"service?action=reinitialzie\" class=\"btn btn-info\" role=\"button\">Neu initialisieren</a>\r\n"
                + "  <p>ActionThread running?: " + actionthread + " </p>\r\n"
                + "  <a href=\"service?action=cancelActionThread\" class=\"btn btn-info\" role=\"button\">ActionThread abbrechen</a>\r\n"
                + "  <a href=\"service?action=activateLB\" class=\"btn btn-info\" role=\"button\">Lichtschranke aktivieren</a>\r\n"
                + "  <a href=\"service?action=deactivateLB\" class=\"btn btn-info\" role=\"button\">Lichtschranke deaktivieren</a>\r\n"
                + "  <p>Achtung, deaktivieren nicht vergessen!</p>\r\n" + "  <h2>Variablen manuell setzen</h2>\r\n"
                + "  <form method=\"post\" action=\"service\">\r\n"
                + "  <label for=\"gate\">Status 0=zu 1=auf 2=öffnet 3=schließt:</label>\r\n"
                + "  <input type=\"text\" name=\"gate\" class=\"form-control\" id=\"gate\" value=\""
                + Control.gate.state + "\" /> <br/>\r\n" + "  <label for=\"moving\">Moving:</label>\r\n"
                + "  <input type=\"text\" name=\"moving\" class=\"form-control\" id=\"moving\" value=\""
                + Control.gate.isMoving() + "\" /> <br/>\r\n" + "  <label for=\"door\">Door:</label>\r\n"
                + "  <input type=\"text\" name=\"door\" class=\"form-control\" id=\"door\" value=\""
                + Control.door.state + "\" /> <br/>\r\n" + "  <label for=\"lb\">LB:</label>\r\n"
                + "  <input type=\"text\" name=\"lb\" class=\"form-control\" id=\"lb\" value=\""
                + Control.lightbarrier.state + "\" /> <br/>\r\n" + "  <label for=\"lbW\">LB-W:</label>\r\n"
                + "  <input type=\"text\" name=\"lbW\" class=\"form-control\" id=\"lbW\" value=\""
                + lbw + "\" /> <br/>\r\n"
                + " <label for=\"passiveMode\">Passive mode:</label>\r\n"
                + "  <input type=\"text\" name=\"passiveMode\" class=\"form-control\" id=\"passiveMode\" value=\""
                + Control.passiveMode + "\" /> <br/>\r\n" + "  <input type=\"submit\" name='submit' class=\"btn\" />\r\n"
                + "  </form>\r\n" + "</div>\r\n" + "</body>\r\n" + "</html>";
    }

}
