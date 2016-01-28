package com.konexy.web;

import com.konexy.integrate.Controller;
import com.konexy.integrate.MqttConnector;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.enterprise.context.ApplicationScoped;
import java.io.StringReader;

/**
 * Created by konexy on 12/11/2015.
 */
@ApplicationScoped
@ServerEndpoint("/message.action")
public class WebAppServer {
    private final String deviceId = "DemoLumi"; // your device Id

    public static final String SEND_CONTROL = "c";
    public static final String SEND_GET_DEVICE_STATUS = "s";

    Controller controller;

    @Inject
    private SessionHandle sessionHandle;

    public WebAppServer() {
        sessionHandle = new SessionHandle();
    }

    @OnOpen
    public void open(Session session) {
        sessionHandle.addSession(session);
        String clientId = session.getRequestParameterMap().get("uid").toString();

        // connect to Konexy for subscribe
        controller = new Controller(sessionHandle, clientId);

        // Get devices state
        getDeviceState(clientId);
    }

    @OnClose
    public void close(Session session) {
        if(sessionHandle==null)
            return;

        sessionHandle.removeSession(session);

        if (sessionHandle.countSession()==0) {
            MqttConnector mqttConnector = new MqttConnector();
            mqttConnector.clearSubscribes();
        }
    }

    @OnMessage
    public void handleMessage(String message, Session session) {
        String clientId = session.getRequestParameterMap().get("uid").toString();
        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonObject jsonMessage = reader.readObject();
            String subdevice = (jsonMessage.getJsonString("sub-device")==null) ? "" : jsonMessage.getString("sub-device");

            if("light".equals(subdevice)) {
                String action = jsonMessage.getString("action");
                int subdeviceNo = jsonMessage.getInt("no");
                // Send control message via Konexy
                String controlMessage = "{\"type\":\"" + SEND_CONTROL + "\",\"light\":\"" + action + "\",\"no\":" + subdeviceNo + "}";
                controller.publish(clientId, controlMessage);
            }
        } catch (Exception ex) {
            System.out.println(message);
            // Log error
        }
    }

    @OnError
    public void error(Throwable error) {
        // Another log here
        System.out.println(error);
    }

    public void getDeviceState(String clientId) {
        String controlMessage = "{\"type\":\"" + SEND_GET_DEVICE_STATUS + "\"}";
        controller.publish(clientId, controlMessage, false);
    }
}
