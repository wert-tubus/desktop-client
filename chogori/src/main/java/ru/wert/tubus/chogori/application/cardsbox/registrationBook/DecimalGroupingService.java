package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import ru.wert.tubus.client.entity.models.Decimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Сервис для определения группы децимального номера.
 * Содержит бизнес-логику группировки Decimal по диапазонам.
 */
public class DecimalGroupingService {

    public enum DecimalGroup {
        SKETCH("Эскиз", d -> "Эскиз".equals(d.getName())),
        DETAILS_700("Детали 700000-744999",
                d -> isInRange(d, 700000, 744999)),
        DETAILS_745("Детали 745000-799999",
                d -> isInRange(d, 745000, 799999)),
        ASSM_300("Сборочные 300000-399999",
                d -> isInRange(d, 300000, 399999)),
        ASSM_400("Сборочные 400000-499999",
                d -> isInRange(d, 400000, 499999)),
        MEDICINE("Медицина 900000-999999",
                d -> isInRange(d, 900000, 999999)),
        OTHER("Прочие", d -> true);

        private final String title;
        private final Predicate<Decimal> matcher;

        DecimalGroup(String title, Predicate<Decimal> matcher) {
            this.title = title;
            this.matcher = matcher;
        }

        public String getTitle() { return title; }

        public boolean matches(Decimal decimal) {
            return matcher.test(decimal);
        }

        private static boolean isInRange(Decimal d, int start, int end) {
            try {
                int value = Integer.parseInt(d.getName());
                return value >= start && value <= end;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    /**
     * Определяет группу для децимального номера
     */
    public static DecimalGroup determineGroup(Decimal decimal) {
        for (DecimalGroup group : DecimalGroup.values()) {
            if (group.matches(decimal) && group != DecimalGroup.OTHER) {
                return group;
            }
        }
        return DecimalGroup.OTHER;
    }

    /**
     * Проверяет, является ли децимальный номер эскизом
     */
    public static boolean isSketch(Decimal decimal) {
        return DecimalGroup.SKETCH.matches(decimal);
    }
}
