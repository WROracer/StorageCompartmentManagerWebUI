package de.wroracer.storagecompartmentui.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wroracer.storagecompartmentui.domain.Data;
import de.wroracer.storagecompartmentui.domain.MQTTMessage;
import de.wroracer.storagecompartmentui.events.DataReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

@Service
@ApplicationScope
public class DataService {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataService.class);
    private final MQTTService mqttService;
    private final ObjectMapper mapper;
    private final Path dataFolder;
    private final List<Consumer<List<Data>>> consumers = new ArrayList<>();
    private final String SENSOR_DATA_TOPIC = "sensor/storage";
    int updateCnt = 0;
    // Event-Publisher
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public DataService(MQTTService mqttService, ObjectMapper mapper, @Value("${mqtt.data.saveFolder}") String dataFolderStr) {
        this.mqttService = mqttService;
        this.mapper = mapper;
        dataFolder = Path.of(dataFolderStr);

        mqttService.subscribe(SENSOR_DATA_TOPIC, (topic, msg) -> {
            byte[] payload = msg.getPayload();
            // ... payload handling omitted
            String smsg = new String(payload);
            System.out.println("Recived message: " + smsg + "; qos: " + msg.getQos());
            MQTTMessage message = new MQTTMessage();
            message.setTopic(topic);
            message.setMsg(smsg);
            message.setQos(msg.getQos());
            message.setRecived(LocalDateTime.now());
            onMQTTMessage(message);
        });
    }

    private void onMQTTMessage(MQTTMessage msg) {
        LOGGER.debug("Got MQTT Message");
        if (msg.getTopic().equals(SENSOR_DATA_TOPIC)) {
            try {
                Data data = mapper.readValue(msg.getMsg(), Data.class);
                data.setPressure(data.getPressure() / 100000d);
                List<Data> dtLst = loadData(msg.getTopic());

                dtLst.add(data);
                saveData(dtLst, msg.getTopic());
                /*updateCnt++;*/
                if (updateCnt % 10 == 0) {
                    publishDataReceived(data, dtLst);
                    updateCnt = 0;
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Data> loadData(String type) {
        Path file = dataFolder.resolve(type + ".json");
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(file);
            TypeReference<List<Data>> typeReference =
                    new TypeReference<>() {
                    };
            return mapper.readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveData(List<Data> data, String type) {
        Path file = dataFolder.resolve(type + ".json");
        if (!Files.exists(file)) {
            if (!Files.exists(file.getParent())) {
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
        formatData(data);
        try {
            String json = mapper.writeValueAsString(data);
            Files.writeString(file, json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void publishDataReceived(Data data, List<Data> allData) {
        DataReceivedEvent event = new DataReceivedEvent(this, data, allData);
        eventPublisher.publishEvent(event);
    }

    private static void formatData(List<Data> data) {
        data.forEach(d -> {
            //d.setFormattedTime(d.getTime().format(DateTimeFormatter.ofPattern("hh:mm:ss dd.MM.yyyy")));
            if (d.getDistance() != null && d.getDistance() < 0) {
                d.setDistance(null);
            }
        });
        data.sort(new Comparator<Data>() {
            @Override
            public int compare(Data o1, Data o2) {
                return o1.getTime().compareTo(o2.getTime());
            }
        });
    }

    public List<String> getAllTypes() {
        try {
            return Files.list(dataFolder).map(f -> f.getFileName().toString().replace(".json", "")).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testData() {
        Data data = new Data();
        data.setDistance(new Random().nextLong());
        data.setTime(LocalDateTime.now());
        try {
            String json = mapper.writeValueAsString(data);
            mqttService.publish(json, "sensors");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void onUpdate(Consumer<List<Data>> consumer) {
        consumers.add(consumer);
    }

    public List<Data> getSensorData() {
        List<Data> data = loadData(SENSOR_DATA_TOPIC);
        return data;
    }

    public void smoothData(List<Data> dataList) {
        smoothBoxes(dataList);     // Glätten der "boxes"-Daten
        smoothTemperature(dataList);  // Glätten der "temperature"-Daten
        smoothHumidity(dataList);    // Glätten der "humidity"-Daten
        smoothPressure(dataList);    // Glätten der "pressure"-Daten
    }

    // Glättung der "boxes"-Werte
    public static void smoothBoxes(List<Data> dataList) {
        if (dataList == null || dataList.size() < 2) return;

        Integer currentValue = null;  // Wert, den wir aktuell glätten
        int startIndex = -1;         // Startindex des Bereichs mit gleichen Werten

        for (int i = 0; i < dataList.size(); i++) {
            Integer value = dataList.get(i).getBoxes();

            if (currentValue == null || !currentValue.equals(value)) {
                // Wenn wir einen neuen Wert finden, speichern wir den Start des Bereichs
                currentValue = value;
                startIndex = i;
            }

            // Wenn wir das Ende des Bereichs erreicht haben (oder die Liste endet)
            if (i == dataList.size() - 1 || !value.equals(dataList.get(i + 1).getBoxes())) {
                // Wenn der Bereich größer als 2 ist, setze die mittleren Werte auf null
                if (i - startIndex > 1) {
                    for (int j = startIndex + 1; j < i; j++) {
                        dataList.get(j).setBoxes(null);  // Setze den mittleren Wert auf null
                    }
                }
            }
        }
    }

    // Glättung der "temperature"-Werte
    public static void smoothTemperature(List<Data> dataList) {
        if (dataList == null || dataList.size() < 2) return;

        Long currentValue = null;  // Wert, den wir aktuell glätten
        int startIndex = -1;       // Startindex des Bereichs mit gleichen Werten

        for (int i = 0; i < dataList.size(); i++) {
            Long value = dataList.get(i).getTemperature();

            if (currentValue == null || !currentValue.equals(value)) {
                // Wenn wir einen neuen Wert finden, speichern wir den Start des Bereichs
                currentValue = value;
                startIndex = i;
            }

            // Wenn wir das Ende des Bereichs erreicht haben (oder die Liste endet)
            if (i == dataList.size() - 1 || !value.equals(dataList.get(i + 1).getTemperature())) {
                // Wenn der Bereich größer als 2 ist, setze die mittleren Werte auf null
                if (i - startIndex > 1) {
                    for (int j = startIndex + 1; j < i; j++) {
                        dataList.get(j).setTemperature(null);  // Setze den mittleren Wert auf null
                    }
                }
            }
        }
    }

    // Glättung der "humidity"-Werte
    public static void smoothHumidity(List<Data> dataList) {
        if (dataList == null || dataList.size() < 2) return;

        Long currentValue = null;  // Wert, den wir aktuell glätten
        int startIndex = -1;       // Startindex des Bereichs mit gleichen Werten

        for (int i = 0; i < dataList.size(); i++) {
            Long value = dataList.get(i).getHumidity();

            if (currentValue == null || !currentValue.equals(value)) {
                // Wenn wir einen neuen Wert finden, speichern wir den Start des Bereichs
                currentValue = value;
                startIndex = i;
            }

            // Wenn wir das Ende des Bereichs erreicht haben (oder die Liste endet)
            if (i == dataList.size() - 1 || !value.equals(dataList.get(i + 1).getHumidity())) {
                // Wenn der Bereich größer als 2 ist, setze die mittleren Werte auf null
                if (i - startIndex > 1) {
                    for (int j = startIndex + 1; j < i; j++) {
                        dataList.get(j).setHumidity(null);  // Setze den mittleren Wert auf null
                    }
                }
            }
        }
    }

    // Glättung der "pressure"-Werte
    public static void smoothPressure(List<Data> dataList) {
        if (dataList == null || dataList.size() < 2) return;

        Double currentValue = null;  // Wert, den wir aktuell glätten
        int startIndex = -1;         // Startindex des Bereichs mit gleichen Werten

        for (int i = 0; i < dataList.size(); i++) {
            Double value = dataList.get(i).getPressure();

            if (currentValue == null || !currentValue.equals(value)) {
                // Wenn wir einen neuen Wert finden, speichern wir den Start des Bereichs
                currentValue = value;
                startIndex = i;
            }

            // Wenn wir das Ende des Bereichs erreicht haben (oder die Liste endet)
            if (i == dataList.size() - 1 || !value.equals(dataList.get(i + 1).getPressure())) {
                // Wenn der Bereich größer als 2 ist, setze die mittleren Werte auf null
                if (i - startIndex > 1) {
                    for (int j = startIndex + 1; j < i; j++) {
                        dataList.get(j).setPressure(null);  // Setze den mittleren Wert auf null
                    }
                }
            }
        }
    }

    public void lockDevice() {
        mqttService.publish("sensors/error", "ERROR");
    }

    public boolean isMQQTConnected() {
        return mqttService.isConnected();
    }
}
