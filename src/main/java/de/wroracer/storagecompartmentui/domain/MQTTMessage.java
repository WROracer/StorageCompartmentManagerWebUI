package de.wroracer.storagecompartmentui.domain;

import java.time.LocalDateTime;

public class MQTTMessage {
    private String msg;
    private int qos;
    private LocalDateTime recived;
    private String topic;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public LocalDateTime getRecived() {
        return recived;
    }

    public void setRecived(LocalDateTime recived) {
        this.recived = recived;
    }
}
