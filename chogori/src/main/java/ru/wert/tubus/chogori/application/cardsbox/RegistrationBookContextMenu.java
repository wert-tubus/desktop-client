package ru.wert.tubus.chogori.application.cardsbox;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.cardsbox.registrationBook.RegisteredPassportItem;
import ru.wert.tubus.chogori.entities.passports.PassportInfo_Patch;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.function.Consumer;

import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

/**
 * Контекстное меню для таблицы паспортов в журнале регистрации.
 * Предоставляет операции редактирования и удаления паспортов.
 */
@Slf4j
public class RegistrationBookContextMenu {

    private final TableView<RegisteredPassportItem> tableView;
    private final Consumer<Passport> editCallback;
    private final Runnable refreshCallback;
    private final Runnable refreshSelectedCallback;

    @Setter
    private Runnable onDeleteCallback;  // Колбэк после успешного удаления

    /**
     * Конструктор контекстного меню для TableView.
     *
     * @param tableView              таблица паспортов (с RegisteredPassportItem)
     * @param editCallback           колбэк для редактирования
     * @param refreshCallback        колбэк для обновления таблиц
     * @param refreshSelectedCallback колбэк для обновления выбранного списка
     */
    public RegistrationBookContextMenu(TableView<RegisteredPassportItem> tableView,
                                       Consumer<Passport> editCallback,
                                       Runnable refreshCallback,
                                       Runnable refreshSelectedCallback) {
        this.tableView = tableView;
        this.editCallback = editCallback;
        this.refreshCallback = refreshCallback;
        this.refreshSelectedCallback = refreshSelectedCallback;

        setupContextMenu();
    }

    /**
     * Настройка контекстного меню.
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        // Пункт меню "Редактировать"
        MenuItem editItem = new MenuItem("Редактировать");
        editItem.setOnAction(event -> {
            RegisteredPassportItem selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getPassport() != null && editCallback != null) {
                editCallback.accept(selectedItem.getPassport());
            }
        });

        // Пункт меню "Удалить из списка"
        MenuItem removeItem = new MenuItem("Удалить из списка");
        removeItem.setOnAction(event -> {
            RegisteredPassportItem selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getPassport() != null) {
                tableView.getItems().remove(selectedItem);
                if (refreshSelectedCallback != null) {
                    refreshSelectedCallback.run();
                }
                log.info("Паспорт {} удален из списка выбранных", selectedItem.getPassport().getNumber());
            }
        });

        // Пункт меню "Удалить из базы данных"
        MenuItem deleteFromDbItem = new MenuItem("Удалить из базы данных");
        deleteFromDbItem.setOnAction(event -> {
            RegisteredPassportItem selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getPassport() != null) {
                deletePassportFromDatabase(selectedItem.getPassport(), selectedItem);
            }
        });

        // Пункт меню "Инфо"
        MenuItem showInfo = new MenuItem("Инфо");
        showInfo.setOnAction(event -> {
            RegisteredPassportItem selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getPassport() != null) {
                PassportInfo_Patch.create(selectedItem.getPassport());
            }
        });

        MenuItem copyToClipboard = new MenuItem("Копировать наименование (Ctrl-C)");
        copyToClipboard.setOnAction(event -> copyPassportNameToClipboard());

        contextMenu.getItems().addAll(editItem, removeItem, deleteFromDbItem, copyToClipboard);
        contextMenu.getItems().add(new SeparatorMenuItem());
        contextMenu.getItems().add(showInfo);

        tableView.setContextMenu(contextMenu);
    }

    /**
     * Копирует наименование выбранного паспорта в системный буфер обмена.
     *
     * @return true если копирование выполнено успешно, false если элемент не выбран
     */
    public boolean copyPassportNameToClipboard() {
        RegisteredPassportItem selectedItem = tableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getPassport() != null) {
            String strToCopy = selectedItem.getPassport().toUsefulString();
            return AppStatic.copyTextToClipboard(strToCopy);
        }
        return false;
    }

    /**
     * Удаление паспорта из базы данных.
     *
     * @param passport      паспорт для удаления
     * @param selectedItem  элемент таблицы (для удаления из списка)
     */
    private void deletePassportFromDatabase(Passport passport, RegisteredPassportItem selectedItem) {
        // Подтверждение удаления
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение удаления");
        confirmAlert.setHeaderText("Удаление паспорта из базы данных");
        confirmAlert.setContentText("Вы действительно хотите удалить паспорт?\n" +
                passport.toUsefulString() + "\n\nЭто действие необратимо!");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // Вызов сервиса удаления
            boolean deleted = ru.wert.tubus.chogori.application.services.ChogoriServices.CH_QUICK_PASSPORTS.delete(passport);

            if (deleted) {
                log.info("Паспорт {} успешно удален из базы данных", passport.getNumber());

                // Удаление из таблицы
                tableView.getItems().remove(selectedItem);

                // Вызов колбэков для обновления
                if (refreshCallback != null) {
                    refreshCallback.run();
                }
                if (refreshSelectedCallback != null) {
                    refreshSelectedCallback.run();
                }
                if (onDeleteCallback != null) {
                    onDeleteCallback.run();
                }

                Warning1.create($ATTENTION,
                        "Паспорт успешно удален",
                        passport.toUsefulString());
            } else {
                Warning1.create($ATTENTION,
                        "Не удалось удалить паспорт",
                        "Попробуйте позже или обратитесь к администратору");
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении паспорта", e);
            Warning1.create($ATTENTION,
                    "Ошибка при удалении паспорта",
                    e.getMessage());
        }
    }
}