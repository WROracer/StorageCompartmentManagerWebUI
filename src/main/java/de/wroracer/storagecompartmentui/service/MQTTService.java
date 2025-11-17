package de.wroracer.storagecompartmentui.service;

import de.wroracer.storagecompartmentui.config.MQTTConfig;
import de.wroracer.storagecompartmentui.domain.MQTTMessage;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@ApplicationScope
public class MQTTService {

    private final IMqttClient publisher;
    private List<MQTTMessage> messages = new ArrayList<>();
    private HashMap<UUID, Consumer<MQTTMessage>> events = new HashMap<>();

    public MQTTService(@Value("${mqtt.client.id}") String mqttClientId, @Value("${mqtt.server.url}") String mqttServerUrl){
        try {
            publisher = new MqttClient(mqttServerUrl,mqttClientId);
            publisher.connect();
            publisher.subscribe("#", (topic, msg) -> {
                byte[] payload = msg.getPayload();
                // ... payload handling omitted
                String smsg = new String(payload);
                System.out.println("Recived message: "+smsg+"; qos: "+msg.getQos());
                MQTTMessage message = new MQTTMessage();
                message.setTopic(topic);
                message.setMsg(smsg);
                message.setQos(msg.getQos());
                message.setRecived(LocalDateTime.now());
                messages.add(message);
                events.values().forEach(c->c.accept(message));
            });
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID registerListener(Consumer<MQTTMessage> listener){
        UUID uuid = UUID.randomUUID();
        events.put(uuid,listener);
        return uuid;
    }

    public void removeListener(UUID uuid){
        events.remove(uuid);
    }

    public List<MQTTMessage> getMessages() {
        return messages;
    }

    public void publishTest(){
        MqttMessage msg = new MqttMessage("Hello World".getBytes(StandardCharsets.UTF_8));
        msg.setQos(0);
        msg.setRetained(true);
        try {
            publisher.publish("test/testTopicFelix",msg);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public void publish(String msgS,String topic){
        MqttMessage msg = new MqttMessage(msgS.getBytes(StandardCharsets.UTF_8));
        msg.setQos(0);
        msg.setRetained(true);
        try {
            publisher.publish(topic,msg);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean test() {
        return true;
    }
}
