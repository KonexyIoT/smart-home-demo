package com.konexy.integrate;

import com.konexy.web.SessionHandle;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.enterprise.context.ApplicationScoped;
import javax.json.*;
import java.io.StringReader;

/**
 * Created by konexy on 12/11/2015.
 */
@ApplicationScoped
public class Controller {
    private final int qos   = 1;
    private final String broker   = "ssl://iot.konexy.com:8883";
    private final String username = "konexydemo";
    private final String password = "demo@konexy.com";
    private final String deviceId = "e4otSLMf"; // your device Id
    private final String logTopicRoot = "log";

    private final String logTopic = "/" + logTopicRoot + "/" + deviceId;

    // log type
    private final String LOG_WARNING = "Warning";
    private final String LOG_NORMAL = "Log";
    private final String LOG_CONTROL = "c";
    private final String LOG_ACK = "Ack";
    private final String LOG_DEVICE_STATUS = "getAll";

    // default alert message
    private final String DEFAULT_ALERT_MSG = "Fired!!!";

    private MqttConnector mqttConnector = new MqttConnector(this);
    private static SessionHandle sessionHandle;

    public Controller() {
    }

    public Controller(SessionHandle sessionHandle, String clientId) {
        this.sessionHandle = sessionHandle;

        // subscribe topic log
        mqttConnector.subscribe(broker, clientId, logTopic, qos, username, password);
    }

    public boolean publish(String clientId, String message) {
        return mqttConnector.publish(broker, clientId, logTopic, qos, username, password, message);
    }

    public boolean publish(String clientId, String message, boolean handleSendEvent) {
        if (handleSendEvent==false){
            mqttConnector.setHandleSendEvent(handleSendEvent);
        }
        return mqttConnector.publish(broker, clientId, logTopic, qos, username, password, message);
    }

    public void publishSuccess() {
        sessionHandle.actionUpdated();
    }

    public void logController(String topic, MqttMessage mqttMessage) {
        // get deviceId
        String[] split = topic.split("log/");
        String deviceId = split[1];

        // parse log
        try {
            JsonReader reader = Json.createReader(new StringReader(mqttMessage.toString()));
            JsonObject jsonMessage = reader.readObject();
            String logType = jsonMessage.getString("type");
            switch (logType) {
                case LOG_NORMAL:
                    int temperature = 0;
                    int timer = jsonMessage.getInt("time");
                    try {
                        temperature = jsonMessage.getInt("temperature");
                    } catch (Exception se1) {
                        // @todo: warning
                        break;
                    }
                    JsonString subDeviceId = jsonMessage.getJsonString("subId");
                    if (subDeviceId==null)
                        sessionHandle.drawTemperature(deviceId, timer, temperature);
                    else
                        sessionHandle.drawTemperature(deviceId, timer, temperature, subDeviceId.toString());
                    break;
                case LOG_WARNING:
                    timer = jsonMessage.getInt("time");
                    subDeviceId = jsonMessage.getJsonString("subId");
                    int subdeviceNo = jsonMessage.getInt("number");
                    String deviceState = jsonMessage.getString("value");
                    String msg;
                    switch (subdeviceNo) {
                        case 1:
                            msg = ("0".equals(deviceState)) ? "Button release" : "Button clicked";
                            break;
                        case 2:
                            msg = ("0".equals(deviceState)) ? "Cửa cuốn được cuộn lên" : "Đang thả cửa cuốn";
                            break;
                        default:
                            msg = DEFAULT_ALERT_MSG;
                            break;
                    }
                    if (subDeviceId==null)
                        sessionHandle.doAlert(deviceId, timer, msg);
                    else
                        sessionHandle.doAlert(deviceId, timer, msg, subDeviceId.toString());
                    break;
                case LOG_CONTROL:
                    String state = jsonMessage.getString("light");
                    subdeviceNo = jsonMessage.getInt("no");
                    sessionHandle.updateLightState(deviceId, subdeviceNo, state);
                    break;
                case LOG_ACK:
                    subdeviceNo = jsonMessage.getInt("number");
                    deviceState = jsonMessage.getString("value");
                    state = ("0".equals(deviceState)) ? "off" : "on";
                    sessionHandle.ackLightState(deviceId, subdeviceNo, state);
                    break;
                case LOG_DEVICE_STATUS:
                    JsonArray lstDevice = jsonMessage.getJsonArray("status");
                    System.out.println("Update status:");
                    for (int i=0;i<lstDevice.size();i++) {
                        JsonObject device = (JsonObject) lstDevice.get(i);
                        System.out.println(device.toString());
                        subdeviceNo = device.getInt("no");
                        int intState = device.getInt("value");
                        state = (intState==0) ? "off" : "on";
                        sessionHandle.updateLightState(deviceId, subdeviceNo, state);
                    }
                    sessionHandle.finishUpdateDeviceStatus();
                    break;
                default:
            }

        } catch (Exception ex) {
            System.out.println("Parse error");
            System.out.println(topic);
            System.out.println(mqttMessage);
            System.out.println(ex.getMessage());
        }
    }
}