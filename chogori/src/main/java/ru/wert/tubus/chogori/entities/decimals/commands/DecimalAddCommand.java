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
 * Команда для добавления нового десятичного классификатора
 */
@Slf4j
public class DecimalAddCommand implements ICommand {

    private Decimal newItem;
    private Decimal_TableView tableView;

    /**
     * Конструктор команды добавления
     * @param newItem новый объект Decimal
     * @param tableView таблица для обновления
     */
    public DecimalAddCommand(Decimal newItem, Decimal_TableView tableView) {
        this.newItem = newItem;
        this.tableView = tableView;
    }

    @Override
    public void execute() {
        try {
            Decimal savedDecimal = CH_DECIMALS.save(newItem);

            Platform.runLater(() -> {
                tableView.easyUpdate(CH_DECIMALS);
                tableView.scrollTo(newItem);
                tableView.getSelectionModel().select(newItem);
            });

            log.info("Добавлен десятичный классификатор {}", newItem.getName());
            AppStatic.createLog(true, String.format("Добавил десятичный классификатор '%s'", newItem.getName()));
        } catch (Exception e) {
            Warning1.create($ATTENTION, $ERROR_WHILE_ADDING_ITEM, $SERVER_IS_NOT_AVAILABLE_MAYBE);
            log.error("При добавлении десятичного классификатора {} произошла ошибка {}", newItem.getName(), e.getMessage());
        }
    }
}