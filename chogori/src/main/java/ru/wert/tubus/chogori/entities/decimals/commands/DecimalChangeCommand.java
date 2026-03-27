package ru.wert.tubus.chogori.entities.decimals.commands;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.entities.decimals.Decimal_TableView;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.winform.warnings.Warning1;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;
import static ru.wert.tubus.winform.warnings.WarningMessages.*;

/**
 * Команда для изменения существующего десятичного классификатора
 */
@Slf4j
public class DecimalChangeCommand implements ICommand {

    private Decimal item;
    private Decimal_TableView tableView;

    /**
     * Конструктор команды изменения
     * @param item изменяемый объект Decimal
     * @param tableView таблица для обновления
     */
    public DecimalChangeCommand(Decimal item, Decimal_TableView tableView) {
        this.item = item;
        this.tableView = tableView;
    }

    @Override
    public void execute() {
        try {
            CH_DECIMALS.update(item);

            Platform.runLater(() -> {
                tableView.easyUpdate(CH_DECIMALS);
                tableView.scrollTo(item);
                tableView.getSelectionModel().select(item);
            });

            log.info("Обновлен десятичный классификатор {}", item.getName());
            AppStatic.createLog(true, String.format("Изменил десятичный классификатор '%s'", item.getName()));
        } catch (Exception e) {
            Warning1.create($ATTENTION, $ERROR_WHILE_CHANGING_ITEM, $ITEM_IS_NOT_AVAILABLE_MAYBE);
            log.error("При обновлении десятичного классификатора {} произошла ошибка {}", item.getName(), e.getMessage());
        }
    }
}
