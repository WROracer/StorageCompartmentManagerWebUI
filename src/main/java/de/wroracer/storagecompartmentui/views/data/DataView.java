package de.wroracer.storagecompartmentui.views.data;

import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.button.Button;
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
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import jdk.dynalink.linker.LinkerServices;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Route("data")
@RouteAlias("")
public class DataView extends VerticalLayout {

    public DataView(DataService dataService){
        RechartsChart<Data> chart = new RechartsChart<>();
        chart.setType(ChartType.LINE);



        chart.addLineSeries("distance", "green");
        chart.addLineSeries("boxes", "red");
        chart.addLineSeries("temperature", "yellow");
        chart.addLineSeries("humidity", "blue");
        chart.addLineSeries("pressure", "pink");

        chart.setGridVisible(true);
        chart.setXAxis(new XAxisProps("formattedTime"));
        chart.setYAxis(new YAxisProps());

        /*ComponentEffect.effect(chart,()->{
            List<Data> dataList = new ArrayList<>();
            for (ValueSignal<Data> dataValueSignal : dataService.getSensorDataSignal().value()) {
                Data d = dataValueSignal.value();
                d.setFormattedTime(d.getTime().format(DateTimeFormatter.ofPattern("hh:mm:ss dd.MM.yyyy")));
                dataList.add(d);
            }
            dataList.sort(new Comparator<Data>() {
                @Override
                public int compare(Data o1, Data o2) {
                    return o1.getTime().compareTo(o2.getTime());
                }
            });
            chart.setData(dataList);
        });*/
        /*dataService.onUpdate(data->{
            data.forEach(d->d.setFormattedTime(d.getTime().format(DateTimeFormatter.ofPattern("hh:mm:ss dd.MM.yyyy"))));
            data.sort(new Comparator<Data>() {
                @Override
                public int compare(Data o1, Data o2) {
                    return o1.getTime().compareTo(o2.getTime());
                }
            });
            chart.setData(data);
        });*/
        chart.setData(dataService.loadData("sensor/storage"));

        chart.setSizeFull();
        add(chart);
        this.setSizeFull();

        Button btnTest = new Button("Test");
        btnTest.addClickListener(c->{
            dataService.testData();
        });

        //add(btnTest);
    }

}
