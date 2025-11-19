package de.wroracer.storagecompartmentui.components.charts;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import de.wroracer.storagecompartmentui.components.charts.domain.*;
import elemental.json.Json;
import elemental.json.JsonObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag("recharts-element")
@JsModule("./src/react-charts.tsx")
@NpmPackage(value = "recharts", version = "3.4.1")
@NpmPackage(value = "moment", version = "2.30.1")
public class RechartsChart<T> extends ReactAdapterComponent {

    private final ChartConfig<T> conf = new ChartConfig<>(ChartType.LINE, new ArrayList<>(), new ArrayList<>());

    private final JsonObject config = Json.createObject();

    public RechartsChart() {
        conf.setLegend(true);
        conf.setTooltip(true);
        updateConfig();
    }

    private void updateConfig() {
        // Konfiguration direkt an das React-Element übergeben
        /*getElement().callJsFunction("updateConfig", config);*/
        setState("config", conf);
    }

    // --- Chart type ---
    public void setType(ChartType type) {
        conf.setType(type);
        updateConfig();
    }

    // --- Dimensions ---
    public void setHeight(int px) {
        conf.setHeight(px);
        updateConfig();
    }

    public void setWidth(int width) {
        conf.setWidth(width);
        updateConfig();
    }

    public void setSpan(LocalDateTime start, LocalDateTime end) {
        conf.setStartDate(start);
        conf.setEndDate(end);
        updateConfig();
    }

    // --- Data ---
    public void setData(List<T> data) {
        conf.setData(data);
        updateConfig();
    }

    // --- X Axis ---
    public void setXAxis(XAxisProps props) {
        conf.setxAxis(props);
        updateConfig();
    }

    // --- Y Axis ---
    public void setYAxis(YAxisProps props) {
        conf.setyAxis(props);
        updateConfig();
    }

    // --- Grid ---
    public void setGridVisible(boolean visible) {
        conf.setGrid(visible);
        updateConfig();
    }

    // --- Series ---
    public void clearSeries() {
        conf.setSeries(new ArrayList<>());
        updateConfig();
    }

    public void addLineSeries(String dataKey, String color) {
        ChartSeries series = new ChartSeries(ChartType.LINE, dataKey, color, "none");
        conf.getSeries().add(series);
        updateConfig();
    }

    public void addBarSeries(String dataKey, String color) {
        ChartSeries series = new ChartSeries(ChartType.BAR, dataKey, color, "none");
        conf.getSeries().add(series);
        updateConfig();
    }
}