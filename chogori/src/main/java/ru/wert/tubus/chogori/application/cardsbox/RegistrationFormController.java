
package ru.wert.tubus.chogori.application.cardsbox;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.IFormView;
import ru.wert.tubus.chogori.components.BXUsers;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.client.entity.models.User;
import ru.wert.tubus.winform.enums.EOperation;
import ru.wert.tubus.winform.warnings.Warning1;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import static java.lang.String.format;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.*;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;
import static ru.wert.tubus.chogori.statics.AppStatic.capitalizeFirstLetter;
import static ru.wert.tubus.chogori.statics.AppStatic.closeWindow;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;
import static ru.wert.tubus.winform.warnings.WarningMessages.$SERVER_IS_NOT_AVAILABLE_MAYBE;

/**
 * Контроллер формы регистрации нового паспорта.
 * Отвечает за создание, редактирование и сохранение паспортов ПИК и эскизов.
 */
@Slf4j
public class RegistrationFormController extends FormView_ACCController<Passport> implements Initializable {

    // ======================== FXML КОМПОНЕНТЫ ========================

    @FXML
    private TextField tfNumber;          // Поле для номера паспорта

    @FXML
    private TextField tfName;            // Поле для наименования

    @FXML
    private TextArea taProduct;          // Поле для описания изделия

    @FXML
    private ComboBox<User> bxUser;       // Выпадающий список пользователей

    @FXML
    private TextField tfDate;            // Поле для даты

    @FXML
    private Button btnAccept;            // Кнопка подтверждения

    @FXML
    private Button btnCancel;            // Кнопка отмены

    @FXML
    private StackPane spIndicator;       // Индикатор загрузки

    // ======================== ПОЛЯ СОСТОЯНИЯ ========================

    private Passport newPassport;        // Новый паспорт до сохранения
    private Passport savedPassport;      // Сохраненный паспорт (после сохранения в БД)
    private boolean accepted = false;    // Флаг подтверждения создания
    private String passportType;         // Тип паспорта: "PIK" или "SKETCH"
    private Decimal currentDecimal;      // Текущая децимальная группа
    private int reservedNumber;          // Зарезервированный порядковый номер
    private boolean numberReserved = false; // Флаг резервирования номера

    private Prefix pikPrefix;            // Префикс "ПИК" из базы данных
    private Prefix sketchPrefix;         // Префикс "-" из базы данных
    private boolean cancelled = false;   // Флаг отмены
    private boolean editMode = false;    // Режим редактирования
    private Passport editingPassport;    // Редактируемый паспорт

    // ======================== ИНИЦИАЛИЗАЦИЯ ========================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Настройка выпадающего списка пользователей (только конструкторы)
        new BXUsers(bxUser, CH_USER_GROUPS.findByName("Конструктор"));
        bxUser.setStyle("-fx-font-size: 14px; -fx-background-color: white");

        // Настройка обработчиков кнопок и валидации
        setupButtonHandlers();
        setupValidation();

        // Загрузка префиксов из базы данных
        loadPrefixes();
    }

    /**
     * Загрузка префиксов из базы данных.
     * Префиксы необходимы для правильного формирования номера паспорта.
     */
    private void loadPrefixes() {
        try {
            // Получение префикса "ПИК" из базы
            pikPrefix = CH_PREFIXES.findByName("ПИК");
            if (pikPrefix == null) {
                log.error("Префикс 'ПИК' не найден в базе данных");
            }

            // Получение префикса "-" из базы (для эскизов)
            sketchPrefix = CH_PREFIXES.findByName("-");
            if (sketchPrefix == null) {
                log.error("Префикс '-' не найден в базе данных");
            }
        } catch (Exception e) {
            log.error("Ошибка при загрузке префиксов", e);
        }
    }

    /**
     * Настройка обработчиков событий для кнопок.
     */
    private void setupButtonHandlers() {
        btnAccept.setOnAction(event -> {
            if (editMode) {
                okPressedForEdit(event, spIndicator, btnAccept);
            } else {
                okPressed(event, spIndicator, btnAccept);
            }
        });
        btnCancel.setOnAction(event -> cancelPressed(event));
    }

    /**
     * Настройка валидации полей формы.
     * Кнопка подтверждения блокируется, если поле наименования пустое.
     */
    private void setupValidation() {
        tfName.textProperty().addListener((observable, oldValue, newValue) -> {
            btnAccept.setDisable(newValue == null || newValue.trim().isEmpty());
        });
    }

    // ======================== ИНИЦИАЛИЗАЦИЯ ОПЕРАЦИИ ========================

    /**
     * Инициализация окна с заданными параметрами.
     *
     * @param operation тип операции (всегда ADD для регистрации)
     * @param formView  представление формы
     * @param commands  команды для выполнения
     */
    @Override
    public void init(EOperation operation, IFormView<Passport> formView, ItemCommands<Passport> commands) {
        super.initSuper(operation, formView, commands, CH_QUICK_PASSPORTS);
    }

    // ======================== УСТАНОВКА ДАННЫХ ========================

    /**
     * Установка данных для создания нового паспорта.
     *
     * @param type    тип паспорта ("PIK" или "SKETCH")
     * @param number  сформированный номер
     * @param decimal децимальная группа
     */
    public void setData(String type, String number, Decimal decimal) {
        this.passportType = type;
        this.currentDecimal = decimal;
        this.editMode = false;

        // Извлечение порядкового номера из сформированного номера
        extractReservedNumber(type, number);

        // Создание нового паспорта
        this.newPassport = createNewPassport(number);

        // Заполнение полей формы
        fillFormFields();

        // Установка начального состояния кнопки
        btnAccept.setDisable(tfName.getText() == null || tfName.getText().trim().isEmpty());

        log.debug("Данные формы инициализированы: тип={}, номер={}", passportType, number);
    }

    /**
     * Установка данных для редактирования существующего паспорта.
     *
     * @param passport паспорт для редактирования
     */
    public void setDataForEdit(Passport passport) {
        this.editMode = true;
        this.editingPassport = passport;
        this.newPassport = passport;

        // Определение типа паспорта по префиксу
        if (passport.getPrefix() != null && "ПИК".equals(passport.getPrefix().getName())) {
            this.passportType = "PIK";
        } else {
            this.passportType = "SKETCH";
        }

        // Заполнение полей формы данными из паспорта
        fillFormFieldsForEdit();

        // Установка начального состояния кнопки
        btnAccept.setDisable(tfName.getText() == null || tfName.getText().trim().isEmpty());

        log.debug("Режим редактирования: паспорт {}", passport.toUsefulString());
    }

    // ======================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ========================

    /**
     * Извлечение порядкового номера из сформированного номера паспорта.
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
     * Создание нового объекта паспорта с предустановленными полями.
     *
     * @param number номер паспорта
     * @return созданный объект Passport
     */
    private Passport createNewPassport(String number) {
        Passport passport = new Passport();

        // Установка префикса в зависимости от типа паспорта
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
     * Заполнение полей формы для нового паспорта.
     */
    private void fillFormFields() {
        // Заполнение поля номера
        if (newPassport != null && newPassport.getNumber() != null) {
            tfNumber.setText(newPassport.getNumber());
        }

        // Очистка полей, ожидающих ввода пользователя
        tfName.clear();
        taProduct.clear();

        // Заполнение поля разработчика текущим пользователем
        if (CH_CURRENT_USER != null && CH_CURRENT_USER.getName() != null) {
            bxUser.getSelectionModel().select(CH_USERS.findByName(CH_CURRENT_USER.getName()));
        } else {
            bxUser.getSelectionModel().select(null);
        }

        // Заполнение поля даты текущей датой
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        tfDate.setText(currentDate.format(formatter));

        log.debug("Поля формы заполнены: номер={}, разработчик={}, дата={}",
                tfNumber.getText(),
                bxUser.getSelectionModel().getSelectedItem() != null ?
                        bxUser.getSelectionModel().getSelectedItem().getName() : "null",
                tfDate.getText());
    }

    /**
     * Заполнение полей формы для редактирования существующего паспорта.
     */
    private void fillFormFieldsForEdit() {
        if (editingPassport == null) return;

        // Заполнение поля номера (не редактируется)
        if (editingPassport.getNumber() != null) {
            tfNumber.setText(editingPassport.getNumber());
            tfNumber.setEditable(false);
        }

        // Заполнение поля наименования
        if (editingPassport.getName() != null) {
            tfName.setText(editingPassport.getName());
        }

        // Заполнение поля изделия
        if (editingPassport.getNote() != null) {
            taProduct.setText(editingPassport.getNote());
        }

        // Заполнение поля разработчика
        if (editingPassport.getUserName() != null) {
            bxUser.getSelectionModel().select(CH_USERS.findByName(editingPassport.getUserName()));
        } else if (CH_CURRENT_USER != null && CH_CURRENT_USER.getName() != null) {
            bxUser.getSelectionModel().select(CH_USERS.findByName(CH_CURRENT_USER.getName()));
        } else {
            bxUser.getSelectionModel().select(null);
        }

        // Заполнение поля даты
        if (editingPassport.getDate() != null) {
            tfDate.setText(editingPassport.getDate());
        } else {
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            tfDate.setText(currentDate.format(formatter));
        }

        log.debug("Поля формы заполнены для редактирования: номер={}, разработчик={}, дата={}",
                tfNumber.getText(),
                bxUser.getSelectionModel().getSelectedItem() != null ?
                        bxUser.getSelectionModel().getSelectedItem().getName() : "null",
                tfDate.getText());
    }

    // ======================== ПЕРЕОПРЕДЕЛЕННЫЕ МЕТОДЫ ========================

    /**
     * Возвращает список полей, которые не могут быть пустыми.
     *
     * @return список значений обязательных полей
     */
    @Override
    public ArrayList<String> getNotNullFields() {
        ArrayList<String> notNullFields = new ArrayList<>();
        notNullFields.add(tfName.getText());
        return notNullFields;
    }

    /**
     * Создает новый объект Passport из данных формы.
     *
     * @return новый Passport
     */
    @Override
    public Passport getNewItem() {
        newPassport.setName(capitalizeFirstLetter(tfName.getText().trim()));
        newPassport.setNote(capitalizeFirstLetter(taProduct.getText().trim()));

        User selectedUser = bxUser.getSelectionModel().getSelectedItem();
        newPassport.setUserName(selectedUser != null ? selectedUser.getName() : "");
        newPassport.setDate(tfDate.getText());

        return newPassport;
    }

    /**
     * Возвращает старый объект для операции изменения.
     *
     * @return старый Passport
     */
    @Override
    public Passport getOldItem() {
        return editMode ? editingPassport : null;
    }

    /**
     * Заполняет поля формы данными из выбранного объекта.
     *
     * @param oldItem объект Passport для редактирования
     */
    @Override
    public void fillFieldsOnTheForm(Passport oldItem) {
        if (oldItem != null) {
            fillFormFieldsForEdit();
        }
    }

    /**
     * Обновляет поля старого объекта данными из формы.
     *
     * @param oldItem объект Passport для обновления
     */
    @Override
    public void changeOldItemFields(Passport oldItem) {
        if (oldItem != null) {
            oldItem.setName(capitalizeFirstLetter(tfName.getText().trim()));
            oldItem.setNote(capitalizeFirstLetter(taProduct.getText().trim()));

            User selectedUser = bxUser.getSelectionModel().getSelectedItem();
            oldItem.setUserName(selectedUser != null ? selectedUser.getName() : "");
            oldItem.setDate(tfDate.getText());
        }
    }

    /**
     * Очищает форму для создания нового объекта.
     */
    @Override
    public void showEmptyForm() {
        fillFormFields();
    }

    /**
     * Проверяет корректность введенных данных.
     *
     * @return true если данные корректны, false в противном случае
     */
    @Override
    public boolean enteredDataCorrect() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            Warning1.create($ATTENTION, "Поле наименования не заполнено!", "Заполните наименование");
            return false;
        }
        return true;
    }

    // ======================== ОБРАБОТКА РЕДАКТИРОВАНИЯ ========================

    /**
     * Обработка нажатия кнопки OK в режиме редактирования.
     */
    protected void okPressedForEdit(javafx.event.Event event, StackPane spIndicator, Button btnOk) {
        if (notNullFieldEmpty()) {
            Warning1.create($ATTENTION, "Некоторые поля не заполнены!", "Заполните все поля");
            return;
        }

        if (enteredDataCorrect()) {
            Passport updatedPassport = getNewItem();
            updatedPassport.setId(editingPassport.getId());

            // Запуск задачи обновления
            updateTask(event, spIndicator, btnOk, updatedPassport);
        }
    }

    /**
     * Задача обновления паспорта в отдельном потоке.
     */
    private void updateTask(javafx.event.Event event, StackPane spIndicator, Button btnOk, Passport passportToUpdate) {
        if (spIndicator != null) {
            spIndicator.setVisible(true);
        }
        if (btnOk != null) {
            btnOk.setDisable(true);
        }

        Task<Void> update = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                log.info("Обновляем паспорт: ID={}, номер={}, наименование={}",
                        passportToUpdate.getId(), passportToUpdate.getNumber(), passportToUpdate.getName());

                try {
                    boolean updated = CH_QUICK_PASSPORTS.update(passportToUpdate);

                    if (updated) {
                        log.info("Паспорт успешно обновлен с ID: {}", passportToUpdate.getId());
                        savedPassport = passportToUpdate;
                        accepted = true;

                        Platform.runLater(() -> {
                            if (event != null) {
                                closeWindow(event);
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            Warning1.create($ATTENTION,
                                    format("Не удалось обновить паспорт \n%s", passportToUpdate.toUsefulString()),
                                    $SERVER_IS_NOT_AVAILABLE_MAYBE);
                        });
                        throw new RuntimeException("Не удалось обновить паспорт в базе данных");
                    }
                } catch (Exception e) {
                    log.error("Ошибка при обновлении паспорта", e);
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                finishUpdate(spIndicator, btnOk);
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                finishUpdate(spIndicator, btnOk);
            }

            @Override
            protected void failed() {
                super.failed();
                finishUpdate(spIndicator, btnOk);
            }
        };

        new Thread(update).start();
    }

    // ======================== ОБРАБОТКА СОЗДАНИЯ ========================

    /**
     * Переопределенный метод okPressed для сохранения паспорта.
     */
    @Override
    protected void okPressed(javafx.event.Event event, StackPane spIndicator, Button btnOk) {
        if (notNullFieldEmpty()) {
            Warning1.create($ATTENTION, "Некоторые поля не заполнены!", "Заполните все поля");
            return;
        }

        if (enteredDataCorrect()) {
            Passport passportToSave = getNewItem();

            // Проверка наличия паспорта в базе данных
            Passport foundPassport = CH_QUICK_PASSPORTS.findByPrefixIdAndNumber(
                    passportToSave.getPrefix(),
                    passportToSave.getNumber()
            );

            if (foundPassport != null) {
                savedPassport = foundPassport;
                log.warn("Паспорт уже существует в базе: {}", foundPassport.toUsefulString());
                Warning1.create($ATTENTION,
                        format("Паспорт уже существует в базе данных:\n%s", foundPassport.toUsefulString()),
                        "Повторная регистрация невозможна");
                return;
            }

            // Запуск задачи сохранения
            savePassportTask(event, spIndicator, btnOk, passportToSave);
        }
    }

    /**
     * Задача сохранения паспорта в отдельном потоке.
     */
    private void savePassportTask(javafx.event.Event event, StackPane spIndicator, Button btnOk, Passport passportToSave) {
        if (spIndicator != null) {
            spIndicator.setVisible(true);
        }
        if (btnOk != null) {
            btnOk.setDisable(true);
        }

        Task<Void> savePassport = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                log.info("Сохраняем паспорт: тип={}, номер={}, наименование={}",
                        passportType, passportToSave.getNumber(), passportToSave.getName());

                try {
                    savedPassport = CH_QUICK_PASSPORTS.save(passportToSave);

                    if (savedPassport != null && savedPassport.getId() != null) {
                        log.info("Паспорт успешно сохранен с ID: {}", savedPassport.getId());
                        accepted = true;
                        numberReserved = false; // Номер успешно использован, откат не нужен

                        Platform.runLater(() -> {
                            if (event != null) {
                                closeWindow(event);
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            Warning1.create($ATTENTION,
                                    format("Не удалось создать паспорт \n%s", passportToSave.toUsefulString()),
                                    $SERVER_IS_NOT_AVAILABLE_MAYBE);
                        });
                        throw new RuntimeException("Не удалось сохранить паспорт в базе данных");
                    }
                } catch (Exception e) {
                    log.error("Ошибка при сохранении паспорта", e);

                    // При ошибке сохранения выполняем откат номера
                    if (numberReserved) {
                        rollbackNumber();
                    }
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                finishUpdate(spIndicator, btnOk);
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                if (numberReserved) {
                    rollbackNumber();
                }
                finishUpdate(spIndicator, btnOk);
            }

            @Override
            protected void failed() {
                super.failed();
                finishUpdate(spIndicator, btnOk);
            }
        };

        new Thread(savePassport).start();
    }

    /**
     * Завершение операции обновления (скрытие индикатора и разблокировка кнопки).
     */
    private void finishUpdate(StackPane spIndicator, Button btnOk) {
        if (spIndicator != null) {
            spIndicator.setVisible(false);
        }
        if (btnOk != null) {
            btnOk.setDisable(false);
        }
    }

    /**
     * Откат номера при ошибке сохранения или отмене операции.
     */
    private void rollbackNumber() {
        try {
            if (currentDecimal != null && reservedNumber > 0) {
                Decimal freshDecimal = CH_DECIMALS.findById(currentDecimal.getId());
                if (freshDecimal != null && freshDecimal.getLastNumber() == reservedNumber) {
                    freshDecimal.setLastNumber(reservedNumber - 1);
                    boolean updated = CH_DECIMALS.update(freshDecimal);
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

    // ======================== ГЕТТЕРЫ ========================

    /**
     * Получение сохраненного паспорта.
     *
     * @return сохраненный паспорт или новый, если сохранение не удалось
     */
    public Passport getSavedPassport() {
        return savedPassport != null ? savedPassport : newPassport;
    }

    /**
     * Проверка успешного создания паспорта.
     *
     * @return true если паспорт создан, false в противном случае
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Проверка, был ли зарезервирован номер.
     *
     * @return true если номер был зарезервирован, false в противном случае
     */
    public boolean isNumberReserved() {
        return numberReserved;
    }

    /**
     * Получение зарезервированного порядкового номера.
     *
     * @return зарезервированный номер
     */
    public int getReservedNumber() {
        return reservedNumber;
    }

    /**
     * Проверка, была ли операция отменена пользователем.
     *
     * @return true если операция отменена, false в противном случае
     */
    public boolean isCancelled() {
        return cancelled;
    }
}