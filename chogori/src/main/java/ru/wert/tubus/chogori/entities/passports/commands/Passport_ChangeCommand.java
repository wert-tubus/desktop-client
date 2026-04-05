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
public class Passport_ChangeCommand implements ICommand {

    private Passport item;
    private Passport_TableView tableView;

    public Passport_ChangeCommand(Passport item, Passport_TableView tableView) {
        this.item = item;
        this.tableView = tableView;
    }

    @Override
    public void execute() {
        try {
            ChogoriServices.CH_QUICK_PASSPORTS.update(item);

            Platform.runLater(() -> {
                // Используем метод из TableView
                tableView.refreshPreservingType();

                // Находим и выделяем измененный элемент
                int updatedIndex = findItemIndex(item);
                if (updatedIndex >= 0) {
                    tableView.scrollTo(updatedIndex);
                    tableView.getSelectionModel().select(updatedIndex);
                    log.info("Обновлен паспорт {}", item.toUsefulString());
                } else {
                    log.debug("Паспорт {} больше не соответствует типу таблицы", item.toUsefulString());
                }
            });

        } catch (Exception e) {
            Warning1.create($ATTENTION, $ERROR_WHILE_CHANGING_ITEM, $ITEM_IS_NOT_AVAILABLE_MAYBE);
            log.error("При обновлении паспорта {} произошла ошибка", item.toUsefulString(), e);
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
