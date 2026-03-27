package ru.wert.tubus.client.entity.models;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.interfaces.Item;

import java.io.Serializable;

/**
 * Класс Decimal представляет десятичный классификатор
 * Используется для управления диапазонами номеров в проектах
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"name"}, callSuper = false)
public class Decimal extends _BaseEntity implements Item, Serializable {

    /** Наименование десятичного классификатора */
    private String name;

    /** Описание десятичного классификатора */
    private String description;

    /** Начальный номер диапазона */
    private Integer initialNumber;

    /** Конечный номер диапазона */
    private Integer lastNumber;

    /**
     * Возвращает строковое представление объекта для отображения в интерфейсе
     * @return наименование классификатора
     */
    @Override
    public String toUsefulString() {
        return name;
    }

    /**
     * Проверяет, является ли диапазон номеров валидным
     * @return true если начальный номер меньше или равен конечному, и оба номера не null
     */
    public boolean isValidRange() {
        if (initialNumber == null || lastNumber == null) {
            return false;
        }
        return initialNumber <= lastNumber;
    }

    /**
     * Проверяет, входит ли номер в диапазон классификатора
     * @param number проверяемый номер
     * @return true если номер находится в диапазоне [initialNumber, lastNumber]
     */
    public boolean containsNumber(int number) {
        if (initialNumber == null || lastNumber == null) {
            return false;
        }
        return number >= initialNumber && number <= lastNumber;
    }

    /**
     * Возвращает количество номеров в диапазоне
     * @return количество номеров или 0 если диапазон не валиден
     */
    public int getRangeSize() {
        if (!isValidRange()) {
            return 0;
        }
        return lastNumber - initialNumber + 1;
    }

    /**
     * Возвращает строковое представление диапазона
     * @return строка вида "initialNumber - lastNumber"
     */
    public String getRangeString() {
        if (initialNumber == null || lastNumber == null) {
            return "не задан";
        }
        return initialNumber + " - " + lastNumber;
    }
}
