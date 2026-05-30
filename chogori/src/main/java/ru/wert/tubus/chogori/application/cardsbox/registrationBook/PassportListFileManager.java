package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.AppPropsSettings;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.client.entity.serviceREST.PrefixService;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

@Slf4j
public class PassportListFileManager {

    private final RegistrationService registrationService;
    private final PrefixService prefixService;
    private final Consumer<Passport> addPassportConsumer;
    private final Runnable clearListRunnable;
    private final Runnable refreshTablesRunnable;
    private final Runnable showLoadingRunnable;
    private final Runnable hideLoadingRunnable;

    private Map<String, Prefix> prefixCache;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public PassportListFileManager(RegistrationService registrationService,
                                   Consumer<Passport> addPassportConsumer,
                                   Runnable clearListRunnable,
                                   Runnable refreshTablesRunnable,
                                   Runnable showLoadingRunnable,
                                   Runnable hideLoadingRunnable) {
        this.registrationService = registrationService;
        this.prefixService = PrefixService.getInstance();
        this.addPassportConsumer = addPassportConsumer;
        this.clearListRunnable = clearListRunnable;
        this.refreshTablesRunnable = refreshTablesRunnable;
        this.showLoadingRunnable = showLoadingRunnable;
        this.hideLoadingRunnable = hideLoadingRunnable;
        initPrefixCache();
    }

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
            List<String> lines = new ArrayList<>();
            lines.add("Список зарегистрированных номеров");
            lines.add("=================================");
            lines.add("Дата экспорта: " + new Date());
            lines.add("");

            int counter = 1;
            for (Passport passport : passports) {
                lines.add(String.format("%d. %s", counter++, passport.getNumber()));
            }

            lines.add("");
            lines.add("Всего номеров: " + passports.size());

            Files.write(exportFile.toPath(), lines, StandardCharsets.UTF_8);
            log.info("Экспортировано {} чертежей в файл {}", passports.size(), exportFile.getAbsolutePath());
            Warning1.create("ОТЛИЧНО!", "Экспорт выполнен", "Список успешно сохранен в файл!");
            return true;
        } catch (IOException e) {
            log.error("Не удалось экспортировать чертежи", e);
            Warning1.create("ОШИБКА!", "Не удалось сохранить файл", e.getMessage());
            return false;
        }
    }

    public void loadFromFile() {
        File selectedFile = chooseFileToLoad();
        if (selectedFile == null) {
            return;
        }

        List<String> numbers = loadPassportNumbersFromFile(selectedFile);
        if (numbers.isEmpty()) {
            Warning1.create($ATTENTION, "Файл пуст или имеет неверный формат",
                    "Не удалось загрузить номера из файла");
            return;
        }

        LoadAction action = showLoadActionDialog(numbers.size());
        if (action == null) {
            return;
        }

        if (showLoadingRunnable != null) {
            showLoadingRunnable.run();
        }

        CompletableFuture.supplyAsync(() -> loadPassportsByNumbers(numbers))
                .thenAccept(passports -> {
                    Platform.runLater(() -> {
                        if (hideLoadingRunnable != null) {
                            hideLoadingRunnable.run();
                        }

                        if (passports == null) {
                            Warning1.create("ОШИБКА!", "Не удалось загрузить чертежи",
                                    "Проверьте подключение к базе данных");
                            return;
                        }

                        // Передаем оригинальные номера для подсчета не найденных
                        applyLoadedPassports(passports, numbers, action);

                        if (refreshTablesRunnable != null) {
                            refreshTablesRunnable.run();
                        }
                    });
                });
    }

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

        String registeredDraftsPath = AppPropsSettings.getInstance().getRegisteredDraftsPath();
        File initialDir = new File(registeredDraftsPath);

        if (!initialDir.exists()) {
            boolean created = initialDir.mkdirs();
            if (created) {
                log.info("Создана директория для сохранения: {}", registeredDraftsPath);
            } else {
                log.warn("Не удалось создать директорию: {}, используется домашняя", registeredDraftsPath);
                initialDir = new File(System.getProperty("user.home"));
            }
        }

        if (initialDir.exists() && initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        File selectedFile = fileChooser.showSaveDialog(null);
        saveSelectedDirToProperties(selectedFile);

        if (selectedFile != null && !selectedFile.getName().toLowerCase().endsWith(".txt")) {
            selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
        }

        return selectedFile;
    }

    private File chooseFileToLoad() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Загрузить список номеров");

        FileChooser.ExtensionFilter txtFilter =
                new FileChooser.ExtensionFilter("Текстовые файлы (*.txt)", "*.txt");
        FileChooser.ExtensionFilter allFilesFilter =
                new FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*");
        fileChooser.getExtensionFilters().addAll(txtFilter, allFilesFilter);

        String registeredDraftsPath = AppPropsSettings.getInstance().getRegisteredDraftsPath();
        File initialDir = new File(registeredDraftsPath);

        if (initialDir.exists() && initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
            log.debug("Начальная директория для загрузки: {}", registeredDraftsPath);
        } else {
            log.warn("Директория из настроек недоступна: {}, используется домашняя", registeredDraftsPath);
            File defaultDir = new File(System.getProperty("user.home"));
            if (defaultDir.exists()) {
                fileChooser.setInitialDirectory(defaultDir);
            }
        }

        File selectedFile = fileChooser.showOpenDialog(null);
        saveSelectedDirToProperties(selectedFile);

        return selectedFile;
    }

    private void saveSelectedDirToProperties(File selectedFile) {
        if (selectedFile == null) {
            log.debug("Файл не выбран, директория не сохраняется");
            return;
        }

        AppPropsSettings settings = AppPropsSettings.getInstance();
        String selectedDirectory = selectedFile.getParent();
        String currentSettingsPath = settings.getRegisteredDraftsPath();

        if (selectedDirectory != null && !selectedDirectory.isEmpty()) {
            File selectedDir = new File(selectedDirectory);

            if (!selectedDir.exists()) {
                boolean created = selectedDir.mkdirs();
                if (created) {
                    log.info("Создана выбранная пользователем директория: {}", selectedDirectory);
                } else {
                    log.error("Не удалось создать директорию: {}", selectedDirectory);
                    return;
                }
            }

            if (!selectedDirectory.equals(currentSettingsPath)) {
                settings.setRegisteredDraftsPath(selectedDirectory);
                settings.saveParams();
                log.info("Директория обновлена в настройках: {} -> {}",
                        currentSettingsPath, selectedDirectory);
            } else {
                log.debug("Директория не изменилась: {}", currentSettingsPath);
            }
        } else {
            log.warn("Не удалось определить директорию для выбранного файла: {}", selectedFile.getAbsolutePath());
        }
    }

    private List<String> loadPassportNumbersFromFile(File file) {
        List<String> numbers = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

            for (String line : lines) {
                if (line == null) continue;

                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // Пропускаем строки-заголовки
                if (trimmed.contains("===") ||
                        trimmed.contains("Дата экспорта") ||
                        trimmed.contains("Всего номеров") ||
                        trimmed.contains("Список зарегистрированных") ||
                        trimmed.contains("=======================================")) {
                    continue;
                }

                // Формат "1. 400000.001"
                if (trimmed.matches("^\\d+\\.\\s+(.+)$")) {
                    String number = trimmed.replaceFirst("^\\d+\\.\\s+", "");
                    if (!number.isEmpty()) {
                        numbers.add(number);
                    }
                }
                // Просто номер без нумерации
                else if (!trimmed.isEmpty() && !trimmed.matches("^\\d+$")) {
                    numbers.add(trimmed);
                }
            }

            log.info("Загружено {} номеров из файла {}", numbers.size(), file.getName());
            return numbers;

        } catch (IOException e) {
            log.error("Ошибка при загрузке номеров из файла {}", file.getName(), e);
            Warning1.create("ОШИБКА!", "Не удалось прочитать файл",
                    "Файл имеет неверный формат или кодировку.\nИспользуйте UTF-8.");
            return new ArrayList<>();
        }
    }

    private List<Passport> loadPassportsByNumbers(List<String> numbers) {
        List<Passport> passports = new ArrayList<>();

        for (String number : numbers) {
            try {
                Passport passport = registrationService.findPassportByNumberFast(number);
                if (passport != null) {
                    passports.add(passport);
                    log.debug("Загружен паспорт: {}", passport.getNumber());
                } else {
                    log.warn("Паспорт с номером {} не найден в базе данных", number);
                }
            } catch (Exception e) {
                log.error("Ошибка при загрузке паспорта {}", number);
            }
        }

        return passports;
    }

    private void applyLoadedPassports(List<Passport> passports, List<String> originalNumbers, LoadAction action) {
        int loadedCount = passports.size();
        int notFoundCount = originalNumbers.size() - loadedCount;

        if (action == LoadAction.REPLACE) {
            clearListRunnable.run();
        }

        for (Passport passport : passports) {
            addPassportConsumer.accept(passport);
        }

        // Формируем информативное сообщение
        String message;
        if (notFoundCount > 0) {
            message = String.format(
                    "Загружено: %d чертежей\n" +
                            "Не найдено в БД: %d чертежей\n" +
                            "Действие: %s",
                    loadedCount, notFoundCount,
                    action == LoadAction.REPLACE ? "список заменен" : "добавлено к списку"
            );
        } else {
            message = String.format(
                    "Загружено: %d чертежей\n" +
                            "Действие: %s",
                    loadedCount,
                    action == LoadAction.REPLACE ? "список заменен" : "добавлено к списку"
            );
        }

        Warning1.create("ЗАГРУЗКА ЗАВЕРШЕНА", "Результат загрузки", message);
        log.info("Загружено {} паспортов ({} не найдено), действие: {}",
                loadedCount, notFoundCount, action);
    }

    private LoadAction showLoadActionDialog(int count) {
        VBox dialogPane = new VBox(15);
        dialogPane.setPadding(new Insets(20));
        dialogPane.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #555; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label headerLabel = new Label("Загрузка списка чертежей");
        headerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #555;");

        Label contentLabel = new Label("Найдено " + count + " номеров.\n\nВыберите действие:");
        contentLabel.setStyle("-fx-text-fill: #ddd; -fx-font-size: 14px;");
        contentLabel.setWrapText(true);

        final LoadAction[] result = {null};

        Button replaceButton = new Button("Заменить текущий список");
        replaceButton.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;");
        replaceButton.setOnMouseEntered(e -> replaceButton.setStyle("-fx-background-color: #5a5a5a; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;"));
        replaceButton.setOnMouseExited(e -> replaceButton.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;"));
        replaceButton.setOnAction(e -> {
            result[0] = LoadAction.REPLACE;
            dialogPane.getScene().getWindow().hide();
        });

        Button appendButton = new Button("Добавить к текущему списку");
        appendButton.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;");
        appendButton.setOnMouseEntered(e -> appendButton.setStyle("-fx-background-color: #5a5a5a; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;"));
        appendButton.setOnMouseExited(e -> appendButton.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;"));
        appendButton.setOnAction(e -> {
            result[0] = LoadAction.APPEND;
            dialogPane.getScene().getWindow().hide();
        });

        Button cancelButton = new Button("Отмена");
        cancelButton.setStyle("-fx-background-color: #6d2e2e; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #8a3a3a; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: #6d2e2e; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15;"));
        cancelButton.setOnAction(e -> {
            result[0] = null;
            dialogPane.getScene().getWindow().hide();
        });

        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.getChildren().addAll(replaceButton, appendButton, cancelButton);

        dialogPane.getChildren().addAll(headerLabel, separator, contentLabel, buttonBar);

        new WindowDecoration("Подтверждение загрузки", dialogPane, false, null, true);

        return result[0];
    }

    private enum LoadAction {
        REPLACE,
        APPEND
    }
}
