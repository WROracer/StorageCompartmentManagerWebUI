package de.wroracer.storagecompartmentui.views.empty;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import de.wroracer.storagecompartmentui.domain.MQTTMessage;
import de.wroracer.storagecompartmentui.service.MQTTService;

import javax.swing.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@PageTitle("Empty")
@Route("")
public class EmptyView extends VerticalLayout {

    private final UUID lisReg;
    private final MQTTService mqttService;
    private final Grid<MQTTMessage> grid;

    public EmptyView(MQTTService mqttService) {
        this.mqttService = mqttService;


        Button btnTest = new Button("Send Test Message");
        btnTest.addClickListener(c->{
            mqttService.publishTest();
        });
        //add(btnTest);

        grid = new Grid<>();
        grid.addColumn(m->m.getRecived().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))).setAutoWidth(true).setFlexGrow(0).setHeader("Recived").setSortable(true);
        grid.addColumn(MQTTMessage::getQos).setAutoWidth(true).setFlexGrow(0).setHeader("QoS").setSortable(true);
        grid.addColumn(MQTTMessage::getTopic).setAutoWidth(true).setHeader("Topic").setSortable(true);
        grid.addColumn(MQTTMessage::getMsg).setAutoWidth(true).setHeader("Message").setFlexGrow(3);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER,GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        add(grid);
        grid.setMultiSort(true);
        this.setSizeFull();




        UI ui = UI.getCurrent();
        lisReg = mqttService.registerListener((ignored) -> {
            ui.access(()->{
                grid.setItems(mqttService.getMessages());
                ui.push();
            });
        });
        grid.setItems(mqttService.getMessages());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        mqttService.removeListener(lisReg);
    }
}
