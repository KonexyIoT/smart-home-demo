package com.konexy.web;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.websocket.Session;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by konexy on 12/11/2015.
 */
@ApplicationScoped
public class SessionHandle {
    private final Set<Session> sessions = new HashSet<>();

    public void addSession(Session session) {
        if(!sessionExists(session))
            sessions.add(session);
        // @todo: send current state to all
    }

    public boolean sessionExists(Session session){
        for(Session nextS:sessions){
            if(nextS.getId().equals(session.getId()))
                return true;
        }
        return false;
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }

    public int countSession() {
        return sessions.size();
    }

    public void doAlert(String deviceId, int timer, String message) {
        System.out.println("doAlert");
        JsonProvider provider = JsonProvider.provider();
        String msg = provider.createObjectBuilder()
                .add("type", "alert")
                .add("device", deviceId)
                .add("time", timer)
                .add("aMessage", message)
                .build().toString();
        sendToAll(msg);
    }

    public void doAlert(String deviceId, int timer, String message, String subdeviceId) {
        System.out.println("doAlert");
        JsonProvider provider = JsonProvider.provider();
        String msg = provider.createObjectBuilder()
                .add("type", "alert")
                .add("device", deviceId)
                .add("sub-device", subdeviceId)
                .add("time", timer)
                .add("aMessage", message)
                .build().toString();
        sendToAll(msg);
    }

    public void drawTemperature(String deviceId, int timer, int temperature) {
        System.out.println("drawTemperature");
        JsonProvider provider = JsonProvider.provider();
        String msg = provider.createObjectBuilder()
                .add("type", "log")
                .add("device", deviceId)
                .add("time", timer)
                .add("temperature", temperature)
                .build().toString();
        sendToAll(msg);
    }

    public void drawTemperature(String deviceId, int timer, int temperature, String subdeviceId) {
        System.out.println("drawTemperature");
        JsonProvider provider = JsonProvider.provider();
        String msg = provider.createObjectBuilder()
                .add("type", "log")
                .add("device", deviceId)
                .add("sub-device", subdeviceId)
                .add("time", timer)
                .add("temperature", temperature)
                .build().toString();
        sendToAll(msg);
    }

    public void updateLightState(String deviceId, int subdeviceNo, String state) {
        System.out.println("updateLightState");
        JsonProvider provider = JsonProvider.provider();
        String msg = provider.createObjectBuilder()
                .add("type", "update")
                .add("device", deviceId)
                .add("sub-device", "light")
                .add("no", subdeviceNo)
                .add("state", state)
                .build().toString();
        sendToAll(msg);
    }

    public void ackLightState(String deviceId, int subdeviceNo, String state) {
        System.out.println("ackLightState");
        JsonProvider provider = JsonProvider.provider();
        String msg = provider.createObjectBuilder()
                .add("type", "ack")
                .add("device", deviceId)
                .add("sub-device", "light")
                .add("no", subdeviceNo)
                .add("state", state)
                .build().toString();
        sendToAll(msg);
    }

    public void actionUpdated() {
        System.out.println("actionUpdated");
        JsonProvider provider = JsonProvider.provider();
        String successMsg = provider.createObjectBuilder()
                .add("type", "message")
                .add("update", "success")
                .build().toString();
        sendToAll(successMsg);
    }

    public void finishUpdateDeviceStatus() {
        System.out.println("finishUpdateDeviceStatus");
        JsonProvider provider = JsonProvider.provider();
        String successMsg = provider.createObjectBuilder()
                .add("type", "finishUpdateDeviceStatus")
                .build().toString();
        sendToAll(successMsg);
    }

    //===================================================
    private void sendToAll(JsonObject message) {
        for (Session session:sessions) {
            sendToSession(session, message.toString());
        }
    }

    private void sendToAll(String message) {
        for (Session session:sessions) {
            sendToSession(session, message);
        }
    }

    private void sendToSession(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException ex) {
            sessions.remove(session);   // save delete
            System.out.println(ex.getMessage());
            // Another log here
        }
    }
}