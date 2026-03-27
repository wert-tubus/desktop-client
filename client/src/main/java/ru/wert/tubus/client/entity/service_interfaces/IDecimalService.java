package ru.wert.tubus.client.entity.service_interfaces;

import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.interfaces.ItemService;

import java.util.List;

/**
 * Интерфейс сервиса для работы с десятичными классификаторами (Decimal)
 * Расширяет базовый интерфейс ItemService
 */
public interface IDecimalService extends ItemService<Decimal> {

    /**
     * Найти десятичный классификатор по имени
     * @param name имя классификатора
     * @return найденный Decimal или null
     */
    Decimal findByName(String name);

    /**
     * Найти все десятичные классификаторы, содержащие текст в имени
     * @param text текст для поиска
     * @return список найденных Decimal
     */
    List<Decimal> findAllByText(String text);
}
