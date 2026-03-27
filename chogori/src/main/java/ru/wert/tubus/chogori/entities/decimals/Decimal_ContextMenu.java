package ru.wert.tubus.chogori.entities.decimals;

import javafx.scene.control.MenuItem;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ContextMenu;
import ru.wert.tubus.chogori.entities.decimals.commands._DecimalCommands;
import ru.wert.tubus.client.entity.models.Decimal;

import java.util.List;

/**
 * Контекстное меню для таблицы десятичных классификаторов
 * Предоставляет доступ к операциям добавления, копирования, изменения и удаления
 */
public class Decimal_ContextMenu extends FormView_ContextMenu<Decimal> {

    private final _DecimalCommands commands;
    private Decimal_TableView tableView;

    /**
     * Конструктор контекстного меню
     * @param tableView таблица десятичных классификаторов
     * @param commands команды для выполнения операций
     * @param usersACCRes путь к FXML файлу окна редактирования
     */
    public Decimal_ContextMenu(Decimal_TableView tableView, _DecimalCommands commands, String usersACCRes) {
        super(tableView, commands, usersACCRes);
        this.commands = commands;
        this.tableView = tableView;

        createMainMenuItems();
    }

    /**
     * Создает основные пункты контекстного меню
     * Определяет доступность пунктов в зависимости от выбранных элементов
     */
    @Override
    public void createMainMenuItems() {
        boolean addItem = true;
        boolean copyItem = true;
        boolean changeItem = true;
        boolean deleteItem = true;

        List<Decimal> selectedDecimals = tableView.getSelectionModel().getSelectedItems();

        if (selectedDecimals.size() == 0) {
            copyItem = false;
            changeItem = false;
            deleteItem = false;
        } else if (selectedDecimals.size() > 1) {
            copyItem = false;
            changeItem = false;
        }

        createMenu(addItem, copyItem, changeItem, deleteItem);
    }

    /**
     * Создает дополнительные пункты контекстного меню
     * @return список дополнительных MenuItem или null если нет
     */
    @Override
    public List<MenuItem> createExtraItems() {
        // Дополнительных пунктов меню нет
        return null;
    }
}
