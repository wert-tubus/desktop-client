package ru.wert.tubus.chogori.application.cardsbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.cardsbox.registrationBook.PassportListFileManager;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.statics.WinformStatic;
import ru.wert.tubus.winform.warnings.Warning1;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;

@Slf4j
public class RegisteredPassportsStorage {

    private static final String STORAGE_FILE = "registered_passports.json";
    private static final String APP_DIR_PATH = WinformStatic.HOME_DIRECTORY;
    private static final File STORAGE_FILE_OBJ = new File(APP_DIR_PATH, STORAGE_FILE);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void saveRegisteredPassports(List<Passport> passports) {
        if (passports == null || passports.isEmpty()) {
            clearSavedState();
            return;
        }

        List<String> numbers = passports.stream()
                .map(Passport::getNumber)
                .filter(n -> n != null && !n.isEmpty())
                .collect(Collectors.toList());

        try {
            String json = objectMapper.writeValueAsString(numbers);
            Files.write(STORAGE_FILE_OBJ.toPath(), json.getBytes(StandardCharsets.UTF_8));
            log.info("Автосохранено {} номеров", numbers.size());
        } catch (IOException e) {
            log.error("Ошибка автосохранения", e);
        }
    }

    public static List<String> loadRegisteredPassportNumbers() {
        if (!STORAGE_FILE_OBJ.exists()) {
            return new ArrayList<>();
        }

        try {
            String content = new String(Files.readAllBytes(STORAGE_FILE_OBJ.toPath()), StandardCharsets.UTF_8);
            List<String> numbers = objectMapper.readValue(content, new TypeReference<List<String>>() {});
            log.info("Автозагружено {} номеров", numbers != null ? numbers.size() : 0);
            return numbers != null ? numbers : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Не удалось загрузить файл автосохранения", e);
            return new ArrayList<>();
        }
    }

    public static boolean exportRegisteredPassportsToFile(List<Passport> passports, String initialFileName) {
        if (passports == null || passports.isEmpty()) {
            Warning1.create("Внимание", "Список пуст", "Нечего экспортировать");
            return false;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить список");
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt")
        );

        File file = fileChooser.showSaveDialog(WF_MAIN_STAGE);
        if (file == null) {
            return false;
        }

        try {
            List<String> numbers = passports.stream()
                    .map(Passport::getNumber)
                    .filter(n -> n != null && !n.isEmpty())
                    .collect(Collectors.toList());

            Files.write(file.toPath(), numbers, StandardCharsets.UTF_8);
            Warning1.create("Успешно", "Экспорт выполнен", "Список сохранен в файл: " + file.getName());
            return true;
        } catch (IOException e) {
            log.error("Ошибка экспорта", e);
            Warning1.create("Ошибка", "Не удалось сохранить файл", e.getMessage());
            return false;
        }
    }

    public static void clearSavedState() {
        if (STORAGE_FILE_OBJ.exists()) {
            boolean deleted = STORAGE_FILE_OBJ.delete();
            log.info("Сохраненное состояние очищено: {}", deleted);
        }
    }
}