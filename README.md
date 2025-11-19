# Storage Compartment Web UI

Dies ist eine Vaadin-Weboberfläche, die zusammen mit folgendem Code verwendet wird:  
[**Storage Compartment Management Controller**](https://github.com/Cottonweaver/StorageCompartmentManagement)  
sowie einem **MQTT-Broker**.

## Verwendete Dependencies

Das Projekt nutzt ein [Spring Boot](https://spring.io/projects/spring-boot) Backend, welches das  
[Vaadin](https://vaadin.com/) Framework hostet.

Für die Verbindung mit dem MQTT-Broker wird der  
[Eclipse Paho Client Mqttv3](https://mvnrepository.com/artifact/org.eclipse.paho/org.eclipse.paho.client.mqttv3)  
verwendet.

Für die Anzeige von Diagrammen kommt [RechartsJS](https://recharts.github.io/en-US/) zum Einsatz, eingebunden als eigene
Vaadin-Komponente:

- [react-charts.tsx](src/main/frontend/src/react-charts.tsx)
- [RechartsChart.java](src/main/java/de/wroracer/storagecompartmentui/components/charts/RechartsChart.java)

## Benutzung

Starten Sie die Jar-Datei mit folgenden JVM-Argumenten:

```bash
java -DMQQTT_URL=127.0.0.1 -jar storagecompartmentui-1.0.0.jar
```

### Verfügbare Parameter

| Parameter   | Beschreibung                                       | Default   |
|-------------|----------------------------------------------------|-----------|
| MQQTT_URL   | URL des MQTT-Servers                               | 127.0.0.1 |
| MQTT_PORT   | Port des MQTT-Servers                              | 1883      |
| DATA_FOLDER | Ordner, in dem empfangene Daten gespeichert werden | ./data    |

## Wie funktioniert das System?

Der [DataService](src/main/java/de/wroracer/storagecompartmentui/service/DataService.java) verbindet sich über
den [MQTTService](src/main/java/de/wroracer/storagecompartmentui/service/MQTTService.java) mit dem MQTT-Broker.
Der DataService abonniert das in den [Properties](src/main/resources/application.properties) konfigurierte Topic.

Eingehende Nachrichten werden mit Jackson geparst und anschließend als JSON-Liste im Datenordner gespeichert.

Öffnet ein Benutzer die [DataView](src/main/java/de/wroracer/storagecompartmentui/views/data/DataView.java), lädt diese
über den DataService alle gespeicherten Daten und zeigt sie im UI an.
Ein eingebauter Timer aktualisiert alle 5 Sekunden die Anzeige, sodass stets die neuesten Daten dargestellt werden.
