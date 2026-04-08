package ru.wert.tubus.chogori.entities.passports.commands;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.List;

import static ru.wert.tubus.winform.warnings.WarningMessages.*;

@Slf4j
public class Passport_DeleteCommand implements ICommand {

    private List<Passport> items;
    private Passport_TableView tableView;

    public Passport_DeleteCommand(List<Passport> items, Passport_TableView tableView) {
        this.items = items;
        this.tableView = tableView;
    }

    @Override
    public void execute() {
        // Запоминаем элемент для выделения после удаления
        Passport itemToSelect = findItemToSelectAfterDeletion();

        // Удаляем элементы
        for (Passport item : items) {
            try {
                ChogoriServices.CH_QUICK_PASSPORTS.delete(item);
                log.info("Удален паспорт {}", item.toUsefulString());
            } catch (Exception e) {
                Warning1.create($ATTENTION, $ERROR_WHILE_DELETING_ITEM, $ITEM_IS_BUSY_MAYBE);
                log.error("При удалении паспорта {} произошла ошибка", item.toUsefulString(), e);
            }
        }

        // Используем refreshPreservingType, который сохраняет текущий фильтр
        tableView.refreshPreservingType();

        // Выделяем нужную строку
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

    private Passport findItemToSelectAfterDeletion() {
        if (tableView.getItems().isEmpty()) return null;

        // Находим индекс первого удаляемого
        int firstDeletedIndex = -1;
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (items.contains(tableView.getItems().get(i))) {
                firstDeletedIndex = i;
                break;
            }
        }

        if (firstDeletedIndex < 0) return null;

        // Пытаемся взять следующий
        if (firstDeletedIndex + 1 < tableView.getItems().size()) {
            Passport next = tableView.getItems().get(firstDeletedIndex + 1);
            if (!items.contains(next)) return next;
        }

        // Пытаемся взять предыдущий
        if (firstDeletedIndex - 1 >= 0) {
            Passport prev = tableView.getItems().get(firstDeletedIndex - 1);
            if (!items.contains(prev)) return prev;
        }

        // Ищем любой неудаляемый
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
}
