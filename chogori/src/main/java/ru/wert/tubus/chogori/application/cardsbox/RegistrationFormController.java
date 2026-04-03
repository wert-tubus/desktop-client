// RegistrationFormController.java (добавлен метод setDataForEdit)
package ru.wert.tubus.chogori.application.cardsbox;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
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
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_USERS;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_USER_GROUPS;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;
import static ru.wert.tubus.chogori.statics.AppStatic.capitalizeFirstLetter;
import static ru.wert.tubus.chogori.statics.AppStatic.closeWindow;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;
import static ru.wert.tubus.winform.warnings.WarningMessages.$SERVER_IS_NOT_AVAILABLE_MAYBE;

/**
 * Контроллер формы регистрации нового паспорта
 * Отвечает за создание и сохранение паспортов ПИК и эскизов
 */
@Slf4j
public class RegistrationFormController extends FormView_ACCController<Passport> implements Initializable {

    @FXML
    private TextField tfNumber;

    @FXML
    private TextField tfName;

    @FXML
    private TextArea taProduct;

    @FXML
    private ComboBox<User> bxUser;

    @FXML
    private TextField tfDate;

    @FXML
    private Button btnAccept;

    @FXML
    private Button btnCancel;

    @FXML
    private StackPane spIndicator;

    private Passport newPassport;           // Новый паспорт до сохранения
    private Passport savedPassport;         // Сохраненный паспорт (после сохранения в БД)
    private boolean accepted = false;       // Флаг подтверждения создания
    private String passportType;            // Тип паспорта: "PIK" или "SKETCH"
    private Decimal currentDecimal;         // Текущая децимальная группа
    private int reservedNumber;             // Зарезервированный порядковый номер
    private boolean numberReserved = false; // Флаг резервирования номера

    private Prefix pikPrefix;               // Префикс "ПИК" из базы данных
    private Prefix sketchPrefix;            // Префикс "-" из базы данных
    private boolean cancelled = false;      // Флаг отмены
    private boolean editMode = false;       // Режим редактирования
    private Passport editingPassport;       // Редактируемый паспорт

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new BXUsers(bxUser, CH_USER_GROUPS.findByName("Конструктор"));

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
     * Настройка валидации полей
     */
    private void setupValidation() {
        // Валидация поля Наименование (не может быть пустым)
        tfName.textProperty().addListener((observable, oldValue, newValue) -> {
            btnAccept.setDisable(newValue == null || newValue.trim().isEmpty());
        });
    }

    /**
     * Инициализация окна с заданными параметрами
     * @param operation тип операции (всегда ADD для регистрации)
     * @param formView представление формы
     * @param commands команды для выполнения
     */
    @Override
    public void init(EOperation operation, IFormView<Passport> formView, ItemCommands<Passport> commands) {
        super.initSuper(operation, formView, commands, ChogoriServices.CH_QUICK_PASSPORTS);
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
        this.editMode = false;

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
     * Установка данных для редактирования существующего паспорта
     *
     * @param passport паспорт для редактирования
     */
    public void setDataForEdit(Passport passport) {
        this.editMode = true;
        this.editingPassport = passport;
        this.newPassport = passport;

        // Определяем тип паспорта
        if (passport.getPrefix() != null && "ПИК".equals(passport.getPrefix().getName())) {
            this.passportType = "PIK";
        } else {
            this.passportType = "SKETCH";
        }

        // Заполняем поля формы данными из паспорта
        fillFormFieldsForEdit();

        // Устанавливаем начальное состояние кнопки
        btnAccept.setDisable(tfName.getText() == null || tfName.getText().trim().isEmpty());

        log.debug("Режим редактирования: паспорт {}", passport.toUsefulString());
    }

    /**
     * Заполнение полей формы для редактирования
     */
    private void fillFormFieldsForEdit() {
        if (editingPassport == null) return;

        // Заполняем поле номера
        if (editingPassport.getNumber() != null) {
            tfNumber.setText(editingPassport.getNumber());
            tfNumber.setEditable(false); // Номер нельзя менять при редактировании
        }

        // Заполняем поле наименования
        if (editingPassport.getName() != null) {
            tfName.setText(editingPassport.getName());
        }

        // Заполняем поле изделия
        if (editingPassport.getNote() != null) {
            taProduct.setText(editingPassport.getNote());
        }

        // Заполняем поле разработчика
        if (editingPassport.getUserName() != null) {
            bxUser.getSelectionModel().select(CH_USERS.findByName(editingPassport.getUserName()));
        } else if (CH_CURRENT_USER != null && CH_CURRENT_USER.getName() != null) {
            bxUser.getSelectionModel().select(CH_USERS.findByName(CH_CURRENT_USER.getName()));
        } else {
            bxUser.getSelectionModel().select(null);
        }

        // Заполняем поле даты
        if (editingPassport.getDate() != null) {
            tfDate.setText(editingPassport.getDate());
        } else {
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            tfDate.setText(currentDate.format(formatter));
        }

        log.debug("Поля формы заполнены для редактирования: номер={}, разработчик={}, дата={}",
                tfNumber.getText(), bxUser.getSelectionModel().getSelectedItem().getName(), tfDate.getText());
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
            bxUser.getSelectionModel().select(CH_USERS.findByName(CH_CURRENT_USER.getName()));
        } else {
            bxUser.getSelectionModel().select(null);
        }

        // Заполняем поле даты текущей датой в формате "dd.MM.yy"
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        tfDate.setText(currentDate.format(formatter));

        log.debug("Поля формы заполнены: номер={}, разработчик={}, дата={}",
                tfNumber.getText(), bxUser.getSelectionModel().getSelectedItem().getName(), tfDate.getText());
    }

    /**
     * Возвращает список полей, которые не могут быть пустыми
     * @return ArrayList<String> список значений обязательных полей
     */
    @Override
    public ArrayList<String> getNotNullFields() {
        ArrayList<String> notNullFields = new ArrayList<>();
        notNullFields.add(tfName.getText());
        return notNullFields;
    }

    /**
     * Создает новый объект Passport из данных формы
     * @return новый Passport
     */
    @Override
    public Passport getNewItem() {
        // Заполняем данные нового паспорта из полей формы
        newPassport.setName(capitalizeFirstLetter(tfName.getText().trim()));
        newPassport.setNote(capitalizeFirstLetter(taProduct.getText().trim()));
        newPassport.setUserName(bxUser.getSelectionModel().getSelectedItem().getName());
        newPassport.setDate(tfDate.getText());

        return newPassport;
    }

    /**
     * Возвращает старый объект для операции изменения
     * @return старый Passport
     */
    @Override
    public Passport getOldItem() {
        return editMode ? editingPassport : null;
    }

    /**
     * Заполняет поля формы данными из выбранного объекта
     * @param oldItem объект Passport для редактирования
     */
    @Override
    public void fillFieldsOnTheForm(Passport oldItem) {
        // Не используется для операции добавления, используется для редактирования
        if (oldItem != null) {
            fillFormFieldsForEdit();
        }
    }

    /**
     * Обновляет поля старого объекта данными из формы
     * @param oldItem объект Passport для обновления
     */
    @Override
    public void changeOldItemFields(Passport oldItem) {
        if (oldItem != null) {
            oldItem.setName(capitalizeFirstLetter(tfName.getText().trim()));
            oldItem.setNote(capitalizeFirstLetter(taProduct.getText().trim()));
            oldItem.setUserName(bxUser.getSelectionModel().getSelectedItem().getName());
            oldItem.setDate(tfDate.getText());
        }
    }

    /**
     * Очищает форму для создания нового объекта
     */
    @Override
    public void showEmptyForm() {
        fillFormFields();
    }

    /**
     * Проверяет корректность введенных данных
     * @return true если данные корректны, false в противном случае
     */
    @Override
    public boolean enteredDataCorrect() {
        // Проверяем, что наименование не пустое
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            Warning1.create($ATTENTION, "Поле наименования не заполнено!", "Заполните наименование");
            return false;
        }

        return true;
    }

    /**
     * Обработка нажатия OK в режиме редактирования
     */
    protected void okPressedForEdit(javafx.event.Event event, StackPane spIndicator, Button btnOk) {
        if (notNullFieldEmpty()) {
            Warning1.create($ATTENTION, "Некоторые поля не заполнены!", "Заполните все поля");
            return;
        }

        if (enteredDataCorrect()) {
            Passport updatedPassport = getNewItem();
            updatedPassport.setId(editingPassport.getId());

            // Запускаем задачу обновления
            updateTask(event, spIndicator, btnOk, updatedPassport);
        }
    }

    /**
     * Задача обновления паспорта в отдельном потоке
     */
    private void updateTask(javafx.event.Event event, StackPane spIndicator, Button btnOk, Passport passportToUpdate) {
        if (spIndicator != null) {
            spIndicator.setVisible(true);
        }
        if (btnOk != null) {
            btnOk.setDisable(true);
        }

        javafx.concurrent.Task<Void> update = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                log.info("Обновляем паспорт: ID={}, номер={}, наименование={}",
                        passportToUpdate.getId(), passportToUpdate.getNumber(), passportToUpdate.getName());

                try {
                    boolean updated = ChogoriServices.CH_QUICK_PASSPORTS.update(passportToUpdate);

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
                if (spIndicator != null) {
                    spIndicator.setVisible(false);
                }
                if (btnOk != null) {
                    btnOk.setDisable(false);
                }
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                if (spIndicator != null) {
                    spIndicator.setVisible(false);
                }
                if (btnOk != null) {
                    btnOk.setDisable(false);
                }
            }

            @Override
            protected void failed() {
                super.failed();
                if (spIndicator != null) {
                    spIndicator.setVisible(false);
                }
                if (btnOk != null) {
                    btnOk.setDisable(false);
                }
            }
        };

        new Thread(update).start();
    }

    /**
     * Переопределенный метод okPressed для сохранения паспорта
     */
    @Override
    protected void okPressed(javafx.event.Event event, StackPane spIndicator, Button btnOk) {
        if (notNullFieldEmpty()) {
            Warning1.create($ATTENTION, "Некоторые поля не заполнены!", "Заполните все поля");
            return;
        }

        if (enteredDataCorrect()) {
            Passport passportToSave = getNewItem();

            // Проверяем наличие паспорта в базе данных
            Passport foundPassport = ChogoriServices.CH_QUICK_PASSPORTS.findByPrefixIdAndNumber(
                    passportToSave.getPrefix(),
                    passportToSave.getNumber()
            );

            if (foundPassport != null) {
                // Паспорт уже существует в базе
                savedPassport = foundPassport;
                log.warn("Паспорт уже существует в базе: {}", foundPassport.toUsefulString());
                Warning1.create($ATTENTION,
                        format("Паспорт уже существует в базе данных:\n%s", foundPassport.toUsefulString()),
                        "Повторная регистрация невозможна");
                return;
            }

            // Запускаем задачу сохранения
            manipulationTask(event, spIndicator, btnOk, passportToSave);
        }
    }

    /**
     * Задача сохранения паспорта в отдельном потоке
     */
    private void manipulationTask(javafx.event.Event event, StackPane spIndicator, Button btnOk, Passport passportToSave) {
        if (spIndicator != null) {
            spIndicator.setVisible(true);
        }
        if (btnOk != null) {
            btnOk.setDisable(true);
        }

        javafx.concurrent.Task<Void> manipulation = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                log.info("Сохраняем паспорт: тип={}, номер={}, наименование={}",
                        passportType, passportToSave.getNumber(), passportToSave.getName());

                try {
                    // Сохраняем паспорт
                    savedPassport = ChogoriServices.CH_QUICK_PASSPORTS.save(passportToSave);

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
                        // Сохранение не удалось
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
                if (spIndicator != null) {
                    spIndicator.setVisible(false);
                }
                if (btnOk != null) {
                    btnOk.setDisable(false);
                }
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                if (spIndicator != null) {
                    spIndicator.setVisible(false);
                }
                if (btnOk != null) {
                    btnOk.setDisable(false);
                }
                // При отмене выполняем откат номера
                if (numberReserved) {
                    rollbackNumber();
                }
            }

            @Override
            protected void failed() {
                super.failed();
                if (spIndicator != null) {
                    spIndicator.setVisible(false);
                }
                if (btnOk != null) {
                    btnOk.setDisable(false);
                }
            }
        };

        new Thread(manipulation).start();
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

    /**
     * Проверить, была ли операция отменена пользователем
     *
     * @return true если операция отменена, false в противном случае
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
