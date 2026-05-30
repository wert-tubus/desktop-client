package ru.wert.tubus.chogori.application.cardsbox;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.cardsbox.registrationBook.RegisteredPassportItem;
import ru.wert.tubus.chogori.entities.passports.PassportInfo_Patch;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.warnings.Warning2;

import java.util.concurrent.CompletableFuture;
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
    private final Runnable disableControlsCallback;
    private final Runnable enableControlsCallback;

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
                                       Runnable refreshSelectedCallback,
                                       Runnable disableControlsCallback,   // НОВЫЙ ПАРАМЕТР
                                       Runnable enableControlsCallback) {  // НОВЫЙ ПАРАМЕТР
        this.tableView = tableView;
        this.editCallback = editCallback;
        this.refreshCallback = refreshCallback;
        this.refreshSelectedCallback = refreshSelectedCallback;
        this.disableControlsCallback = disableControlsCallback;
        this.enableControlsCallback = enableControlsCallback;

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

    private void showLoadingCursorAndDisableControls() {
        if (disableControlsCallback != null) {
            disableControlsCallback.run();
        }
    }

    private void hideLoadingCursorAndEnableControls() {
        if (enableControlsCallback != null) {
            enableControlsCallback.run();
        }
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
        boolean confirmed = Warning2.create("Подтверждение удаления",
                "Удаление чертежа из базы данных",
                String.format("Вы действительно хотите удалить чертеж '%s'?\n\n" +
                        "ВНИМАНИЕ: Операция необратима!", passport.getName()));

        if (!confirmed) {
            return;
        }

        // БЛОКИРУЕМ КОНТРОЛЫ ПЕРЕД ОПЕРАЦИЕЙ УДАЛЕНИЯ
        showLoadingCursorAndDisableControls();

        // Сохраняем ссылки для использования в асинхронном блоке
        Passport finalPassport = passport;
        RegisteredPassportItem finalSelectedItem = selectedItem;

        // Выполняем удаление в отдельном потоке
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Вызов сервиса удаления
                boolean deleted = ru.wert.tubus.chogori.application.services.ChogoriServices.CH_QUICK_PASSPORTS.delete(finalPassport);

                if (deleted) {
                    log.info("Паспорт {} успешно удален из базы данных", finalPassport.getNumber());
                    return true;
                } else {
                    log.error("Не удалось удалить паспорт: {}", finalPassport.getNumber());
                    return false;
                }
            } catch (Exception e) {
                log.error("Ошибка при удалении паспорта {}: {}", finalPassport.getNumber(), e.getMessage(), e);
                return false;
            }
        });

        // Обработка результата в UI потоке
        future.thenAcceptAsync(success -> Platform.runLater(() -> {
            // РАЗБЛОКИРУЕМ КОНТРОЛЫ ПОСЛЕ ОПЕРАЦИИ
            hideLoadingCursorAndEnableControls();

            if (success) {
                // Удаление из таблицы
                tableView.getItems().remove(finalSelectedItem);

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

                Warning1.create("УСПЕШНО!",
                        "Паспорт успешно удален",
                        finalPassport.toUsefulString());
            } else {
                Warning1.create($ATTENTION,
                        "Не удалось удалить чертеж",
                        "Попробуйте позже или обратитесь к администратору");
            }
        }), Platform::runLater);
    }
}