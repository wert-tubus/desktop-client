package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_DEFAULT_PREFIX;

/**
 * Сервис для операций с паспортами (поиск номеров, форматирование и т.д.)
 */
@Slf4j
public class RegistrationService {

    private static final String SKETCH_NAME = "Эскиз";
    private static final String PIK_PREFIX = "ПИК";

    /**
     * Получение следующего доступного номера для паспорта ПИК
     */
    public String getNextPIKNumber(Decimal decimal) {
        Decimal freshDecimal = CH_DECIMALS.findById(decimal.getId());
        if (freshDecimal == null) {
            throw new RuntimeException("Децимальная группа не найдена");
        }

        List<Passport> allPassports = getAllPassports();
        int currentLastNumber = freshDecimal.getLastNumber();
        int initialNumber = freshDecimal.getInitialNumber();

        Integer freeNumber = findFreePIKNumber(freshDecimal.getName(), allPassports, initialNumber, currentLastNumber);

        if (freeNumber != null) {
            return formatPIKNumber(freshDecimal.getName(), freeNumber);
        } else {
            int newNumber = currentLastNumber + 1;
            String formattedNumber = formatPIKNumber(freshDecimal.getName(), newNumber);
            freshDecimal.setLastNumber(newNumber);

            if (!CH_DECIMALS.update(freshDecimal)) {
                throw new RuntimeException("Не удалось обновить lastNumber в базе данных");
            }
            return formattedNumber;
        }
    }

    /**
     * Получение следующего доступного номера для эскизного паспорта
     */
    public String getNextSketchNumber(Decimal decimal) {
        Decimal freshDecimal = CH_DECIMALS.findById(decimal.getId());
        if (freshDecimal == null) {
            throw new RuntimeException("Децимальная группа не найдена");
        }

        List<Passport> allPassports = getAllPassports();
        int currentLastNumber = freshDecimal.getLastNumber();
        int initialNumber = freshDecimal.getInitialNumber();

        Integer freeNumber = findFreeSketchNumber(allPassports, initialNumber, currentLastNumber);

        if (freeNumber != null) {
            return formatSketchNumber(freeNumber);
        } else {
            int newNumber = currentLastNumber + 1;
            String formattedNumber = formatSketchNumber(newNumber);
            freshDecimal.setLastNumber(newNumber);

            if (!CH_DECIMALS.update(freshDecimal)) {
                throw new RuntimeException("Не удалось обновить lastNumber в базе данных");
            }
            return formattedNumber;
        }
    }

    /**
     * Поиск свободного номера ПИК в диапазоне
     */
    private Integer findFreePIKNumber(String decimalName, List<Passport> allPassports, int start, int end) {
        Set<Integer> usedNumbers = allPassports.stream()
                .filter(p -> p.getPrefix() != null && PIK_PREFIX.equals(p.getPrefix().getName()))
                .filter(p -> p.getNumber() != null && p.getNumber().startsWith(decimalName + "."))
                .map(p -> {
                    String[] parts = p.getNumber().split("\\.");
                    if (parts.length == 2) {
                        try {
                            return Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            log.warn("Неверный формат номера: {}", p.getNumber());
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (int i = start; i <= end; i++) {
            if (!usedNumbers.contains(i)) {
                return i;
            }
        }
        return null;
    }

    /**
     * Поиск свободного номера эскиза в диапазоне
     */
    private Integer findFreeSketchNumber(List<Passport> allPassports, int start, int end) {
        Set<Integer> usedNumbers = allPassports.stream()
                .filter(p -> (p.getPrefix() == null || p.getPrefix().getName() == null || "-".equals(p.getPrefix().getName())))
                .filter(p -> p.getNumber() != null && p.getNumber().startsWith("Э"))
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getNumber().substring(1));
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (int i = start; i <= end; i++) {
            if (!usedNumbers.contains(i)) {
                return i;
            }
        }
        return null;
    }

    private String formatPIKNumber(String decimalName, int number) {
        return String.format("%s.%03d", decimalName, number);
    }

    private String formatSketchNumber(int number) {
        return String.format("Э%05d", number);
    }

    private List<Passport> getAllPassports() {
        try {
            return ChogoriServices.CH_PASSPORTS.findAll();
        } catch (Exception e) {
            log.error("Ошибка при загрузке паспортов из БД", e);
            return new ArrayList<>();
        }
    }

    public Passport getPassportByNumber(String number) {
        try {
            return getAllPassports().stream()
                    .filter(p -> p.getNumber() != null && p.getNumber().equals(number))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Ошибка при поиске паспорта по номеру {}", number, e);
            return null;
        }
    }

    public void rollbackLastNumber(Decimal decimal, int reservedNumber) {
        try {
            Decimal freshDecimal = CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal != null && freshDecimal.getLastNumber() == reservedNumber) {
                freshDecimal.setLastNumber(reservedNumber - 1);
                CH_DECIMALS.update(freshDecimal);
                log.info("Выполнен откат lastNumber для decimal {} с {} на {}",
                        decimal.getName(), reservedNumber, reservedNumber - 1);
            }
        } catch (Exception e) {
            log.error("Ошибка при откате lastNumber", e);
        }
    }
}
