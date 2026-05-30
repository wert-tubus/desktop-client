package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.collections.ObservableList;
import ru.wert.tubus.chogori.application.cardsbox.RegisteredPassportsStorage;
import ru.wert.tubus.client.entity.models.Passport;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DRAFTS;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_PASSPORTS;

/**
 * Менеджер для управления списком недавно зарегистрированных номеров
 */
@Slf4j
public class RegisteredPassportsManager {

    private final ObservableList<RegisteredPassportItem> registeredItems;
    private final RegistrationService registrationService;

    public RegisteredPassportsManager(ObservableList<RegisteredPassportItem> registeredItems, RegistrationService registrationService) {
        this.registeredItems = registeredItems;
        this.registrationService = registrationService;
    }

    /**
     * Проверяет, есть ли чертежи для паспорта
     */
    private boolean checkDraftsExist(Passport passport) {
        if (passport == null || passport.getId() == null) {
            return false;
        }
        try {
            return !CH_DRAFTS.findByPassport(passport).isEmpty();
        } catch (Exception e) {
            log.error("Ошибка при проверке наличия чертежей для паспорта {}", passport.getNumber(), e);
            return false;
        }
    }

    /**
     * Добавление паспорта в список с сохранением состояния
     */
    /**
     * Добавление паспорта в список с сохранением состояния
     */
    public void addPassport(Passport passport) {
        if (passport == null || passport.getNumber() == null) {
            log.warn("Попытка добавить null паспорт или паспорт без номера");
            return;
        }

        // Проверяем, существует ли паспорт в БД (не удален ли он)
        if (!isPassportExists(passport)) {
            log.warn("Паспорт {} не существует в базе данных (возможно, удален), пропускаем добавление",
                    passport.getNumber());
            return;
        }

        // Проверяем наличие чертежей (асинхронно)
        CompletableFuture.supplyAsync(() -> checkDraftsExist(passport))
                .thenAccept(hasDrafts -> {
                    RegisteredPassportItem newItem = new RegisteredPassportItem(passport, hasDrafts);

                    javafx.application.Platform.runLater(() -> {
                        // Проверяем, нет ли уже такого паспорта в списке
                        boolean exists = registeredItems.stream()
                                .anyMatch(item -> {
                                    Passport p = item.getPassport();
                                    return p != null && p.getNumber() != null &&
                                            p.getNumber().equals(passport.getNumber());
                                });

                        if (!exists) {
                            registeredItems.add(newItem);
                            saveState();
                            log.info("Паспорт {} добавлен в список выбранных (чертежи: {})",
                                    passport.getNumber(), hasDrafts);
                        } else {
                            log.debug("Паспорт {} уже существует в списке", passport.getNumber());
                        }
                    });
                })
                .exceptionally(ex -> {
                    log.error("Ошибка при проверке чертежей для паспорта {}", passport.getNumber(), ex);
                    javafx.application.Platform.runLater(() -> {
                        // При ошибке добавляем с hasDrafts = false
                        RegisteredPassportItem newItem = new RegisteredPassportItem(passport, false);
                        boolean exists = registeredItems.stream()
                                .anyMatch(item -> {
                                    Passport p = item.getPassport();
                                    return p != null && p.getNumber() != null &&
                                            p.getNumber().equals(passport.getNumber());
                                });
                        if (!exists) {
                            registeredItems.add(newItem);
                            saveState();
                            log.info("Паспорт {} добавлен в список выбранных (чертежи: неизвестно)",
                                    passport.getNumber());
                        }
                    });
                    return null;
                });
    }

    /**
     * Проверяет, существует ли паспорт в базе данных
     */
    private boolean isPassportExists(Passport passport) {
        if (passport == null || passport.getId() == null) {
            return false;
        }

        try {
            // Прямой запрос к БД по ID - самый надежный способ
            Passport found = CH_PASSPORTS.findById(passport.getId());
            return found != null;
        } catch (Exception e) {
            log.error("Ошибка проверки существования паспорта {} (ID: {})",
                    passport.getNumber(), passport.getId());
            return false;
        }
    }

    /**
     * Очистка списка
     */
    public void clear() {
        if (!registeredItems.isEmpty()) {
            registeredItems.clear();
            saveState();
            log.info("Список выбранных паспортов очищен");
        }
    }

    /**
     * Обновление списка (удаление неактуальных паспортов)
     * Оптимизированная версия с использованием прямых запросов
     */
    public void refresh() {
        if (registeredItems.isEmpty()) {
            return;
        }

        List<RegisteredPassportItem> validItems = new ArrayList<>();
        List<RegisteredPassportItem> invalidItems = new ArrayList<>();

        for (RegisteredPassportItem item : registeredItems) {
            Passport passport = item.getPassport();
            if (passport == null || passport.getNumber() == null) {
                invalidItems.add(item);
                continue;
            }

            Passport freshPassport = registrationService.findPassportByNumberFast(passport.getNumber());
            if (freshPassport != null) {
                item.setPassport(freshPassport);
                item.setNote(freshPassport.getNote());  // Обновляем note
                CompletableFuture.runAsync(() -> {
                    boolean hasDrafts = checkDraftsExist(freshPassport);
                    javafx.application.Platform.runLater(() -> item.setHasDrafts(hasDrafts));
                });
                validItems.add(item);
            } else {
                invalidItems.add(item);
                log.warn("Паспорт {} не найден в БД, удаляем из списка выбранных", passport.getNumber());
            }
        }

        if (!invalidItems.isEmpty()) {
            registeredItems.removeAll(invalidItems);
            saveState();
            log.info("Удалено {} неактуальных паспортов из списка выбранных", invalidItems.size());
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

        for (int i = 0; i < registeredItems.size(); i++) {
            RegisteredPassportItem item = registeredItems.get(i);
            Passport p = item.getPassport();
            if (p != null && p.getNumber() != null && p.getNumber().equals(oldPassport.getNumber())) {
                // Обновляем паспорт
                item.setPassport(updatedPassport);
                item.setNote(updatedPassport.getNote());

                // Принудительно обновляем строку таблицы
                registeredItems.set(i, item);

                // Проверяем наличие чертежей для обновленного паспорта
                CompletableFuture.runAsync(() -> {
                    boolean hasDrafts = checkDraftsExist(updatedPassport);
                    javafx.application.Platform.runLater(() -> item.setHasDrafts(hasDrafts));
                });

                saveState();
                log.info("Паспорт {} обновлен на {}", oldPassport.getNumber(), updatedPassport.getNumber());
                return;
            }
        }

        log.debug("Паспорт {} не найден в списке для обновления", oldPassport.getNumber());
    }

    /**
     * Восстановление состояния из хранилища
     * Оптимизированная версия с массовой загрузкой и СОХРАНЕНИЕМ ПОРЯДКА из файла
     */
    public void restoreState() {
        List<String> savedNumbers = RegisteredPassportsStorage.loadRegisteredPassportNumbers();
        if (savedNumbers.isEmpty()) {
            log.info("Нет сохраненных выбранных паспортов для восстановления");
            return;
        }

        log.debug("Начинаем восстановление {} сохраненных номеров", savedNumbers.size());

        // Загружаем все паспорта в Map для быстрого доступа
        Map<String, Passport> passportMap = registrationService.loadPassportsByNumbersMap(savedNumbers);

        // ВАЖНО: Сохраняем порядок из savedNumbers, а не из Map
        List<RegisteredPassportItem> restored = new ArrayList<>();
        List<String> notFoundNumbers = new ArrayList<>();

        for (String number : savedNumbers) {  // Итерируемся в порядке из файла
            Passport passport = passportMap.get(number);
            if (passport != null) {
                boolean hasDrafts = checkDraftsExist(passport);
                RegisteredPassportItem item = new RegisteredPassportItem(passport, hasDrafts);
                item.setNote(passport.getNote());
                restored.add(item);
            } else {
                notFoundNumbers.add(number);
                log.warn("Паспорт с номером {} не найден в БД", number);
            }
        }

        if (!restored.isEmpty()) {
            registeredItems.setAll(restored);  // Порядок сохранен
            log.info("Восстановлено {} выбранных паспортов", restored.size());

            if (!notFoundNumbers.isEmpty()) {
                log.info("{} номеров не найдено, обновляем сохраненное состояние", notFoundNumbers.size());
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
        if (!registeredItems.isEmpty()) {
            List<Passport> passports = new ArrayList<>();
            for (RegisteredPassportItem item : registeredItems) {
                passports.add(item.getPassport());
            }
            RegisteredPassportsStorage.saveRegisteredPassports(passports);
            log.debug("Сохранено {} паспортов", passports.size());
        } else {
            RegisteredPassportsStorage.clearSavedState();
            log.debug("Список пуст, состояние очищено");
        }
    }

    /**
     * Получение списка элементов паспортов
     */
    public ObservableList<RegisteredPassportItem> getList() {
        return registeredItems;
    }

    /**
     * Проверка, пуст ли список
     */
    public boolean isEmpty() {
        return registeredItems.isEmpty();
    }

    /**
     * Размер списка
     */
    public int size() {
        return registeredItems.size();
    }

    /**
     * Проверка наличия паспорта в списке по номеру
     */
    public boolean contains(String number) {
        if (number == null) return false;
        return registeredItems.stream()
                .map(RegisteredPassportItem::getPassport)
                .anyMatch(p -> p != null && p.getNumber() != null && p.getNumber().equals(number));
    }

    /**
     * Удаление паспорта из списка по номеру
     */
    public boolean remove(String number) {
        if (number == null) return false;

        RegisteredPassportItem toRemove = registeredItems.stream()
                .filter(item -> {
                    Passport p = item.getPassport();
                    return p != null && p.getNumber() != null && p.getNumber().equals(number);
                })
                .findFirst()
                .orElse(null);

        if (toRemove != null) {
            boolean removed = registeredItems.remove(toRemove);
            if (removed) {
                saveState();
                log.info("Паспорт {} удален из списка", number);
            }
            return removed;
        }

        return false;
    }

    /**
     * Обновление статуса наличия чертежей для всех элементов
     */
    public void updateAllDraftsStatus() {
        for (RegisteredPassportItem item : registeredItems) {
            Passport passport = item.getPassport();
            if (passport != null) {
                CompletableFuture.runAsync(() -> {
                    boolean hasDrafts = checkDraftsExist(passport);
                    javafx.application.Platform.runLater(() -> item.setHasDrafts(hasDrafts));
                });
            }
        }
        log.debug("Обновлен статус чертежей для {} элементов", registeredItems.size());
    }

    /**
     * Обновление статуса наличия чертежей для конкретного паспорта
     */
    public void updateDraftsStatusForPassport(Passport passport) {
        if (passport == null || passport.getNumber() == null) return;

        for (RegisteredPassportItem item : registeredItems) {
            Passport p = item.getPassport();
            if (p != null && p.getNumber() != null && p.getNumber().equals(passport.getNumber())) {
                CompletableFuture.runAsync(() -> {
                    boolean hasDrafts = checkDraftsExist(passport);
                    javafx.application.Platform.runLater(() -> item.setHasDrafts(hasDrafts));
                });
                return;
            }
        }
    }
}
