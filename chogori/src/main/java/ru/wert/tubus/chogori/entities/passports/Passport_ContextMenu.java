package ru.wert.tubus.chogori.entities.passports;

import javafx.scene.control.MenuItem;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ContextMenu;
import ru.wert.tubus.chogori.entities.passports.commands._Passport_Commands;
import ru.wert.tubus.client.entity.models.Passport;

import java.util.Arrays;
import java.util.List;

/**
 * Контекстное меню для таблицы паспортов
 * Предоставляет доступ к операциям добавления, изменения и удаления
 * (копирование не предусмотрено для паспортов)
 */
public class Passport_ContextMenu extends FormView_ContextMenu<Passport> {

    private final _Passport_Commands commands;
    private Passport_TableView tableView;

    /**
     * Конструктор контекстного меню
     * @param tableView таблица паспортов
     * @param commands команды для выполнения операций
     * @param passportsACCRes путь к FXML файлу окна редактирования
     */
    public Passport_ContextMenu(Passport_TableView tableView, _Passport_Commands commands, String passportsACCRes) {
        super(tableView, commands, passportsACCRes);
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
        boolean addItem = false;       //обавление паспорта не предусмотрено, только через его регистрацию
        boolean copyItem = false;      // Копирование паспортов не предусмотрено
        boolean changeItem = true;
        boolean deleteItem = true;

        List<Passport> selectedPassports = tableView.getSelectionModel().getSelectedItems();

        if (selectedPassports.isEmpty()) {
            changeItem = false;
            deleteItem = false;
        }

        createMenu(addItem, copyItem, changeItem, deleteItem);
    }

    /**
     * Создает дополнительные пункты контекстного меню
     * @return список дополнительных MenuItem или null если нет
     */
    @Override
    public List<MenuItem> createExtraItems() {
        MenuItem info = new MenuItem("Инфо");
        info.setOnAction(event -> PassportInfo_Patch.create(
                tableView.getSelectionModel().getSelectedItem(),
                null
        ));
        return Arrays.asList(info);
    }
}
