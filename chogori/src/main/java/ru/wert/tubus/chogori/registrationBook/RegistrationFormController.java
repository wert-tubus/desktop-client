package ru.wert.tubus.chogori.registrationBook;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.client.entity.models.User;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;

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

    private Passport newPassport;
    private Stage dialogStage;
    private boolean accepted = false;
    private String passportType; // "PIK" или "DRAFT"

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupButtonHandlers();
        setupValidation();
    }

    private void setupButtonHandlers() {
        btnAccept.setOnAction(event -> accept());
        btnCancel.setOnAction(event -> cancel());
    }

    private void setupValidation() {
        // Валидация поля Наименование (не может быть пустым)
        tfName.textProperty().addListener((observable, oldValue, newValue) -> {
            btnAccept.setDisable(newValue == null || newValue.trim().isEmpty());
        });
    }

    /**
     * Установка данных для нового паспорта
     * @param type тип паспорта ("PIK" или "DRAFT")
     * @param dialogStage диалоговое окно
     */
    public void setData(String type, Stage dialogStage) {
        this.passportType = type;
        this.dialogStage = dialogStage;

        // Создаем новый паспорт
        this.newPassport = createNewPassport();

        // Заполняем поля формы
        fillFormFields();

        // Устанавливаем начальное состояние кнопки
        btnAccept.setDisable(tfName.getText() == null || tfName.getText().trim().isEmpty());
    }

    /**
     * Создание нового паспорта
     */
    private Passport createNewPassport() {
        Passport passport = new Passport();

        // Устанавливаем префикс в зависимости от типа
        Prefix prefix = new Prefix();
        if ("PIK".equals(passportType)) {
            prefix.setName("ПИК");
            // Временный номер, позже будет заменен на реальный
            passport.setNumber("000000.000");
        } else {
            prefix.setName("-");
            // Временный номер, позже будет заменен на реальный
            passport.setNumber("Э00000");
        }
        passport.setPrefix(prefix);

        return passport;
    }

    /**
     * Заполнение полей формы
     */
    private void fillFormFields() {
        // Номер - временный
        if (newPassport.getNumber() != null) {
            tfNumber.setText(newPassport.getNumber());
        }

        // Наименование - пустое, ждет ввода
        tfName.clear();

        // Изделие - пустое, ждет ввода
        taProduct.clear();

        // Разработчик - текущий пользователь
        User currentUser = CH_CURRENT_USER;
        if (currentUser != null && currentUser.getName() != null) {
            tfDeveloper.setText(currentUser.getName());
        } else {
            tfDeveloper.setText("Неизвестный разработчик");
        }

        // Дата - текущая дата
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        tfDate.setText(currentDate.format(formatter));
    }

    /**
     * Принять изменения
     */
    private void accept() {
        try {
            // Заполняем данные нового паспорта
            newPassport.setName(tfName.getText().trim());
            newPassport.setNote(taProduct.getText().trim());

            accepted = true;
            dialogStage.close();
        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать паспорт: " + e.getMessage());
        }
    }

    /**
     * Отмена
     */
    private void cancel() {
        accepted = false;
        dialogStage.close();
    }

    /**
     * Получить созданный паспорт
     */
    public Passport getNewPassport() {
        return newPassport;
    }

    /**
     * Проверить, был ли паспорт создан
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Показать диалоговое окно с ошибкой
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
