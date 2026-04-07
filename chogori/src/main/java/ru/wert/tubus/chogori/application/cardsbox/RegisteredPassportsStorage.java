package ru.wert.tubus.chogori.application.cardsbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.statics.WinformStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Хранилище для сохранения состояния выбранных паспортов.
 * Использует постоянную директорию приложения для хранения данных.
 */
@Slf4j
public class RegisteredPassportsStorage {

    /** Имя файла для сохранения состояния выбранных паспортов (JSON формат) */
    private static final String STORAGE_FILE = "registered_passports.json";

    /** Полный путь к директории приложения */
    private static final String APP_DIR_PATH = WinformStatic.HOME_DIRECTORY;

    /** Объект для сериализации/десериализации JSON */
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Создание директории приложения, если её нет
        try {
            Path appDir = Paths.get(APP_DIR_PATH);
            if (!Files.exists(appDir)) {
                Files.createDirectories(appDir);
                log.info("Создана директория приложения: {}", APP_DIR_PATH);
            }
        } catch (IOException e) {
            log.error("Не удалось создать директорию приложения", e);
        }
    }

    /**
     * Получение пути к файлу хранения.
     *
     * @return файл для хранения состояния
     */
    private static File getStorageFile() {
        return new File(APP_DIR_PATH, STORAGE_FILE);
    }

    /**
     * Сохраняет список выбранных паспортов.
     * Сохраняются только номера паспортов как уникальные идентификаторы.
     *
     * @param passports список выбранных паспортов
     */
    public static void saveRegisteredPassports(List<Passport> passports) {
        try {
            File storageFile = getStorageFile();
            List<String> passportNumbers = passports.stream()
                    .map(Passport::getNumber)
                    .filter(number -> number != null && !number.isEmpty())
                    .collect(Collectors.toList());

            objectMapper.writeValue(storageFile, passportNumbers);
            log.info("Сохранено {} номеров выбранных паспортов в файл {}",
                    passportNumbers.size(), storageFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Не удалось сохранить выбранные паспорта", e);
        }
    }

    /**
     * Загружает список номеров выбранных паспортов из файла.
     *
     * @return список номеров паспортов, либо пустой список если файл не найден
     */
    public static List<String> loadRegisteredPassportNumbers() {
        File storageFile = getStorageFile();
        if (!storageFile.exists()) {
            log.info("Файл с сохраненными выбранными паспортами не найден: {}", storageFile.getAbsolutePath());
            return new ArrayList<>();
        }

        try {
            List<String> passportNumbers = objectMapper.readValue(storageFile, new TypeReference<List<String>>() {});
            log.info("Загружено {} номеров выбранных паспортов из файла {}",
                    passportNumbers.size(), storageFile.getAbsolutePath());
            return passportNumbers;
        } catch (IOException e) {
            log.error("Не удалось загрузить выбранные паспорта", e);
            return new ArrayList<>();
        }
    }

    /**
     * Экспортирует список выбранных паспортов в текстовый файл.
     * Позволяет пользователю выбрать имя файла.
     *
     * @param passports    список выбранных паспортов
     * @param initialFileName начальное имя файла
     * @return true если экспорт успешен, false в противном случае
     */
    public static boolean exportRegisteredPassportsToFile(List<Passport> passports, String initialFileName) {
        try {
            // Используем FileChooser для выбора файла
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Сохранить список номеров");
            fileChooser.setInitialFileName(initialFileName);

            // Устанавливаем расширение файла по умолчанию
            javafx.stage.FileChooser.ExtensionFilter txtFilter =
                    new javafx.stage.FileChooser.ExtensionFilter("Текстовые файлы (*.txt)", "*.txt");
            javafx.stage.FileChooser.ExtensionFilter allFilesFilter =
                    new javafx.stage.FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*");
            fileChooser.getExtensionFilters().addAll(txtFilter, allFilesFilter);
            fileChooser.setSelectedExtensionFilter(txtFilter);

            // Устанавливаем начальную директорию - домашнюю папку пользователя
            File initialDir = new File(System.getProperty("user.home"));
            if (initialDir.exists()) {
                fileChooser.setInitialDirectory(initialDir);
            }

            // Показываем диалог сохранения
            File exportFile = fileChooser.showSaveDialog(null);

            if (exportFile == null) {
                log.info("Пользователь отменил экспорт");
                return false;
            }

            // Добавляем расширение .txt, если его нет
            String filePath = exportFile.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".txt")) {
                exportFile = new File(filePath + ".txt");
            }

            // Формирование содержимого файла
            StringBuilder content = new StringBuilder();
            content.append("Список зарегистрированных номеров\n");
            content.append("=================================\n");
            content.append("Дата экспорта: ").append(new Date()).append("\n\n");

            for (int i = 0; i < passports.size(); i++) {
                Passport p = passports.get(i);
                content.append(String.format("%d. %s\n", i + 1, p.toUsefulString()));
            }

            content.append("\n").append("Всего номеров: ").append(passports.size());

            // Запись файла
            Files.write(exportFile.toPath(), content.toString().getBytes());
            log.info("Экспортировано {} паспортов в файл {}", passports.size(), exportFile.getAbsolutePath());

            return true;
        } catch (IOException e) {
            log.error("Не удалось экспортировать выбранные паспорта", e);
            return false;
        }
    }

    /**
     * Очищает сохраненное состояние выбранных паспортов.
     * Удаляет файл с сохраненными номерами.
     */
    public static void clearSavedState() {
        File storageFile = getStorageFile();
        if (storageFile.exists()) {
            boolean deleted = storageFile.delete();
            log.info("Сохраненное состояние выбранных паспортов очищено: {}", deleted);
        }
    }
}
