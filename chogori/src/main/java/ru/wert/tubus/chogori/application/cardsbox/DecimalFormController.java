package ru.wert.tubus.chogori.application.cardsbox;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.IFormView;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.winform.enums.EOperation;
import ru.wert.tubus.winform.warnings.Warning1;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import static java.lang.String.format;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;
import static ru.wert.tubus.chogori.statics.AppStatic.closeWindow;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;
import static ru.wert.tubus.winform.warnings.WarningMessages.$SERVER_IS_NOT_AVAILABLE_MAYBE;

/**
 * Контроллер формы добавления/изменения десятичного классификатора (Decimal).
 * Отвечает за создание, редактирование и сохранение десятичных классификаторов.
 */
@Slf4j
public class DecimalFormController extends FormView_ACCController<Decimal> implements Initializable {

    // ======================== FXML КОМПОНЕНТЫ ========================

    @FXML@Getter
    private TextField tfName;              // Поле для децимального номера (6 цифр)

    @FXML@Getter
    private TextArea taDescription;        // Поле для описания

    @FXML
    private TextField tfInitialNumber;     // Поле для начального номера диапазона (3 цифры)

    @FXML
    private Button btnOk;                  // Кнопка подтверждения

    @FXML
    private Button btnCancel;              // Кнопка отмены

    @FXML
    private StackPane spIndicator;         // Индикатор загрузки

    // ======================== ПОЛЯ СОСТОЯНИЯ ========================

    private Decimal newDecimal;            // Новый классификатор до сохранения
    private Decimal savedDecimal;          // Сохраненный классификатор (после сохранения в БД)
    private boolean accepted = false;      // Флаг подтверждения создания
    private boolean editMode = false;      // Режим редактирования
    private Decimal editingDecimal;        // Редактируемый классификатор

    // Диапазон изменяемого децимального номера (для отката при ошибке)
    private Integer initialNumberOfOldItem;
    private Integer lastNumberOfOldItem;

    // Константы
    public static final String SKETCH = "Эскиз";
    private static final int NAME_MAX_LENGTH = 6;
    private static final int INITIAL_NUMBER_MAX_LENGTH = 3;
    private static final int DESCRIPTION_MAX_LENGTH = 250;

    // ======================== ИНИЦИАЛИЗАЦИЯ ========================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Настройка обработчиков кнопок
        setupButtonHandlers();

        // Настройка валидации и форматирования полей
        setupValidationAndFormatting();
    }

    /**
     * Настройка обработчиков событий для кнопок.
     */
    private void setupButtonHandlers() {
        btnOk.setOnAction(event -> {
            if (editMode) {
                okPressedForEdit(event, spIndicator, btnOk);
            } else {
                okPressed(event, spIndicator, btnOk);
            }
        });
        btnCancel.setOnAction(event -> cancelPressed(event));
    }

    /**
     * Настройка валидации и форматирования полей.
     * Кнопка подтверждения блокируется, если обязательные поля не заполнены.
     */
    private void setupValidationAndFormatting() {
        // Создание индикатора загрузки
        if (spIndicator != null) {
            // Индикатор создается через вспомогательный метод при необходимости
            // AppStatic.createSpIndicator(spIndicator);
        }

        // Форматирование поля tfName: только цифры, максимум 6 символов
        tfName.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                String digits = newValue.replaceAll("\\D", "");
                if (digits.length() > NAME_MAX_LENGTH) {
                    digits = digits.substring(0, NAME_MAX_LENGTH);
                }
                if (!digits.equals(newValue)) {
                    tfName.setText(digits);
                }
            }

            // Блокировка кнопки ОК при пустом обязательном поле
            updateOkButtonState();
        });

        // Форматирование поля tfInitialNumber: только цифры, максимум 3 символа
        tfInitialNumber.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                String digits = newValue.replaceAll("\\D", "");
                if (digits.length() > INITIAL_NUMBER_MAX_LENGTH) {
                    digits = digits.substring(0, INITIAL_NUMBER_MAX_LENGTH);
                }
                if (!digits.equals(newValue)) {
                    tfInitialNumber.setText(digits);
                }
            }

            // Блокировка кнопки ОК при пустом обязательном поле
            updateOkButtonState();
        });

        // Начальное состояние кнопки
        updateOkButtonState();
    }

    /**
     * Обновление состояния кнопки ОК в зависимости от заполнения обязательных полей.
     */
    private void updateOkButtonState() {
        boolean nameEmpty = tfName.getText() == null || tfName.getText().trim().isEmpty();
        boolean initialNumberEmpty = tfInitialNumber.getText() == null || tfInitialNumber.getText().trim().isEmpty();
        btnOk.setDisable(nameEmpty || initialNumberEmpty);
    }

    // ======================== ИНИЦИАЛИЗАЦИЯ ОПЕРАЦИИ ========================

    /**
     * Инициализация окна с заданными параметрами.
     *
     * @param operation тип операции (ADD или CHANGE)
     * @param formView  представление формы
     * @param commands  команды для выполнения
     */
    @Override
    public void init(EOperation operation, IFormView<Decimal> formView, ItemCommands<Decimal> commands) {
        super.initSuper(operation, formView, commands, CH_DECIMALS);
        setInitialValues();
    }

    /**
     * Установка начальных значений для формы.
     */
    public void setInitialValues() {
        if (!editMode && operation == EOperation.CHANGE && formView != null) {
            // Режим редактирования
            editMode = true;
            editingDecimal = formView.getAllSelectedItems().get(0);

            if (editingDecimal != null && SKETCH.equals(editingDecimal.getName())) {
                Warning1.create("Внимание!",
                        "Эскиз не подлежит изменению!",
                        "Закройте окно.");
                Platform.runLater(() -> {
                    if (tfName.getScene() != null) {
                        tfName.getScene().getWindow().hide();
                    }
                });
                return;
            }

            fillFormFieldsForEdit();
        } else {
            // Режим создания
            editMode = false;
            showEmptyForm();
        }
    }

    // ======================== УСТАНОВКА ДАННЫХ ========================

    /**
     * Установка данных для создания нового классификатора.
     *
     * @param decimal предварительно созданный классификатор (опционально)
     */
    public void setData(Decimal decimal) {
        this.editMode = false;
        this.newDecimal = decimal != null ? decimal : new Decimal();
        fillFormFields();
        updateOkButtonState();

        log.debug("Данные формы инициализированы для создания");
    }

    /**
     * Установка значений редактируемой децимальной группы
     * @param decimal редактируемая децимальная группа
     */
    public void setDataToEdit(Decimal decimal) {
        this.editMode = true;
        this.editingDecimal = decimal;  // <-- ДОБАВИТЬ ЭТУ СТРОКУ
        this.newDecimal = decimal != null ? decimal : new Decimal();
        fillFormFields();
        updateOkButtonState();

        log.debug("Данные формы инициализированы для редактирования");
    }

    // ======================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ========================

    /**
     * Заполнение полей формы для нового классификатора.
     */
    private void fillFormFields() {
        if (newDecimal != null) {
            if (newDecimal.getName() != null) {
                tfName.setText(newDecimal.getName());
            } else {
                tfName.clear();
            }

            if (newDecimal.getDescription() != null) {
                taDescription.setText(newDecimal.getDescription());
            } else {
                taDescription.clear();
            }

            if (newDecimal.getInitialNumber() != null) {
                tfInitialNumber.setText(String.format("%03d", newDecimal.getInitialNumber()));
            } else {
                tfInitialNumber.clear();
            }
        } else {
            showEmptyForm();
        }

        log.debug("Поля формы заполнены для создания");
    }

    /**
     * Заполнение полей формы для редактирования существующего классификатора.
     */
    private void fillFormFieldsForEdit() {
        if (editingDecimal == null) return;

        // Заполнение поля децимального номера (не редактируется)
        if (editingDecimal.getName() != null) {
            tfName.setText(editingDecimal.getName());
            tfName.setEditable(false);  // Оставляем заблокированным
        }

        // Заполнение поля описания
        if (editingDecimal.getDescription() != null) {
            taDescription.setText(editingDecimal.getDescription());
        } else {
            taDescription.clear();
        }

        // Заполнение поля начального номера - РАЗРЕШАЕМ РЕДАКТИРОВАНИЕ
        if (editingDecimal.getInitialNumber() != null) {
            tfInitialNumber.setText(String.format("%03d", editingDecimal.getInitialNumber()));
            tfInitialNumber.setEditable(true);  // <-- ИЗМЕНИТЬ НА true
        } else {
            tfInitialNumber.clear();
            tfInitialNumber.setEditable(true);
        }

        log.debug("Поля формы заполнены для редактирования: name={}, initialNumber={}",
                editingDecimal.getName(), editingDecimal.getInitialNumber());
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
        notNullFields.add(tfInitialNumber.getText());
        return notNullFields;
    }

    /**
     * Создает новый объект Decimal из данных формы.
     *
     * @return новый Decimal
     */
    @Override
    public Decimal getNewItem() {
        String name = tfName.getText().trim();
        String description = taDescription.getText();
        Integer initialNumber = parseInteger(tfInitialNumber.getText().trim());

        if (newDecimal == null) {
            newDecimal = new Decimal();
        }

        newDecimal.setName(name);
        newDecimal.setDescription(description);
        newDecimal.setInitialNumber(initialNumber);

        // При создании lastNumber равен initialNumber
        if (!editMode && initialNumber != null) {
            newDecimal.setLastNumber(initialNumber);
        }

        return newDecimal;
    }

    /**
     * Возвращает старый объект для операции изменения.
     *
     * @return старый Decimal
     */
    @Override
    public Decimal getOldItem() {
        return editMode ? editingDecimal : null;
    }

    /**
     * Заполняет поля формы данными из выбранного объекта.
     *
     * @param oldItem объект Decimal для редактирования
     */
    @Override
    public void fillFieldsOnTheForm(Decimal oldItem) {
        if (oldItem != null) {
            fillFormFieldsForEdit();
        }
    }

    /**
     * Обновляет поля старого объекта данными из формы.
     *
     * @param oldItem объект Decimal для обновления
     */
    @Override
    public void changeOldItemFields(Decimal oldItem) {
        if (oldItem == null) return;

        oldItem.setName(tfName.getText().trim());
        oldItem.setDescription(taDescription.getText());

        // При редактировании начальный номер не меняется, если диапазон уже использовался
        if (lastNumberOfOldItem == null || lastNumberOfOldItem.equals(initialNumberOfOldItem)) {
            oldItem.setInitialNumber(parseInteger(tfInitialNumber.getText()));
        }
    }

    /**
     * Очищает форму для создания нового объекта.
     */
    @Override
    public void showEmptyForm() {
        tfName.clear();
        taDescription.clear();
        tfInitialNumber.clear();

        // В режиме создания поля должны быть редактируемыми
        tfName.setEditable(true);
        tfInitialNumber.setEditable(true);

        log.debug("Форма очищена для создания нового классификатора");
    }

    /**
     * Проверяет корректность введенных данных.
     *
     * @return true если данные корректны, false в противном случае
     */
    @Override
    public boolean enteredDataCorrect() {
        // Проверяем, что имя не пустое и соответствует маске XXXXXX (6 цифр)
        String name = tfName.getText().trim();
        if (name.isEmpty() || !name.matches("\\d{6}")) {
            Warning1.create($ATTENTION,
                    "Децимальный номер отсутствует или\nне соответствует маске ХХХХХХ (6 цифр)",
                    "Введите корректный децимальный номер");
            return false;
        }

        // Проверяем начальное значение: не пустое, является числом и соответствует маске ХХХ (3 цифры)
        String initialNumberText = tfInitialNumber.getText().trim();
        if (initialNumberText.isEmpty() || !initialNumberText.matches("\\d{3}")) {
            Warning1.create($ATTENTION,
                    "Начальное значение отсутствует или\nне соответствует маске ХХХ (3 цифры)",
                    "Введите корректное начальное значение");
            return false;
        }

        // Проверяем, что описание не превышает максимальную длину
        String description = taDescription.getText();
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            Warning1.create($ATTENTION,
                    "Описание превышает допустимую длину в " + DESCRIPTION_MAX_LENGTH + " символов",
                    "Текущая длина: " + description.length() + " символов. Сократите описание");
            return false;
        }

        // Проверяем, что число корректно парсится
        Integer initialNumber = parseInteger(initialNumberText);
        if (initialNumber == null) {
            Warning1.create($ATTENTION,
                    "Начальное значение не является корректным числом",
                    "Введите корректное начальное значение");
            return false;
        }

        // При создании проверяем уникальность имени
        if (!editMode) {
            Decimal existingDecimal = CH_DECIMALS.findByName(name);
            if (existingDecimal != null) {
                Warning1.create($ATTENTION,
                        format("Децимальный классификатор с номером %s уже существует", name),
                        "Используйте другой номер");
                return false;
            }
        } else {
            // При редактировании проверяем, что имя не изменилось на уже существующее
            if (editingDecimal != null && !editingDecimal.getName().equals(name)) {
                Decimal existingDecimal = CH_DECIMALS.findByName(name);
                if (existingDecimal != null) {
                    Warning1.create($ATTENTION,
                            format("Децимальный классификатор с номером %s уже существует", name),
                            "Используйте другой номер");
                    return false;
                }
            }
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
            Decimal updatedDecimal = getNewItem();
            updatedDecimal.setId(editingDecimal.getId());
            updatedDecimal.setLastNumber(editingDecimal.getLastNumber()); // Сохраняем текущий lastNumber

            // Запуск задачи обновления
            updateTask(event, spIndicator, btnOk, updatedDecimal);
        }
    }

    /**
     * Задача обновления классификатора в отдельном потоке.
     */
    private void updateTask(javafx.event.Event event, StackPane spIndicator, Button btnOk, Decimal decimalToUpdate) {
        if (spIndicator != null) {
            spIndicator.setVisible(true);
        }
        if (btnOk != null) {
            btnOk.setDisable(true);
        }

        Task<Void> update = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                log.info("Обновляем десятичный классификатор: ID={}, name={}, initialNumber={}",
                        decimalToUpdate.getId(), decimalToUpdate.getName(), decimalToUpdate.getInitialNumber());

                try {
                    boolean updated = CH_DECIMALS.update(decimalToUpdate);

                    if (updated) {
                        log.info("Классификатор успешно обновлен с ID: {}", decimalToUpdate.getId());
                        savedDecimal = decimalToUpdate;
                        accepted = true;

                        Platform.runLater(() -> {
                            if (event != null) {
                                closeWindow(event);
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            Warning1.create($ATTENTION,
                                    format("Не удалось обновить классификатор \n%s", decimalToUpdate.toUsefulString()),
                                    $SERVER_IS_NOT_AVAILABLE_MAYBE);
                        });
                        throw new RuntimeException("Не удалось обновить классификатор в базе данных");
                    }
                } catch (Exception e) {
                    log.error("Ошибка при обновлении классификатора", e);
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
     * Переопределенный метод okPressed для сохранения классификатора.
     */
    @Override
    protected void okPressed(javafx.event.Event event, StackPane spIndicator, Button btnOk) {
        if (notNullFieldEmpty()) {
            Warning1.create($ATTENTION, "Некоторые поля не заполнены!", "Заполните все поля");
            return;
        }

        if (enteredDataCorrect()) {
            Decimal decimalToSave = getNewItem();

            // Проверка наличия классификатора в базе данных
            Decimal foundDecimal = CH_DECIMALS.findByName(decimalToSave.getName());

            if (foundDecimal != null) {
                savedDecimal = foundDecimal;
                log.warn("Классификатор уже существует в базе: {}", foundDecimal.toUsefulString());
                Warning1.create($ATTENTION,
                        format("Классификатор уже существует в базе данных:\n%s", foundDecimal.toUsefulString()),
                        "Повторное создание невозможно");
                return;
            }

            // Запуск задачи сохранения
            saveDecimalTask(event, spIndicator, btnOk, decimalToSave);
        }
    }

    /**
     * Задача сохранения классификатора в отдельном потоке.
     */
    private void saveDecimalTask(javafx.event.Event event, StackPane spIndicator, Button btnOk, Decimal decimalToSave) {
        if (spIndicator != null) {
            spIndicator.setVisible(true);
        }
        if (btnOk != null) {
            btnOk.setDisable(true);
        }

        Task<Void> saveDecimal = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                log.info("Сохраняем десятичный классификатор: name={}, initialNumber={}, description={}",
                        decimalToSave.getName(), decimalToSave.getInitialNumber(), decimalToSave.getDescription());

                try {
                    savedDecimal = CH_DECIMALS.save(decimalToSave);

                    if (savedDecimal != null && savedDecimal.getId() != null) {
                        log.info("Классификатор успешно сохранен с ID: {}", savedDecimal.getId());
                        accepted = true;

                        Platform.runLater(() -> {
                            if (event != null) {
                                closeWindow(event);
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            Warning1.create($ATTENTION,
                                    format("Не удалось создать классификатор \n%s", decimalToSave.toUsefulString()),
                                    $SERVER_IS_NOT_AVAILABLE_MAYBE);
                        });
                        throw new RuntimeException("Не удалось сохранить классификатор в базе данных");
                    }
                } catch (Exception e) {
                    log.error("Ошибка при сохранении классификатора", e);
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

        new Thread(saveDecimal).start();
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
     * Вспомогательный метод для парсинга целых чисел.
     *
     * @param text текст для парсинга
     * @return Integer или null если текст пустой или не число
     */
    private Integer parseInteger(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            log.warn("Ошибка парсинга числа: {}", text, e);
            return null;
        }
    }

    // ======================== ГЕТТЕРЫ ========================

    /**
     * Получение сохраненного классификатора.
     *
     * @return сохраненный классификатор или новый, если сохранение не удалось
     */
    public Decimal getSavedDecimal() {
        return savedDecimal != null ? savedDecimal : newDecimal;
    }

    /**
     * Проверка успешного создания классификатора.
     *
     * @return true если классификатор создан, false в противном случае
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Проверка, находится ли форма в режиме редактирования.
     *
     * @return true если режим редактирования, false в противном случае
     */
    public boolean isEditMode() {
        return editMode;
    }

    /**
     * Получение редактируемого классификатора.
     *
     * @return редактируемый классификатор или null
     */
    public Decimal getEditingDecimal() {
        return editingDecimal;
    }
}
