package de.wroracer.storagecompartmentui.service;

import de.wroracer.storagecompartmentui.domain.MQTTMessage;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@ApplicationScope
public class MQTTService {
    private final static Logger LOGGER = LoggerFactory.getLogger(DataService.class);
    private final IMqttClient publisher;
    private final List<MQTTMessage> messages = new ArrayList<>();

    public MQTTService(@Value("${mqtt.client.id}") String mqttClientId, @Value("${mqtt.server.url}") String mqttServerUrl) {
        IMqttClient tmpPublisher;
        try {
            tmpPublisher = new MqttClient(mqttServerUrl, mqttClientId);
            tmpPublisher.connect();
        } catch (MqttException e) {
            LOGGER.warn("MQTT Server connection Problem", e);
            tmpPublisher = null;
        }
        publisher = tmpPublisher;
    }

    /**
     * Subscriben zu einem Topic auf dem MQTT Broker
     *
     * @param topic
     * @param listener message Listener
     * @return status 'true'=Verbunden && Subscribed; 'false'=Kein Verbindung || Fehler beim Subscriben
     */
    public boolean subscribe(String topic, IMqttMessageListener listener) {
        if (isConnected()) {
            try {
                publisher.subscribe(topic, listener);
                return true;
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     *
     * @return Verbindungs status zum MQTT Brocker
     */
    public boolean isConnected() {
        if (publisher != null) {
            return publisher.isConnected();
        }
        return false;
    }

    /**
     * Publishe eine Nachricht an ein Topic
     *
     * @param topic
     * @param msgS  nachricht
     * @return status 'true'=Verbunden && Gesendet; 'false'=Kein Verbindung || Fehler beim Senden
     */
    public boolean publish(String msgS, String topic) {
        if (isConnected()) {
            MqttMessage msg = new MqttMessage(msgS.getBytes(StandardCharsets.UTF_8));
            msg.setQos(0);
            msg.setRetained(true);
            try {
                publisher.publish(topic, msg);
                return true;
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}
