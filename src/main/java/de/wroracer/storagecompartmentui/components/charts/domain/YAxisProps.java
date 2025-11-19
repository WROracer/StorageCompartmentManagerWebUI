package de.wroracer.storagecompartmentui.components.charts.domain;

public class YAxisProps {

    // Beispiel für YAxis-Eigenschaften
    private String width;
    private String[] domain = new String[]{"auto", "auto"};
    private String unit = "";

    public YAxisProps() {
        width = "auto";
    }

    public String getUnit() {
        return unit;
    }

    public YAxisProps setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public String[] getDomain() {
        return domain;
    }

    public YAxisProps setDomain(String[] domain) {
        this.domain = domain;
        return this;
    }

    public String getWidth() {
        return width;
    }

    public YAxisProps setWidth(String width) {
        this.width = width;
        return this;
    }
}