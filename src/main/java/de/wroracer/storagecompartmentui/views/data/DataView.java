package de.wroracer.storagecompartmentui.views.data;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.signals.ValueSignal;
import de.wroracer.storagecompartmentui.components.charts.RechartsChart;
import de.wroracer.storagecompartmentui.components.charts.domain.ChartType;
import de.wroracer.storagecompartmentui.components.charts.domain.XAxisProps;
import de.wroracer.storagecompartmentui.components.charts.domain.YAxisProps;
import de.wroracer.storagecompartmentui.domain.Data;
import de.wroracer.storagecompartmentui.service.DataService;
import org.springframework.web.context.annotation.ApplicationScope;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Route("data")
@RouteAlias("")
@ApplicationScope
public class DataView extends VerticalLayout {

    private final UI ui;
    private final RechartsChart<Data> chartBoxes;
    private final RechartsChart<Data> chartTemperature;
    private final RechartsChart<Data> chartHumidity;
    private final RechartsChart<Data> chartPressure;
    private final ValueSignal<TimeSpan> timeSpanValueSignal = new ValueSignal<>(TimeSpan.TEN_MINUTE);
    @org.jetbrains.annotations.NotNull
    private final DataService dataService;
    private ScheduledFuture<?> scheduledFuture;

    public DataView(DataService dataService) {
        this.dataService = dataService;
        this.ui = UI.getCurrent();

        ComboBox<TimeSpan> cbTime = new ComboBox<>("Zeitspanne");
        cbTime.setItems(TimeSpan.values());
        cbTime.setValue(TimeSpan.TEN_MINUTE);
        cbTime.setItemLabelGenerator(TimeSpan::getDisplay);
        cbTime.addValueChangeListener(v -> {
            if (v.isFromClient()) {
                timeSpanValueSignal.value(v.getValue());
                updateCharts(dataService.getSensorData());
            }
        });

        //Gen UI Objects
        chartBoxes = createChartBoxes();
        chartTemperature = createChartTemperature();
        chartHumidity = createChartHumidity();
        chartPressure = createChartPressure();

        updateCharts(dataService.getSensorData());
        add(cbTime);
        add(new HorizontalLayout(chartBoxes, chartTemperature));
        add(new HorizontalLayout(chartHumidity, chartPressure));
        this.setSizeFull();

        Button btnTest = new Button("Test");
        btnTest.addClickListener(c -> {
            dataService.testData();
        });

        //add(btnTest);
    }

    private void updateCharts(List<Data> _data) {
        List<Data> data = filterData(_data);
        ui.access(() -> {
            chartBoxes.setData(data);
            chartPressure.setData(data);
            chartTemperature.setData(data);
            chartHumidity.setData(data);
            ui.push();
        });
    }

    private RechartsChart<Data> createChartBoxes() {
        RechartsChart<Data> chartAll = new RechartsChart<>();
        chartAll.setType(ChartType.LINE);


        /*chartAll.addLineSeries("distance", "green");*/
        chartAll.addLineSeries("boxes", "red");
        /*chartAll.addLineSeries("temperature", "orange");*/
        /*chartAll.addLineSeries("humidity", "blue");*/
        /*chartAll.addLineSeries("pressure", "pink");*/

        chartAll.setGridVisible(true);
        chartAll.setXAxis(new XAxisProps("formattedTime"));
        chartAll.setYAxis(new YAxisProps());

        chartAll.setWidth(800);
        chartAll.setHeight(400);
        return chartAll;
    }

    private RechartsChart<Data> createChartTemperature() {
        RechartsChart<Data> chartAll = new RechartsChart<>();
        chartAll.setType(ChartType.LINE);


        /*chartAll.addLineSeries("distance", "green");*/
        /*chartAll.addLineSeries("boxes", "red");*/
        chartAll.addLineSeries("temperature", "orange");
        /*chartAll.addLineSeries("humidity", "blue");*/
        /*chartAll.addLineSeries("pressure", "pink");*/

        chartAll.setGridVisible(true);
        chartAll.setXAxis(new XAxisProps("formattedTime"));
        chartAll.setYAxis(new YAxisProps());

        chartAll.setWidth(800);
        chartAll.setHeight(400);
        return chartAll;
    }

    private RechartsChart<Data> createChartHumidity() {
        RechartsChart<Data> chartAll = new RechartsChart<>();
        chartAll.setType(ChartType.LINE);


        /*chartAll.addLineSeries("distance", "green");*/
        /*chartAll.addLineSeries("boxes", "red");*/
        /*chartAll.addLineSeries("temperature", "orange");*/
        chartAll.addLineSeries("humidity", "blue");
        /*chartAll.addLineSeries("pressure", "pink");*/

        chartAll.setGridVisible(true);
        chartAll.setXAxis(new XAxisProps("formattedTime"));
        chartAll.setYAxis(new YAxisProps());

        chartAll.setWidth(800);
        chartAll.setHeight(400);
        return chartAll;
    }

    private RechartsChart<Data> createChartPressure() {
        RechartsChart<Data> chartAll = new RechartsChart<>();
        chartAll.setType(ChartType.LINE);


        /*chartAll.addLineSeries("distance", "green");*/
        /*chartAll.addLineSeries("boxes", "red");*/
        /*chartAll.addLineSeries("temperature", "orange");*/
        /*chartAll.addLineSeries("humidity", "blue");*/
        chartAll.addLineSeries("pressure", "pink");

        chartAll.setGridVisible(true);
        chartAll.setXAxis(new XAxisProps("formattedTime"));
        chartAll.setYAxis(new YAxisProps());

        chartAll.setWidth(800);
        chartAll.setHeight(400);
        return chartAll;
    }

    private List<Data> filterData(List<Data> data) {
        LocalDateTime min = LocalDateTime.now();  // Startwert: Jetzt
        switch (timeSpanValueSignal.value()) {
            case FIVE_MINUTE:
                min = min.minusMinutes(5);
                break;
            case TEN_MINUTE:
                min = min.minusMinutes(10);
                break;
            case HALF_HOUR:
                min = min.minusMinutes(30);
                break;
            case HOUR:
                min = min.minusHours(1);
                break;
            case TWELVE_HOUR:
                min = min.minusHours(12);
                break;
            case DAY:
                min = min.minusDays(1);
                break;
            case WEEK:
                min = min.minusWeeks(1);
                break;
            case MONTH:
                min = min.minusMonths(1);
                break;
            case YEAR:
                min = min.minusYears(1);
                break;
            case ALL:
                min = null;  // Alle Daten ohne Filter
        }
        if (min == null) {
            return data;
        }

        // Filter die Daten nach dem berechneten "min" Zeitstempel
        LocalDateTime finalMin = min;
        return data.stream()
                .filter(d -> d.getTime().isAfter(finalMin))  // filtere nur die Daten, die nach "min" liegen
                .collect(Collectors.toList());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        startAutoRefresh();
    }

    // Auto-Refresh starten
    private void startAutoRefresh() {
        // ScheduledExecutorService für wiederholte Ausführung
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            reloadData();  // Hier werden die Daten geladen

        }, 0, 5, TimeUnit.SECONDS);
    }

    private void reloadData() {
        updateCharts(dataService.getSensorData());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        scheduledFuture.cancel(true);
    }

}
