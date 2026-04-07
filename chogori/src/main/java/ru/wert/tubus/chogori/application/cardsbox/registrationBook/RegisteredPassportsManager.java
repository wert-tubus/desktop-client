package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.collections.ObservableList;
import ru.wert.tubus.chogori.application.cardsbox.RegisteredPassportsStorage;
import ru.wert.tubus.client.entity.models.Passport;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Менеджер для управления списком выбранных паспортов
 */
@Slf4j
public class RegisteredPassportsManager {

    private final ObservableList<Passport> registeredPassports;
    private final PassportService passportService;

    public RegisteredPassportsManager(ObservableList<Passport> registeredPassports, PassportService passportService) {
        this.registeredPassports = registeredPassports;
        this.passportService = passportService;
    }

    /**
     * Добавление паспорта в список с сохранением состояния
     */
    public void addPassport(Passport passport) {
        boolean exists = registeredPassports.stream()
                .anyMatch(p -> p.getNumber() != null && p.getNumber().equals(passport.getNumber()));

        if (!exists) {
            registeredPassports.add(passport);
            sortPassports();
            saveState();
            log.info("Паспорт {} добавлен в список выбранных", passport.getNumber());
        }
    }

    /**
     * Очистка списка
     */
    public void clear() {
        registeredPassports.clear();
        saveState();
        log.info("Список выбранных паспортов очищен");
    }

    /**
     * Обновление списка (удаление неактуальных паспортов)
     */
    public void refresh() {
        List<Passport> validPassports = new ArrayList<>();
        List<Passport> invalidPassports = new ArrayList<>();

        for (Passport passport : registeredPassports) {
            Passport freshPassport = passportService.getPassportByNumber(passport.getNumber());
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
            sortPassports();
            saveState();
            log.info("Удалено {} неактуальных паспортов из списка выбранных", invalidPassports.size());
        }
    }

    /**
     * Обновление существующего паспорта в списке
     */
    public void updatePassport(Passport oldPassport, Passport updatedPassport) {
        int index = registeredPassports.indexOf(oldPassport);
        if (index >= 0) {
            registeredPassports.set(index, updatedPassport);
            sortPassports();
            saveState();
        }
    }

    /**
     * Восстановление состояния из хранилища
     */
    public void restoreState() {
        List<String> savedNumbers = RegisteredPassportsStorage.loadRegisteredPassportNumbers();
        if (savedNumbers.isEmpty()) {
            log.info("Нет сохраненных выбранных паспортов для восстановления");
            return;
        }

        List<Passport> restored = new ArrayList<>();
        for (String number : savedNumbers) {
            Passport passport = passportService.getPassportByNumber(number);
            if (passport != null) {
                restored.add(passport);
            } else {
                log.warn("Паспорт с номером {} не найден в БД", number);
            }
        }

        if (!restored.isEmpty()) {
            registeredPassports.setAll(restored);
            sortPassports();
            log.info("Восстановлено {} выбранных паспортов", restored.size());

            if (restored.size() != savedNumbers.size()) {
                saveState();
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
        } else {
            RegisteredPassportsStorage.clearSavedState();
        }
    }

    private void sortPassports() {
        registeredPassports.sort(Comparator.comparing(Passport::getNumber, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public ObservableList<Passport> getList() {
        return registeredPassports;
    }

    public boolean isEmpty() {
        return registeredPassports.isEmpty();
    }

    public int size() {
        return registeredPassports.size();
    }
}
