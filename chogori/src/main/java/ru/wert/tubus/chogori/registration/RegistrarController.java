package ru.wert.tubus.chogori.registration;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;


public class RegistrarController implements Initializable {

    @FXML
    private Tab tpPIK;

    @FXML
    private Tab tpDrafts;

    @FXML
    private ListView<Passport> lvPIK;

    @FXML
    private ListView<Passport> lvDrafts;

    @FXML
    private ListView<Passport> lvListOFNumbers;

    @FXML
    private HBox hbButtons;

    @FXML
    private Button btnGetPIKNumber;

    @FXML
    private Button btnGetDraftNumber;

    @FXML
    private Button btnSaveAllNumbers;

    private ObservableList<Passport> allPassportsList;
    private ObservableList<Passport> pikPassportsList;
    private ObservableList<Passport> draftPassportsList;
    private ObservableList<Passport> selectedPassportsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Инициализируем список для выбранных паспортов
        selectedPassportsList = FXCollections.observableArrayList();
        lvListOFNumbers.setItems(selectedPassportsList);

        // Настраиваем отображение для lvListOFNumbers
        setupListViewDisplay(lvListOFNumbers);

        // Загружаем все паспорта
        loadAllPassports();

        // Заполняем списки
        fillPIKListView();
        fillDraftsListView();

        // Настраиваем обработчики кнопок
        setupButtonHandlers();

        // Настраиваем обработчики выбора в списках
        setupSelectionHandlers();
    }

    private void setupListViewDisplay(ListView<Passport> listView) {
        listView.setCellFactory(lv -> new ListCell<Passport>() {
            @Override
            protected void updateItem(Passport item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toUsefulString());
                }
            }
        });
    }

    private void loadAllPassports() {
        try {
            allPassportsList = FXCollections.observableArrayList(
                    ChogoriServices.CH_QUICK_PASSPORTS.findAll()
            );
        } catch (Exception e) {
            showError("Ошибка загрузки данных", "Не удалось загрузить список паспортов: " + e.getMessage());
            allPassportsList = FXCollections.observableArrayList();
        }
    }

    private void fillPIKListView() {
        // Фильтруем паспорта с префиксом "ПИК" и номером по маске "######.###"
        Pattern pikPattern = Pattern.compile("\\d{6}\\.\\d{3}");

        List<Passport> pikPassports = allPassportsList.stream()
                .filter(passport -> {
                    Prefix prefix = passport.getPrefix();
                    String number = passport.getNumber();
                    return prefix != null
                            && "ПИК".equals(prefix.getName())
                            && number != null
                            && pikPattern.matcher(number).matches();
                })
                .sorted(Comparator.comparing(Passport::getNumber))
                .collect(Collectors.toList());

        pikPassportsList = FXCollections.observableArrayList(pikPassports);
        lvPIK.setItems(pikPassportsList);
        setupListViewDisplay(lvPIK);
    }

    private void fillDraftsListView() {
        // Фильтруем паспорта с префиксом "-" или null и номером по маске "Э#####"
        Pattern draftPattern = Pattern.compile("Э\\d{5}");

        List<Passport> draftPassports = allPassportsList.stream()
                .filter(passport -> {
                    Prefix prefix = passport.getPrefix();
                    String number = passport.getNumber();
                    boolean prefixCondition = prefix == null || "-".equals(prefix.getName());
                    return prefixCondition
                            && number != null
                            && draftPattern.matcher(number).matches();
                })
                .sorted(Comparator.comparing(Passport::getNumber))
                .collect(Collectors.toList());

        draftPassportsList = FXCollections.observableArrayList(draftPassports);
        lvDrafts.setItems(draftPassportsList);
        setupListViewDisplay(lvDrafts);
    }

    private void setupButtonHandlers() {
        btnGetPIKNumber.setOnAction(event -> getPIKNumber());
        btnGetDraftNumber.setOnAction(event -> getDraftNumber());
        btnSaveAllNumbers.setOnAction(event -> saveAllNumbers());
    }

    private void setupSelectionHandlers() {
        // Обработчик выбора в списке ПИК
        lvPIK.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        lvDrafts.getSelectionModel().clearSelection();
                    }
                }
        );

        // Обработчик выбора в списке эскизов
        lvDrafts.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        lvPIK.getSelectionModel().clearSelection();
                    }
                }
        );
    }

    /**
     * Получить номер ПИК - создаем новый паспорт ПИК
     */
    private void getPIKNumber() {
        try {
            openCreateDialog("PIK", "Создание паспорта ПИК");
        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать паспорт ПИК: " + e.getMessage());
        }
    }

    /**
     * Получить эскизный номер - создаем новый эскизный паспорт
     */
    private void getDraftNumber() {
        try {
            openCreateDialog("DRAFT", "Создание эскизного паспорта");
        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать эскизный паспорт: " + e.getMessage());
        }
    }

    /**
     * Открыть диалог создания нового паспорта
     */
    private void openCreateDialog(String passportType, String windowTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/registrar/registrarDraft.fxml"));
            Parent parent = loader.load();

            RegistrarDraftController controller = loader.getController();

            // Создаем модальное окно
            new WindowDecoration(windowTitle, parent, true, WF_MAIN_STAGE, false);

            // Обрабатываем результат
            if (controller.isAccepted()) {
                Passport newPassport = controller.getNewPassport();
                addToSelectedList(newPassport);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Ошибка", "Не удалось открыть форму создания паспорта: " + ex.getMessage());
        }
    }

    /**
     * Добавить паспорт в список выбранных
     */
    private void addToSelectedList(Passport passport) {
        // Проверяем, нет ли уже такого паспорта в списке
        boolean exists = selectedPassportsList.stream()
                .anyMatch(p -> p.getNumber() != null && p.getNumber().equals(passport.getNumber())
                        && p.getName() != null && p.getName().equals(passport.getName()));

        if (!exists) {
            selectedPassportsList.add(passport);
            // Сортируем список по номеру
            selectedPassportsList.sort(Comparator.comparing(Passport::getNumber));
            showInfo("Успешно", "Паспорт добавлен в список");
        } else {
            showWarning("Дубликат", "Этот паспорт уже добавлен в список");
        }
    }

    /**
     * Сохранить все номера
     */
    private void saveAllNumbers() {
        try {
            if (selectedPassportsList.isEmpty()) {
                showWarning("Нет данных", "Список паспортов пуст. Добавьте паспорта перед сохранением.");
                return;
            }

            // TODO: Здесь будет логика получения реальных номеров
            // Для каждого паспорта в списке нужно получить реальный номер
            for (Passport passport : selectedPassportsList) {
                if ("ПИК".equals(passport.getPrefix().getName())) {
                    // Получаем реальный номер ПИК
                    String realNumber = getRealPIKNumber();
                    passport.setNumber(realNumber);
                } else {
                    // Получаем реальный эскизный номер
                    String realNumber = getRealDraftNumber();
                    passport.setNumber(realNumber);
                }
            }

            // TODO: Сохраняем паспорта в базу данных
            // for (Passport passport : selectedPassportsList) {
            //     ChogoriServices.CH_QUICK_PASSPORTS.save(passport);
            // }

            showInfo("Сохранение", "Все номера успешно получены и сохранены (" + selectedPassportsList.size() + " паспортов)");

            // Очищаем список после сохранения
            selectedPassportsList.clear();

        } catch (Exception e) {
            showError("Ошибка сохранения", "Не удалось сохранить номера: " + e.getMessage());
        }
    }

    /**
     * Получить реальный номер ПИК
     * TODO: Реализовать логику получения номера
     */
    private String getRealPIKNumber() {
        // Здесь будет логика получения номера ПИК
        return "123456.789"; // Временный номер
    }

    /**
     * Получить реальный эскизный номер
     * TODO: Реализовать логику получения номера
     */
    private String getRealDraftNumber() {
        // Здесь будет логика получения эскизного номера
        return "Э12345"; // Временный номер
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

    /**
     * Показать диалоговое окно с предупреждением
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Показать диалоговое окно с информацией
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
