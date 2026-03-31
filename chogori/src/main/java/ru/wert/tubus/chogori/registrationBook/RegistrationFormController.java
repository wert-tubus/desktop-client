package ru.wert.tubus.chogori.registrationBook;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.winform.warnings.Warning1;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import static java.lang.String.format;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;
import static ru.wert.tubus.winform.warnings.WarningMessages.$SERVER_IS_NOT_AVAILABLE_MAYBE;

/**
 * Контроллер формы регистрации нового паспорта
 * Отвечает за создание и сохранение паспортов ПИК и эскизов
 */
@Slf4j
public class RegistrationFormController implements Initializable {

    @FXML
    private TextField tfNumber;

    @FXML
    private TextField tfName;

    @FXML
    private TextArea taProduct;

    @FXML
    private TextField tfDeveloper;

    @FXML
    private TextField tfDate;

    @FXML
    private Button btnAccept;

    @FXML
    private Button btnCancel;

    private Passport newPassport;           // Новый паспорт до сохранения
    private Passport savedPassport;         // Сохраненный паспорт (после сохранения в БД)
    private boolean accepted = false;       // Флаг подтверждения создания
    private boolean cancelled = false;      // Флаг отмены
    private String passportType;            // Тип паспорта: "PIK" или "SKETCH"
    private Decimal currentDecimal;         // Текущая децимальная группа
    private int reservedNumber;             // Зарезервированный порядковый номер
    private boolean numberReserved = false; // Флаг резервирования номера

    private Prefix pikPrefix;               // Префикс "ПИК" из базы данных
    private Prefix sketchPrefix;            // Префикс "-" из базы данных

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupButtonHandlers();
        setupValidation();
        loadPrefixes(); // Загружаем префиксы из базы
    }

    /**
     * Загрузка префиксов из базы данных
     */
    private void loadPrefixes() {
        try {
            // Получаем префикс "ПИК" из базы
            pikPrefix = ChogoriServices.CH_PREFIXES.findByName("ПИК");
            if (pikPrefix == null) {
                log.error("Префикс 'ПИК' не найден в базе данных");
            }

            // Получаем префикс "-" из базы
            sketchPrefix = ChogoriServices.CH_PREFIXES.findByName("-");
            if (sketchPrefix == null) {
                log.error("Префикс '-' не найден в базе данных");
            }
        } catch (Exception e) {
            log.error("Ошибка при загрузке префиксов", e);
        }
    }

    /**
     * Настройка обработчиков кнопок
     */
    private void setupButtonHandlers() {
        btnAccept.setOnAction(event -> accept());
        btnCancel.setOnAction(event -> cancel());
    }

    /**
     * Настройка валидации полей
     */
    private void setupValidation() {
        // Валидация поля Наименование (не может быть пустым)
        tfName.textProperty().addListener((observable, oldValue, newValue) -> {
            btnAccept.setDisable(newValue == null || newValue.trim().isEmpty());
        });
    }

    /**
     * Установка данных для нового паспорта
     *
     * @param type    тип паспорта ("PIK" или "SKETCH")
     * @param number  сформированный номер
     * @param decimal децимальная группа
     */
    public void setData(String type, String number, Decimal decimal) {
        this.passportType = type;
        this.currentDecimal = decimal;

        // Извлекаем порядковый номер из сформированного номера
        extractReservedNumber(type, number);

        // Создаем новый паспорт
        this.newPassport = createNewPassport(number);

        // Заполняем поля формы
        fillFormFields();

        // Устанавливаем начальное состояние кнопки
        btnAccept.setDisable(tfName.getText() == null || tfName.getText().trim().isEmpty());

        log.debug("Данные формы инициализированы: тип={}, номер={}", passportType, number);
    }

    /**
     * Извлекает порядковый номер из сформированного номера
     *
     * @param type   тип паспорта
     * @param number сформированный номер
     */
    private void extractReservedNumber(String type, String number) {
        if ("PIK".equals(type)) {
            // Для ПИК номер имеет формат "XXXXXX.XXX"
            String[] parts = number.split("\\.");
            if (parts.length == 2) {
                try {
                    this.reservedNumber = Integer.parseInt(parts[1]);
                    this.numberReserved = true;
                    log.debug("Зарезервирован номер ПИК: {}", reservedNumber);
                } catch (NumberFormatException e) {
                    log.error("Ошибка при извлечении номера из строки: {}", number, e);
                }
            }
        } else {
            // Для эскиза номер имеет формат "ЭXXXXX"
            if (number != null && number.length() > 1) {
                try {
                    this.reservedNumber = Integer.parseInt(number.substring(1));
                    this.numberReserved = true;
                    log.debug("Зарезервирован номер эскиза: {}", reservedNumber);
                } catch (NumberFormatException e) {
                    log.error("Ошибка при извлечении номера из строки: {}", number, e);
                }
            }
        }
    }

    /**
     * Создание нового паспорта
     *
     * @param number номер паспорта
     * @return созданный объект Passport
     */
    private Passport createNewPassport(String number) {
        Passport passport = new Passport();

        // Устанавливаем префикс в зависимости от типа (используем существующие объекты из базы)
        if ("PIK".equals(passportType)) {
            if (pikPrefix != null) {
                passport.setPrefix(pikPrefix);
            } else {
                log.error("Префикс 'ПИК' не загружен, создаем временный объект");
                Prefix prefix = new Prefix();
                prefix.setName("ПИК");
                passport.setPrefix(prefix);
            }
        } else {
            if (sketchPrefix != null) {
                passport.setPrefix(sketchPrefix);
            } else {
                log.error("Префикс '-' не загружен, создаем временный объект");
                Prefix prefix = new Prefix();
                prefix.setName("-");
                passport.setPrefix(prefix);
            }
        }
        passport.setNumber(number);

        return passport;
    }

    /**
     * Заполнение полей формы
     */
    private void fillFormFields() {
        // Заполняем поле номера
        if (newPassport != null && newPassport.getNumber() != null) {
            tfNumber.setText(newPassport.getNumber());
        }

        // Очищаем поле наименования (ждет ввода пользователя)
        tfName.clear();

        // Очищаем поле изделия (ждет ввода пользователя)
        taProduct.clear();

        // Заполняем поле разработчика текущим пользователем
        if (CH_CURRENT_USER != null && CH_CURRENT_USER.getName() != null) {
            tfDeveloper.setText(CH_CURRENT_USER.getName());
        } else {
            tfDeveloper.setText("Неизвестный разработчик");
        }

        // Заполняем поле даты текущей датой в формате "dd.MM.yy"
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        tfDate.setText(currentDate.format(formatter));

        log.debug("Поля формы заполнены: номер={}, разработчик={}, дата={}",
                tfNumber.getText(), tfDeveloper.getText(), tfDate.getText());
    }

    /**
     * Принять изменения и сохранить паспорт в базе данных
     */
    private void accept() {
        try {
            // Проверяем, что данные инициализированы
            if (newPassport == null) {
                throw new RuntimeException("Данные формы не инициализированы");
            }

            // Заполняем данные нового паспорта из полей формы
            newPassport.setName(tfName.getText().trim());
            newPassport.setNote(taProduct.getText().trim());
            newPassport.setUserName(tfDeveloper.getText());
            newPassport.setDate(tfDate.getText());

            log.info("Сохраняем паспорт: тип={}, номер={}, наименование={}",
                    passportType, newPassport.getNumber(), newPassport.getName());

            // Проверяем наличие паспорта в базе данных
            Passport foundPassport = ChogoriServices.CH_QUICK_PASSPORTS.findByPrefixIdAndNumber(
                    newPassport.getPrefix(),
                    newPassport.getNumber()
            );

            if (foundPassport == null) {
                // Паспорта нет в базе - сохраняем
                savedPassport = ChogoriServices.CH_QUICK_PASSPORTS.save(newPassport);

                if (savedPassport != null && savedPassport.getId() != null) {
                    log.info("Паспорт успешно сохранен с ID: {}", savedPassport.getId());
                    accepted = true;
                    numberReserved = false; // Номер успешно использован, откат не нужен
                } else {
                    // Сохранение не удалось
                    Warning1.create($ATTENTION,
                            format("Не удалось создать паспорт \n%s", newPassport.toUsefulString()),
                            $SERVER_IS_NOT_AVAILABLE_MAYBE);
                    throw new RuntimeException("Не удалось сохранить паспорт в базе данных");
                }
            } else {
                // Паспорт уже существует в базе
                savedPassport = foundPassport;
                log.warn("Паспорт уже существует в базе: {}", foundPassport.toUsefulString());
                Warning1.create($ATTENTION,
                        format("Паспорт уже существует в базе данных:\n%s", foundPassport.toUsefulString()),
                        "Повторная регистрация невозможна");
                log.error("Повторная регистрация пасспорта в базе данных");
            }

            // Закрываем окно
            closeWindow();

        } catch (Exception e) {
            log.error("Ошибка при сохранении паспорта", e);

            // При ошибке сохранения выполняем откат номера
            if (numberReserved) {
                rollbackNumber();
            }
        }
    }

    /**
     * Откат номера при ошибке сохранения или отмене
     */
    private void rollbackNumber() {
        try {
            if (currentDecimal != null && reservedNumber > 0) {
                // Получаем свежие данные из базы
                Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(currentDecimal.getId());
                if (freshDecimal != null && freshDecimal.getLastNumber() == reservedNumber) {
                    freshDecimal.setLastNumber(reservedNumber - 1);
                    boolean updated = ChogoriServices.CH_DECIMALS.update(freshDecimal);
                    if (updated) {
                        log.info("Выполнен откат номера {} для decimal {}", reservedNumber, currentDecimal.getName());
                    } else {
                        log.warn("Не удалось выполнить откат номера {} для decimal {}", reservedNumber, currentDecimal.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при откате номера", e);
        }
    }

    /**
     * Отмена создания паспорта
     */
    private void cancel() {
        cancelled = true;
        log.debug("Создание паспорта отменено пользователем");
        closeWindow();
    }

    /**
     * Закрытие окна
     */
    private void closeWindow() {
        if (tfNumber.getScene() != null && tfNumber.getScene().getWindow() != null) {
            tfNumber.getScene().getWindow().hide();
        } else {
            log.warn("Не удалось закрыть окно: сцена равна null");
        }
    }

    /**
     * Получить сохраненный паспорт
     *
     * @return сохраненный паспорт или новый, если сохранение не удалось
     */
    public Passport getSavedPassport() {
        return savedPassport != null ? savedPassport : newPassport;
    }

    /**
     * Проверить, был ли паспорт успешно создан и сохранен
     *
     * @return true если паспорт создан, false в противном случае
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Проверить, была ли операция отменена пользователем
     *
     * @return true если операция отменена, false в противном случае
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Проверить, был ли зарезервирован номер
     *
     * @return true если номер был зарезервирован, false в противном случае
     */
    public boolean isNumberReserved() {
        return numberReserved;
    }

    /**
     * Получить зарезервированный порядковый номер
     *
     * @return зарезервированный номер
     */
    public int getReservedNumber() {
        return reservedNumber;
    }
}
