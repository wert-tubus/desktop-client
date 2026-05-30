package ru.wert.tubus.chogori.components;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import ru.wert.tubus.chogori.entities.drafts.Draft_TableView;

import static ru.wert.tubus.chogori.images.BtnImages.BTN_COLUMNS_IMG;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER_GROUP;

/**
 * Класс описывает кнопку, при нажатии на которую на экране рядом с кнопкой появляется всплывающее окно
 * с чек-боксами, содержащие статус выводимых в таблицу чертежей: ДЕЙСТВУЮЩИЕ, ЗАМЕНЕННЫЕ, АННУЛИРОВАННЫЕ
 */
public class BtnMenuDraftsColumns extends MenuButton {

    private CheckBox cbUseId;
    private CheckBox cbUseDraftType;
    private CheckBox cbUseStatus;
    private CheckBox cbUseRemarks;
    private CheckBox cbUseInitialName;
    private CheckBox cbUseCreationTime;
    private CheckBox cbUseNote;

    private final Draft_TableView tableView;

    public BtnMenuDraftsColumns(Draft_TableView tableView) {
        this.tableView = tableView;

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
        // Порядок должен соответствовать порядку колонок в Draft_TableView.setTableColumns():
        // 0: ID
        // 1: Дец.номер (всегда видима, не в меню)
        // 2: Наименование (всегда видима, не в меню)
        // 3: Тип/стр
        // 4: Статус
        // 5: K (Комментарии)
        // 6: Имя файла (Исходное наименование)
        // 7: Создан
        // 8: Примечание

        // ID (колонка 0)
        CustomMenuItem useIdItem = new CustomMenuItem();
        cbUseId = new CheckBox("ID");
        useIdItem.setContent(cbUseId);
        useIdItem.setHideOnClick(false);

        // ТИП ДОКУМЕНТА (колонка 3)
        CustomMenuItem useDraftTypeItem = new CustomMenuItem();
        cbUseDraftType = new CheckBox("Тип/стр");
        useDraftTypeItem.setContent(cbUseDraftType);
        useDraftTypeItem.setHideOnClick(false);

        // СТАТУС (колонка 4)
        CustomMenuItem useStatusItem = new CustomMenuItem();
        cbUseStatus = new CheckBox("Статус");
        useStatusItem.setContent(cbUseStatus);
        useStatusItem.setHideOnClick(false);

        // КОММЕНТАРИИ (колонка 5)
        CustomMenuItem useRemarksItem = new CustomMenuItem();
        cbUseRemarks = new CheckBox("Комментарии(К)");
        useRemarksItem.setContent(cbUseRemarks);
        useRemarksItem.setHideOnClick(false);

        // ИСХОДНОЕ НАИМЕНОВАНИЕ ФАЙЛА (колонка 6)
        CustomMenuItem useInitialNameItem = new CustomMenuItem();
        cbUseInitialName = new CheckBox("Исходное наименование");
        useInitialNameItem.setContent(cbUseInitialName);
        useInitialNameItem.setHideOnClick(false);

        // ДОКУМЕНТ СОЗДАН (колонка 7)
        CustomMenuItem useCreationTimeItem = new CustomMenuItem();
        cbUseCreationTime = new CheckBox("Документ создан");
        useCreationTimeItem.setContent(cbUseCreationTime);
        useCreationTimeItem.setHideOnClick(false);

        // ПРИМЕЧАНИЕ (колонка 8)
        CustomMenuItem useNoteItem = new CustomMenuItem();
        cbUseNote = new CheckBox("Примечание");
        useNoteItem.setContent(cbUseNote);
        useNoteItem.setHideOnClick(false);

        if (CH_CURRENT_USER_GROUP.isAdministrate()) getItems().add(useIdItem);
        getItems().addAll(useDraftTypeItem, useStatusItem, useRemarksItem, useInitialNameItem, useCreationTimeItem, useNoteItem);
    }

    /**
     * Обновляет состояние CheckBox из фактической видимости колонок таблицы
     */
    private void updateCheckBoxesFromActualColumns() {
        if (tableView.getColumns().size() >= 9) {
            if (cbUseId != null) cbUseId.setSelected(tableView.getColumns().get(0).isVisible());
            if (cbUseDraftType != null) cbUseDraftType.setSelected(tableView.getColumns().get(3).isVisible());
            if (cbUseStatus != null) cbUseStatus.setSelected(tableView.getColumns().get(4).isVisible());
            if (cbUseRemarks != null) cbUseRemarks.setSelected(tableView.getColumns().get(5).isVisible());
            if (cbUseInitialName != null) cbUseInitialName.setSelected(tableView.getColumns().get(6).isVisible());
            if (cbUseCreationTime != null) cbUseCreationTime.setSelected(tableView.getColumns().get(7).isVisible());
            if (cbUseNote != null) cbUseNote.setSelected(tableView.getColumns().get(8).isVisible());
        }
    }

    /**
     * Применяет изменения после закрытия меню
     */
    private void applyChanges() {
        Platform.runLater(() -> {
            if (tableView.getColumns().size() >= 9) {
                // Устанавливаем видимость колонок непосредственно
                tableView.getColumns().get(0).setVisible(cbUseId != null && cbUseId.isSelected());     // ID
                tableView.getColumns().get(3).setVisible(cbUseDraftType != null && cbUseDraftType.isSelected()); // Тип/стр
                tableView.getColumns().get(4).setVisible(cbUseStatus != null && cbUseStatus.isSelected());     // Статус
                tableView.getColumns().get(5).setVisible(cbUseRemarks != null && cbUseRemarks.isSelected());   // Комментарии
                tableView.getColumns().get(6).setVisible(cbUseInitialName != null && cbUseInitialName.isSelected()); // Имя файла
                tableView.getColumns().get(7).setVisible(cbUseCreationTime != null && cbUseCreationTime.isSelected()); // Создан
                tableView.getColumns().get(8).setVisible(cbUseNote != null && cbUseNote.isSelected());       // Примечание

                // Синхронизируем поля showXxx в Draft_TableView
                syncShowFieldsFromColumns();

                tableView.updateView();
                tableView.refresh();
            }
        });
    }

    /**
     * Синхронизирует поля showXxx с фактической видимостью колонок
     */
    private void syncShowFieldsFromColumns() {
        if (tableView.getColumns().size() >= 9) {
            tableView.setShowId(tableView.getColumns().get(0).isVisible());
            tableView.setShowDraftType(tableView.getColumns().get(3).isVisible());
            tableView.setShowStatus(tableView.getColumns().get(4).isVisible());
            tableView.setShowRemarks(tableView.getColumns().get(5).isVisible());
            tableView.setShowInitialName(tableView.getColumns().get(6).isVisible());
            tableView.setShowCreationTime(tableView.getColumns().get(7).isVisible());
            tableView.setShowNote(tableView.getColumns().get(8).isVisible());
        }
    }
}
