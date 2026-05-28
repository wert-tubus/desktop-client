package ru.wert.tubus.chogori.entities.decimals.commands;

import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.entities.decimals.Decimal_TableView;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.List;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_PASSPORTS;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_DEFAULT_PREFIX;
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
            // Проверка наличия связанных Passport перед удалением
            if (hasLinkedPassports(item)) {
                Warning1.create($ATTENTION, $ERROR_WHILE_DELETING_ITEM,
                        String.format("Десятичный классификатор '%s' нельзя удалить, так как с ним связаны паспорта", item.getName()));
                log.warn("Невозможно удалить Decimal {}, так как существуют связанные Passport", item.getName());
                continue; // Пропускаем удаление этого элемента
            }

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

    /**
     * Проверяет, есть ли у Decimal связанные Passport с префиксом по умолчанию
     * @param decimal проверяемый десятичный классификатор
     * @return true - если есть хотя бы один связанный Passport, false - если нет
     */
    private boolean hasLinkedPassports(Decimal decimal) {
        String decimalName = decimal.getName(); // Например "745222"

        // Ищем все паспорта, содержащие в номере этот decimal
        List<Passport> passports = CH_PASSPORTS.findAllByText(decimalName);

        if (passports == null || passports.isEmpty()) {
            return false;
        }

        // Проверяем, есть ли паспорт с нужным префиксом и номером, начинающимся с decimalName
        for (Passport passport : passports) {
            if (passport.getPrefix() != null &&
                    passport.getNumber() != null &&
                    CH_DEFAULT_PREFIX.equals(passport.getPrefix()) &&
                    passport.getNumber().startsWith(decimalName + ".")) {
                return true;
            }
        }

        return false;
    }
}
