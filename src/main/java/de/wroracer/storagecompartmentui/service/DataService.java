package de.wroracer.storagecompartmentui.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@ApplicationScope
public class DataService {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataService.class);
    private final MQTTService mqttService;
    private final ObjectMapper mapper;
    private final Path dataFolder;
    private final String lockTopic;
    private final String sensorDataTopic;

    public DataService(MQTTService mqttService, ObjectMapper mapper, @Value("${mqtt.data.saveFolder}") String dataFolderStr, @Value("${mqtt.topic.sensor.data}") String sensorTopic, @Value("${mqtt.topic.sensor.lock}") String lockTopic) {
        this.mqttService = mqttService;
        this.mapper = mapper;
        this.sensorDataTopic = sensorTopic;
        dataFolder = Path.of(dataFolderStr);
        this.lockTopic = lockTopic;

        mqttService.subscribe(sensorDataTopic, (topic, msg) -> {
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
        if (msg.getTopic().equals(sensorDataTopic)) {
            try {
                Data data = mapper.readValue(msg.getMsg(), Data.class);

                double seaLevel = 1005;
                double atmospheric = data.getPressure() / 100.0F;
                double height = 44330.0 * (1.0 - Math.pow(atmospheric / seaLevel, 0.1903));
                height = Math.round(height * 100) / 100d;
                data.setHeight(height);

                data.setPressure(data.getPressure() / 100000d);
                data.setTemperature(Math.round(data.getTemperature() * 100) / 100d);
                data.setHumidity(Math.round(data.getHumidity() * 100) / 100d);
                data.setPressure(Math.round(data.getPressure() * 10000) / 10000d);
                List<Data> dtLst = loadData(msg.getTopic());

                dtLst.add(data);
                saveData(dtLst, msg.getTopic());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Funktion zum laden von gespeicherten Daten (
     *
     * @param type / Topic, von dem die Daten geladen werden sollen.
     * @return Alle daten die von diesem Topic/Type Entfangen wurden
     */
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

    // Glättung der "Height"-Werte (inkl. letzter Wert eines gleichen Bereichs)

    /**
     * @return alle typen/topic die gespeichert sind
     */
    public List<String> getAllTypes() {
        try {
            return Files.list(dataFolder).map(f -> f.getFileName().toString().replace(".json", "")).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Alle Daten die im Sensor Topic (via Properteis configurierbar) sind
     */
    public List<Data> getSensorData() {
        List<Data> data = loadData(sensorDataTopic);
        return data;
    }

    /**
     * Hier werden alle Daten punkte Gesmooth, über die dafür exitierenden methoden.
     *
     * @param dataList Liste mit daten über denn gesmooth wird
     */
    public void smoothData(List<Data> dataList) {
        smoothBoxes(dataList);     // Glätten der "boxes"-Daten
        smoothTemperature(dataList);  // Glätten der "temperature"-Daten
        smoothHumidity(dataList);    // Glätten der "humidity"-Daten
        smoothPressure(dataList);    // Glätten der "pressure"-Daten
        smoothHeight(dataList); // Glätten der "height"-Daten
    }

    /**
     * Hier werden die daten von feld boxes geglättet, für die anzeige im Chart:
     * Das funktioniert wie folgend:
     * Daten kommen wie folgt 1 1 1 4 4 7 7 7 7 7 8 8
     * Dieses wird zu 1 null 1 null 1 4 4 7 null null null 7 8 8
     * Ein wert der auf null ist wird im Chart nicht als wert punkt angezeigt. sommit ist der chart nicht überfülllt mit datenpunkten.
     *
     * @param dataList Liste mit daten über denn gesmooth wird
     */
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

    // Glättung der "boxes"-Werte

    /**
     * Hier werden die daten von feld temperature geglättet, für die anzeige im Chart:
     * Das funktioniert wie folgend:
     * Daten kommen wie folgt 1 1 1 4 4 7 7 7 7 7 8 8
     * Dieses wird zu 1 null 1 null null 4 null 7 null null null null 8 null
     * Ein wert der auf null ist wird im Chart nicht als wert punkt angezeigt. sommit ist der chart nicht überfülllt mit datenpunkten.
     *
     * @param dataList Liste mit daten über denn gesmooth wird
     */
    public static void smoothTemperature(List<Data> dataList) {
        if (dataList == null || dataList.size() < 2) return;

        Double fistValue = dataList.getFirst().getTemperature();
        Double lastValue = dataList.getLast().getTemperature();
        Double currentValue = null;  // Wert, den wir aktuell glätten
        int startIndex = -1;       // Startindex des Bereichs mit gleichen Werten

        for (int i = 0; i < dataList.size(); i++) {
            Double value = dataList.get(i).getTemperature();

            if (currentValue == null || !currentValue.equals(value)) {
                // Wenn wir einen neuen Wert finden, speichern wir den Start des Bereichs
                currentValue = value;
                startIndex = i;
            }

            // Wenn wir das Ende des Bereichs erreicht haben (oder die Liste endet)
            if (i == dataList.size() - 1 || !value.equals(dataList.get(i + 1).getTemperature())) {
                // Wenn der Bereich größer als 2 ist, setze die mittleren Werte auf null
                if (i - startIndex > 1) {
                    // Setze alle mittleren Werte und auch das letzte Element auf null
                    for (int j = startIndex + 1; j <= i; j++) {
                        dataList.get(j).setTemperature(null);  // Setze den Wert auf null
                    }
                }
            }
        }
        dataList.getFirst().setTemperature(fistValue);
        dataList.getLast().setTemperature(lastValue);
    }

    // Glättung der "temperature"-Werte (inkl. letzter Wert eines gleichen Bereichs)

    /**
     * Hier werden die daten von feld humidity geglättet, für die anzeige im Chart:
     * Das funktioniert wie folgend:
     * Daten kommen wie folgt 1 1 1 4 4 7 7 7 7 7 8 8
     * Dieses wird zu 1 null 1 null null 4 null 7 null null null null 8 null
     * Ein wert der auf null ist wird im Chart nicht als wert punkt angezeigt. sommit ist der chart nicht überfülllt mit datenpunkten.
     *
     * @param dataList Liste mit daten über denn gesmooth wird
     */
    public static void smoothHumidity(List<Data> dataList) {
        if (dataList == null || dataList.size() < 2) return;

        Double firstValue = dataList.getFirst().getHumidity();
        Double lastValue = dataList.getLast().getHumidity();
        Double currentValue = null;  // Wert, den wir aktuell glätten
        int startIndex = -1;       // Startindex des Bereichs mit gleichen Werten

        for (int i = 0; i < dataList.size(); i++) {
            Double value = dataList.get(i).getHumidity();

            if (currentValue == null || !currentValue.equals(value)) {
                // Wenn wir einen neuen Wert finden, speichern wir den Start des Bereichs
                currentValue = value;
                startIndex = i;
            }

            // Wenn wir das Ende des Bereichs erreicht haben (oder die Liste endet)
            if (i == dataList.size() - 1 || !value.equals(dataList.get(i + 1).getHumidity())) {
                // Wenn der Bereich größer als 2 ist, setze die mittleren Werte auf null
                if (i - startIndex > 1) {
                    for (int j = startIndex + 1; j <= i; j++) {
                        dataList.get(j).setHumidity(null);  // Setze den mittleren Wert auf null
                    }
                }
            }
        }
        dataList.getFirst().setHumidity(firstValue);
        dataList.getLast().setHumidity(lastValue);
    }

    // Glättung der "humidity"-Werte

    /**
     * Hier werden die daten von feld pressure geglättet, für die anzeige im Chart:
     * Das funktioniert wie folgend:
     * Daten kommen wie folgt 1 1 1 4 4 7 7 7 7 7 8 8
     * Dieses wird zu 1 null 1 null null 4 null 7 null null null null 8 null
     * Ein wert der auf null ist wird im Chart nicht als wert punkt angezeigt. sommit ist der chart nicht überfülllt mit datenpunkten.
     *
     * @param dataList Liste mit daten über denn gesmooth wird
     */
    public static void smoothPressure(List<Data> dataList) {
        if (dataList == null || dataList.size() < 2) return;

        Double fistValue = dataList.getFirst().getPressure();
        Double lastValue = dataList.getLast().getPressure();
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
                    for (int j = startIndex + 1; j <= i; j++) {
                        dataList.get(j).setPressure(null);  // Setze den mittleren Wert auf null
                    }
                }
            }
        }
        dataList.getFirst().setPressure(fistValue);
        dataList.getLast().setPressure(lastValue);
    }

    // Glättung der "pressure"-Werte

    /**
     * Hier werden die daten von feld height geglättet, für die anzeige im Chart:
     * Das funktioniert wie folgend:
     * Daten kommen wie folgt 1 1 1 4 4 7 7 7 7 7 8 8
     * Dieses wird zu 1 null 1 null null 4 null 7 null null null null 8 null
     * Ein wert der auf null ist wird im Chart nicht als wert punkt angezeigt. sommit ist der chart nicht überfülllt mit datenpunkten.
     *
     * @param dataList Liste mit daten über denn gesmooth wird
     */
    public static void smoothHeight(List<Data> dataList) {
        if (dataList == null || dataList.size() < 2) return;

        Double fistValue = dataList.getFirst().getHeight();
        Double lastValue = dataList.getLast().getHeight();
        Double currentValue = null;  // Wert, den wir aktuell glätten
        int startIndex = -1;       // Startindex des Bereichs mit gleichen Werten

        for (int i = 0; i < dataList.size(); i++) {
            Double value = dataList.get(i).getHeight();

            if (currentValue == null || !currentValue.equals(value)) {
                // Wenn wir einen neuen Wert finden, speichern wir den Start des Bereichs
                currentValue = value;
                startIndex = i;
            }

            // Wenn wir das Ende des Bereichs erreicht haben (oder die Liste endet)
            if (i == dataList.size() - 1 || !value.equals(dataList.get(i + 1).getHeight())) {
                // Wenn der Bereich größer als 2 ist, setze die mittleren Werte auf null
                if (i - startIndex > 1) {
                    // Setze alle mittleren Werte und auch das letzte Element auf null
                    for (int j = startIndex + 1; j <= i; j++) {
                        dataList.get(j).setHeight(null);  // Setze den Wert auf null
                    }
                }
            }
        }
        dataList.getFirst().setHeight(fistValue);
        dataList.getLast().setHeight(lastValue);
    }

    /**
     * Bei ausführung wird via MQTT an den Controller gesendet das er sich sperren soll.
     */
    public void lockDevice() {
        mqttService.publish("ERROR", lockTopic);
    }

    /**
     * @return Status zur Connection mit MQTT;
     */
    public boolean isMQQTConnected() {
        return mqttService.isConnected();
    }
}
