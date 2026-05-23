package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.scene.control.TableView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.application.services.ChogoriServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DRAFTS;

@Slf4j
public class RegistrationBookManipulator {

    private final RegistrationBookController controller;
    private final TableView<RegisteredPassportItem> tableView;
    private final RegisteredPassportsManager manager;

    // Паттерн для извлечения ID паспортов из строки вида "pik! PP#123 PP#456 ..."
    private static final Pattern PASSPORT_ID_PATTERN = Pattern.compile("PP#(\\d+)");

    public RegistrationBookManipulator(RegistrationBookController controller) {
        this.controller = controller;
        this.tableView = controller.getLvRegisteredPassports();
        this.manager = controller.getRegisteredPassportsManager();

        setupDragAndDrop();
    }

    /**
     * Настройка обработчика Drag-and-Drop для TableView.
     * Поддерживает перемещение паспортов из Passport_TableView.
     */
    private void setupDragAndDrop() {
        // Разрешаем вставку только при перетаскивании с данными в нужном формате
        tableView.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString() && dragboard.getString().startsWith("pik!")) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        // Обработчик непосредственно вставки
        tableView.setOnDragDropped(this::handleDragDropped);
    }

    /**
     * Обработчик события вставки перетаскиваемых элементов.
     *
     * @param event событие перетаскивания
     */
    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;

        if (dragboard.hasString()) {
            String data = dragboard.getString();
            log.debug("Получены данные при перетаскивании: {}", data);

            if (data.startsWith("pik!")) {
                List<Long> passportIds = extractPassportIds(data);

                if (!passportIds.isEmpty()) {
                    loadPassportsByIdsAsync(passportIds);
                    success = true;
                } else {
                    log.warn("Не найдено ID паспортов в строке: {}", data);
                }
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * Извлекает ID паспортов из строки формата "pik! PP#123 PP#456 ..."
     *
     * @param data строка с данными перетаскивания
     * @return список ID паспортов
     */
    private List<Long> extractPassportIds(String data) {
        List<Long> ids = new ArrayList<>();
        Matcher matcher = PASSPORT_ID_PATTERN.matcher(data);

        while (matcher.find()) {
            try {
                long id = Long.parseLong(matcher.group(1));
                ids.add(id);
            } catch (NumberFormatException e) {
                log.warn("Не удалось распарсить ID: {}", matcher.group(1));
            }
        }

        return ids;
    }

    /**
     * Асинхронно загружает паспорта из базы данных по списку ID и добавляет их в таблицу.
     *
     * @param ids список ID паспортов
     */
    private void loadPassportsByIdsAsync(List<Long> ids) {
        // Показываем индикацию загрузки
        controller.showLoadingCursorAndDisableControls();

        CompletableFuture.supplyAsync(() -> {
            List<Passport> passports = new ArrayList<>();
            for (Long id : ids) {
                try {
                    Passport passport = ChogoriServices.CH_QUICK_PASSPORTS.findById(id);
                    if (passport != null) {
                        passports.add(passport);
                    } else {
                        log.warn("Паспорт с ID {} не найден в базе данных", id);
                    }
                } catch (Exception e) {
                    log.error("Ошибка при загрузке паспорта с ID {}: {}", id, e.getMessage());
                }
            }
            return passports;
        }).thenAccept(passports -> {
            // Проверяем наличие чертежей для каждого паспорта
            CompletableFuture.supplyAsync(() -> {
                List<RegisteredPassportItem> items = new ArrayList<>();
                for (Passport passport : passports) {
                    boolean hasDrafts = checkDraftsExist(passport);
                    items.add(new RegisteredPassportItem(passport, hasDrafts));
                }
                return items;
            }).thenAccept(items -> {
                javafx.application.Platform.runLater(() -> {
                    addPassportItemsToTable(items);
                    controller.hideLoadingCursorAndEnableControls();
                });
            });
        }).exceptionally(ex -> {
            log.error("Ошибка при загрузке паспортов", ex);
            javafx.application.Platform.runLater(() -> {
                controller.hideLoadingCursorAndEnableControls();
                ru.wert.tubus.winform.warnings.Warning1.create(
                        "ОШИБКА!",
                        "Не удалось загрузить паспорта",
                        ex.getMessage() != null ? ex.getMessage() : "Неизвестная ошибка"
                );
            });
            return null;
        });
    }

    /**
     * Проверяет наличие чертежей для паспорта.
     *
     * @param passport паспорт для проверки
     * @return true если есть хотя бы один чертеж
     */
    private boolean checkDraftsExist(Passport passport) {
        if (passport == null || passport.getId() == null) {
            return false;
        }
        try {
            return !CH_DRAFTS.findByPassport(passport).isEmpty();
        } catch (Exception e) {
            log.error("Ошибка при проверке наличия чертежей для паспорта {}",
                    passport.getNumber(), e);
            return false;
        }
    }

    /**
     * Добавляет список элементов RegisteredPassportItem в таблицу.
     * Игнорирует дубликаты (паспорта, уже присутствующие в таблице).
     *
     * @param itemsToAdd список элементов для добавления
     */
    private void addPassportItemsToTable(List<RegisteredPassportItem> itemsToAdd) {
        if (itemsToAdd == null || itemsToAdd.isEmpty()) {
            log.debug("Нет элементов для добавления");
            return;
        }

        List<RegisteredPassportItem> currentItems = new ArrayList<>(tableView.getItems());

        int addedCount = 0;
        for (RegisteredPassportItem newItem : itemsToAdd) {
            Passport newPassport = newItem.getPassport();

            // Проверяем, нет ли уже такого паспорта в таблице
            boolean exists = currentItems.stream()
                    .anyMatch(item -> {
                        Passport p = item.getPassport();
                        return p != null && newPassport != null &&
                                p.getId().equals(newPassport.getId());
                    });

            if (!exists) {
                currentItems.add(newItem);
                addedCount++;
                log.debug("Паспорт {} добавлен в журнал регистрации",
                        newPassport != null ? newPassport.toUsefulString() : "null");
            } else {
                log.debug("Паспорт {} уже есть в списке, пропущен",
                        newPassport != null ? newPassport.toUsefulString() : "null");
            }
        }

        if (addedCount > 0) {
            tableView.getItems().setAll(currentItems);

            // Сохраняем состояние после добавления
            if (manager != null) {
                manager.saveState();
            }

            log.debug("В журнал регистрации добавлено {} новых паспортов", addedCount);
        }
    }

    /**
     * Синхронная версия добавления паспортов (для обратной совместимости).
     *
     * @param passportsToAdd список паспортов для добавления
     */
    @Deprecated
    private void addPassportsToList(List<Passport> passportsToAdd) {
        if (passportsToAdd == null || passportsToAdd.isEmpty()) {
            return;
        }

        List<RegisteredPassportItem> items = new ArrayList<>();
        for (Passport passport : passportsToAdd) {
            boolean hasDrafts = checkDraftsExist(passport);
            items.add(new RegisteredPassportItem(passport, hasDrafts));
        }

        addPassportItemsToTable(items);
    }
}
