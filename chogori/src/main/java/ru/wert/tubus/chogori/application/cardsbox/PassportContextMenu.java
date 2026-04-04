package ru.wert.tubus.chogori.application.cardsbox;

import javafx.scene.control.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.function.Consumer;

import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

/**
 * Контекстное меню для списка паспортов в журнале регистрации.
 * Предоставляет операции редактирования и удаления паспортов.
 */
@Slf4j
public class PassportContextMenu {

    private final ListView<Passport> listView;
    private final Consumer<Passport> editCallback;
    private final Runnable refreshCallback;
    private final Runnable refreshSelectedCallback;

    @Setter
    private Runnable onDeleteCallback;  // Колбэк после успешного удаления

    /**
     * Конструктор контекстного меню.
     *
     * @param listView               список паспортов
     * @param editCallback           колбэк для редактирования
     * @param refreshCallback        колбэк для обновления таблиц
     * @param refreshSelectedCallback колбэк для обновления выбранного списка
     */
    public PassportContextMenu(ListView<Passport> listView,
                               Consumer<Passport> editCallback,
                               Runnable refreshCallback,
                               Runnable refreshSelectedCallback) {
        this.listView = listView;
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
            Passport selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null && editCallback != null) {
                editCallback.accept(selected);
            }
        });

        // Пункт меню "Удалить из списка"
        MenuItem removeItem = new MenuItem("Удалить из списка");
        removeItem.setOnAction(event -> {
            Passport selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                listView.getItems().remove(selected);
                if (refreshSelectedCallback != null) {
                    refreshSelectedCallback.run();
                }
                log.info("Паспорт {} удален из списка выбранных", selected.getNumber());
            }
        });

        // Пункт меню "Удалить из базы данных"
        MenuItem deleteFromDbItem = new MenuItem("Удалить из базы данных");
        deleteFromDbItem.setOnAction(event -> {
            Passport selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deletePassportFromDatabase(selected);
            }
        });

        contextMenu.getItems().addAll(editItem, removeItem, deleteFromDbItem);
        listView.setContextMenu(contextMenu);
    }

    /**
     * Удаление паспорта из базы данных.
     *
     * @param passport паспорт для удаления
     */
    private void deletePassportFromDatabase(Passport passport) {
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
            // Вызов сервиса удаления (замените на ваш сервис)
            boolean deleted = ru.wert.tubus.chogori.application.services.ChogoriServices.CH_QUICK_PASSPORTS.delete(passport);

            if (deleted) {
                log.info("Паспорт {} успешно удален из базы данных", passport.getNumber());

                // Удаление из списка выбранных
                listView.getItems().remove(passport);

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