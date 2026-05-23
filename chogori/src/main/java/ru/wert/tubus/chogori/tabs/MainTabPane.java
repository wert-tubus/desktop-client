package ru.wert.tubus.chogori.tabs;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.Event;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import lombok.Getter;
import ru.wert.tubus.client.interfaces.ITabController;
import ru.wert.tubus.client.interfaces.SearchableTab;
import ru.wert.tubus.chogori.popups.HintPopup;
import ru.wert.tubus.client.interfaces.UpdatableTabController;

import java.util.Iterator;
import java.util.List;

import static ru.wert.tubus.chogori.statics.UtilStaticNodes.*;

/**
 * Класс создает панель TabPane и входящие в нее вкладки Tab
 * в классе описывается контекстное меню вкладки, содержащее управление вкладками
 */
public class MainTabPane extends TabPane {

    private HintPopup hint; //Подсказка, дублирующая наименование вкладки
    private ITabController searchablePane;
    @Getter
    final BooleanBinding empty = Bindings.isNotEmpty(getTabs());

    /**
     * Конструктор
     */
    public MainTabPane() {
        setSide(Side.BOTTOM);
        setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);

//        selectionModelProperty().addListener((observable, oldValue, newValue)->{
//            ((AppTab)newValue.getSelectedItem()).update();
//        });
        //Наименование вкладки не умещается на ярлыке
        setTabMaxWidth(150.0);
    }


    /**
     * Создает вкладку, добавляет ей контекстное меню
     * Если вкладка уже существует, то открывает ее
     * @param name String, наименование вкладки
     * @param content Node, узел типа Parent
     * @param showTab boolean, опциональное - открывать созданную вкладку или не открывать
     */
    public void createNewTab(String tabId, String name, Node content, boolean showTab, ITabController searchableTabController){
        this.searchablePane = searchableTabController;

        Platform.runLater(()->{
            if (searchableTabController instanceof SearchableTab)
                showSearchPane();
            else
                hideSearchPane();


            AppTab tab = tabIsAvailable(tabId);
            if (tab == null) {

                tab = new AppTab(name, content, searchableTabController);
                tab.setId(tabId);

                getTabs().add(tab);//вкладку добавляем к TabPane
                setContextMenu(createContextMenu());

            }

            createTitle(tab, name);

            tab.setContextMenu(tabMenu());

            tab.setOnClosed((event)->{
                if(getTabs().isEmpty())
                    hideSearchPane();
            });

            AppTab finalTab = tab;
            tab.setOnSelectionChanged(e->{
                if(finalTab.getTabController() instanceof UpdatableTabController){
                    ((UpdatableTabController) finalTab.getTabController()).updateTab();
                }
                if(searchableTabController instanceof SearchableTab) {
                    ((SearchableTab) searchableTabController).tuneSearching();
                    showSearchPane();
                } else
                    hideSearchPane();
            });

            if (showTab) {
                //ОТКРЫВАЕМ созданную вкладку
                getSelectionModel().select(tab);
                //Создаем поле поиска
                if(searchableTabController instanceof SearchableTab) {
                    showSearchPane();
                } else
                    hideSearchPane();
            }
        });
    }

    private void showSearchPane() {
        if(!SEARCH_CONTAINER.getChildren().contains(PANE_WITH_SEARCH))
            SEARCH_CONTAINER.getChildren().add(PANE_WITH_SEARCH);
    }

    private void hideSearchPane() {
        if(SEARCH_CONTAINER.getChildren().contains(PANE_WITH_SEARCH))
            SEARCH_CONTAINER.getChildren().removeAll(PANE_WITH_SEARCH);
    }

    /**
     * Создает Label, который отображает наименование вкладки
     * @param tab Tab
     * @param name String
     */
    private void createTitle(Tab tab, String name){
        tab.setText("");
        Label lblTitle = new Label(name);

        lblTitle.setStyle("-fx-text-fill: black");

        //======   ПОДСКАЗКА    ==================================================

        lblTitle.setOnMouseEntered(event ->{
            hint = new HintPopup(lblTitle ,name, 0.0);
            hint.showHint();
        });
        lblTitle.setOnMouseExited(event ->{
            if(hint != null){
                hint.closeHint();
            }
        });

        //=========================================================================

        lblTitle.setOnMouseClicked(event ->{
            getSelectionModel().select(tab);
        });

        tab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                tab.setStyle("-fx-background-color: #A6B7D0");
                requestLayout();
            }
            else
                tab.setStyle("-fx-background-color: white");
        });

        tab.setGraphic(lblTitle);
    }



    /**
     * Возвращает вкладку, если она уже существует
     * @param tabId String id вкладки
     * @return Tab или null
     */
    public AppTab tabIsAvailable(String tabId){
        for(Tab tab : getTabs()){
            if(tab.getId().equals(tabId))
                return (AppTab)tab;
        }
        return null;
    }

    /**
     * Создает контекстное меню
     * @return ContextMenu
     */
    private ContextMenu createContextMenu(){
        ContextMenu menu = new ContextMenu();
        MenuItem menuItem = new MenuItem("Закрыть все");
        menuItem.setOnAction(this::closeAllTabs);
        return menu;
    }

    /**
     * Закрывает текущую вкладку
     * @param event Event
     */
    public void closeThisTab(Event event){
        Tab selectedTab = getSelectionModel().getSelectedItem();
        getTabs().remove(selectedTab);
        if(getTabs().isEmpty()) hideSearchPane();
    }


    /**
     * Закрывает все вкладки
     * @param event Event
     */
    public void closeAllTabs(Event event){
        List<Tab> tabs = getTabs();
        tabs.clear();
        hideSearchPane();
    }

    /**
     * Закрывает все вкладки кроме текущей
     * @param event Event
     */
    public void closeAllTabsButThis(Event event){
        Iterator<Tab> i = getTabs().iterator();
        while(i.hasNext()){
            Tab tab = i.next();
            if(!tab.isSelected())
            i.remove();
        }
    }

    /**
     * Закрывает все вкладки слева
     * @param event Event
     */
    public void closeAllTabsToLeft(Event event){
        Iterator<Tab> i = getTabs().iterator();
        while(i.hasNext()){
            Tab tab = i.next();
            if(tab.isSelected()) return;
            i.remove();
        }
    }

    /**
     * Закрывает все вкладки справа
     * @param event Event
     */
    public void closeAllTabsToRight(Event event){
        boolean mustClose = false;
        Iterator<Tab> i = getTabs().iterator();
        while(i.hasNext()){
            Tab tab = i.next();
            if(tab.isSelected()){
                mustClose = true;
                continue;
            }
            if(mustClose)
                i.remove();
        }

    }

    /**
     * Контекстное меню появляется при клике правой кнопкой на ярлыке таба
     */
    private ContextMenu tabMenu(){
        ContextMenu menu = new ContextMenu();

        //Закрыть текущую
        MenuItem closeThis = new MenuItem("Закрыть");
        closeThis.setOnAction(this::closeThisTab);

        //Закрыть все вкладки
        MenuItem closeAll = new MenuItem("Закрыть все");
        closeAll.setOnAction(this::closeAllTabs);

        //Закрыть все вкладки кроме текущей
        MenuItem closeAllButThis = new MenuItem("Закрыть все кроме текущей");
        closeAllButThis.setOnAction(this::closeAllTabsButThis);

        //Закрыть все вкладки от текущей слева
        MenuItem closeAllToLeft = new MenuItem("Закрыть все слева  <--");
        closeAllToLeft.setOnAction(this::closeAllTabsToLeft);

        //Закрыть все вкладки от текущей справа
        MenuItem closeAllToRight = new MenuItem("Закрыть все справа -->");
        closeAllToRight.setOnAction(this::closeAllTabsToRight);

        menu.getItems().add(closeThis);
        menu.getItems().add(closeAll);
        menu.getItems().add(closeAllButThis);
        menu.getItems().add(closeAllToLeft);
        menu.getItems().add(closeAllToRight);
        return menu;
    }
}
