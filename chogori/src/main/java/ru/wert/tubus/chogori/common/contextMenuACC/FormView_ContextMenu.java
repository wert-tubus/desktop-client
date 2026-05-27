package ru.wert.tubus.chogori.common.contextMenuACC;

import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import lombok.Getter;
import ru.wert.tubus.client.interfaces.Item;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.interfaces.IFormView;
import ru.wert.tubus.winform.enums.EOperation;

import java.util.List;

public abstract class FormView_ContextMenu<P extends Item> extends ContextMenu {

    private final IFormView<P> formView;
    private final ItemCommands<P> commands;
    private final String itemACCRes;
    protected FormViewACCWindow<P> accWindow;

    //нельзя делать FINAL
    @Getter private MenuItem ADD;
    @Getter private MenuItem COPY;
    @Getter private MenuItem CHANGE;
    @Getter private MenuItem DELETE;

    public abstract List<MenuItem> createExtraItems();
    public abstract void createMainMenuItems();

    public FormView_ContextMenu(IFormView<P> formView, ItemCommands<P> commands, String itemACCRes) {
        this.formView = formView;
        this.commands = commands;
        this.itemACCRes = itemACCRes;

        ADD = new MenuItem("Добавить");
        ADD.setOnAction(this::add);

        COPY = new MenuItem("Добавить копией");
        COPY.setOnAction(this::copy);

        CHANGE = new MenuItem("Изменить");
        CHANGE.setOnAction(this::change);

        DELETE = new MenuItem("Удалить");
        DELETE.setOnAction(this::delete);

        setOnShowing(e->{
                createMainMenuItems();
        });

    }


    protected void setAddMenuName(String name){
        ADD.setText(name);
    }


    protected void createMenu(boolean addItem, boolean copyItem, boolean changeItem, boolean deleteItem){

            getItems().clear();

            if(addItem) getItems().add(ADD);
            if(copyItem) getItems().add(COPY);
            if(changeItem) getItems().add(CHANGE);
            if(deleteItem) getItems().add(DELETE);

            List<MenuItem> extraItems = createExtraItems();

            if(extraItems != null){
                if(!extraItems.isEmpty()) {
                    if(!getItems().isEmpty()) getItems().add(new SeparatorMenuItem());
                    getItems().addAll(extraItems);
                }
            }

    }


    private void add(Event event){
        accWindow = new FormViewACCWindow<P>();
        accWindow.create(EOperation.ADD, formView, commands, itemACCRes);
    }

    private void copy(Event event){
        accWindow = new FormViewACCWindow<P>();
        accWindow.create(EOperation.COPY, formView,  commands, itemACCRes);
    }

    private void change(Event event){
        accWindow = new FormViewACCWindow<P>();
        accWindow.create(EOperation.CHANGE, formView,  commands, itemACCRes);
    }

    private void delete(Event event){
        if(isShowing()) hide();
        List<P> items = formView.getAllSelectedItems();
        commands.delete(event, items);
    }

    public FormViewACCWindow<P> getACCWindow(){
        return accWindow;
    }

    public void setAccWindow(FormViewACCWindow<P> accWindow){
        this.accWindow = accWindow;
    }



}
