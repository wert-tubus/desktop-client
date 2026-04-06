package ru.wert.tubus.chogori.entities.decimals;

import javafx.scene.control.TableColumn;
import lombok.Getter;
import lombok.Setter;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.Sorting;
import ru.wert.tubus.chogori.common.tableView.RoutineTableView;
import ru.wert.tubus.chogori.entities.decimals.commands._DecimalCommands;
import ru.wert.tubus.chogori.entities.drafts.Draft_ContextMenu;
import ru.wert.tubus.chogori.statics.Comparators;
import ru.wert.tubus.client.entity.models.Decimal;

import java.util.ArrayList;
import java.util.List;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;

/**
 * Таблица для отображения десятичных классификаторов (Decimal)
 * Реализует основные операции CRUD и поиск
 */
public class Decimal_TableView extends RoutineTableView<Decimal> implements Sorting<Decimal> {

    private static final String accWindowRes = "/chogori-fxml/decimals/decimalsACC.fxml";
    private final _DecimalCommands commands;
    private Decimal_ContextMenu contextMenu;
    private List<Decimal> currentItemList = new ArrayList<>();
    @Getter @Setter private Object modifyingItem;

    private String searchedText = "";

    /**
     * Конструктор таблицы десятичных классификаторов
     * @param itemName наименование элемента для отображения
     * @param useContextMenu флаг использования контекстного меню
     */
    public Decimal_TableView(String itemName, boolean useContextMenu) {
        super(itemName);

        commands = new _DecimalCommands(this);

        if (useContextMenu)
            createContextMenu();
    }

    /**
     * Устанавливает колонки таблицы
     * Порядок колонок: Наименование, Начальный номер, Конечный номер, Описание
     */
    @Override
    public void setTableColumns() {
        TableColumn<Decimal, String> tcName = Decimal_Columns.createTcName();
        TableColumn<Decimal, Integer> tcInitialNumber = Decimal_Columns.createTcInitialNumber();
        TableColumn<Decimal, Integer> tcLastNumber = Decimal_Columns.createTcLastNumber();
        TableColumn<Decimal, String> tcDescription = Decimal_Columns.createTcDescription();

        getColumns().addAll(tcName, tcInitialNumber, tcLastNumber, tcDescription);
    }

    @Override
    public ItemCommands<Decimal> getCommands() {
        return null;
    }

    @Override
    public String getAccWindowRes() {
        return null;
    }

    /**
     * Создает контекстное меню для таблицы
     */
    @Override
    public void createContextMenu() {
        setOnContextMenuRequested(event -> {
            contextMenu = new Decimal_ContextMenu(this, commands, accWindowRes);
            contextMenu.show(this.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * Подготавливает список всех десятичных классификаторов
     * @return список всех Decimal
     */
    @Override
    public List<Decimal> prepareList() {
        return CH_DECIMALS.findAll();
    }

    @Override
    public void setSearchedText(String searchedText) {
        this.searchedText = searchedText;
    }

    @Override
    public String getSearchedText() {
        return searchedText;
    }

    @Override //IFormView
    public List<Decimal> getCurrentItemSearchedList() {
        return currentItemList;
    }

    @Override //IFormView
    public void setCurrentItemSearchedList(List<Decimal> currentItemList) {
        this.currentItemList = currentItemList;
    }

    @Override
    public void setAccController(FormView_ACCController<Decimal> accController) {

    }

    @Override
    public FormView_ACCController<Decimal> getAccController() {
        return null;
    }

    /**
     * Сортирует список десятичных классификаторов
     * @param list список для сортировки
     */
    @Override
    public void sortItemList(List<Decimal> list) {
        list.sort(Comparators.usefulStringComparator());
    }
}
