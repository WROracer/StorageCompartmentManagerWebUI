package de.wroracer.storagecompartmentui.components.charts.domain;

import java.util.List;

// Klasse für ChartConfig
public class ChartConfig<T> {
    private ChartType type;
    private List<T> data; // List of key-value pairs
    private Integer width = 200;   // optional
    private Integer height = 100;  // optional
    private Boolean legend = false;  // optional
    private Boolean tooltip = false; // optional
    private Boolean grid = false;    // optional
    private XAxisProps xAxis; // optional
    private YAxisProps yAxis; // optional
    private List<ChartSeries> series;

    // Konstruktor
    public ChartConfig(ChartType type, List<T> data, List<ChartSeries> series) {
        this.type = type;
        this.data = data;
        this.series = series;
    }

    // Getter und Setter
    public ChartType getType() {
        return type;
    }

    public void setType(ChartType type) {
        this.type = type;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Boolean getLegend() {
        return legend;
    }

    public void setLegend(Boolean legend) {
        this.legend = legend;
    }

    public Boolean getTooltip() {
        return tooltip;
    }

    public void setTooltip(Boolean tooltip) {
        this.tooltip = tooltip;
    }

    public Boolean getGrid() {
        return grid;
    }

    public void setGrid(Boolean grid) {
        this.grid = grid;
    }

    public XAxisProps getxAxis() {
        return xAxis;
    }

    public void setxAxis(XAxisProps xAxis) {
        this.xAxis = xAxis;
    }

    public YAxisProps getyAxis() {
        return yAxis;
    }

    public void setyAxis(YAxisProps yAxis) {
        this.yAxis = yAxis;
    }

    public List<ChartSeries> getSeries() {
        return series;
    }

    public void setSeries(List<ChartSeries> series) {
        this.series = series;
    }
}