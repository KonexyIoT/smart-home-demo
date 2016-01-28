package com.konexy.web;

import com.konexy.integrate.Controller;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringReader;

/**
 * Created by konexy on 12/31/2015.
 */
@WebServlet(name = "RestHandle", urlPatterns = { "/control.action" })
public class RestHandle extends HttpServlet {
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String message = request.getParameter("message");
        String clientId = request.getParameter("message");
        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonObject jsonMessage = reader.readObject();
            String subdevice = (jsonMessage.getJsonString("sub-device")==null) ? "" : jsonMessage.getString("sub-device");

            if("light".equals(subdevice)) {
                String action = jsonMessage.getString("action");
                int subdeviceNo = jsonMessage.getInt("no");
                // Send control message via Konexy
                String controlMessage = "{\"type\":\"" + WebAppServer.SEND_CONTROL + "\",\"light\":\"" + action + "\",\"no\":" + subdeviceNo + "}";
                Controller controller = new Controller();;
                controller.publish(clientId, controlMessage);
            }
        } catch (Exception ex) {
            System.out.println(message);
            // Log error
        }
    }
}
