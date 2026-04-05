package ru.wert.tubus.chogori.entities.passports.commands;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.winform.warnings.Warning1;

import static ru.wert.tubus.winform.warnings.WarningMessages.*;

@Slf4j
public class Passport_AddCommand implements ICommand {

    private Passport newItem;
    private Passport_TableView tableView;

    public Passport_AddCommand(Passport newItem, Passport_TableView tableView) {
        this.newItem = newItem;
        this.tableView = tableView;
    }

    @Override
    public void execute() {
        try {
            ChogoriServices.CH_QUICK_PASSPORTS.save(newItem);

            Platform.runLater(() -> {
                // Используем метод из TableView
                tableView.refreshPreservingType();

                // Находим и выделяем добавленный элемент
                int addedIndex = findItemIndex(newItem);
                if (addedIndex >= 0) {
                    tableView.scrollTo(addedIndex);
                    tableView.getSelectionModel().select(addedIndex);
                    log.info("Добавлен паспорт {}, выбран индекс {}", newItem.toUsefulString(), addedIndex);
                }
            });

        } catch (Exception e) {
            Warning1.create($ATTENTION, $ERROR_WHILE_ADDING_ITEM, $SERVER_IS_NOT_AVAILABLE_MAYBE);
            log.error("При добавлении паспорта {} произошла ошибка", newItem.toUsefulString(), e);
        }
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
