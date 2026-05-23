package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.collections.ObservableList;
import ru.wert.tubus.chogori.application.cardsbox.RegisteredPassportsStorage;
import ru.wert.tubus.client.entity.models.Passport;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Менеджер для управления списком недавно зарегистрированных номеров
 */
@Slf4j
public class RegisteredPassportsManager {

    private final ObservableList<Passport> registeredPassports;
    private final RegistrationService registrationService;

    public RegisteredPassportsManager(ObservableList<Passport> registeredPassports, RegistrationService registrationService) {
        this.registeredPassports = registeredPassports;
        this.registrationService = registrationService;
    }

    /**
     * Добавление паспорта в список с сохранением состояния
     */
    public void addPassport(Passport passport) {
        if (passport == null || passport.getNumber() == null) {
            log.warn("Попытка добавить null паспорт или паспорт без номера");
            return;
        }

        boolean exists = registeredPassports.stream()
                .anyMatch(p -> p.getNumber() != null && p.getNumber().equals(passport.getNumber()));

        if (!exists) {
            registeredPassports.add(passport);
            saveState();
            log.info("Паспорт {} добавлен в список выбранных", passport.getNumber());
        } else {
            log.debug("Паспорт {} уже существует в списке", passport.getNumber());
        }
    }

    /**
     * Очистка списка
     */
    public void clear() {
        if (!registeredPassports.isEmpty()) {
            registeredPassports.clear();
            saveState();
            log.info("Список выбранных паспортов очищен");
        }
    }

    /**
     * Обновление списка (удаление неактуальных паспортов)
     * Оптимизированная версия с использованием прямых запросов
     */
    public void refresh() {
        if (registeredPassports.isEmpty()) {
            return;
        }

        List<Passport> validPassports = new ArrayList<>();
        List<Passport> invalidPassports = new ArrayList<>();

        for (Passport passport : registeredPassports) {
            // Используем быстрый поиск вместо загрузки всех паспортов
            Passport freshPassport = registrationService.findPassportByNumberFast(passport.getNumber());
            if (freshPassport != null) {
                validPassports.add(freshPassport);
            } else {
                invalidPassports.add(passport);
                log.warn("Паспорт {} не найден в БД, удаляем из списка выбранных", passport.getNumber());
            }
        }

        if (!invalidPassports.isEmpty()) {
            registeredPassports.clear();
            registeredPassports.addAll(validPassports);
            saveState();
            log.info("Удалено {} неактуальных паспортов из списка выбранных", invalidPassports.size());
        }
    }

    /**
     * Обновление существующего паспорта в списке
     */
    public void updatePassport(Passport oldPassport, Passport updatedPassport) {
        if (oldPassport == null || updatedPassport == null) {
            log.warn("Попытка обновления с null параметрами");
            return;
        }

        int index = registeredPassports.indexOf(oldPassport);
        if (index >= 0) {
            registeredPassports.set(index, updatedPassport);
            saveState();
            log.info("Паспорт {} обновлен на {}", oldPassport.getNumber(), updatedPassport.getNumber());
        } else {
            log.debug("Паспорт {} не найден в списке для обновления", oldPassport.getNumber());
        }
    }

    /**
     * Восстановление состояния из хранилища
     * Оптимизированная версия с массовой загрузкой
     */
    public void restoreState() {
        List<String> savedNumbers = RegisteredPassportsStorage.loadRegisteredPassportNumbers();
        if (savedNumbers.isEmpty()) {
            log.info("Нет сохраненных выбранных паспортов для восстановления");
            return;
        }

        log.debug("Начинаем восстановление {} сохраненных номеров", savedNumbers.size());

        // Используем оптимизированную массовую загрузку
        Map<String, Passport> passportMap = registrationService.loadPassportsByNumbersMap(savedNumbers);

        List<Passport> restored = new ArrayList<>();
        List<String> notFoundNumbers = new ArrayList<>();

        for (String number : savedNumbers) {
            Passport passport = passportMap.get(number);
            if (passport != null) {
                restored.add(passport);
            } else {
                notFoundNumbers.add(number);
                log.warn("Паспорт с номером {} не найден в БД", number);
            }
        }

        if (!restored.isEmpty()) {
            registeredPassports.setAll(restored);
            log.info("Восстановлено {} выбранных паспортов", restored.size());

            // Если какие-то номера не найдены, сохраняем только валидные
            if (!notFoundNumbers.isEmpty()) {
                log.info("{} номеров не найдено, обновляем сохраненное состояние", notFoundNumbers.size());
                saveState(); // Сохраняем только валидные номера
            }
        } else {
            log.warn("Не найдены паспорта для сохраненных номеров: {}", savedNumbers);
            RegisteredPassportsStorage.clearSavedState();
        }
    }

    /**
     * Сохранение состояния
     */
    public void saveState() {
        if (!registeredPassports.isEmpty()) {
            RegisteredPassportsStorage.saveRegisteredPassports(registeredPassports);
            log.debug("Сохранено {} паспортов", registeredPassports.size());
        } else {
            RegisteredPassportsStorage.clearSavedState();
            log.debug("Список пуст, состояние очищено");
        }
    }

    /**
     * Получение списка паспортов
     */
    public ObservableList<Passport> getList() {
        return registeredPassports;
    }

    /**
     * Проверка, пуст ли список
     */
    public boolean isEmpty() {
        return registeredPassports.isEmpty();
    }

    /**
     * Размер списка
     */
    public int size() {
        return registeredPassports.size();
    }

    /**
     * Проверка наличия паспорта в списке по номеру
     */
    public boolean contains(String number) {
        if (number == null) return false;
        return registeredPassports.stream()
                .anyMatch(p -> p.getNumber() != null && p.getNumber().equals(number));
    }

    /**
     * Удаление паспорта из списка по номеру
     */
    public boolean remove(String number) {
        if (number == null) return false;

        Passport toRemove = registeredPassports.stream()
                .filter(p -> p.getNumber() != null && p.getNumber().equals(number))
                .findFirst()
                .orElse(null);

        if (toRemove != null) {
            boolean removed = registeredPassports.remove(toRemove);
            if (removed) {
                saveState();
                log.info("Паспорт {} удален из списка", number);
            }
            return removed;
        }

        return false;
    }

    /**
     * Очистка кэша паспортов в сервисе
     */
    public void clearCache() {
        registrationService.clearPassportCache();
        log.debug("Кэш паспортов очищен");
    }
}
