package ru.wert.tubus.chogori.entities.passports.commands;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.chogori.entities.passports.PassportType;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.ArrayList;
import java.util.List;

import static ru.wert.tubus.winform.warnings.WarningMessages.*;

@Slf4j
public class Passport_DeleteCommand implements ICommand {

    private final List<Passport> items;
    private final Passport_TableView tableView;

    public Passport_DeleteCommand(List<Passport> items, Passport_TableView tableView) {
        this.items = items;
        this.tableView = tableView;
    }

    @Override
    public void execute() {
        // Запоминаем текущую выделенную позицию до начала удаления
        int originalSelectedIndex = tableView.getSelectionModel().getSelectedIndex();
        Passport originalSelectedItem = originalSelectedIndex >= 0
                ? tableView.getItems().get(originalSelectedIndex)
                : null;

        // Показываем индикатор загрузки
        showProgressIndicator(true);

        // Создаем Task для выполнения в фоновом потоке
        Task<Void> deleteTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Запоминаем элемент для выделения после удаления
                Passport itemToSelect = findItemToSelectAfterDeletion();
                boolean anyDeleted = false;

                // Удаляем элементы с предварительной проверкой
                for (Passport passport : items) {
                    // Проверка в фоновом потоке: есть ли у паспорта чертежи?
                    if (hasDrafts(passport)) {
                        String message = String.format(
                                "Невозможно удалить номер '%s' — существуют связанные чертежи.",
                                passport.toUsefulString()
                        );
                        Platform.runLater(() ->
                                Warning1.create($ATTENTION, message, "Сначала удалите или переместите чертежи")
                        );
                        log.warn("Попытка удаления паспорта {} заблокирована: имеются чертежи", passport.toUsefulString());
                        continue;
                    }

                    try {
                        ChogoriServices.CH_QUICK_PASSPORTS.delete(passport);
                        anyDeleted = true;
                        log.info("Удалён паспорт {}", passport.toUsefulString());
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                Warning1.create($ATTENTION, $ERROR_WHILE_DELETING_ITEM, $ITEM_IS_BUSY_MAYBE)
                        );
                        log.error("При удалении паспорта {} произошла ошибка", passport.toUsefulString(), e);
                    }
                }

                final boolean finalAnyDeleted = anyDeleted;
                final Passport finalItemToSelect = itemToSelect;

                // Обновляем UI после удаления
                Platform.runLater(() -> {
                    if (finalAnyDeleted) {
                        tableView.refreshPreservingType();
                        restoreSelection(finalItemToSelect);

                        // Уведомление об удалении
                        if (tableView.getCardsBoxController() != null) {
                            for (Passport deleted : items) {
                                tableView.getCardsBoxController().notifyPassportDeleted(deleted);
                            }
                        }
                    } else {
                        restoreOriginalSelection(originalSelectedItem, originalSelectedIndex);
                    }
                    showProgressIndicator(false);
                });

                return null;
            }

            @Override
            protected void failed() {
                super.failed();
                Platform.runLater(() -> {
                    showProgressIndicator(false);
                    log.error("Task удаления паспортов провалился", getException());
                });
            }
        };

        // Запускаем Task в отдельном потоке
        new Thread(deleteTask).start();
    }

    /**
     * Показывает или скрывает индикатор загрузки
     */
    private void showProgressIndicator(boolean show) {
        Platform.runLater(() -> {
            if (tableView.getCardsBoxController() != null) {
                if (tableView.getPassportType() == PassportType.PIK) {
                    tableView.getCardsBoxController().getProgressbarPIK().setVisible(show);
                } else if (tableView.getPassportType() == PassportType.SKETCHES) {
                    tableView.getCardsBoxController().getProgressbarSketch().setVisible(show);
                }
            }
        });
    }

    private void restoreOriginalSelection(Passport originalSelectedItem, int originalSelectedIndex) {
        if (originalSelectedItem == null || originalSelectedIndex < 0) {
            return;
        }

        Platform.runLater(() -> {
            int currentIndex = findItemIndex(originalSelectedItem);
            if (currentIndex >= 0) {
                tableView.scrollTo(currentIndex);
                tableView.getSelectionModel().select(currentIndex);
            } else if (originalSelectedIndex < tableView.getItems().size()) {
                tableView.scrollTo(originalSelectedIndex);
                tableView.getSelectionModel().select(originalSelectedIndex);
            }
        });
    }

    private boolean hasDrafts(Passport passport) {
        if (passport == null || passport.getId() == null) {
            return false;
        }
        return !ChogoriServices.CH_QUICK_DRAFTS.findByPassport(passport).isEmpty();
    }

    private Passport findItemToSelectAfterDeletion() {
        if (tableView.getItems().isEmpty()) return null;

        int firstDeletedIndex = -1;
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (items.contains(tableView.getItems().get(i))) {
                firstDeletedIndex = i;
                break;
            }
        }

        if (firstDeletedIndex < 0) return null;

        if (firstDeletedIndex + 1 < tableView.getItems().size()) {
            Passport next = tableView.getItems().get(firstDeletedIndex + 1);
            if (!items.contains(next)) return next;
        }

        if (firstDeletedIndex - 1 >= 0) {
            Passport prev = tableView.getItems().get(firstDeletedIndex - 1);
            if (!items.contains(prev)) return prev;
        }

        for (Passport p : tableView.getItems()) {
            if (!items.contains(p)) return p;
        }

        return null;
    }

    private int findItemIndex(Passport passport) {
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (tableView.getItems().get(i).getId().equals(passport.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void restoreSelection(Passport itemToSelect) {
        if (itemToSelect != null) {
            int rowToSelect = findItemIndex(itemToSelect);
            if (rowToSelect >= 0) {
                final int finalRow = rowToSelect;
                Platform.runLater(() -> {
                    tableView.scrollTo(finalRow);
                    tableView.getSelectionModel().select(finalRow);
                });
            }
        } else if (!tableView.getItems().isEmpty()) {
            Platform.runLater(() -> {
                tableView.scrollTo(0);
                tableView.getSelectionModel().select(0);
            });
        }
    }
}
