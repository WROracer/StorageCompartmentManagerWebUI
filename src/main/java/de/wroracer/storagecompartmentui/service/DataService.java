package de.wroracer.storagecompartmentui.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.Signal;
import de.wroracer.storagecompartmentui.domain.Data;
import de.wroracer.storagecompartmentui.domain.MQTTMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

@Service
@ApplicationScope
public class DataService {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataService.class);


    private Path dataFolder;

    private final MQTTService mqttService;

    private final ObjectMapper mapper;

    private ListSignal<Data> sensorDataSignal = new ListSignal<>(Data.class);

    int updateCnt = 0;

    public DataService(MQTTService mqttService,ObjectMapper mapper,@Value("${mqtt.data.saveFolder}") String dataFolderStr){
        this.mqttService = mqttService;
        this.mapper = mapper;
        mqttService.registerListener(this::onMQTTMessage);
        dataFolder = Path.of(dataFolderStr);

        List<Data> sensorData = loadData("sensor/storage");
        Signal.runWithoutTransaction(()->{
        for (Data sensorDatum : sensorData) {
            sensorDataSignal.insertFirst(sensorDatum);
        }});
    }

    public ListSignal<Data> getSensorDataSignal() {
        return sensorDataSignal;
    }

    private void onMQTTMessage(MQTTMessage msg) {
        LOGGER.debug("Got MQTT Message");
        if (msg.getTopic().equals("sensor/storage")) {
            try {
                Data data = mapper.readValue(msg.getMsg(), Data.class);
                List<Data> dtLst = loadData(msg.getTopic());
                dtLst.add(data);
                saveData(dtLst, msg.getTopic());
                updateCnt++;
                if (msg.getTopic().equals("sensor/storage") && updateCnt%10 == 0) {
                    for (Consumer<List<Data>> consumer : consumers) {
                        consumer.accept(dtLst);
                    }
                    updateCnt = 0;
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> getAllTypes(){
        try {
            return  Files.list(dataFolder).map(f->f.getFileName().toString().replace(".json","")).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Data> loadData(String type){
        Path file = dataFolder.resolve(type+".json");
        if (!Files.exists(file)){
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(file);
            TypeReference<List<Data>> typeReference =
                    new TypeReference<>() {};
            return mapper.readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveData(List<Data> data, String type){
        Path file = dataFolder.resolve(type+".json");
        if (!Files.exists(file)){
            if (!Files.exists(file.getParent())){
                try {
                    Files.createDirectories(file.getParent());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                Files.createFile(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        data.forEach(d->d.setFormattedTime(d.getTime().format(DateTimeFormatter.ofPattern("hh:mm:ss dd.MM.yyyy"))));
        data.sort(new Comparator<Data>() {
            @Override
            public int compare(Data o1, Data o2) {
                return o1.getTime().compareTo(o2.getTime());
            }
        });
        try {
            String json =  mapper.writeValueAsString(data);
            Files.writeString(file,json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testData(){
        Data data = new Data();
        data.setDistance(new Random().nextInt());
        data.setTime(LocalDateTime.now());
        try {
            String json = mapper.writeValueAsString(data);
            mqttService.publish(json,"sensors");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Consumer<List<Data>>> consumers = new ArrayList<>();

    public void onUpdate(Consumer<List<Data>> consumer) {
        consumers.add(consumer);
    }
}
