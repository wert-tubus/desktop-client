package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.client.entity.serviceREST.PrefixService;
import ru.wert.tubus.winform.warnings.Warning1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_PASSPORTS;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

/**
 * Менеджер для загрузки и сохранения списка паспортов в файл.
 * Обеспечивает асинхронную работу с файловой системой и базой данных.
 */
@Slf4j
public class PassportListFileManager {

    private final PassportService passportService;
    private final PrefixService prefixService;
    private final Consumer<Passport> addPassportConsumer;
    private final Runnable clearListRunnable;
    private final Runnable refreshTablesRunnable;
    private final Runnable showLoadingRunnable;
    private final Runnable hideLoadingRunnable;

    // Кэш префиксов для быстрого доступа
    private Map<String, Prefix> prefixCache;

    /**
     * Конструктор менеджера.
     *
     * @param passportService      сервис для работы с паспортами
     * @param addPassportConsumer  функция добавления паспорта в список
     * @param clearListRunnable    функция очистки списка
     * @param refreshTablesRunnable функция обновления таблиц
     * @param showLoadingRunnable  функция показа индикации загрузки
     * @param hideLoadingRunnable  функция скрытия индикации загрузки
     */
    public PassportListFileManager(PassportService passportService,
                                   Consumer<Passport> addPassportConsumer,
                                   Runnable clearListRunnable,
                                   Runnable refreshTablesRunnable,
                                   Runnable showLoadingRunnable,
                                   Runnable hideLoadingRunnable) {
        this.passportService = passportService;
        this.prefixService = PrefixService.getInstance();
        this.addPassportConsumer = addPassportConsumer;
        this.clearListRunnable = clearListRunnable;
        this.refreshTablesRunnable = refreshTablesRunnable;
        this.showLoadingRunnable = showLoadingRunnable;
        this.hideLoadingRunnable = hideLoadingRunnable;
        initPrefixCache();
    }

    /**
     * Инициализация кэша префиксов.
     */
    private void initPrefixCache() {
        prefixCache = new HashMap<>();
        try {
            List<Prefix> prefixes = prefixService.findAll();
            for (Prefix prefix : prefixes) {
                prefixCache.put(prefix.getName(), prefix);
            }
            log.info("Загружено {} префиксов в кэш", prefixCache.size());
        } catch (Exception e) {
            log.error("Ошибка при загрузке префиксов", e);
        }
    }

    /**
     * Экспорт списка паспортов в файл.
     *
     * @param passports список паспортов для экспорта
     * @param initialFileName начальное имя файла
     * @return true если экспорт успешен
     */
    public boolean exportToFile(List<Passport> passports, String initialFileName) {
        if (passports == null || passports.isEmpty()) {
            Warning1.create($ATTENTION, "Список пуст", "Нечего экспортировать");
            return false;
        }

        File exportFile = chooseFileToSave(initialFileName);
        if (exportFile == null) {
            log.info("Пользователь отменил экспорт");
            return false;
        }

        try {
            String content = formatPassportsForExport(passports);
            Files.write(exportFile.toPath(), content.getBytes());
            log.info("Экспортировано {} паспортов в файл {}", passports.size(), exportFile.getAbsolutePath());
            Warning1.create("ОТЛИЧНО!", "Экспорт выполнен", "Список успешно сохранен в файл!");
            return true;
        } catch (IOException e) {
            log.error("Не удалось экспортировать паспорта", e);
            Warning1.create("ОШИБКА!", "Не удалось сохранить файл", e.getMessage());
            return false;
        }
    }

    /**
     * Загрузка списка паспортов из файла.
     * Асинхронно загружает данные из БД и обновляет список.
     */
    public void loadFromFile() {
        File selectedFile = chooseFileToLoad();
        if (selectedFile == null) {
            return;
        }

        List<PassportInfo> passportInfos = loadPassportInfosFromFile(selectedFile);
        if (passportInfos.isEmpty()) {
            Warning1.create($ATTENTION, "Файл пуст или имеет неверный формат",
                    "Не удалось загрузить номера из файла");
            return;
        }

        // Показываем диалог выбора действия
        LoadAction action = showLoadActionDialog(passportInfos.size());
        if (action == null) {
            return;
        }

        // Показываем индикацию загрузки
        if (showLoadingRunnable != null) {
            showLoadingRunnable.run();
        }

        CompletableFuture.supplyAsync(() -> loadPassportsFromDatabase(passportInfos))
                .thenAccept(passports -> {
                    Platform.runLater(() -> {
                        if (hideLoadingRunnable != null) {
                            hideLoadingRunnable.run();
                        }

                        if (passports == null) {
                            Warning1.create("ОШИБКА!", "Не удалось загрузить паспорта",
                                    "Проверьте подключение к базе данных");
                            return;
                        }

                        applyLoadedPassports(passports, action);

                        if (refreshTablesRunnable != null) {
                            refreshTablesRunnable.run();
                        }

                        String actionText = action == LoadAction.REPLACE ? "заменой" : "добавлением";
                        Warning1.create("УСПЕШНО!", "Загрузка завершена",
                                String.format("Загружено паспортов: %d (с %s)", passports.size(), actionText));
                    });
                })
                .exceptionally(ex -> {
                    log.error("Ошибка при асинхронной загрузке паспортов", ex);
                    Platform.runLater(() -> {
                        if (hideLoadingRunnable != null) {
                            hideLoadingRunnable.run();
                        }
                        Warning1.create("ОШИБКА!", "Не удалось загрузить паспорта",
                                ex.getMessage() != null ? ex.getMessage() : "Неизвестная ошибка");
                    });
                    return null;
                });
    }

    /**
     * Сохраняет текущее состояние выбранных паспортов в JSON файл (автосохранение).
     *
     * @param passports список паспортов для автосохранения
     * @param storageFile файл для сохранения
     */
    public static void autoSave(List<Passport> passports, File storageFile) {
        if (storageFile == null) return;

        try {
            List<String> passportNumbers = passports.stream()
                    .map(Passport::getNumber)
                    .filter(number -> number != null && !number.isEmpty())
                    .collect(Collectors.toList());

            com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(storageFile, passportNumbers);
            log.info("Автосохранение: сохранено {} номеров", passportNumbers.size());
        } catch (IOException e) {
            log.error("Не удалось выполнить автосохранение", e);
        }
    }

    /**
     * Загружает номера из JSON файла автосохранения.
     *
     * @param storageFile файл с автосохранением
     * @return список номеров паспортов
     */
    public static List<String> autoLoad(File storageFile) {
        if (storageFile == null || !storageFile.exists()) {
            return new ArrayList<>();
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(storageFile,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (IOException e) {
            log.error("Не удалось загрузить автосохранение", e);
            return new ArrayList<>();
        }
    }

    // ======================== ПРИВАТНЫЕ МЕТОДЫ ========================

    /**
     * Открывает диалог выбора файла для сохранения.
     */
    private File chooseFileToSave(String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить список номеров");
        fileChooser.setInitialFileName(initialFileName);

        FileChooser.ExtensionFilter txtFilter =
                new FileChooser.ExtensionFilter("Текстовые файлы (*.txt)", "*.txt");
        FileChooser.ExtensionFilter allFilesFilter =
                new FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*");
        fileChooser.getExtensionFilters().addAll(txtFilter, allFilesFilter);
        fileChooser.setSelectedExtensionFilter(txtFilter);

        File initialDir = new File(System.getProperty("user.home"));
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        File file = fileChooser.showSaveDialog(null);
        if (file != null && !file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getAbsolutePath() + ".txt");
        }
        return file;
    }

    /**
     * Открывает диалог выбора файла для загрузки.
     */
    private File chooseFileToLoad() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Загрузить список номеров");

        FileChooser.ExtensionFilter txtFilter =
                new FileChooser.ExtensionFilter("Текстовые файлы (*.txt)", "*.txt");
        FileChooser.ExtensionFilter allFilesFilter =
                new FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*");
        fileChooser.getExtensionFilters().addAll(txtFilter, allFilesFilter);

        File initialDir = new File(System.getProperty("user.home"));
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        return fileChooser.showOpenDialog(null);
    }

    /**
     * Форматирует список паспортов для экспорта в текстовый файл.
     * Сохраняет в формате "номер" для простоты последующей загрузки.
     */
    private String formatPassportsForExport(List<Passport> passports) {
        StringBuilder content = new StringBuilder();
        content.append("Список зарегистрированных номеров\n");
        content.append("=================================\n");
        content.append("Дата экспорта: ").append(new java.util.Date()).append("\n\n");

        for (int i = 0; i < passports.size(); i++) {
            Passport p = passports.get(i);
            content.append(String.format("%d. %s\n", i + 1, p.getNumber()));
        }

        content.append("\n").append("Всего номеров: ").append(passports.size());
        return content.toString();
    }

    /**
     * Загружает информацию о паспортах из текстового файла.
     *
     * @param file файл для загрузки
     * @return список объектов PassportInfo (префикс и номер)
     */
    private List<PassportInfo> loadPassportInfosFromFile(File file) {
        List<PassportInfo> passportInfos = new ArrayList<>();

        try {
            if (!file.exists()) {
                log.error("Файл не существует: {}", file.getAbsolutePath());
                return passportInfos;
            }

            List<String> lines = Files.readAllLines(file.toPath());

            for (String line : lines) {
                line = line.trim();

                // Пропускаем пустые строки и заголовки
                if (line.isEmpty() || line.startsWith("Список зарегистрированных номеров") ||
                        line.startsWith("===") || line.startsWith("Дата экспорта:") ||
                        line.startsWith("Всего номеров:")) {
                    continue;
                }

                String content = line;

                // Удаляем нумерацию в начале строки "1. "
                if (line.matches("^\\d+\\..*")) {
                    int dotIndex = line.indexOf('.');
                    if (dotIndex > 0 && dotIndex + 1 < line.length()) {
                        content = line.substring(dotIndex + 1).trim();
                    }
                }

                if (content.isEmpty()) {
                    continue;
                }

                // Извлекаем префикс и номер
                PassportInfo passportInfo = extractPassportInfo(content);
                if (passportInfo != null) {
                    passportInfos.add(passportInfo);
                    log.debug("Извлечен паспорт: prefix='{}', number='{}' из строки: '{}'",
                            passportInfo.getPrefixName(), passportInfo.getNumber(), line);
                }
            }

            log.info("Загружено {} паспортов из файла {}", passportInfos.size(), file.getAbsolutePath());

        } catch (IOException e) {
            log.error("Ошибка при загрузке номеров из файла", e);
        }

        return passportInfos;
    }

    /**
     * Извлекает информацию о паспорте из строки.
     *
     * Примеры:
     * "700000.001" -> prefix=null, number="700000.001"
     * "ПИК.700000.001 Деталь 01" -> prefix="ПИК", number="700000.001"
     * "Э12345" -> prefix="Э", number="12345"
     *
     * @param rawString исходная строка
     * @return объект PassportInfo или null
     */
    private PassportInfo extractPassportInfo(String rawString) {
        if (rawString == null || rawString.isEmpty()) {
            return null;
        }

        // Эскизный номер (начинается с буквы Э)
        if (rawString.startsWith("Э")) {
            String numbers = rawString.substring(1).replaceAll("[^0-9]", "");
            if (!numbers.isEmpty()) {
                return new PassportInfo("Э", numbers);
            }
            return null;
        }

        // ПИК номер
        String prefixName = null;
        String number = null;

        // Удаляем префикс "ПИК." если есть
        if (rawString.startsWith("ПИК.")) {
            prefixName = "ПИК";
            String afterPrefix = rawString.substring(4);
            // Извлекаем номер (цифры.цифры)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(afterPrefix);
            if (matcher.find()) {
                number = matcher.group(1);
            }
        } else {
            // Пробуем извлечь номер напрямую (формат "700000.001")
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(rawString);
            if (matcher.find()) {
                number = matcher.group(1);
            }
        }

        if (number != null && !number.isEmpty()) {
            return new PassportInfo(prefixName, number);
        }

        log.warn("Не удалось извлечь информацию о паспорте из строки: {}", rawString);
        return null;
    }

    /**
     * Загружает полные объекты Passport из базы данных по информации о префиксе и номере.
     */
    private List<Passport> loadPassportsFromDatabase(List<PassportInfo> passportInfos) {
        List<Passport> passports = new ArrayList<>();

        for (PassportInfo info : passportInfos) {
            try {
                Passport passport = null;

                // Если указан префикс, используем findByPrefixIdAndNumber
                if (info.getPrefixName() != null && !info.getPrefixName().isEmpty()) {
                    Prefix prefix = prefixCache.get(info.getPrefixName());
                    if (prefix != null) {
                        passport = CH_PASSPORTS.findByPrefixIdAndNumber(prefix, info.getNumber());
                    } else {
                        log.warn("Префикс {} не найден в кэше", info.getPrefixName());
                    }
                }

                // Если префикс не указан или не найден, пробуем найти по номеру
                if (passport == null) {
                    // Ищем по номеру через findAllByName или другой метод
                    // Это зависит от реализации API
                    passport = findPassportByNumber(info.getNumber());
                }

                if (passport != null) {
                    passports.add(passport);
                    log.debug("Загружен паспорт: {}", passport.getNumber());
                } else {
                    log.warn("Паспорт с номером {} не найден в базе данных", info.getNumber());
                }
            } catch (Exception e) {
                log.error("Ошибка при загрузке паспорта {}", info.getNumber(), e);
            }
        }

        return passports;
    }

    /**
     * Поиск паспорта по номеру (без указания префикса).
     * Пробует найти среди всех префиксов.
     */
    private Passport findPassportByNumber(String number) {
        for (Prefix prefix : prefixCache.values()) {
            try {
                Passport passport = CH_PASSPORTS.findByPrefixIdAndNumber(prefix, number);
                if (passport != null) {
                    return passport;
                }
            } catch (Exception e) {
                log.debug("Не найден паспорт с префиксом {} и номером {}", prefix.getName(), number);
            }
        }
        return null;
    }

    /**
     * Применяет загруженные паспорта к текущему списку.
     */
    private void applyLoadedPassports(List<Passport> passports, LoadAction action) {
        if (action == LoadAction.REPLACE) {
            clearListRunnable.run();
        }

        int added = 0;
        for (Passport passport : passports) {
            addPassportConsumer.accept(passport);
            added++;
        }

        log.info("Загружено {} паспортов (действие: {})", added, action);
    }

    /**
     * Показывает диалог выбора действия при загрузке.
     *
     * @return выбранное действие или null, если пользователь отменил
     */
    private LoadAction showLoadActionDialog(int count) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение загрузки");
        confirmAlert.setHeaderText("Загрузка списка паспортов");
        confirmAlert.setContentText("Найдено " + count + " номеров.\n\nВыберите действие:");

        ButtonType replaceButton = new ButtonType("Заменить текущий список");
        ButtonType appendButton = new ButtonType("Добавить к текущему списку");
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

        confirmAlert.getButtonTypes().setAll(replaceButton, appendButton, cancelButton);

        ButtonType result = confirmAlert.showAndWait().orElse(cancelButton);

        if (result == replaceButton) {
            return LoadAction.REPLACE;
        } else if (result == appendButton) {
            return LoadAction.APPEND;
        } else {
            return null;
        }
    }

    /**
     * Внутренний класс для хранения информации о паспорте.
     */
    private static class PassportInfo {
        private final String prefixName;
        private final String number;

        public PassportInfo(String prefixName, String number) {
            this.prefixName = prefixName;
            this.number = number;
        }

        public String getPrefixName() {
            return prefixName;
        }

        public String getNumber() {
            return number;
        }

        @Override
        public String toString() {
            return (prefixName != null ? prefixName + "." : "") + number;
        }
    }

    /**
     * Типы действий при загрузке.
     */
    private enum LoadAction {
        REPLACE,
        APPEND
    }
}
