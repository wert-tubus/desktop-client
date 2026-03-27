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
     * Создает индикатор загрузки
     */
    @FXML
    void initialize() {
        AppStatic.createSpIndicator(spIndicator);
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
        return new Decimal(
                tfName.getText().trim(),
                taDescription.getText(),
                parseInteger(tfInitialNumber.getText()),
                parseInteger(tfInitialNumber.getText())
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
        Decimal oldDecimal = (Decimal) oldItem;
        tfName.setText(oldDecimal.getName());
        taDescription.setText(oldDecimal.getDescription());
        tfInitialNumber.setText(oldDecimal.getInitialNumber() != null ? oldDecimal.getInitialNumber().toString() : "");
    }

    /**
     * Обновляет поля старого объекта данными из формы
     * @param oldItem объект Decimal для обновления
     */
    @Override
    public void changeOldItemFields(Decimal oldItem) {
        Decimal oldDecimal = (Decimal) oldItem;
        oldDecimal.setName(tfName.getText().trim());
        oldDecimal.setDescription(taDescription.getText());
        oldDecimal.setInitialNumber(parseInteger(tfInitialNumber.getText()));
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
        // Проверяем, что имя не пустое
        if (tfName.getText().trim().isEmpty()) {
            return false;
        }

        // Проверяем, что если указаны начальный и конечный номера, то начальный не больше конечного
        Integer initial = parseInteger(tfInitialNumber.getText());

        return initial == null;
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
