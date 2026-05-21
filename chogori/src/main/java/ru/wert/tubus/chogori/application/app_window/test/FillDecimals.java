package ru.wert.tubus.chogori.application.app_window.test;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.setteings.ChogoriSettings;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.client.entity.serviceREST.DecimalService;
import ru.wert.tubus.client.entity.serviceREST.PassportService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс FillDecimals извлекает уникальные децимальные группы из паспортов
 * и сохраняет их как десятичные классификаторы (Decimal) в базу данных.
 *
 * Децимальная группа - это первые 6 цифр из номера паспорта.
 * Например: для номера "745222.222" группа будет "745222"
 *
 * Для каждой группы вычисляется максимальный номер (число после точки),
 * который сохраняется в поля initialNumber и lastNumber как максимальное значение + 1.
 *
 * Условия отбора паспортов:
 * 1. Префикс паспорта должен соответствовать префиксу предприятия из ChogoriSettings
 * 2. Номер паспорта должен содержать не менее 6 цифр и точку
 */
@Slf4j
public class FillDecimals {

    // Регулярное выражение для извлечения первых 6 цифр из номера (децимальная группа)
    private static final Pattern DECIMAL_GROUP_PATTERN = Pattern.compile("^(\\d{6})");

    // Регулярное выражение для извлечения номера после точки (порядковый номер)
    private static final Pattern SEQUENCE_NUMBER_PATTERN = Pattern.compile("^\\d{6}\\.(\\d+)");

    // Регулярное выражение для проверки, что номер начинается с 6 цифр и содержит точку
    private static final Pattern VALID_NUMBER_PATTERN = Pattern.compile("^\\d{6}\\.\\d+");

    /**
     * Главный метод запуска процесса заполнения децимальных групп
     */
    public static void fill(ActionEvent event) {
        // Получаем сервисы для работы с базой данных
        PassportService passportService = PassportService.getInstance();
        DecimalService decimalService = DecimalService.getInstance();

        // Получаем все паспорта из базы
        List<Passport> allPassports = passportService.findAll();
        if (allPassports == null || allPassports.isEmpty()) {
            showAlert("Нет данных", "В базе данных нет паспортов для обработки");
            return;
        }

        log.info("Найдено паспортов: {}", allPassports.size());
        log.info("Префикс предприятия: {}", getEnterprisePrefixName());

        // Извлекаем децимальные группы с их максимальными номерами
        Map<String, Integer> groupMaxNumbers = extractDecimalGroupsWithMaxNumbers(allPassports);

        if (groupMaxNumbers.isEmpty()) {
            showAlert("Нет данных", "Не найдено ни одной децимальной группы в паспортах.\n" +
                    "Проверьте, что паспорта имеют префикс '" + getEnterprisePrefixName() +
                    "' и номер в формате 'XXXXXX.XXX'");
            return;
        }

        log.info("Найдено уникальных децимальных групп: {}", groupMaxNumbers.size());

        // Выводим статистику по группам
        for (Map.Entry<String, Integer> entry : groupMaxNumbers.entrySet()) {
            log.debug("Группа: {}, максимальный номер: {}", entry.getKey(), entry.getValue());
        }

        // Получаем уже существующие в базе децимальные группы
        Map<String, Decimal> existingDecimalsMap = getExistingDecimalsMap(decimalService);

        // Определяем, какие группы нужно добавить, а какие обновить
        Set<String> groupsToAdd = new LinkedHashSet<>();
        Map<String, Integer> groupsToUpdate = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : groupMaxNumbers.entrySet()) {
            String groupName = entry.getKey();
            Integer maxNumber = entry.getValue();

            if (existingDecimalsMap.containsKey(groupName)) {
                // Группа существует - проверяем, нужно ли обновить максимальный номер
                Decimal existingDecimal = existingDecimalsMap.get(groupName);
                Integer currentMax = existingDecimal.getLastNumber();

                // Если текущий lastNumber меньше нового максимального номера + 1
                int newLastNumber = maxNumber + 1;
                if (currentMax == null || currentMax < newLastNumber) {
                    groupsToUpdate.put(groupName, maxNumber);
                    log.debug("Группа {} требует обновления: currentMax={}, newMax={}",
                            groupName, currentMax, maxNumber);
                } else {
                    log.debug("Группа {} уже актуальна: currentMax={} >= newMax={}",
                            groupName, currentMax, maxNumber);
                }
            } else {
                // Новая группа
                groupsToAdd.add(groupName);
            }
        }

        if (groupsToAdd.isEmpty() && groupsToUpdate.isEmpty()) {
            showAlert("Информация", "Все найденные децимальные группы уже существуют в базе данных\n" +
                    "и имеют актуальные максимальные номера.\n\n" +
                    "Найдено групп: " + groupMaxNumbers.size());
            return;
        }

        log.info("Новых групп для добавления: {}", groupsToAdd.size());
        log.info("Групп для обновления: {}", groupsToUpdate.size());

        // Создаем и показываем диалоговое окно с прогрессом
        showProgressDialog(groupsToAdd, groupsToUpdate, groupMaxNumbers, decimalService);
    }

    /**
     * Возвращает имя префикса предприятия из настроек
     *
     * @return имя префикса или "ПИК" по умолчанию
     */
    private static String getEnterprisePrefixName() {
        Prefix enterprisePrefix = ChogoriSettings.CH_DEFAULT_PREFIX;
        if (enterprisePrefix != null && enterprisePrefix.getName() != null) {
            return enterprisePrefix.getName();
        }
        return "ПИК"; // Значение по умолчанию
    }

    /**
     * Проверяет, соответствует ли префикс паспорта префиксу предприятия
     *
     * @param passport паспорт для проверки
     * @return true если префикс соответствует
     */
    private static boolean hasEnterprisePrefix(Passport passport) {
        Prefix enterprisePrefix = ChogoriSettings.CH_DEFAULT_PREFIX;

        // Если префикс предприятия не задан, пропускаем все паспорта
        if (enterprisePrefix == null || enterprisePrefix.getName() == null) {
            log.warn("Префикс предприятия не задан в настройках ChogoriSettings.CH_DEFAULT_PREFIX");
            return false;
        }

        Prefix passportPrefix = passport.getPrefix();
        if (passportPrefix == null || passportPrefix.getName() == null) {
            log.debug("Паспорт {} не имеет префикса", passport);
            return false;
        }

        boolean matches = enterprisePrefix.getName().equals(passportPrefix.getName());
        if (!matches) {
            log.debug("Префикс паспорта '{}' не совпадает с префиксом предприятия '{}'",
                    passportPrefix.getName(), enterprisePrefix.getName());
        }

        return matches;
    }

    /**
     * Извлекает порядковый номер из полного номера паспорта
     *
     * @param number номер паспорта (например "745222.224")
     * @return порядковый номер (224) или null, если формат некорректен
     */
    private static Integer extractSequenceNumber(String number) {
        if (number == null || number.isEmpty()) {
            return null;
        }

        Matcher matcher = SEQUENCE_NUMBER_PATTERN.matcher(number);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.error("Ошибка преобразования номера: {}", matcher.group(1), e);
                return null;
            }
        }

        return null;
    }

    /**
     * Извлекает децимальную группу из номера паспорта
     * Группа - это первые 6 цифр номера
     *
     * @param number номер паспорта (например "745222.224")
     * @return децимальная группа (6 цифр) или null, если номер некорректен
     */
    private static String extractDecimalGroupFromNumber(String number) {
        if (number == null || number.isEmpty()) {
            return null;
        }

        // Проверяем, что номер начинается с 6 цифр и содержит точку
        if (!VALID_NUMBER_PATTERN.matcher(number).find()) {
            log.debug("Номер '{}' не соответствует формату 'XXXXXX.XXX'", number);
            return null;
        }

        // Извлекаем первые 6 цифр
        Matcher matcher = DECIMAL_GROUP_PATTERN.matcher(number);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Извлекает уникальные децимальные группы с их максимальными номерами из списка паспортов
     *
     * @param passports список паспортов
     * @return Map (децимальная группа -> максимальный порядковый номер)
     */
    private static Map<String, Integer> extractDecimalGroupsWithMaxNumbers(List<Passport> passports) {
        Map<String, Integer> groupMaxNumbers = new LinkedHashMap<>();
        String enterprisePrefixName = getEnterprisePrefixName();
        int skippedNoPrefix = 0;
        int skippedWrongPrefix = 0;
        int skippedInvalidNumber = 0;
        int skippedInvalidSequence = 0;

        for (Passport passport : passports) {
            // Проверяем префикс паспорта
            if (!hasEnterprisePrefix(passport)) {
                if (passport.getPrefix() == null || passport.getPrefix().getName() == null) {
                    skippedNoPrefix++;
                } else {
                    skippedWrongPrefix++;
                }
                continue;
            }

            // Получаем номер паспорта
            String number = passport.getNumber();
            if (number == null || number.isEmpty()) {
                log.debug("Паспорт {} имеет пустой номер", passport);
                continue;
            }

            // Извлекаем децимальную группу
            String group = extractDecimalGroupFromNumber(number);
            if (group == null || group.isEmpty()) {
                skippedInvalidNumber++;
                log.debug("Не удалось извлечь группу из номера: '{}'", number);
                continue;
            }

            // Извлекаем порядковый номер
            Integer sequenceNumber = extractSequenceNumber(number);
            if (sequenceNumber == null) {
                skippedInvalidSequence++;
                log.debug("Не удалось извлечь порядковый номер из: '{}'", number);
                continue;
            }

            // Обновляем максимальное значение для группы
            Integer currentMax = groupMaxNumbers.get(group);
            if (currentMax == null || sequenceNumber > currentMax) {
                groupMaxNumbers.put(group, sequenceNumber);
                log.debug("Паспорт {}.{} -> группа: {}, номер: {}",
                        enterprisePrefixName, number, group, sequenceNumber);
            }
        }

        log.info("Статистика обработки паспортов:");
        log.info("  - Всего паспортов: {}", passports.size());
        log.info("  - Уникальных групп найдено: {}", groupMaxNumbers.size());
        log.info("  - Пропущено (нет префикса): {}", skippedNoPrefix);
        log.info("  - Пропущено (неверный префикс): {}", skippedWrongPrefix);
        log.info("  - Пропущено (неверный формат номера): {}", skippedInvalidNumber);
        log.info("  - Пропущено (неверный порядковый номер): {}", skippedInvalidSequence);
        log.info("  - Обработано успешно: {}", passports.size() - skippedNoPrefix - skippedWrongPrefix -
                skippedInvalidNumber - skippedInvalidSequence);

        return groupMaxNumbers;
    }

    /**
     * Получает все существующие децимальные группы из базы данных в виде Map
     *
     * @param decimalService сервис для работы с Decimal
     * @return Map (название группы -> объект Decimal)
     */
    private static Map<String, Decimal> getExistingDecimalsMap(DecimalService decimalService) {
        Map<String, Decimal> existingDecimals = new HashMap<>();

        try {
            List<Decimal> decimals = decimalService.findAll();
            if (decimals != null) {
                for (Decimal decimal : decimals) {
                    if (decimal.getName() != null && !decimal.getName().isEmpty()) {
                        existingDecimals.put(decimal.getName(), decimal);
                    }
                }
            }
            log.info("Существующих групп в базе: {}", existingDecimals.size());
        } catch (Exception e) {
            log.error("Ошибка при получении существующих Decimal групп", e);
        }

        return existingDecimals;
    }

    /**
     * Создает объект Decimal из названия группы и максимального номера
     *
     * @param groupName название группы (6 цифр)
     * @param maxNumber максимальный найденный порядковый номер
     * @return готовый объект Decimal с заполненными полями
     */
    private static Decimal createDecimalFromGroup(String groupName, Integer maxNumber) {
        Decimal decimal = new Decimal();
        decimal.setName(groupName);

        // Вычисляем следующее доступное число (максимальное + 1)
        int nextNumber = maxNumber + 1;
        decimal.setInitialNumber(nextNumber);
        decimal.setLastNumber(nextNumber);

        decimal.setDescription(String.format(
                "Автоматически создан из паспортов. Децимальная группа: %s. " +
                        "Максимальный найденный номер: %d. Следующий доступный: %d",
                groupName, maxNumber, nextNumber));

        return decimal;
    }

    /**
     * Обновляет существующий объект Decimal новыми значениями диапазона
     *
     * @param existingDecimal существующий объект Decimal
     * @param maxNumber новый максимальный порядковый номер
     * @return true если были изменения
     */
    private static boolean updateDecimalRange(Decimal existingDecimal, Integer maxNumber) {
        int newLastNumber = maxNumber + 1;

        // Проверяем, нужно ли обновлять
        if (existingDecimal.getLastNumber() != null && existingDecimal.getLastNumber() >= newLastNumber) {
            return false;
        }

        // Обновляем диапазон
        existingDecimal.setInitialNumber(newLastNumber);
        existingDecimal.setLastNumber(newLastNumber);

        // Обновляем описание
        String oldDesc = existingDecimal.getDescription();
        String newDesc = String.format(
                "Обновлен из паспортов. Децимальная группа: %s. " +
                        "Максимальный найденный номер: %d. Следующий доступный: %d. %s",
                existingDecimal.getName(), maxNumber, newLastNumber,
                (oldDesc != null ? "(" + oldDesc + ")" : ""));

        // Ограничиваем длину описания, если нужно
        if (newDesc.length() > 255) {
            newDesc = newDesc.substring(0, 252) + "...";
        }
        existingDecimal.setDescription(newDesc);

        return true;
    }

    /**
     * Показывает диалоговое окно с прогрессом добавления и обновления групп
     *
     * @param groupsToAdd набор новых групп для добавления
     * @param groupsToUpdate группы для обновления с их максимальными номерами
     * @param allGroups все группы с максимальными номерами (для статистики)
     * @param decimalService сервис для сохранения
     */
    private static void showProgressDialog(Set<String> groupsToAdd,
                                           Map<String, Integer> groupsToUpdate,
                                           Map<String, Integer> allGroups,
                                           DecimalService decimalService) {
        int totalOperations = groupsToAdd.size() + groupsToUpdate.size();

        // Создаем диалоговое окно
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Добавление/обновление децимальных групп");
        dialog.setHeaderText("Извлечение и сохранение децимальных групп из паспортов\n" +
                "Префикс предприятия: " + getEnterprisePrefixName() + "\n" +
                "Новых групп: " + groupsToAdd.size() + ", требует обновления: " + groupsToUpdate.size());
        dialog.setResizable(true);

        // Создаем контейнер для содержимого
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER);

        // Прогресс-бар
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(600);

        // Метка прогресса
        Label progressLabel = new Label("0 / " + totalOperations);
        progressLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // Метка текущей обрабатываемой группы
        Label currentGroupLabel = new Label("Готов к началу...");
        currentGroupLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
        currentGroupLabel.setWrapText(true);

        // Область для лога успешных операций
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        logArea.setStyle("-fx-font-size: 11px; -fx-font-family: monospace; -fx-text-fill: #008000;");
        logArea.setPromptText("Лог операций...");

        TitledPane logPane = new TitledPane("Лог операций", logArea);
        logPane.setExpanded(true);
        logPane.setAnimated(true);

        // Область для ошибок
        TextArea errorArea = new TextArea();
        errorArea.setEditable(false);
        errorArea.setPrefHeight(100);
        errorArea.setStyle("-fx-font-size: 11px; -fx-font-family: monospace; -fx-text-fill: #cc0000;");
        errorArea.setPromptText("Ошибки будут отображаться здесь...");

        TitledPane errorPane = new TitledPane("Лог ошибок", errorArea);
        errorPane.setExpanded(false);
        errorPane.setAnimated(true);

        // Область для статистики
        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefHeight(100);
        statsArea.setStyle("-fx-font-size: 11px; -fx-font-family: monospace; -fx-text-fill: #0000cc;");

        // Выводим начальную статистику по группам
        StringBuilder statsBuilder = new StringBuilder();
        statsBuilder.append("Найденные группы и их максимальные номера:\n");
        statsBuilder.append("----------------------------------------\n");
        for (Map.Entry<String, Integer> entry : allGroups.entrySet()) {
            statsBuilder.append(String.format("Группа %s: макс. номер %d (следующий: %d)\n",
                    entry.getKey(), entry.getValue(), entry.getValue() + 1));
        }
        statsArea.setText(statsBuilder.toString());

        TitledPane statsPane = new TitledPane("Статистика по группам", statsArea);
        statsPane.setExpanded(false);
        statsPane.setAnimated(true);

        content.getChildren().addAll(progressBar, progressLabel, currentGroupLabel, logPane, statsPane, errorPane);
        dialog.getDialogPane().setContent(content);

        // Добавляем кнопку отмены
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelButton);

        // Списки для логов и ошибок
        List<String> successLogs = new ArrayList<>();
        List<String> errorLogs = new ArrayList<>();

        // Создаем задачу для фонового выполнения
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int processed = 0;
                int addedCount = 0;
                int updatedCount = 0;
                int errorCount = 0;
                long startTime = System.currentTimeMillis();

                // Сначала добавляем новые группы
                for (String groupName : groupsToAdd) {
                    if (isCancelled()) {
                        updateMessage("Операция отменена");
                        break;
                    }

                    Integer maxNumber = allGroups.get(groupName);
                    updateMessage("Добавление: " + groupName);
                    Platform.runLater(() -> currentGroupLabel.setText("Добавление группы: " + groupName));

                    log.info("Добавление новой децимальной группы: {}, макс. номер: {}", groupName, maxNumber);

                    try {
                        Decimal decimal = createDecimalFromGroup(groupName, maxNumber);
                        Decimal savedDecimal = decimalService.save(decimal);

                        if (savedDecimal != null && savedDecimal.getId() != null) {
                            addedCount++;
                            String successMsg = String.format("✓ ДОБАВЛЕНА группа: %s, диапазон: %d - %d (макс. найден: %d)",
                                    groupName, savedDecimal.getInitialNumber(), savedDecimal.getLastNumber(), maxNumber);
                            successLogs.add(successMsg);
                            log.info(successMsg);

                            final List<String> currentLogs = new ArrayList<>(successLogs);
                            Platform.runLater(() -> {
                                logArea.setText(String.join("\n", currentLogs));
                                logArea.setScrollTop(Double.MAX_VALUE);
                            });
                        } else {
                            String errorMsg = String.format("✗ Не удалось сохранить группу: %s", groupName);
                            errorLogs.add(errorMsg);
                            errorCount++;
                            log.error(errorMsg);
                        }

                    } catch (Exception e) {
                        String errorMsg = String.format("✗ Ошибка при сохранении группы %s: %s", groupName, e.getMessage());
                        errorLogs.add(errorMsg);
                        errorCount++;
                        log.error(errorMsg, e);
                    }

                    processed++;
                    updateProgress((double) processed / totalOperations, 1.0);
                    updateProgressDisplay(processed, totalOperations, addedCount, updatedCount, errorCount, progressLabel);
                }

                // Затем обновляем существующие группы
                for (Map.Entry<String, Integer> entry : groupsToUpdate.entrySet()) {
                    if (isCancelled()) {
                        updateMessage("Операция отменена");
                        break;
                    }

                    String groupName = entry.getKey();
                    Integer maxNumber = entry.getValue();

                    updateMessage("Обновление: " + groupName);
                    Platform.runLater(() -> currentGroupLabel.setText("Обновление группы: " + groupName));

                    log.info("Обновление децимальной группы: {}, новый макс. номер: {}", groupName, maxNumber);

                    try {
                        // Получаем существующий Decimal из базы (нужно получить свежие данные)
                        Decimal existingDecimal = decimalService.findByName(groupName);

                        if (existingDecimal == null) {
                            String errorMsg = String.format("✗ Группа %s не найдена в базе при обновлении", groupName);
                            errorLogs.add(errorMsg);
                            errorCount++;
                            log.error(errorMsg);
                        } else if (updateDecimalRange(existingDecimal, maxNumber)) {
                            boolean success = decimalService.update(existingDecimal);
                            if (success) {
                                updatedCount++;
                                String successMsg = String.format("✓ ОБНОВЛЕНА группа: %s, новый диапазон: %d - %d (макс. найден: %d)",
                                        groupName, existingDecimal.getInitialNumber(), existingDecimal.getLastNumber(), maxNumber);
                                successLogs.add(successMsg);
                                log.info(successMsg);

                                final List<String> currentLogs = new ArrayList<>(successLogs);
                                Platform.runLater(() -> {
                                    logArea.setText(String.join("\n", currentLogs));
                                    logArea.setScrollTop(Double.MAX_VALUE);
                                });
                            } else {
                                String errorMsg = String.format("✗ Не удалось обновить группу: %s", groupName);
                                errorLogs.add(errorMsg);
                                errorCount++;
                                log.error(errorMsg);
                            }
                        } else {
                            // Обновление не требуется
                            log.debug("Группа {} не требует обновления", groupName);
                        }

                    } catch (Exception e) {
                        String errorMsg = String.format("✗ Ошибка при обновлении группы %s: %s", groupName, e.getMessage());
                        errorLogs.add(errorMsg);
                        errorCount++;
                        log.error(errorMsg, e);
                    }

                    processed++;
                    updateProgress((double) processed / totalOperations, 1.0);
                    updateProgressDisplay(processed, totalOperations, addedCount, updatedCount, errorCount, progressLabel);

                    // Обновляем область ошибок
                    final List<String> currentErrors = new ArrayList<>(errorLogs);
                    int finalErrorCount = errorCount;
                    Platform.runLater(() -> {
                        if (!currentErrors.isEmpty()) {
                            errorArea.setText(String.join("\n", currentErrors));
                            if (finalErrorCount > 0) {
                                errorPane.setExpanded(true);
                            }
                        }
                    });
                }

                // Финальное сообщение
                long totalTime = (System.currentTimeMillis() - startTime) / 1000;
                String finalMessage = String.format("Операция завершена. Добавлено: %d, Обновлено: %d, Ошибок: %d, Время: %d сек",
                        addedCount, updatedCount, errorCount, totalTime);
                updateMessage(finalMessage);
                log.info(finalMessage);

                return null;
            }
        };

        // Привязываем прогресс-бар к задаче
        progressBar.progressProperty().bind(task.progressProperty());

        // Подписываемся на изменения сообщения задачи
        task.messageProperty().addListener((obs, old, newMsg) -> {
            if (newMsg != null && !newMsg.equals(old)) {
                final String msg = newMsg;
                Platform.runLater(() -> {
                    if (!msg.startsWith("Операция завершена")) {
                        currentGroupLabel.setText(msg);
                    }
                });
            }
        });

        // Обработка успешного завершения
        task.setOnSucceeded(e -> {
            dialog.close();
            String resultMessage = task.getTitle() != null ? task.getTitle() : "Операция завершена";
            if (!errorLogs.isEmpty()) {
                showAlert("Завершено с ошибками", resultMessage + "\n\nДетали ошибок:\n" +
                        String.join("\n", errorLogs.subList(0, Math.min(15, errorLogs.size()))));
            } else {
                showAlert("Завершено успешно", resultMessage);
            }
        });

        // Обработка ошибки выполнения
        task.setOnFailed(e -> {
            dialog.close();
            Throwable exception = task.getException();
            String errorDetails = exception != null ? exception.getMessage() : "Неизвестная ошибка";
            log.error("Ошибка выполнения задачи", exception);
            showAlert("Ошибка", "Произошла ошибка при обработке:\n" + errorDetails);
        });

        // Обработка отмены
        task.setOnCancelled(e -> {
            dialog.close();
            showAlert("Отменено", "Операция была отменена пользователем");
        });

        // Запускаем задачу в отдельном потоке
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

        // Показываем диалог и ждем завершения
        dialog.showAndWait();
    }

    /**
     * Обновляет отображение прогресса в UI
     */
    private static void updateProgressDisplay(int processed, int total, int added, int updated, int errors, Label progressLabel) {
        Platform.runLater(() -> progressLabel.setText(
                String.format("Обработано: %d / %d | Добавлено: %d | Обновлено: %d | Ошибок: %d",
                        processed, total, added, updated, errors)));
    }

    /**
     * Показывает информационное диалоговое окно
     *
     * @param title заголовок окна
     * @param message сообщение для отображения
     */
    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(Math.min(400, textArea.getScrollTop() + 200));
        textArea.setStyle("-fx-font-size: 12px; -fx-font-family: monospace;");

        alert.getDialogPane().setContent(textArea);
        alert.setResizable(true);
        alert.showAndWait();
    }
}
