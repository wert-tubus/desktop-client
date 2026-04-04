// SelectedPassportsStorage.java
package ru.wert.tubus.chogori.application.cardsbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.wert.tubus.winform.statics.WinformStatic.WF_TEMPDIR;

@Slf4j
public class SelectedPassportsStorage {

    private static final String STORAGE_FILE = "selected_passports.json";
    private static final String EXPORT_FILE = "selected_passports_export.txt";
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Сохраняет список выбранных паспортов (сохраняем номер + префикс как уникальный ключ)
     */
    public static void saveSelectedPassports(List<Passport> passports) {
        if (WF_TEMPDIR == null) {
            log.warn("WF_TEMPDIR is null, cannot save selected passports");
            return;
        }

        try {
            File storageFile = new File(WF_TEMPDIR, STORAGE_FILE);
            List<String> passportNumbers = passports.stream()
                    .map(Passport::getNumber)
                    .filter(number -> number != null && !number.isEmpty())
                    .collect(Collectors.toList());

            objectMapper.writeValue(storageFile, passportNumbers);
            log.info("Saved {} selected passport numbers to {}", passportNumbers.size(), storageFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save selected passports", e);
        }
    }

    /**
     * Загружает список номеров выбранных паспортов
     */
    public static List<String> loadSelectedPassportNumbers() {
        if (WF_TEMPDIR == null) {
            log.warn("WF_TEMPDIR is null, cannot load selected passports");
            return new ArrayList<>();
        }

        File storageFile = new File(WF_TEMPDIR, STORAGE_FILE);
        if (!storageFile.exists()) {
            log.info("No saved selected passports file found");
            return new ArrayList<>();
        }

        try {
            List<String> passportNumbers = objectMapper.readValue(storageFile, new TypeReference<List<String>>() {});
            log.info("Loaded {} selected passport numbers from {}", passportNumbers.size(), storageFile.getAbsolutePath());
            return passportNumbers;
        } catch (IOException e) {
            log.error("Failed to load selected passports", e);
            return new ArrayList<>();
        }
    }

    /**
     * Экспортирует список выбранных паспортов в текстовый файл
     */
    public static void exportSelectedPassportsToFile(List<Passport> passports, String userHomePath) {
        if (WF_TEMPDIR == null) {
            log.warn("WF_TEMPDIR is null, cannot export selected passports");
            return;
        }

        try {
            // Создаем файл в домашней директории пользователя
            File exportFile;
            if (userHomePath != null && !userHomePath.isEmpty()) {
                exportFile = new File(userHomePath, EXPORT_FILE);
            } else {
                exportFile = new File(WF_TEMPDIR, EXPORT_FILE);
            }

            StringBuilder content = new StringBuilder();
            content.append("Список выбранных паспортов\n");
            content.append("==========================\n");
            content.append("Дата экспорта: ").append(new java.util.Date()).append("\n\n");

            for (int i = 0; i < passports.size(); i++) {
                Passport p = passports.get(i);
                content.append(String.format("%d. %s\n", i + 1, p.toUsefulString()));
            }

            content.append("\n").append("Всего паспортов: ").append(passports.size());

            java.nio.file.Files.write(exportFile.toPath(), content.toString().getBytes());
            log.info("Exported {} selected passports to {}", passports.size(), exportFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to export selected passports", e);
        }
    }

    /**
     * Очищает сохраненное состояние
     */
    public static void clearSavedState() {
        if (WF_TEMPDIR == null) return;

        File storageFile = new File(WF_TEMPDIR, STORAGE_FILE);
        if (storageFile.exists()) {
            boolean deleted = storageFile.delete();
            log.info("Cleared saved selected passports state: {}", deleted);
        }
    }
}
