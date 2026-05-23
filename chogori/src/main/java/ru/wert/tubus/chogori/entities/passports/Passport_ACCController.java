package ru.wert.tubus.chogori.entities.passports;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.components.BXUsers;
import ru.wert.tubus.chogori.setteings.ChogoriSettings;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.IFormView;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.client.entity.models.User;
import ru.wert.tubus.winform.enums.EOperation;
import ru.wert.tubus.winform.warnings.Warning1;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_USERS;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_USER_GROUPS;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;
import static ru.wert.tubus.chogori.statics.AppStatic.capitalizeFirstLetter;
import static ru.wert.tubus.winform.warnings.WarningMessages.*;

/**
 * Контроллер окна добавления/изменения паспорта (Passport)
 * Обрабатывает ввод данных и взаимодействие с пользователем
 */
@Slf4j
public class Passport_ACCController extends FormView_ACCController<Passport> {

    @FXML
    private TextField tfNumber;  // Децимальный номер

    @FXML
    private TextField tfName;      // Наименование

    @FXML
    private TextArea taProduct;             // Изделие (note)

    @FXML
    private ComboBox<User> bxUser;               // Разработчик (userName)

    @FXML
    private TextField tfDate;               // Дата

    @FXML
    private StackPane spIndicator;

    @FXML
    private Button btnOk;

    // Для хранения старого номера при редактировании
    private String oldNumber;
    private Prefix oldPrefix;

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
     * Создает индикатор загрузки и добавляет слушатели форматирования
     */
    @FXML
    void initialize() {
        AppStatic.createSpIndicator(spIndicator);
        new BXUsers(bxUser, CH_USER_GROUPS.findByName("Конструктор"));
        bxUser.setStyle("-fx-font-size: 14px; -fx-background-color: white");

        // Валидация поля Наименование (не может быть пустым)
        tfName.textProperty().addListener((observable, oldValue, newValue) -> {
            btnOk.setDisable(newValue == null || newValue.trim().isEmpty());
        });


    }

    /**
     * Инициализация окна с заданными параметрами
     * @param operation тип операции (добавление/изменение)
     * @param formView представление формы
     * @param commands команды для выполнения
     */
    @Override
    public void init(EOperation operation, IFormView<Passport> formView, ItemCommands<Passport> commands) {
        super.initSuper(operation, formView, commands, ChogoriServices.CH_QUICK_PASSPORTS);
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
     * Создает новый объект Passport из данных формы
     * @return новый Passport
     */
    @Override
    public Passport getNewItem() {
        Passport passport = new Passport();

        // Устанавливаем префикс и номер из децимального номера
        String fullNumber = tfNumber.getText().trim();
        if (fullNumber != null && !fullNumber.isEmpty()) {
            if (fullNumber.contains(".")) {
                String[] parts = fullNumber.split("\\.");
                if (parts.length == 2) {
                    Prefix prefix = ChogoriServices.CH_PREFIXES.findByName(parts[0]);
                    if (prefix == null) {
                        prefix = new Prefix();
                        prefix.setName(parts[0]);
                    }
                    passport.setPrefix(prefix);
                    passport.setNumber(parts[1]);
                } else {
                    setEmptyPrefix(passport);
                    passport.setNumber(fullNumber);
                }
            } else {
                setEmptyPrefix(passport);
                passport.setNumber(fullNumber);
            }
        }

        passport.setName(capitalizeFirstLetter(tfName.getText().trim()));
        passport.setNote(capitalizeFirstLetter(taProduct.getText().trim()));
        passport.setUserName(bxUser.getSelectionModel().getSelectedItem().getName());
        passport.setDate(tfDate.getText());

        return passport;
    }

    /**
     * Устанавливает пустой префикс ("-")
     * @param passport паспорт
     */
    private void setEmptyPrefix(Passport passport) {
        Prefix emptyPrefix = ChogoriServices.CH_PREFIXES.findByName("-");
        if (emptyPrefix == null) {
            emptyPrefix = new Prefix();
            emptyPrefix.setName("-");
        }
        passport.setPrefix(emptyPrefix);
    }

    /**
     * Возвращает старый объект для операции изменения
     * @return старый Passport
     */
    @Override
    public Passport getOldItem() {
        return formView.getAllSelectedItems().get(0);
    }

    /**
     * Заполняет поля формы данными из выбранного объекта
     * @param oldItem объект Passport для редактирования
     */
    @Override
    public void fillFieldsOnTheForm(Passport oldItem) {
        if (oldItem == null) return;

        // Сохраняем старый номер и префикс для проверок
        oldPrefix = oldItem.getPrefix();
        oldNumber = oldItem.getNumber();

        // Заполняем поле децимального номера
        if (oldItem.getNumber() != null) {
            String prefix = (oldItem.getPrefix() != null && !"-".equals(oldItem.getPrefix().getName()))
                    ? oldItem.getPrefix().getName() + "." : "";
            tfNumber.setText(prefix + oldItem.getNumber());
            tfNumber.setEditable(false); // Номер нельзя менять при редактировании
        }

        // Заполняем поле наименования
        if (oldItem.getName() != null) {
            tfName.setText(oldItem.getName());
        }

        // Заполняем поле изделия (note)
        if (oldItem.getNote() != null) {
            taProduct.setText(oldItem.getNote());
        }

        // Заполняем поле разработчика
        if (oldItem.getUserName() != null) {
            bxUser.getSelectionModel().select(CH_USERS.findByName(oldItem.getUserName()));
        } else {
            setCurrentUser();
        }

        // Заполняем поле даты
        if (oldItem.getDate() != null) {
            tfDate.setText(oldItem.getDate());
        } else {
            setCurrentDate();
        }
    }

    /**
     * Обновляет поля старого объекта данными из формы
     * @param oldItem объект Passport для обновления
     */
    @Override
    public void changeOldItemFields(Passport oldItem) {
        if (oldItem == null) return;

        oldItem.setName(capitalizeFirstLetter(tfName.getText().trim()));
        oldItem.setNote(capitalizeFirstLetter(taProduct.getText().trim()));
        oldItem.setUserName(bxUser.getSelectionModel().getSelectedItem().getName());
        oldItem.setDate(tfDate.getText());

        // Префикс и номер не меняем при редактировании
        oldItem.setPrefix(oldPrefix);
        oldItem.setNumber(oldNumber);
    }

    /**
     * Очищает форму для создания нового объекта
     */
    @Override
    public void showEmptyForm() {
        tfNumber.clear();
        tfNumber.setEditable(true);
        tfName.clear();
        taProduct.clear();
        setCurrentUser();
        setCurrentDate();
        btnOk.setDisable(true);
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
            Warning1.create($ATTENTION,
                    "Поле наименования не заполнено!",
                    "Заполните наименование");
            return false;
        }

        // Проверяем, что наименование не превышает допустимую длину
        if (name.length() > 250) {
            Warning1.create($ATTENTION,
                    "Наименование превышает допустимую длину в 250 символов",
                    "Текущая длина: " + name.length() + " символов. Сократите наименование");
            return false;
        }

        // Проверяем, что описание (изделие) не превышает допустимую длину
        String product = taProduct.getText();
        if (product != null && product.length() > 500) {
            Warning1.create($ATTENTION,
                    "Описание изделия превышает допустимую длину в 500 символов",
                    "Текущая длина: " + product.length() + " символов. Сократите описание");
            return false;
        }

        return true;
    }

    /**
     * Устанавливает текущего пользователя в поле разработчика
     */
    private void setCurrentUser() {
        if (ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER != null
                && ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER.getName() != null) {
            bxUser.getSelectionModel().select(CH_USERS.findByName(CH_CURRENT_USER.getName()));
        } else {
            bxUser.getSelectionModel().select(null);
        }
    }

    /**
     * Устанавливает текущую дату в поле даты
     */
    private void setCurrentDate() {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        tfDate.setText(currentDate.format(formatter));
    }


}
