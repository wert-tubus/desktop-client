package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.client.entity.serviceREST.PassportService;
import ru.wert.tubus.client.entity.serviceREST.PrefixService;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_DEFAULT_PREFIX;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_EMPTY_PREFIX;

/**
 * Сервис для операций с паспортами (поиск номеров, форматирование и т.д.)
 */
@Slf4j
public class RegistrationService {

    private static final String SKETCH_NAME = "Эскиз";
    private static final String PIK_PREFIX = "ПИК";

    private final PassportService passportService;
    private final PrefixService prefixService;

    // Кэш для префиксов
    private Prefix defaultPrefixCache;
    private Prefix emptyPrefixCache;

    // Кэш для паспортов
    private final Map<String, Passport> passportCache = new ConcurrentHashMap<>();

    public RegistrationService() {
        this.passportService = (PassportService) ChogoriServices.CH_PASSPORTS;
        this.prefixService = PrefixService.getInstance();
        initPrefixCache();
    }

    /**
     * Инициализация кэша префиксов
     */
    private void initPrefixCache() {
        defaultPrefixCache = CH_DEFAULT_PREFIX;
        emptyPrefixCache = CH_EMPTY_PREFIX;

        if (defaultPrefixCache == null) {
            log.error("Префикс по умолчанию '{}' не найден в БД", CH_DEFAULT_PREFIX);
        }
        if (emptyPrefixCache == null) {
            log.error("Пустой префикс '{}' не найден в БД", CH_EMPTY_PREFIX);
        }
    }

    /**
     * Быстрый поиск паспорта по номеру с определением префикса
     * Использует прямой запрос к БД через findByPrefixIdAndNumber
     */
    public Passport findPassportByNumberFast(String number) {
        if (number == null || number.isEmpty()) {
            return null;
        }

        // Проверяем кэш
        if (passportCache.containsKey(number)) {
            return passportCache.get(number);
        }

        Passport passport = null;

        // Определяем тип номера
        if (number.startsWith("Э")) {
            // Эскизный номер
            if (emptyPrefixCache != null) {
                passport = passportService.findByPrefixIdAndNumber(emptyPrefixCache, number);
            }
        } else if (number.contains(".")) {
            // ПИК номер (например, 745222.001)
            if (defaultPrefixCache != null) {
                passport = passportService.findByPrefixIdAndNumber(defaultPrefixCache, number);
            }
        } else {
            // Неизвестный формат - пробуем оба варианта
            if (defaultPrefixCache != null) {
                passport = passportService.findByPrefixIdAndNumber(defaultPrefixCache, number);
            }

            if (passport == null && emptyPrefixCache != null) {
                passport = passportService.findByPrefixIdAndNumber(emptyPrefixCache, number);
            }
        }

        // Сохраняем в кэш (даже null, чтобы не повторять запросы)
        passportCache.put(number, passport);

        if (passport != null) {
            log.debug("Паспорт {} найден через прямой запрос", number);
        } else {
            log.debug("Паспорт {} не найден в БД", number);
        }

        return passport;
    }

    /**
     * Массовая загрузка паспортов по списку номеров (оптимизированная версия)
     * Для восстановления состояния
     */
    public Map<String, Passport> loadPassportsByNumbersMap(List<String> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Passport> result = new HashMap<>();

        // Загружаем каждый паспорт через быстрый метод (с кэшированием)
        for (String number : numbers) {
            Passport passport = findPassportByNumberFast(number);
            if (passport != null) {
                result.put(number, passport);
            }
        }

        log.info("Загружено {}/{} паспортов", result.size(), numbers.size());
        return result;
    }

    /**
     * Очистка кэша паспортов (при обновлении/удалении)
     */
    public void clearPassportCache() {
        passportCache.clear();
        log.debug("Кэш паспортов очищен");
    }

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

    /**
     * Получение всех паспортов (только для операций генерации номеров)
     */
    private List<Passport> getAllPassports() {
        try {
            return passportService.findAll();
        } catch (Exception e) {
            log.error("Ошибка при загрузке паспортов из БД", e);
            return new ArrayList<>();
        }
    }

    /**
     * @deprecated Используйте findPassportByNumberFast для производительности
     */
    @Deprecated
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
