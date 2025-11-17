package de.wroracer.storagecompartmentui.components.charts.domain;

// Dummy Klassen für XAxisProps und YAxisProps (je nach Bedarf erweitern)
public class XAxisProps {

    public XAxisProps(String datakey){
        this.datakey = datakey;
    }

    // Beispiel für XAxis-Eigenschaften
    private String datakey;

    public String getDatakey() {
        return datakey;
    }

    public void setDatakey(String datakey) {
        this.datakey = datakey;
    }
}