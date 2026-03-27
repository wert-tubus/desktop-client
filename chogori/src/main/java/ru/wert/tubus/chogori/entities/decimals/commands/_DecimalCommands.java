package ru.wert.tubus.chogori.entities.decimals.commands;

import javafx.event.Event;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.entities.decimals.Decimal_TableView;
import ru.wert.tubus.client.entity.models.Decimal;

import java.util.List;

/**
 * Класс-агрегатор команд для работы с десятичными классификаторами
 * Реализует интерфейс ItemCommands
 */
public class _DecimalCommands implements ItemCommands<Decimal> {

    private final Decimal_TableView tableView;
    private String accWindowRes;

    /**
     * Конструктор
     * @param tableView таблица десятичных классификаторов
     */
    public _DecimalCommands(Decimal_TableView tableView) {
        this.tableView = tableView;
    }

    /**
     * Добавление нового десятичного классификатора
     * @param event событие
     * @param newItem новый объект
     */
    @Override
    public void add(Event event, Decimal newItem) {
        ICommand command = new DecimalAddCommand((Decimal) newItem, tableView);
        command.execute();
    }

    /**
     * Копирование десятичного классификатора (не реализовано)
     * @param event событие
     */
    @Override
    public void copy(Event event) {
        // Копирование не требуется
    }

    /**
     * Удаление десятичных классификаторов
     * @param event событие
     * @param items список удаляемых объектов
     */
    @Override
    public void delete(Event event, List<Decimal> items) {
        ICommand command = new DecimalDeleteCommand(items, tableView);
        command.execute();
    }

    /**
     * Изменение десятичного классификатора
     * @param event событие
     * @param item изменяемый объект
     */
    @Override
    public void change(Event event, Decimal item) {
        ICommand command = new DecimalChangeCommand(item, tableView);
        command.execute();
    }
}
