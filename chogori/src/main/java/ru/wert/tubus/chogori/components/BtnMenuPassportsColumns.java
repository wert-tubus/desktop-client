package ru.wert.tubus.chogori.components;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;

import static ru.wert.tubus.chogori.images.BtnImages.BTN_COLUMNS_IMG;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;

/**
 * Класс описывает кнопку, при нажатии на которую на экране рядом с кнопкой появляется всплывающее окно
 * с чек-боксами, содержащие статус выводимых в таблицу чертежей: ДЕЙСТВУЮЩИЕ, ЗАМЕНЕННЫЕ, АННУЛИРОВАННЫЕ
 */
public class BtnMenuPassportsColumns extends MenuButton {

    private CheckBox cbUseId;
    private CheckBox cbUseIdentity;
    private CheckBox cbShowNote;
    private CheckBox cbShowUser;
    private CheckBox cbShowDate;

    private final Passport_TableView tableView;

    public BtnMenuPassportsColumns(Passport_TableView tableView) {
        this.tableView = tableView;

        setId("patchButton");

        setGraphic(new ImageView(BTN_COLUMNS_IMG));
        setTooltip(new Tooltip("Показать колонки"));

        setId("menu-button-no-arrow");

        createMenuItems();

        // При открытии меню обновляем состояние CheckBox из реальной видимости колонок
        showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Обновляем CheckBox из фактической видимости колонок
                updateCheckBoxesFromActualColumns();
            } else {
                // При закрытии меню применяем изменения
                applyChanges();
            }
        });
    }

    private void createMenuItems() {
        // Порядок должен соответствовать порядку колонок в таблице:
        // Колонка 0: ID
        // Колонка 1: Идентификатор
        // Колонка 2: Изделие
        // Колонка 3: Пользователь
        // Колонка 4: Дата

        //ID (колонка 0)
        CustomMenuItem useIdItem = new CustomMenuItem();
        cbUseId = new CheckBox("ID");
        useIdItem.setContent(cbUseId);
        useIdItem.setHideOnClick(false);

        //ИДЕНТИФИКАТОР (колонка 1)
        CustomMenuItem useIdentityItem = new CustomMenuItem();
        cbUseIdentity = new CheckBox("Идентификатор");
        useIdentityItem.setContent(cbUseIdentity);
        useIdentityItem.setHideOnClick(false);

        //ИЗДЕЛИЕ (колонка 2)
        CustomMenuItem showNoteItem = new CustomMenuItem();
        cbShowNote = new CheckBox("Изделие");
        showNoteItem.setContent(cbShowNote);
        showNoteItem.setHideOnClick(false);

        //ПОЛЬЗОВАТЕЛЬ (колонка 3)
        CustomMenuItem showUserItem = new CustomMenuItem();
        cbShowUser = new CheckBox("Пользователь");
        showUserItem.setContent(cbShowUser);
        showUserItem.setHideOnClick(false);

        //ДАТА (колонка 4)
        CustomMenuItem showDateItem = new CustomMenuItem();
        cbShowDate = new CheckBox("Дата");
        showDateItem.setContent(cbShowDate);
        showDateItem.setHideOnClick(false);

        if (CH_CURRENT_USER.getUserGroup().isAdministrate())
            getItems().addAll(useIdItem);

        getItems().addAll(useIdentityItem, showNoteItem, showUserItem, showDateItem);
    }

    /**
     * Обновляет состояние CheckBox из фактической видимости колонок таблицы
     */
    private void updateCheckBoxesFromActualColumns() {
        if (tableView.getColumns().size() >= 5) {
            if (cbUseId != null) cbUseId.setSelected(tableView.getColumns().get(0).isVisible());
            if (cbUseIdentity != null) cbUseIdentity.setSelected(tableView.getColumns().get(1).isVisible());
            if (cbShowNote != null) cbShowNote.setSelected(tableView.getColumns().get(2).isVisible());
            if (cbShowUser != null) cbShowUser.setSelected(tableView.getColumns().get(3).isVisible());
            if (cbShowDate != null) cbShowDate.setSelected(tableView.getColumns().get(4).isVisible());
        }
    }

    /**
     * Применяет изменения после закрытия меню
     */
    private void applyChanges() {
        Platform.runLater(() -> {
            if (tableView.getColumns().size() >= 5) {
                // Устанавливаем видимость колонок непосредственно
                tableView.getColumns().get(0).setVisible(cbUseId != null && cbUseId.isSelected());
                tableView.getColumns().get(1).setVisible(cbUseIdentity != null && cbUseIdentity.isSelected());
                tableView.getColumns().get(2).setVisible(cbShowNote != null && cbShowNote.isSelected());
                tableView.getColumns().get(3).setVisible(cbShowUser != null && cbShowUser.isSelected());
                tableView.getColumns().get(4).setVisible(cbShowDate != null && cbShowDate.isSelected());

                // Синхронизируем поля showXxx
                tableView.syncShowFieldsFromColumns();

                tableView.updateView();
                tableView.refresh();
            }
        });
    }
}
