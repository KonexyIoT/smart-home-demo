package com.konexy.integrate;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.enterprise.context.ApplicationScoped;
import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by konexy on 12/11/2015.
 */
@ApplicationScoped
public class MqttConnector implements MqttCallback{
    private static final String KEYSTORE_PATH = "/konexy_client.jks";
    private static final char[] KEYSTORE_PASSWORD = "konexy@2014".toCharArray();

    private static final int MQTT_CONNECTION_CONNECTION_TIME_OUT_DEFAULT = 120; // 2 min
    private static final int MQTT_CONNECTION_KEEP_ALIVE_DEFAULT = 180;  // 3 min

    private boolean handleSendEvent = true;

    // Ready for multiple subscribe
    private final Set<MqttClient> mqttClients = new HashSet<>();

    private static Controller controller;

    public MqttConnector() {}

    public MqttConnector(Controller controller) {
        this.controller = controller;
    }

    public void clearSubscribes() {
        for (MqttClient client : mqttClients) {
            try {
                client.disconnect();
                client.close();
            } catch (MqttException me) {
                System.out.println(me.getMessage());
            }
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        // @todo: close or warning application when the mqtt connection lost
        System.out.println("mqtt gone.");
    }

    @Override
    public void messageArrived(String message, MqttMessage mqttMessage) throws Exception {
        controller.logController(message, mqttMessage);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        if (handleSendEvent) {
            controller.publishSuccess();
        }

        handleSendEvent = true; // Finish sending first update
    }

    public boolean subscribe(String broker, String clientId, String topic, int qos, String username, String pwd) {
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient mqttClient = null;

        try {
            mqttClient = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(pwd.toCharArray());
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(MQTT_CONNECTION_CONNECTION_TIME_OUT_DEFAULT);
            connOpts.setKeepAliveInterval(MQTT_CONNECTION_KEEP_ALIVE_DEFAULT);

            try{
                SSLSocketFactory sslSocketFactory = configureSSLSocketFactory();
                connOpts.setSocketFactory(sslSocketFactory);
            } catch (Exception ex) {
                System.out.println("configureSSL failure");
                System.out.println(ex.getMessage());
            }

            mqttClient.setCallback(this);
            mqttClient.connect(connOpts);
            mqttClient.subscribe(topic, qos);
            mqttClients.add(mqttClient);
        } catch(MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("except " + me);
            me.printStackTrace();

            try {
                if (mqttClient != null){
                    mqttClient.disconnect();
                }
            } catch (MqttException me2) {
                System.out.println(me2.getMessage());
            }

            try {
                if (mqttClient != null){
                    mqttClient.close();
                }
            } catch (MqttException me3) {
                System.out.println(me3.getMessage());
            }

            return false;
        }

        return true;
    }

    public boolean publish(String broker, String clientId, String topic, int qos, String username, String pwd,
                           String message){
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient mqttClient = null;

        try {
            mqttClient = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(pwd.toCharArray());
            connOpts.setCleanSession(true);

            try{
                SSLSocketFactory sslSocketFactory = configureSSLSocketFactory();
                connOpts.setSocketFactory(sslSocketFactory);
            } catch (Exception ex) {
                System.out.println("configureSSL failure");
                System.out.println(ex.getMessage());
            }

            mqttClient.setCallback(this);
            mqttClient.connect(connOpts);

            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            mqttClient.publish(topic, mqttMessage);
            mqttClient.disconnect();
            mqttClient.close();
        } catch(MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("except " + me);
            me.printStackTrace();

            try {
                if (mqttClient != null){
                    mqttClient.disconnect();
                }
            } catch (MqttException me2) {
                System.out.println(me2.getMessage());
            }

            try {
                if (mqttClient != null){
                    mqttClient.close();
                }
            } catch (MqttException me3) {
                System.out.println(me3.getMessage());
            }

            return false;
        }

        return true;
    }

    private static SSLSocketFactory configureSSLSocketFactory() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream in = null;
        try {
            File file = new File(KEYSTORE_PATH);
            in = new FileInputStream(file);
            ks.load(in, KEYSTORE_PASSWORD);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, KEYSTORE_PASSWORD);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sc = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = tmf.getTrustManagers();
        sc.init(kmf.getKeyManagers(), trustManagers, null);

        SSLSocketFactory ssf = sc.getSocketFactory();

        return ssf;
    }

    public void setHandleSendEvent(boolean handleSendEvent) {
        this.handleSendEvent = handleSendEvent;
    }
}
