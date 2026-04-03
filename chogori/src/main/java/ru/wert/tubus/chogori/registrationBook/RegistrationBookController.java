// RegistrationBookController.java (обновленная версия)
package ru.wert.tubus.chogori.registrationBook;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.entities.passports.Passport_ACCController;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.client.interfaces.UpdatableTabController;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

@Slf4j
public class RegistrationBookController implements Initializable, UpdatableTabController {


    @FXML
    private ListView<Passport> lvListOFNumbers;

    @FXML
    private Button btnAddDecimalGroup;

    @FXML
    private ListView<Decimal> lvDecimalGroups;


    private ObservableList<Passport> selectedPassportsList;

    private ObservableList<Decimal> allDecimalGroupsList;

    private ObservableList<Passport> allPassportsList;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Инициализируем список для выбранных паспортов
        selectedPassportsList = FXCollections.observableArrayList();
        lvListOFNumbers.setItems(selectedPassportsList);


        // Загружаем список децимальных групп
        loadDecimalGroups();
        fillAllDecimalGroups();

        // Настраиваем обработчики кнопок
        setupBtnAddDecimalGroup();

        // Настраиваем обработчики выбора в списках
        setupSelectionHandlers();

        // Настраиваем обработчик двойного клика для списка децимальных групп
        setupDecimalGroupsDoubleClickHandler();

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
     * При двойном клике проверяем название группы:
     * - если "Эскиз" - создаем эскизный паспорт
     * - иначе - создаем паспорт ПИК
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
        // TODO: Реализовать добавление новой децимальной группы
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
     * Настраивает обработчики выбора в списках
     * Обеспечивает возможность выбора только одного элемента
     */
    private void setupSelectionHandlers() {
//        // Обработчик выбора в списке ПИК
//        lvPIK.getSelectionModel().selectedItemProperty().addListener(
//                (observable, oldValue, newValue) -> {
//                    if (newValue != null) {
//                        lvSketches.getSelectionModel().clearSelection();
//                    }
//                }
//        );
//
//        // Обработчик выбора в списке эскизов
//        lvSketches.getSelectionModel().selectedItemProperty().addListener(
//                (observable, oldValue, newValue) -> {
//                    if (newValue != null) {
//                        lvPIK.getSelectionModel().clearSelection();
//                    }
//                }
//        );
    }

    /**
     * Получить следующий доступный номер для паспорта ПИК
     *
     * @param decimal децимальная группа
     * @return следующий доступный номер в формате "XXXXXX.XXX"
     */
    private String getNextPIKNumber(Decimal decimal) {
        try {
            // Получаем текущий lastNumber из базы данных (на случай параллельной работы)
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal == null) {
                throw new RuntimeException("Децимальная группа не найдена");
            }

            int currentLastNumber = freshDecimal.getLastNumber();
            int initialNumber = freshDecimal.getInitialNumber();

            // Проверяем, есть ли свободные номера в диапазоне
            Integer freeNumber = findFreePIKNumber(freshDecimal, initialNumber, currentLastNumber);

            if (freeNumber != null) {
                // Возвращаем свободный номер
                return formatPIKNumber(freshDecimal.getName(), freeNumber);
            } else {
                // Свободных номеров нет, используем lastNumber + 1
                int newNumber = currentLastNumber + 1;
                String formattedNumber = formatPIKNumber(freshDecimal.getName(), newNumber);

                // Обновляем lastNumber в базе данных
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
     *
     * @param decimal децимальная группа
     * @param start   начальное значение диапазона
     * @param end     конечное значение диапазона
     * @return первый свободный номер или null, если все номера заняты
     */
    private Integer findFreePIKNumber(Decimal decimal, int start, int end) {
        try {
            // Получаем все существующие паспорта с префиксом "ПИК" и децимальной характеристикой
            List<Passport> existingPassports = allPassportsList.stream()
                    .filter(p -> p.getPrefix() != null
                            && "ПИК".equals(p.getPrefix().getName())
                            && p.getNumber() != null
                            && p.getNumber().startsWith(decimal.getName() + "."))
                    .collect(Collectors.toList());

            // Извлекаем все занятые порядковые номера
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

            // Ищем первый свободный номер в диапазоне
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
     *
     * @param decimalName децимальная характеристика (6 цифр)
     * @param number      порядковый номер (3 цифры)
     * @return отформатированный номер в виде "XXXXXX.XXX"
     */
    private String formatPIKNumber(String decimalName, int number) {
        return String.format("%s.%03d", decimalName, number);
    }

    /**
     * Получить следующий доступный номер для эскизного паспорта
     *
     * @param decimal децимальная группа (Эскиз)
     * @return следующий доступный номер в формате "ЭXXXXX"
     */
    private String getNextSketchNumber(Decimal decimal) {
        try {
            // Получаем текущий lastNumber из базы данных (на случай параллельной работы)
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal == null) {
                throw new RuntimeException("Децимальная группа не найдена");
            }

            int currentLastNumber = freshDecimal.getLastNumber();
            int initialNumber = freshDecimal.getInitialNumber();

            // Проверяем, есть ли свободные номера в диапазоне
            Integer freeNumber = findFreeSketchNumber(freshDecimal, initialNumber, currentLastNumber);

            if (freeNumber != null) {
                // Возвращаем свободный номер
                return formatSketchNumber(freeNumber);
            } else {
                // Свободных номеров нет, используем lastNumber + 1
                int newNumber = currentLastNumber + 1;
                String formattedNumber = formatSketchNumber(newNumber);

                // Обновляем lastNumber в базе данных
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
     *
     * @param decimal децимальная группа
     * @param start   начальное значение диапазона
     * @param end     конечное значение диапазона
     * @return первый свободный номер или null, если все номера заняты
     */
    private Integer findFreeSketchNumber(Decimal decimal, int start, int end) {
        try {
            // Получаем все существующие паспорта с префиксом "-" (эскизы)
            List<Passport> existingPassports = allPassportsList.stream()
                    .filter(p -> (p.getPrefix() == null || "-".equals(p.getPrefix().getName()))
                            && p.getNumber() != null
                            && p.getNumber().startsWith("Э"))
                    .collect(Collectors.toList());

            // Извлекаем все занятые порядковые номера
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

            // Ищем первый свободный номер в диапазоне
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
     *
     * @param number порядковый номер (5 цифр)
     * @return отформатированный номер в виде "ЭXXXXX"
     */
    private String formatSketchNumber(int number) {
        return String.format("Э%05d", number);
    }

    /**
     * Создать новый паспорт ПИК
     *
     * @param decimal децимальная группа
     */
    private void getPIKNumber(Decimal decimal) {
        try {
            String nextNumber = getNextPIKNumber(decimal);
//            openCreateDialog("PIK", "Регистрация номера", nextNumber, decimal);
        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать паспорт ПИК: " + e.getMessage());
        }
    }

    /**
     * Создать новый эскизный паспорт
     *
     * @param decimal децимальная группа
     */
    private void getSketchNumber(Decimal decimal) {
        try {
            String nextNumber = getNextSketchNumber(decimal);
//            openCreateDialog("SKETCH", "Регистрация номера", nextNumber, decimal);
        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать эскизный паспорт: " + e.getMessage());
        }
    }

    /**
     * Обновляет списки паспортов после добавления нового
     */
    private void refreshPassportLists() {
//        fillPIKListView();
//        fillSketchesListView();
        refreshSelectedList();
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
        selectedPassportsList.removeAll(toRemove);
    }

    /**
     * Откат lastNumber при отмене или ошибке
     *
     * @param decimal        децимальная группа
     * @param reservedNumber зарезервированный номер
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
     * Добавить паспорт в список выбранных
     *
     * @param passport добавляемый паспорт
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
        } else {
            Warning1.create($ATTENTION,
                    "Этот паспорт уже добавлен в список",
                    "Дубликаты не допустимы");
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
        // Обновление данных при переключении на вкладку
        loadAllPassports();
        refreshPassportLists();
        loadDecimalGroups();
        fillAllDecimalGroups();
    }
}
