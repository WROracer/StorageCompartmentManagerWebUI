package de.wroracer.storagecompartmentui.events;

import de.wroracer.storagecompartmentui.domain.Data;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class DataReceivedEvent extends ApplicationEvent {
    private final Data data;
    private final List<Data> allData;

    public DataReceivedEvent(Object source, Data data, List<Data> allData) {
        super(source);
        this.data = data;
        this.allData = allData;
    }

    public Data getData() {
        return data;
    }

    public List<Data> getAllData() {
        return allData;
    }
}