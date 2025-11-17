package de.wroracer.storagecompartmentui.domain;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Data {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd_HH:mm:ss")
    private LocalDateTime time;

    private String formattedTime;

    private int boxes;
    private long temperature;
    private long humidity;
    private long pressure;

    public int getBoxes() {
        return boxes;
    }

    public long getHumidity() {
        return humidity;
    }

    public long getPressure() {
        return pressure;
    }

    public long getTemperature() {
        return temperature;
    }

    public void setBoxes(int boxes) {
        this.boxes = boxes;
    }

    public void setHumidity(long humidity) {
        this.humidity = humidity;
    }

    public void setPressure(long pressure) {
        this.pressure = pressure;
    }

    public void setTemperature(long temperature) {
        this.temperature = temperature;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public void setFormattedTime(String formattedTime) {
        this.formattedTime = formattedTime;
    }

    private long distance;

    public LocalDateTime getTime() {
        return time;
    }

    public long getDistance() {
        return distance;
    }

    public void setDistance(long distance) {
        this.distance = distance;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
