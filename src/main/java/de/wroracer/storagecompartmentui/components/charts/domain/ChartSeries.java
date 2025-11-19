package de.wroracer.storagecompartmentui.components.charts.domain;

// Klasse für ChartSeries
public class ChartSeries {
    private ChartType type;
    private String dataKey;
    private String stroke; // optional
    private String fill;   // optional
    private boolean isAnimationActive = false;

    // Konstruktor
    public ChartSeries(ChartType type, String dataKey, String stroke, String fill) {
        this.type = type;
        this.dataKey = dataKey;
        this.stroke = stroke;
        this.fill = fill;
    }

    public boolean isAnimationActive() {
        return isAnimationActive;
    }

    public void setAnimationActive(boolean animationActive) {
        isAnimationActive = animationActive;
    }

    // Getter und Setter
    public ChartType getType() {
        return type;
    }

    public void setType(ChartType type) {
        this.type = type;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }

    public String getStroke() {
        return stroke;
    }

    public void setStroke(String stroke) {
        this.stroke = stroke;
    }

    public String getFill() {
        return fill;
    }

    public void setFill(String fill) {
        this.fill = fill;
    }
}