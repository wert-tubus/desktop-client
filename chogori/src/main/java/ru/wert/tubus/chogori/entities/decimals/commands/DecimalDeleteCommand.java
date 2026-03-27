package ru.wert.tubus.chogori.entities.decimals.commands;

import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.entities.decimals.Decimal_TableView;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.List;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;
import static ru.wert.tubus.winform.warnings.WarningMessages.*;

/**
 * Команда для удаления десятичных классификаторов
 */
@Slf4j
public class DecimalDeleteCommand implements ICommand {

    private List<Decimal> items;
    private Decimal_TableView tableView;

    /**
     * Конструктор команды удаления
     * @param items список удаляемых объектов Decimal
     * @param tableView таблица для обновления
     */
    public DecimalDeleteCommand(List<Decimal> items, Decimal_TableView tableView) {
        this.items = items;
        this.tableView = tableView;
    }

    @Override
    public void execute() {
        // После удаления таблица "подтянется вверх" и поэтому нужна позиция первого из удаляемых элементов
        int row = tableView.getItems().lastIndexOf(items.get(0));

        for (Decimal item : items) {
            try {
                CH_DECIMALS.delete(item);
                log.info("Удален десятичный классификатор {}", item.getName());
                AppStatic.createLog(true, String.format("Удалил десятичный классификатор '%s'", item.getName()));
            } catch (Exception e) {
                Warning1.create($ATTENTION, $ERROR_WHILE_DELETING_ITEM, $ITEM_IS_BUSY_MAYBE);
                log.error("При удалении десятичного классификатора {} произошла ошибка {}", item.getName(), e.getMessage());
            }
        }

        tableView.easyUpdate(CH_DECIMALS);
        tableView.getSelectionModel().select(row);
    }
}
