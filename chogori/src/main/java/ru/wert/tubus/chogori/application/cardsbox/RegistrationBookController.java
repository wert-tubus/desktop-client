// RegistrationBookController.java (полностью обновленная версия)
package ru.wert.tubus.chogori.application.cardsbox;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.entities.passports.Passport_PatchController;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.interfaces.UpdatableTabController;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

@Slf4j
public class RegistrationBookController implements Initializable, UpdatableTabController {

    @FXML
    private ListView<Passport> lvListOFNumbers;

    @FXML
    private Button btnAddDecimalGroup;

    @FXML
    private Button btnClear;

    @FXML
    private Button btnSave;

    @FXML
    private ListView<Decimal> lvDecimalGroups;

    private ObservableList<Passport> selectedPassportsList;
    private ObservableList<Decimal> allDecimalGroupsList;
    private ObservableList<Passport> allPassportsList;

    private PassportContextMenu selectedContextMenu;

    @Setter
    private Passport_PatchController passportPIKController;
    @Setter
    private Passport_PatchController passportSketchController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Инициализируем список для выбранных паспортов
        selectedPassportsList = FXCollections.observableArrayList();
        lvListOFNumbers.setItems(selectedPassportsList);

        // Настраиваем отображение для lvListOFNumbers
        setupListViewDisplay(lvListOFNumbers);

        // Загружаем все паспорта
        loadAllPassports();

        // Восстанавливаем сохраненное состояние ПОСЛЕ загрузки allPassportsList
        restoreSelectedPassportsState();

        // Загружаем список децимальных групп
        loadDecimalGroups();
        fillAllDecimalGroups();

        // Настраиваем обработчики кнопок
        setupButtons();
        setupBtnAddDecimalGroup();

        // Настраиваем обработчик двойного клика для списка децимальных групп
        setupDecimalGroupsDoubleClickHandler();

        // Настраиваем контекстные меню для списков паспортов
        setupContextMenus();
    }

    /**
     * Настройка кнопок
     */
    private void setupButtons() {
        // Кнопка очистки списка
        if (btnClear != null) {
            btnClear.setOnAction(event -> clearSelectedList());
        }

        // Кнопка сохранения списка в файл
        if (btnSave != null) {
            btnSave.setOnAction(event -> exportSelectedListToFile());
        }
    }

    /**
     * Настройка контекстных меню для всех списков паспортов
     */
    private void setupContextMenus() {
        // Контекстное меню для списка выбранных паспортов
        selectedContextMenu = new PassportContextMenu(
                lvListOFNumbers,
                this::editPassport,
                this::refreshPassportLists,
                this::refreshSelectedList
        );
    }

    /**
     * Редактирование паспорта
     *
     * @param passport паспорт для редактирования
     */
    private void editPassport(Passport passport) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/registrationForm.fxml"));
            Parent parent = loader.load();

            RegistrationFormController controller = loader.getController();
            controller.setDataForEdit(passport);

            new WindowDecoration("Редактирование паспорта", parent, false, WF_MAIN_STAGE, true);

            // Обрабатываем результат после закрытия окна
            if (controller.isAccepted()) {
                Passport updatedPassport = controller.getSavedPassport();
                if (updatedPassport != null) {
                    // Обновляем локальный список паспортов
                    int index = allPassportsList.indexOf(passport);
                    if (index >= 0) {
                        allPassportsList.set(index, updatedPassport);
                    }
                    // Обновляем списки
                    refreshPassportLists();

                    // Обновляем выбранные паспорта
                    int selectedIndex = selectedPassportsList.indexOf(passport);
                    if (selectedIndex >= 0) {
                        selectedPassportsList.set(selectedIndex, updatedPassport);
                        selectedPassportsList.sort(Comparator.comparing(Passport::getNumber));
                        saveSelectedPassportsState();
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Ошибка", "Не удалось открыть форму редактирования паспорта: " + ex.getMessage());
        }
    }

    /**
     * Обновление списка выбранных паспортов (удаление неактуальных)
     */
    private void refreshSelectedList() {
        // Удаляем из выбранных те паспорта, которых больше нет в allPassportsList
        Set<Passport> currentPassports = new HashSet<>(allPassportsList);
        List<Passport> toRemove = selectedPassportsList.stream()
                .filter(p -> !currentPassports.contains(p))
                .collect(Collectors.toList());
        if (!toRemove.isEmpty()) {
            selectedPassportsList.removeAll(toRemove);
            saveSelectedPassportsState();
        }
    }

    /**
     * Настраивает отображение элементов в ListView для паспортов
     */
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

    /**
     * Загружаем список децимальных групп из базы данных
     */
    private void loadDecimalGroups() {
        try {
            allDecimalGroupsList = FXCollections.observableArrayList(
                    ChogoriServices.CH_DECIMALS.findAll()
            );
        } catch (Exception e) {
            showError("Ошибка загрузки данных", "Не удалось загрузить децимальные группы: " + e.getMessage());
            allDecimalGroupsList = FXCollections.observableArrayList();
        }
    }

    /**
     * Заполняет ListView децимальных групп
     */
    private void fillAllDecimalGroups() {
        lvDecimalGroups.setItems(allDecimalGroupsList);
        setupListViewDecimalGroups(lvDecimalGroups);
    }

    /**
     * Настраивает отображение элементов в списке децимальных групп
     */
    private void setupListViewDecimalGroups(ListView<Decimal> listView) {
        listView.setCellFactory(lv -> new ListCell<Decimal>() {
            @Override
            protected void updateItem(Decimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    /**
     * Настройка обработчика двойного клика для списка децимальных групп
     */
    private void setupDecimalGroupsDoubleClickHandler() {
        lvDecimalGroups.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Decimal selectedDecimal = lvDecimalGroups.getSelectionModel().getSelectedItem();
                if (selectedDecimal != null) {
                    String decimalName = selectedDecimal.getName();
                    if ("Эскиз".equals(decimalName)) {
                        getSketchNumber(selectedDecimal);
                    } else {
                        getPIKNumber(selectedDecimal);
                    }
                }
            }
        });
    }

    /**
     * Настройка кнопки добавления децимальной группы
     */
    private void setupBtnAddDecimalGroup() {
        if (btnAddDecimalGroup != null) {
            btnAddDecimalGroup.setOnAction(event -> addDecimalGroup());
        }
    }

    /**
     * Добавление новой децимальной группы
     */
    private void addDecimalGroup() {
        // TODO: Реализовать добавление новой децимальной группы
        Warning1.create($ATTENTION, "Функция в разработке", "Добавление децимальных групп будет доступно в следующей версии");
    }

    /**
     * Очистка списка выбранных паспортов
     */
    @FXML
    private void clearSelectedList() {
        if (selectedPassportsList.isEmpty()) {
            Warning1.create($ATTENTION, "Список уже пуст", "Нечего очищать");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение очистки");
        confirmAlert.setHeaderText("Очистка списка выбранных паспортов");
        confirmAlert.setContentText("Вы действительно хотите очистить весь список?\n" +
                "Всего паспортов в списке: " + selectedPassportsList.size());

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selectedPassportsList.clear();
            saveSelectedPassportsState();
            log.info("Selected passports list cleared");
        }
    }

    /**
     * Экспорт списка выбранных паспортов в файл
     */
    @FXML
    private void exportSelectedListToFile() {
        if (selectedPassportsList.isEmpty()) {
            Warning1.create($ATTENTION, "Список пуст", "Нечего экспортировать");
            return;
        }

        // Выбираем директорию для сохранения
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Выберите папку для сохранения списка");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedDirectory = directoryChooser.showDialog(WF_MAIN_STAGE);
        if (selectedDirectory != null) {
            SelectedPassportsStorage.exportSelectedPassportsToFile(selectedPassportsList, selectedDirectory.getAbsolutePath());

            Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
            infoAlert.setTitle("Экспорт выполнен");
            infoAlert.setHeaderText(null);
            infoAlert.setContentText("Список успешно сохранен в файл:\n" +
                    selectedDirectory.getAbsolutePath() + "/selected_passports_export.txt");
            infoAlert.showAndWait();
        }
    }

    /**
     * Загружает все паспорта из базы данных
     */
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

    /**
     * Получить следующий доступный номер для паспорта ПИК
     */
    private String getNextPIKNumber(Decimal decimal) {
        try {
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal == null) {
                throw new RuntimeException("Децимальная группа не найдена");
            }

            int currentLastNumber = freshDecimal.getLastNumber();
            int initialNumber = freshDecimal.getInitialNumber();

            Integer freeNumber = findFreePIKNumber(freshDecimal, initialNumber, currentLastNumber);

            if (freeNumber != null) {
                return formatPIKNumber(freshDecimal.getName(), freeNumber);
            } else {
                int newNumber = currentLastNumber + 1;
                String formattedNumber = formatPIKNumber(freshDecimal.getName(), newNumber);

                freshDecimal.setLastNumber(newNumber);
                boolean updated = ChogoriServices.CH_DECIMALS.update(freshDecimal);

                if (!updated) {
                    throw new RuntimeException("Не удалось обновить lastNumber в базе данных");
                }

                return formattedNumber;
            }
        } catch (Exception e) {
            log.error("Ошибка при получении следующего номера ПИК", e);
            throw new RuntimeException("Не удалось получить следующий номер: " + e.getMessage());
        }
    }

    /**
     * Поиск свободного номера в диапазоне ПИК
     */
    private Integer findFreePIKNumber(Decimal decimal, int start, int end) {
        try {
            List<Passport> existingPassports = allPassportsList.stream()
                    .filter(p -> p.getPrefix() != null
                            && "ПИК".equals(p.getPrefix().getName())
                            && p.getNumber() != null
                            && p.getNumber().startsWith(decimal.getName() + "."))
                    .collect(Collectors.toList());

            Set<Integer> usedNumbers = new HashSet<>();
            for (Passport passport : existingPassports) {
                String number = passport.getNumber();
                String[] parts = number.split("\\.");
                if (parts.length == 2) {
                    try {
                        usedNumbers.add(Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        log.warn("Неверный формат номера: {}", number);
                    }
                }
            }

            for (int i = start; i <= end; i++) {
                if (!usedNumbers.contains(i)) {
                    return i;
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Ошибка при поиске свободного номера", e);
            return null;
        }
    }

    /**
     * Форматирование номера ПИК
     */
    private String formatPIKNumber(String decimalName, int number) {
        return String.format("%s.%03d", decimalName, number);
    }

    /**
     * Получить следующий доступный номер для эскизного паспорта
     */
    private String getNextSketchNumber(Decimal decimal) {
        try {
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal == null) {
                throw new RuntimeException("Децимальная группа не найдена");
            }

            int currentLastNumber = freshDecimal.getLastNumber();
            int initialNumber = freshDecimal.getInitialNumber();

            Integer freeNumber = findFreeSketchNumber(freshDecimal, initialNumber, currentLastNumber);

            if (freeNumber != null) {
                return formatSketchNumber(freeNumber);
            } else {
                int newNumber = currentLastNumber + 1;
                String formattedNumber = formatSketchNumber(newNumber);

                freshDecimal.setLastNumber(newNumber);
                boolean updated = ChogoriServices.CH_DECIMALS.update(freshDecimal);

                if (!updated) {
                    throw new RuntimeException("Не удалось обновить lastNumber в базе данных");
                }

                return formattedNumber;
            }
        } catch (Exception e) {
            log.error("Ошибка при получении следующего номера эскиза", e);
            throw new RuntimeException("Не удалось получить следующий номер: " + e.getMessage());
        }
    }

    /**
     * Поиск свободного номера в диапазоне эскизов
     */
    private Integer findFreeSketchNumber(Decimal decimal, int start, int end) {
        try {
            List<Passport> existingPassports = allPassportsList.stream()
                    .filter(p -> (p.getPrefix() == null || "-".equals(p.getPrefix().getName()))
                            && p.getNumber() != null
                            && p.getNumber().startsWith("Э"))
                    .collect(Collectors.toList());

            Set<Integer> usedNumbers = new HashSet<>();
            for (Passport passport : existingPassports) {
                String number = passport.getNumber();
                if (number.length() > 1) {
                    try {
                        usedNumbers.add(Integer.parseInt(number.substring(1)));
                    } catch (NumberFormatException e) {
                        log.warn("Неверный формат номера: {}", number);
                    }
                }
            }

            for (int i = start; i <= end; i++) {
                if (!usedNumbers.contains(i)) {
                    return i;
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Ошибка при поиске свободного номера", e);
            return null;
        }
    }

    /**
     * Форматирование номера эскиза
     */
    private String formatSketchNumber(int number) {
        return String.format("Э%05d", number);
    }

    /**
     * Создать новый паспорт ПИК
     */
    private void getPIKNumber(Decimal decimal) {
        try {
            String nextNumber = getNextPIKNumber(decimal);
            openCreateDialog("PIK", "Регистрация номера", nextNumber, decimal);
        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать паспорт ПИК: " + e.getMessage());
        }
    }

    /**
     * Создать новый эскизный паспорт
     */
    private void getSketchNumber(Decimal decimal) {
        try {
            String nextNumber = getNextSketchNumber(decimal);
            openCreateDialog("SKETCH", "Регистрация номера", nextNumber, decimal);
        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать эскизный паспорт: " + e.getMessage());
        }
    }

    /**
     * Открыть диалог создания нового паспорта
     */
    private void openCreateDialog(String passportType, String windowTitle, String number, Decimal decimal) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/registrationForm.fxml"));
            Parent parent = loader.load();

            RegistrationFormController controller = loader.getController();
            controller.setData(passportType, number, decimal);

            new WindowDecoration(windowTitle, parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                Passport savedPassport = controller.getSavedPassport();
                if (savedPassport != null) {
                    addToSelectedListWithSave(savedPassport);
                    allPassportsList.add(savedPassport);
                    refreshPassportLists();
                }
            } else if (controller.isCancelled() && controller.isNumberReserved()) {
                rollbackLastNumber(decimal, controller.getReservedNumber());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Ошибка", "Не удалось открыть форму создания паспорта: " + ex.getMessage());
        }
    }

    /**
     * Обновляет списки паспортов после добавления нового
     */
    private void refreshPassportLists() {
        if (passportPIKController != null && passportPIKController.getPassportsTable() != null) {
            passportPIKController.getPassportsTable().updateView();
        }
        if (passportSketchController != null && passportSketchController.getPassportsTable() != null) {
            passportSketchController.getPassportsTable().updateView();
        }
        refreshSelectedList();
    }

    /**
     * Откат lastNumber при отмене или ошибке
     */
    private void rollbackLastNumber(Decimal decimal, int reservedNumber) {
        try {
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal != null && freshDecimal.getLastNumber() == reservedNumber) {
                freshDecimal.setLastNumber(reservedNumber - 1);
                ChogoriServices.CH_DECIMALS.update(freshDecimal);
                log.info("Выполнен откат lastNumber для decimal {} с {} на {}",
                        decimal.getName(), reservedNumber, reservedNumber - 1);
            }
        } catch (Exception e) {
            log.error("Ошибка при откате lastNumber", e);
        }
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

    @Override
    public void updateTab() {
        loadAllPassports();
        refreshPassportLists();
        loadDecimalGroups();
        fillAllDecimalGroups();
        restoreSelectedPassportsState();
    }

    // ========== СОХРАНЕНИЕ СОСТОЯНИЯ ==========

    /**
     * Сохраняет состояние выбранных паспортов
     */
    private void saveSelectedPassportsState() {
        if (selectedPassportsList != null && !selectedPassportsList.isEmpty()) {
            SelectedPassportsStorage.saveSelectedPassports(selectedPassportsList);
        } else {
            SelectedPassportsStorage.clearSavedState();
        }
    }

    /**
     * Восстанавливает выбранные паспорта из сохраненного состояния
     */
    private void restoreSelectedPassportsState() {
        List<String> savedPassportNumbers = SelectedPassportsStorage.loadSelectedPassportNumbers();
        if (savedPassportNumbers.isEmpty()) {
            log.info("No saved selected passports to restore");
            return;
        }

        // Находим паспорта по номеру из общего списка
        List<Passport> restoredPassports = allPassportsList.stream()
                .filter(p -> p.getNumber() != null && savedPassportNumbers.contains(p.getNumber()))
                .collect(Collectors.toList());

        if (!restoredPassports.isEmpty()) {
            selectedPassportsList.setAll(restoredPassports);
            selectedPassportsList.sort(Comparator.comparing(Passport::getNumber));
            log.info("Restored {} selected passports", restoredPassports.size());

            if (restoredPassports.size() != savedPassportNumbers.size()) {
                saveSelectedPassportsState();
            }
        } else {
            log.warn("No matching passports found for saved numbers: {}", savedPassportNumbers);
            SelectedPassportsStorage.clearSavedState();
        }
    }

    /**
     * Добавляет паспорт в список выбранных с сохранением состояния
     */
    private void addToSelectedListWithSave(Passport passport) {
        boolean exists = selectedPassportsList.stream()
                .anyMatch(p -> p.getNumber() != null && p.getNumber().equals(passport.getNumber()));

        if (!exists) {
            selectedPassportsList.add(passport);
            selectedPassportsList.sort(Comparator.comparing(Passport::getNumber));
            saveSelectedPassportsState();
        } else {
            Warning1.create($ATTENTION,
                    "Этот паспорт уже добавлен в список",
                    "Дубликаты не допустимы");
        }
    }

}