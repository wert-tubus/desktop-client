package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.warnings.Warning1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

/**
 * Менеджер для загрузки и сохранения списка паспортов в файл.
 * Обеспечивает асинхронную работу с файловой системой и базой данных.
 */
@Slf4j
public class PassportListFileManager {

    private final PassportService passportService;
    private final Consumer<Passport> addPassportConsumer;
    private final Runnable clearListRunnable;
    private final Runnable refreshTablesRunnable;
    private final Runnable showLoadingRunnable;
    private final Runnable hideLoadingRunnable;

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
        this.addPassportConsumer = addPassportConsumer;
        this.clearListRunnable = clearListRunnable;
        this.refreshTablesRunnable = refreshTablesRunnable;
        this.showLoadingRunnable = showLoadingRunnable;
        this.hideLoadingRunnable = hideLoadingRunnable;
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

        List<String> passportNumbers = loadPassportNumbersFromFile(selectedFile);
        if (passportNumbers.isEmpty()) {
            Warning1.create($ATTENTION, "Файл пуст или имеет неверный формат",
                    "Не удалось загрузить номера из файла");
            return;
        }

        // Показываем диалог выбора действия
        LoadAction action = showLoadActionDialog(passportNumbers.size());
        if (action == null) {
            return; // Пользователь отменил
        }

        // Показываем индикацию загрузки
        if (showLoadingRunnable != null) {
            showLoadingRunnable.run();
        }

        CompletableFuture.supplyAsync(() -> loadPassportsFromDatabase(passportNumbers))
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
     */
    private String formatPassportsForExport(List<Passport> passports) {
        StringBuilder content = new StringBuilder();
        content.append("Список зарегистрированных номеров\n");
        content.append("=================================\n");
        content.append("Дата экспорта: ").append(new java.util.Date()).append("\n\n");

        for (int i = 0; i < passports.size(); i++) {
            Passport p = passports.get(i);
            content.append(String.format("%d. %s\n", i + 1, p.toUsefulString()));
        }

        content.append("\n").append("Всего номеров: ").append(passports.size());
        return content.toString();
    }

    /**
     * Загружает номера паспортов из текстового файла.
     */
    private List<String> loadPassportNumbersFromFile(File file) {
        List<String> passportNumbers = new ArrayList<>();

        try {
            if (!file.exists()) {
                log.error("Файл не существует: {}", file.getAbsolutePath());
                return passportNumbers;
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

                // Ищем строки с номерами в формате "1. 700-123456"
                if (line.matches("^\\d+\\..*")) {
                    int dotIndex = line.indexOf('.');
                    if (dotIndex > 0 && dotIndex + 1 < line.length()) {
                        String number = line.substring(dotIndex + 1).trim();
                        if (!number.isEmpty()) {
                            passportNumbers.add(number);
                        }
                    }
                }
                // Если файл просто содержит список номеров (по одному в строке)
                else if (!line.isEmpty() && isValidPassportNumberFormat(line)) {
                    passportNumbers.add(line);
                }
            }

            log.info("Загружено {} номеров паспортов из файла {}", passportNumbers.size(), file.getAbsolutePath());

        } catch (IOException e) {
            log.error("Ошибка при загрузке номеров из файла", e);
        }

        return passportNumbers;
    }

    /**
     * Проверка формата номера паспорта.
     */
    private boolean isValidPassportNumberFormat(String number) {
        if (number == null || number.isEmpty()) {
            return false;
        }
        // Эскизный номер (начинается с Э)
        if (number.startsWith("Э") && number.length() > 1) {
            return true;
        }
        // ПИК номер (начинается с цифры)
        return number.length() > 0 && Character.isDigit(number.charAt(0));
    }

    /**
     * Загружает полные объекты Passport из базы данных по номерам.
     */
    private List<Passport> loadPassportsFromDatabase(List<String> passportNumbers) {
        List<Passport> passports = new ArrayList<>();
        for (String number : passportNumbers) {
            try {
                Passport passport = passportService.getPassportByNumber(number);
                if (passport != null) {
                    passports.add(passport);
                } else {
                    log.warn("Паспорт с номером {} не найден в базе данных", number);
                }
            } catch (Exception e) {
                log.error("Ошибка при загрузке паспорта {}", number, e);
            }
        }
        return passports;
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
     * Типы действий при загрузке.
     */
    private enum LoadAction {
        REPLACE,
        APPEND
    }
}
