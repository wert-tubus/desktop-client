package ru.wert.tubus.chogori.entities.decimals;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.IFormView;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.winform.enums.EOperation;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.ArrayList;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;

/**
 * Контроллер окна добавления/изменения десятичного классификатора (Decimal)
 * Обрабатывает ввод данных и взаимодействие с пользователем
 */
public class Decimal_ACCController extends FormView_ACCController<Decimal> {

    @FXML
    private TextField tfName;

    @FXML
    private TextArea taDescription;

    @FXML
    private TextField tfInitialNumber;

    @FXML
    private StackPane spIndicator;

    @FXML
    private Button btnOk;

    //Диапазон изменяемого децимального номера
    private Integer initialNumberOfOldItem;
    private Integer lastNumberOfOldItem;

    /**
     * Обработчик нажатия кнопки "Отмена"
     * @param event событие нажатия
     */
    @FXML
    void cancel(ActionEvent event) {
        super.cancelPressed(event);
    }

    /**
     * Обработчик нажатия кнопки "ОК"
     * @param event событие нажатия
     */
    @FXML
    void ok(ActionEvent event) {
        super.okPressed(event, spIndicator, btnOk);
    }

    /**
     * Инициализация контроллера
     * Создает индикатор загрузки и добавляет слушатель форматирования
     */
    @FXML
    void initialize() {
        AppStatic.createSpIndicator(spIndicator);

        // Добавляем слушатель для автоматического форматирования поля tfInitialNumber
        tfInitialNumber.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                // Удаляем все нецифровые символы
                String digits = newValue.replaceAll("\\D", "");
                // Ограничиваем длину 3 цифрами
                if (digits.length() > 3) {
                    digits = digits.substring(0, 3);
                }
                // Обновляем текст, если он изменился
                if (!digits.equals(newValue)) {
                    tfInitialNumber.setText(digits);
                }
            }
        });
    }

    /**
     * Инициализация окна с заданными параметрами
     * @param operation тип операции (добавление/изменение)
     * @param formView представление формы
     * @param commands команды для выполнения
     */
    @Override
    public void init(EOperation operation, IFormView<Decimal> formView, ItemCommands<Decimal> commands) {
        super.initSuper(operation, formView, commands, CH_DECIMALS);
        setInitialValues();
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
     * Создает новый объект Decimal из данных формы
     * @return новый Decimal
     */
    @Override
    public Decimal getNewItem() {
        // Парсим число из строки формата ХХХ
        Integer initialNumber = parseInteger(tfInitialNumber.getText().trim());

        return new Decimal(
                tfName.getText().trim(),
                taDescription.getText(),
                initialNumber,
                initialNumber  // currentNumber = initialNumber при создании
        );
    }

    /**
     * Возвращает старый объект для операции изменения
     * @return старый Decimal
     */
    @Override
    public Decimal getOldItem() {
        return formView.getAllSelectedItems().get(0);
    }

    /**
     * Заполняет поля формы данными из выбранного объекта
     * @param oldItem объект Decimal для редактирования
     */
    @Override
    public void fillFieldsOnTheForm(Decimal oldItem) {
        initialNumberOfOldItem = oldItem.getInitialNumber();
        lastNumberOfOldItem = oldItem.getLastNumber();

        tfName.setText(oldItem.getName());
        taDescription.setText(oldItem.getDescription());

        // Преобразуем Integer в строку формата ХХХ (с ведущими нулями)
        if (oldItem.getInitialNumber() != null) {
            tfInitialNumber.setText(String.format("%03d", oldItem.getInitialNumber()));
        } else {
            tfInitialNumber.setText("");
        }

    }

    /**
     * Обновляет поля старого объекта данными из формы
     * @param oldItem объект Decimal для обновления
     */
    @Override
    public void changeOldItemFields(Decimal oldItem) {
        oldItem.setName(tfName.getText().trim());
        oldItem.setDescription(taDescription.getText());
        if(lastNumberOfOldItem == null)
            oldItem.setInitialNumber(parseInteger(tfInitialNumber.getText()));
    }

    /**
     * Очищает форму для создания нового объекта
     */
    @Override
    public void showEmptyForm() {
        tfName.setText("");
        taDescription.setText("");
        tfInitialNumber.setText("");
    }

    /**
     * Проверяет корректность введенных данных
     * @return true если данные корректны, false в противном случае
     */
    @Override
    public boolean enteredDataCorrect() {
        // Проверяем, что имя не пустое и соответствует маске XXXXXX (6 цифр)
        String name = tfName.getText().trim();
        if (name.isEmpty() || !name.matches("\\d{6}")) {
            Warning1.create("Внимание!",
                    "Децимальный номер отсутствует или\nне соответствует маске ХХХХХХ (6 цифр)",
                    "Введите корректный децимальный номер");
            return false;
        }

        // Проверяем начальное значение: не пустое, является числом и соответствует маске ХХХ (3 цифры)
        String initialNumberText = tfInitialNumber.getText().trim();
        if (initialNumberText.isEmpty() || !initialNumberText.matches("\\d{3}")) {
            Warning1.create("Внимание!",
                    "Начальное значение отсутствует или\nне соответствует маске ХХХ (3 цифры)",
                    "Введите корректное начальное значение");
            return false;
        }

        // Проверяем, что число корректно парсится
        Integer initialNumber = parseInteger(initialNumberText);
        if (initialNumber == null) {
            return false;
        }

        return true;
    }

    /**
     * Вспомогательный метод для парсинга целых чисел
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
            return null;
        }
    }
}
