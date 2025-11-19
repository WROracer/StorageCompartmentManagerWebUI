package de.wroracer.storagecompartmentui.views.data;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.*;
import com.vaadin.signals.ValueSignal;
import de.wroracer.storagecompartmentui.components.charts.RechartsChart;
import de.wroracer.storagecompartmentui.components.charts.domain.ChartType;
import de.wroracer.storagecompartmentui.components.charts.domain.XAxisProps;
import de.wroracer.storagecompartmentui.components.charts.domain.YAxisProps;
import de.wroracer.storagecompartmentui.domain.Data;
import de.wroracer.storagecompartmentui.service.DataService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Route("data")
@RouteAlias("")
public class DataView extends VerticalLayout implements BeforeEnterObserver {

    private final UI ui;
    private final RechartsChart<Data> chartBoxes;
    private final RechartsChart<Data> chartTemperature;
    private final RechartsChart<Data> chartHumidity;
    private final RechartsChart<Data> chartPressure;
    private final ValueSignal<TimeSpan> timeSpanValueSignal = new ValueSignal<>(TimeSpan.TEN_MINUTE);
    private final ValueSignal<Integer> selTabValueSignal = new ValueSignal<>(0);
    @org.jetbrains.annotations.NotNull
    private final DataService dataService;
    private final RechartsChart<Data> chartAll;
    private final String QUERY_TIME = "time";
    private final String QUERY_TAB = "type";
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
                updateQueryParam();
            }
        });
        ComponentEffect.effect(cbTime, () -> {
            cbTime.setValue(timeSpanValueSignal.value());
        });

        //Gen UI Objects
        chartBoxes = createChartBoxes();
        chartTemperature = createChartTemperature();
        chartHumidity = createChartHumidity();
        chartPressure = createChartPressure();
        chartAll = createChartAll();

        VerticalLayout layDisc = new VerticalLayout(new HorizontalLayout(chartBoxes, chartTemperature), new HorizontalLayout(chartHumidity, chartPressure));


        Tabs tabs = new Tabs(new Tab("Getrennt"), new Tab("Zusammen"));
        tabs.addSelectedChangeListener(s -> {
            if (s.isFromClient()) {
                selTabValueSignal.value(tabs.getSelectedIndex());
                updateQueryParam();
            }
            if (s.getSelectedTab().getLabel().equals("Getrennt")) {
                add(layDisc);
                remove(chartAll);
            } else {
                remove(layDisc);
                add(chartAll);
            }
        });
        ComponentEffect.effect(tabs, () -> {
            int tab = selTabValueSignal.value();
            tabs.setSelectedIndex(tab);
        });

        Button btnLock = new Button("Sperren");
        btnLock.addThemeVariants(ButtonVariant.LUMO_ERROR);
        btnLock.setIcon(VaadinIcon.LOCK.create());

        btnLock.addClickListener(c -> {
            dataService.lockDevice();
            Notification.show("Lock: alle Geräte wurden Gesperrt, sie übertragen keine neuen daten!!!!");
        });


        add(new H1("Storage Compartment Monitor"));
        updateCharts(dataService.getSensorData());
        HorizontalLayout hlHeader = new HorizontalLayout(cbTime, tabs, btnLock);
        hlHeader.setAlignItems(Alignment.END);
        hlHeader.setWidthFull();
        if (!dataService.isMQQTConnected()) {
            Span spanError = new Span();
            spanError.getStyle().setBackgroundColor("var(--lumo-error-text-color)");
            spanError.getStyle().set("border-style", "dotted");
            spanError.getStyle().setBorderRadius("20px");
            spanError.getStyle().set("border-color", "var(--lumo-error-color)");
            spanError.getStyle().setColor("var(--lumo-error-contrast-color)");
            spanError.setWidthFull();
            spanError.getStyle().setPadding("5px");
            spanError.setText("Keine Verbindung zum MQTT Server. Bitte versuchen sie es Später erneut");
            add(spanError);
        }
        add(hlHeader);
        add(layDisc);
        this.setSizeFull();

        Button btnTest = new Button("Test");
        btnTest.addClickListener(c -> {
            dataService.testData();
        });

        //add(btnTest);
    }

    private void updateCharts(List<Data> data) {
        //List<Data> data = filterData(_data);
        TSP tsp = getTimeSpan();
        ui.access(() -> {
            chartBoxes.setData(data);
            chartBoxes.setSpan(tsp.start, tsp.end);
            chartPressure.setData(data);
            chartPressure.setSpan(tsp.start, tsp.end);
            chartTemperature.setData(data);
            chartTemperature.setSpan(tsp.start, tsp.end);
            chartHumidity.setData(data);
            chartHumidity.setSpan(tsp.start, tsp.end);
            chartAll.setData(data);
            chartAll.setSpan(tsp.start, tsp.end);
            ui.push();
        });
    }

    private void updateQueryParam() {
        String deepLinkingUrl = RouteConfiguration.forSessionScope()
                .getUrl(getClass()) + "?" + QUERY_TAB + "=" + selTabValueSignal.value() + "&" + QUERY_TIME + "=" + timeSpanValueSignal.value().ordinal();
        // Assign the full deep linking URL directly using
        // History object: changes the URL in the browser,
        // but doesn't reload the page.
        getUI().get().getPage().getHistory()
                .replaceState(null, deepLinkingUrl);
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
        chartAll.setYAxis(new YAxisProps().setDomain(new String[]{"0", "2"}));

        chartAll.setWidth(810);
        chartAll.setHeight(360);
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
        chartAll.setYAxis(new YAxisProps().setUnit("°C"));

        chartAll.setWidth(810);
        chartAll.setHeight(360);
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

        chartAll.setWidth(810);
        chartAll.setHeight(360);
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
        chartAll.setYAxis(new YAxisProps().setUnit("Bar"));

        chartAll.setWidth(810);
        chartAll.setHeight(360);
        return chartAll;
    }

    private RechartsChart<Data> createChartAll() {
        RechartsChart<Data> chartAll = new RechartsChart<>();
        chartAll.setType(ChartType.LINE);


        chartAll.addLineSeries("distance", "green");
        chartAll.addLineSeries("boxes", "red");
        chartAll.addLineSeries("temperature", "orange");
        chartAll.addLineSeries("humidity", "blue");
        chartAll.addLineSeries("pressure", "pink");

        chartAll.setGridVisible(true);
        chartAll.setXAxis(new XAxisProps("formattedTime"));
        chartAll.setYAxis(new YAxisProps());

        chartAll.setWidth(1600);
        chartAll.setHeight(760);
        return chartAll;
    }

    private TSP getTimeSpan() {
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
        LocalDateTime max = LocalDateTime.now();
        return new TSP(min, max);
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

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        int time = Integer.parseInt(beforeEnterEvent.getLocation().getQueryParameters().getSingleParameter(QUERY_TIME).orElse("0"));
        int tab = Integer.parseInt(beforeEnterEvent.getLocation().getQueryParameters().getSingleParameter(QUERY_TAB).orElse("0"));
        timeSpanValueSignal.value(TimeSpan.values()[time]);
        selTabValueSignal.value(tab);
    }

    private record TSP(LocalDateTime start, LocalDateTime end) {

    }
}
