package ru.wert.tubus.chogori;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.winform.statics.WinformStatic;
import ru.wert.tubus.winform.warnings.Warning1;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
@Getter
@Setter
public class AppPropsSettings {

    // -- Сохраняемые параметры --
    private String registeredDraftsPath; // Путь до хранилища зарегистрированных чертежей

    // -- Добавляйте новые настройки здесь --
    // private String windowPosition;
    // private Boolean rememberLogin;
    // ...

    private static AppPropsSettings instance;
    private Properties props = new Properties();

    // Директория для хранения настроек
    private static final String HOME_DIRECTORY = WinformStatic.HOME_DIRECTORY;
    private static final String CONFIG_FILE_NAME = "app-settings.properties";
    private static final String CONFIG_FILE_PATH = HOME_DIRECTORY + File.separator + CONFIG_FILE_NAME;

    // Ключи свойств
    private static final String KEY_REGISTERED_DRAFTS_PATH = "REGISTERED_DRAFTS_PATH";

    // Значения по умолчанию
    private static final String DEFAULT_REGISTERED_DRAFTS_PATH = "C:/";

    private AppPropsSettings() {
        init();
    }

    public static AppPropsSettings getInstance() {
        if (instance == null) {
            instance = new AppPropsSettings();
        }
        return instance;
    }

    /**
     * Инициализация: создание директории и файла настроек при необходимости,
     * загрузка параметров
     */
    private void init() {
        try {
            createHomeDirectoryIfNotExists();
            createConfigFileIfNotExists();
            loadParams();
        } catch (Exception e) {
            log.error("Ошибка при инициализации AppPropsSettings", e);
            Warning1.create("Ошибка!", "Ошибка при инициализации настроек приложения", e.getMessage());
        }
    }

    /**
     * Создание домашней директории, если она не существует
     */
    private void createHomeDirectoryIfNotExists() {
        Path homePath = Paths.get(HOME_DIRECTORY);
        if (!Files.exists(homePath)) {
            try {
                Files.createDirectories(homePath);
                log.info("Создана директория: {}", HOME_DIRECTORY);
            } catch (IOException e) {
                log.error("Не удалось создать директорию: {}", HOME_DIRECTORY, e);
                throw new RuntimeException("Не удалось создать директорию: " + HOME_DIRECTORY, e);
            }
        }
    }

    /**
     * Создание файла настроек со значениями по умолчанию, если он не существует
     */
    private void createConfigFileIfNotExists() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            try {
                // Устанавливаем значения по умолчанию
                setDefaultValues();
                // Сохраняем в файл
                saveParams();
                log.info("Создан файл настроек с параметрами по умолчанию: {}", CONFIG_FILE_PATH);
            } catch (Exception e) {
                log.error("Не удалось создать файл настроек: {}", CONFIG_FILE_PATH, e);
                throw new RuntimeException("Не удалось создать файл настроек: " + CONFIG_FILE_PATH, e);
            }
        }
    }

    /**
     * Установка значений по умолчанию для всех настроек
     */
    private void setDefaultValues() {
        this.registeredDraftsPath = DEFAULT_REGISTERED_DRAFTS_PATH;
        // При добавлении новых настроек - инициализируйте их значениями по умолчанию здесь
        // this.windowPosition = "0,0";
        // this.rememberLogin = false;

        // Записываем в Properties
        props.setProperty(KEY_REGISTERED_DRAFTS_PATH, registeredDraftsPath);
    }

    /**
     * Загрузка параметров из файла настроек
     */
    public void loadParams() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH)) {
            props.load(fis);

            // Загрузка текущих параметров
            this.registeredDraftsPath = props.getProperty(KEY_REGISTERED_DRAFTS_PATH, DEFAULT_REGISTERED_DRAFTS_PATH);

            // При добавлении новых настроек - добавляйте загрузку здесь
            // this.windowPosition = props.getProperty(KEY_WINDOW_POSITION, DEFAULT_WINDOW_POSITION);
            // this.rememberLogin = Boolean.parseBoolean(props.getProperty(KEY_REMEMBER_LOGIN, "false"));

            log.info("Настройки загружены из файла: {}", CONFIG_FILE_PATH);

        } catch (IOException e) {
            log.error("Не удалось загрузить файл настроек: {}", CONFIG_FILE_PATH, e);
            Warning1.create("Внимание!", "Не могу найти или прочитать файл свойств", "Будут использованы значения по умолчанию.");

            // При ошибке загрузки устанавливаем значения по умолчанию
            setDefaultValues();
        }
    }

    /**
     * Сохранение параметров в файл настроек
     */
    public void saveParams() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE_PATH)) {

            // Записываем текущие значения в Properties
            props.setProperty(KEY_REGISTERED_DRAFTS_PATH,
                    registeredDraftsPath != null ? registeredDraftsPath : DEFAULT_REGISTERED_DRAFTS_PATH);

            // При добавлении новых настроек - добавляйте сохранение здесь
            // props.setProperty(KEY_WINDOW_POSITION, windowPosition != null ? windowPosition : "");
            // props.setProperty(KEY_REMEMBER_LOGIN, String.valueOf(rememberLogin));

            props.store(fos, "Application Settings - Auto-generated file. Do not edit manually.");
            log.info("Настройки сохранены в файл: {}", CONFIG_FILE_PATH);

        } catch (IOException e) {
            log.error("Не удалось сохранить файл настроек: {}", CONFIG_FILE_PATH, e);
            Warning1.create("Ошибка!", "Не удалось сохранить файл настроек", "Ошибка: " + e.getMessage());
        }
    }

    /**
     * Сброс всех настроек к значениям по умолчанию
     */
    public void resetToDefaults() {
        setDefaultValues();
        saveParams();
        log.info("Настройки сброшены к значениям по умолчанию");
    }

    /**
     * Получение полного пути к файлу настроек
     */
    public String getConfigFilePath() {
        return CONFIG_FILE_PATH;
    }
}
