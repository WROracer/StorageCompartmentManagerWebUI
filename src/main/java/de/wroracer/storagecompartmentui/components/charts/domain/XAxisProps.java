package de.wroracer.storagecompartmentui.components.charts.domain;

// Dummy Klassen für XAxisProps und YAxisProps (je nach Bedarf erweitern)
public class XAxisProps {

    // Beispiel für XAxis-Eigenschaften
    private String datakey;
    private String tickFormatter;
    public XAxisProps(String datakey) {
        this.datakey = datakey;
    }

    public XAxisProps setTickFormatter(String tickFormatter) {
        this.tickFormatter = tickFormatter;
        return this;
    }

    public String getDatakey() {
        return datakey;
    }

    public XAxisProps setDatakey(String datakey) {
        this.datakey = datakey;
        return this;
    }
}