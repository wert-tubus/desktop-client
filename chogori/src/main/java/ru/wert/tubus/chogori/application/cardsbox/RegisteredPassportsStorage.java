package ru.wert.tubus.chogori.application.cardsbox;

import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.cardsbox.registrationBook.PassportListFileManager;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.statics.WinformStatic;

import java.io.File;
import java.util.List;

/**
 * Хранилище для сохранения состояния выбранных паспортов.
 * Использует PassportListFileManager для файловых операций.
 */
@Slf4j
public class RegisteredPassportsStorage {

    private static final String STORAGE_FILE = "registered_passports.json";
    private static final String APP_DIR_PATH = WinformStatic.HOME_DIRECTORY;
    private static final File STORAGE_FILE_OBJ = new File(APP_DIR_PATH, STORAGE_FILE);

    /**
     * Сохраняет список выбранных паспортов (автосохранение).
     */
    public static void saveRegisteredPassports(List<Passport> passports) {
        PassportListFileManager.autoSave(passports, STORAGE_FILE_OBJ);
    }

    /**
     * Загружает список номеров выбранных паспортов из файла автосохранения.
     */
    public static List<String> loadRegisteredPassportNumbers() {
        return PassportListFileManager.autoLoad(STORAGE_FILE_OBJ);
    }

    /**
     * Экспорт списка в файл (ручной экспорт).
     */
    public static boolean exportRegisteredPassportsToFile(List<Passport> passports, String initialFileName) {
        PassportListFileManager tempManager = new PassportListFileManager(
                null, null, null, null, null, null
        );
        // Временно создаем экземпляр для использования метода exportToFile
        // Лучше рефакторинг: сделать exportToFile статическим методом
        return false; // TODO: перенести логику экспорта
    }

    /**
     * Очищает сохраненное состояние.
     */
    public static void clearSavedState() {
        if (STORAGE_FILE_OBJ.exists()) {
            boolean deleted = STORAGE_FILE_OBJ.delete();
            log.info("Сохраненное состояние очищено: {}", deleted);
        }
    }
}