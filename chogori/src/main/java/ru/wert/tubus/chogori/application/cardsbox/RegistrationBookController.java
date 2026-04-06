package ru.wert.tubus.chogori.application.cardsbox;

import javafx.application.Platform;
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
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.chogori.entities.passports.PassportType;
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

/**
 * Контроллер журнала регистрации паспортов.
 * Отвечает за управление списками децимальных групп и выбранных паспортов,
 * а также за создание новых паспортов ПИК и эскизов.
 *
 * Важно: Все операции с паспортами всегда обращаются напрямую к базе данных,
 * без использования кэшированных списков, чтобы избежать рассинхронизации.
 */
@Slf4j
public class RegistrationBookController implements Initializable, UpdatableTabController {

    // ======================== FXML КОМПОНЕНТЫ ========================

    @FXML
    private ListView<Passport> lvListOFNumbers;      // Список выбранных паспортов

    @FXML
    private Button btnAddDecimalGroup;                // Кнопка добавления децимальной группы

    @FXML
    private Button btnClear;                          // Кнопка очистки списка

    @FXML
    private Button btnSave;                           // Кнопка сохранения списка в файл

    @FXML
    private ListView<Decimal> lvDecimalGroups;       // Список децимальных групп

    // ======================== ПОЛЯ ДАННЫХ ========================

    private ObservableList<Passport> selectedPassportsList;  // Список выбранных паспортов
    private ObservableList<Decimal> allDecimalGroupsList;    // Список всех децимальных групп

    private PassportContextMenu selectedContextMenu;         // Контекстное меню для списка паспортов

    @Setter
    private Passport_PatchController passportPIKController;    // Контроллер таблицы ПИК паспортов

    @Setter
    private Passport_PatchController passportSketchController; // Контроллер таблицы эскизных паспортов

    // ======================== ИНИЦИАЛИЗАЦИЯ ========================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Инициализация списка выбранных паспортов
        selectedPassportsList = FXCollections.observableArrayList();
        lvListOFNumbers.setItems(selectedPassportsList);

        // Настройка отображения списка паспортов
        setupListViewDisplay(lvListOFNumbers);

        // Восстановление ранее выбранных паспортов (по номерам, без загрузки всех паспортов)
        restoreSelectedPassportsState();

        // Загрузка децимальных групп
        loadDecimalGroups();
        fillAllDecimalGroups();

        // Настройка обработчиков кнопок
        setupButtons();
        setupBtnAddDecimalGroup();

        // Настройка обработчика двойного клика для списка децимальных групп
        setupDecimalGroupsDoubleClickHandler();

        // Настройка контекстного меню для списка паспортов
        setupContextMenus();
    }

    // ======================== НАСТРОЙКА ОТОБРАЖЕНИЯ ========================

    /**
     * Настройка отображения элементов в списке паспортов.
     *
     * @param listView список для настройки
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
     * Настройка отображения элементов в списке децимальных групп.
     *
     * @param listView список для настройки
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

    // ======================== ЗАГРУЗКА ДАННЫХ ========================

    /**
     * Загрузка списка децимальных групп из базы данных.
     */
    private void loadDecimalGroups() {
        try {
            allDecimalGroupsList = FXCollections.observableArrayList(
                    ChogoriServices.CH_DECIMALS.findAll()
            );
            log.info("Загружено {} децимальных групп", allDecimalGroupsList.size());
        } catch (Exception e) {
            log.error("Ошибка при загрузке децимальных групп", e);
            Warning1.create("ОШИБКА!","Ошибка загрузки данных", "Не удалось загрузить децимальные группы: " + e.getMessage());
            allDecimalGroupsList = FXCollections.observableArrayList();
        }
    }

    /**
     * Заполнение списка децимальных групп.
     */
    private void fillAllDecimalGroups() {
        lvDecimalGroups.setItems(allDecimalGroupsList);
        setupListViewDecimalGroups(lvDecimalGroups);
    }

    /**
     * Получение актуального списка всех паспортов из базы данных.
     * Используется только для проверки существования паспортов и поиска свободных номеров.
     *
     * @return список всех паспортов из БД
     */
    private List<Passport> getAllPassportsFromDatabase() {
        try {
            return ChogoriServices.CH_QUICK_PASSPORTS.findAll();
        } catch (Exception e) {
            log.error("Ошибка при загрузке паспортов из БД", e);
            return new ArrayList<>();
        }
    }

    /**
     * Получение паспорта по номеру из базы данных.
     *
     * @param number номер паспорта
     * @return паспорт или null
     */
    private Passport getPassportByNumber(String number) {
        try {
            // Ищем паспорт по номеру в БД
            List<Passport> allPassports = getAllPassportsFromDatabase();
            return allPassports.stream()
                    .filter(p -> p.getNumber() != null && p.getNumber().equals(number))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Ошибка при поиске паспорта по номеру {}", number, e);
            return null;
        }
    }

    // ======================== НАСТРОЙКА ОБРАБОТЧИКОВ ========================

    /**
     * Настройка обработчиков кнопок.
     */
    private void setupButtons() {
        if (btnClear != null) {
            btnClear.setOnAction(event -> clearSelectedList());
        }

        if (btnSave != null) {
            btnSave.setOnAction(event -> exportSelectedListToFile());
        }
    }

    /**
     * Настройка кнопки добавления децимальной группы.
     */
    private void setupBtnAddDecimalGroup() {
        if (btnAddDecimalGroup != null) {
            btnAddDecimalGroup.setOnAction(event -> addDecimalGroup());
        }
    }

    /**
     * Настройка обработчика двойного клика для списка децимальных групп.
     * При двойном клике открывается форма создания нового паспорта.
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
     * Настройка контекстного меню для списка выбранных паспортов.
     */
    private void setupContextMenus() {
        selectedContextMenu = new PassportContextMenu(
                lvListOFNumbers,
                this::editPassport,
                this::refreshPassportTables,
                this::refreshSelectedList
        );

        // Установка колбэка на удаление для синхронизации
        selectedContextMenu.setOnDeleteCallback(this::refreshAfterDelete);
    }

    // ======================== ОПЕРАЦИИ С ПАСПОРТАМИ ========================

    /**
     * Создание нового паспорта ПИК.
     *
     * @param decimal децимальная группа
     */
    private void getPIKNumber(Decimal decimal) {
        try {
            String nextNumber = getNextPIKNumber(decimal);
            openCreateDialog("PIK", "Номер ПИК", nextNumber, decimal);
        } catch (Exception e) {
            log.error("Ошибка при создании паспорта ПИК", e);
            Warning1.create("ОШИБКА!","Не удалось создать паспорт ПИК", e.getMessage());
        }
    }

    /**
     * Создание нового эскизного паспорта.
     *
     * @param decimal децимальная группа
     */
    private void getSketchNumber(Decimal decimal) {
        try {
            String nextNumber = getNextSketchNumber(decimal);
            openCreateDialog("SKETCH", "Эскизный номер", nextNumber, decimal);
        } catch (Exception e) {
            log.error("Ошибка при создании эскизного паспорта", e);
            Warning1.create("ОШИБКА!","Не удалось создать эскизный паспорт", e.getMessage());
        }
    }

    /**
     * Получение следующего доступного номера для паспорта ПИК.
     * Всегда обращается к актуальным данным из БД.
     *
     * @param decimal децимальная группа
     * @return следующий доступный номер
     */
    private String getNextPIKNumber(Decimal decimal) {
        try {
            // Получаем свежие данные децимальной группы из БД
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal == null) {
                throw new RuntimeException("Децимальная группа не найдена");
            }

            // Получаем актуальный список паспортов из БД
            List<Passport> allPassports = getAllPassportsFromDatabase();

            int currentLastNumber = freshDecimal.getLastNumber();
            int initialNumber = freshDecimal.getInitialNumber();

            // Поиск свободного номера в текущем диапазоне
            Integer freeNumber = findFreePIKNumber(freshDecimal.getName(), allPassports, initialNumber, currentLastNumber);

            if (freeNumber != null) {
                return formatPIKNumber(freshDecimal.getName(), freeNumber);
            } else {
                // Если свободных номеров нет, увеличиваем lastNumber
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
     * Поиск свободного номера ПИК в диапазоне.
     *
     * @param decimalName   имя децимальной группы
     * @param allPassports  актуальный список всех паспортов
     * @param start         начальное значение диапазона
     * @param end           конечное значение диапазона
     * @return свободный номер или null
     */
    private Integer findFreePIKNumber(String decimalName, List<Passport> allPassports, int start, int end) {
        try {
            // Сбор всех использованных номеров для данной децимальной группы
            Set<Integer> usedNumbers = allPassports.stream()
                    .filter(p -> p.getPrefix() != null
                            && "ПИК".equals(p.getPrefix().getName())
                            && p.getNumber() != null
                            && p.getNumber().startsWith(decimalName + "."))
                    .map(p -> {
                        String[] parts = p.getNumber().split("\\.");
                        if (parts.length == 2) {
                            try {
                                return Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                log.warn("Неверный формат номера: {}", p.getNumber());
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Поиск первого свободного номера
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
     * Форматирование номера ПИК.
     *
     * @param decimalName имя децимальной группы
     * @param number      порядковый номер
     * @return отформатированный номер
     */
    private String formatPIKNumber(String decimalName, int number) {
        return String.format("%s.%03d", decimalName, number);
    }

    /**
     * Получение следующего доступного номера для эскизного паспорта.
     * Всегда обращается к актуальным данным из БД.
     *
     * @param decimal децимальная группа
     * @return следующий доступный номер
     */
    private String getNextSketchNumber(Decimal decimal) {
        try {
            // Получаем свежие данные децимальной группы из БД
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal == null) {
                throw new RuntimeException("Децимальная группа не найдена");
            }

            // Получаем актуальный список паспортов из БД
            List<Passport> allPassports = getAllPassportsFromDatabase();

            int currentLastNumber = freshDecimal.getLastNumber();
            int initialNumber = freshDecimal.getInitialNumber();

            // Поиск свободного номера в текущем диапазоне
            Integer freeNumber = findFreeSketchNumber(allPassports, initialNumber, currentLastNumber);

            if (freeNumber != null) {
                return formatSketchNumber(freeNumber);
            } else {
                // Если свободных номеров нет, увеличиваем lastNumber
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
     * Поиск свободного номера эскиза в диапазоне.
     *
     * @param allPassports актуальный список всех паспортов
     * @param start        начальное значение диапазона
     * @param end          конечное значение диапазона
     * @return свободный номер или null
     */
    private Integer findFreeSketchNumber(List<Passport> allPassports, int start, int end) {
        try {
            // Сбор всех использованных номеров эскизов
            Set<Integer> usedNumbers = allPassports.stream()
                    .filter(p -> (p.getPrefix() == null
                            || p.getPrefix().getName() == null
                            || "-".equals(p.getPrefix().getName())))
                    .filter(p -> p.getNumber() != null && p.getNumber().startsWith("Э"))
                    .map(p -> {
                        try {
                            return Integer.parseInt(p.getNumber().substring(1));
                        } catch (Exception e) {
//                            log.warn("Неверный формат номера эскиза: {}", p.getNumber());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Поиск первого свободного номера
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
     * Форматирование номера эскиза.
     *
     * @param number порядковый номер
     * @return отформатированный номер (например, "Э15001")
     */
    private String formatSketchNumber(int number) {
        return String.format("Э%05d", number);
    }

    // ======================== ДИАЛОГОВЫЕ ОКНА ========================

    /**
     * Открытие диалога создания нового паспорта.
     *
     * @param passportType тип паспорта ("PIK" или "SKETCH")
     * @param windowTitle  заголовок окна
     * @param number       предварительно сформированный номер
     * @param decimal      децимальная группа
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
                    // Передаем созданный паспорт для выделения
                    refreshPassportTables(savedPassport);
                }
            } else if (controller.isCancelled() && controller.isNumberReserved()) {
                rollbackLastNumber(decimal, controller.getReservedNumber());
            }

        } catch (IOException ex) {
            log.error("Ошибка при открытии формы создания паспорта", ex);
            Warning1.create("ОШИБКА!","Не удалось открыть форму создания паспорта", ex.getMessage());
        }
    }

    /**
     * Редактирование существующего паспорта.
     *
     * @param passport паспорт для редактирования
     */
    private void editPassport(Passport passport) {
        try {
            // Получаем актуальную версию паспорта из БД перед редактированием
            Passport freshPassport = getPassportByNumber(passport.getNumber());
            if (freshPassport == null) {
                Warning1.create("ОШИБКА!","Паспорт не найден в базе данных", "Пояаится после перезагрузки");
                refreshSelectedList();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/registrationForm.fxml"));
            Parent parent = loader.load();

            RegistrationFormController controller = loader.getController();
            controller.setDataForEdit(freshPassport);

            new WindowDecoration("Редактирование паспорта", parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                // Обновляем таблицы, передавая отредактированный паспорт для выделения
                refreshPassportTables(freshPassport);

                // Обновляем выбранный паспорт в списке
                Passport updatedPassport = controller.getSavedPassport();
                if (updatedPassport != null) {
                    int selectedIndex = selectedPassportsList.indexOf(passport);
                    if (selectedIndex >= 0) {
                        selectedPassportsList.set(selectedIndex, updatedPassport);
                        selectedPassportsList.sort(Comparator.comparing(Passport::getNumber));
                        saveSelectedPassportsState();
                    }
                }
            }

        } catch (IOException ex) {
            log.error("Ошибка при открытии формы редактирования паспорта", ex);
            Warning1.create("ОШИБКА!","Не удалось открыть форму редактирования паспорта", ex.getMessage());
        }
    }

    // ======================== УПРАВЛЕНИЕ СПИСКАМИ ========================

    /**
     * Очистка списка выбранных паспортов.
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
            log.info("Список выбранных паспортов очищен");
        }
    }

    /**
     * Экспорт списка выбранных паспортов в файл.
     */
    @FXML
    private void exportSelectedListToFile() {
        if (selectedPassportsList.isEmpty()) {
            Warning1.create($ATTENTION, "Список пуст", "Нечего экспортировать");
            return;
        }

        // Формируем имя файла по умолчанию с текущей датой
        String defaultFileName = "Новые номера.txt";

        boolean exported = SelectedPassportsStorage.exportSelectedPassportsToFile(selectedPassportsList, defaultFileName);

        if (exported) {
            Warning1.create("ОТЛИЧНО!", "Экспорт выполнен", "Список успешно сохранен!");
        }
    }

    /**
     * Добавление новой децимальной группы (заглушка).
     */
    private void addDecimalGroup() {
        Warning1.create($ATTENTION, "Функция в разработке",
                "Добавление децимальных групп будет доступно в следующей версии");
    }

    /**
     * Откат lastNumber при отмене или ошибке создания паспорта.
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

    // ======================== ОБНОВЛЕНИЕ ТАБЛИЦ И ВЫДЕЛЕНИЕ ========================

    /**
     * Обновление таблиц паспортов (tvPIK и tvSketch) с сохранением выделения.
     * Используется для общего обновления без выделения конкретного паспорта.
     */
    private void refreshPassportTables() {
        refreshPassportTables(null);
    }

    /**
     * Обновление таблиц паспортов с возможностью выделения добавленного/измененного паспорта.
     *
     * @param passportToSelect паспорт, который нужно выделить (может быть null)
     */
    private void refreshPassportTables(Passport passportToSelect) {
        // Обновляем таблицу PIK
        if (passportPIKController != null && passportPIKController.getPassportsTable() != null) {
            Passport_TableView tvPIK = passportPIKController.getPassportsTable();
            tvPIK.refreshPreservingType();

            // Выделяем добавленный паспорт в таблице PIK, если он соответствует типу
            if (passportToSelect != null && isPIKPassport(passportToSelect)) {
                selectPassportInTable(tvPIK, passportToSelect);
            }
        }

        // Обновляем таблицу эскизов
        if (passportSketchController != null && passportSketchController.getPassportsTable() != null) {
            Passport_TableView tvSketch = passportSketchController.getPassportsTable();
            tvSketch.refreshPreservingType();

            // Выделяем добавленный паспорт в таблице эскизов, если он соответствует типу
            if (passportToSelect != null && isSketchPassport(passportToSelect)) {
                selectPassportInTable(tvSketch, passportToSelect);
            }
        }

        log.debug("Таблицы паспортов обновлены");
    }

    /**
     * Проверяет, является ли паспорт ПИК.
     *
     * @param passport проверяемый паспорт
     * @return true если паспорт ПИК
     */
    private boolean isPIKPassport(Passport passport) {
        return passport.getPrefix() != null
                && "ПИК".equals(passport.getPrefix().getName())
                && passport.getNumber() != null
                && passport.getNumber().matches("\\d{6}\\.\\d{3}");
    }

    /**
     * Проверяет, является ли паспорт эскизным.
     *
     * @param passport проверяемый паспорт
     * @return true если паспорт эскизный
     */
    private boolean isSketchPassport(Passport passport) {
        boolean prefixCondition = passport.getPrefix() == null
                || "-".equals(passport.getPrefix().getName());
        return prefixCondition
                && passport.getNumber() != null
                && passport.getNumber().matches("Э\\d{5}");
    }

    /**
     * Выделяет паспорт в таблице.
     *
     * @param tableView таблица, в которой нужно выделить
     * @param passport  паспорт для выделения
     */
    private void selectPassportInTable(Passport_TableView tableView, Passport passport) {
        Platform.runLater(() -> {
            int index = findPassportIndex(tableView, passport);
            if (index >= 0) {
                tableView.scrollTo(index);
                tableView.getSelectionModel().select(index);
                log.debug("Выделен паспорт {} в таблице, индекс: {}", passport.getNumber(), index);
            } else {
                log.debug("Паспорт {} не найден в таблице после обновления", passport.getNumber());
            }
        });
    }

    /**
     * Находит индекс паспорта в таблице.
     *
     * @param tableView таблица
     * @param passport  паспорт для поиска
     * @return индекс или -1 если не найден
     */
    private int findPassportIndex(Passport_TableView tableView, Passport passport) {
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (tableView.getItems().get(i).getId().equals(passport.getId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Обновление списка выбранных паспортов (удаление неактуальных).
     * Проверяет существование каждого паспорта в БД.
     */
    private void refreshSelectedList() {
        List<Passport> validPassports = new ArrayList<>();
        List<Passport> invalidPassports = new ArrayList<>();

        for (Passport passport : selectedPassportsList) {
            Passport freshPassport = getPassportByNumber(passport.getNumber());
            if (freshPassport != null) {
                validPassports.add(freshPassport);
            } else {
                invalidPassports.add(passport);
                log.warn("Паспорт {} не найден в БД, удаляем из списка выбранных", passport.getNumber());
            }
        }

        if (!invalidPassports.isEmpty()) {
            selectedPassportsList.clear();
            selectedPassportsList.addAll(validPassports);
            selectedPassportsList.sort(Comparator.comparing(Passport::getNumber));
            saveSelectedPassportsState();
            log.info("Удалено {} неактуальных паспортов из списка выбранных", invalidPassports.size());
        }
    }

    /**
     * Обновление после удаления паспорта.
     */
    private void refreshAfterDelete() {
        refreshSelectedList();
        // При удалении не передаем паспорт для выделения
        refreshPassportTables(null);
        log.info("Выполнено обновление после удаления");
    }

    /**
     * Добавление паспорта в список выбранных с сохранением состояния.
     *
     * @param passport паспорт для добавления
     */
    private void addToSelectedListWithSave(Passport passport) {
        boolean exists = selectedPassportsList.stream()
                .anyMatch(p -> p.getNumber() != null && p.getNumber().equals(passport.getNumber()));

        if (!exists) {
            selectedPassportsList.add(passport);
            selectedPassportsList.sort(Comparator.comparing(Passport::getNumber));
            saveSelectedPassportsState();
            log.info("Паспорт {} добавлен в список выбранных", passport.getNumber());
        } else {
            Warning1.create($ATTENTION,
                    "Этот паспорт уже добавлен в список",
                    "Дубликаты не допустимы");
        }
    }

    // ======================== СОХРАНЕНИЕ СОСТОЯНИЯ ========================

    /**
     * Сохранение состояния выбранных паспортов (сохраняем только номера).
     */
    private void saveSelectedPassportsState() {
        if (selectedPassportsList != null && !selectedPassportsList.isEmpty()) {
            SelectedPassportsStorage.saveSelectedPassports(selectedPassportsList);
        } else {
            SelectedPassportsStorage.clearSavedState();
        }
    }

    /**
     * Восстановление выбранных паспортов из сохраненного состояния.
     * Загружает паспорта по номерам из БД.
     */
    private void restoreSelectedPassportsState() {
        List<String> savedPassportNumbers = SelectedPassportsStorage.loadSelectedPassportNumbers();
        if (savedPassportNumbers.isEmpty()) {
            log.info("Нет сохраненных выбранных паспортов для восстановления");
            return;
        }

        List<Passport> restoredPassports = new ArrayList<>();

        for (String number : savedPassportNumbers) {
            Passport passport = getPassportByNumber(number);
            if (passport != null) {
                restoredPassports.add(passport);
            } else {
                log.warn("Паспорт с номером {} не найден в БД", number);
            }
        }

        if (!restoredPassports.isEmpty()) {
            selectedPassportsList.setAll(restoredPassports);
            selectedPassportsList.sort(Comparator.comparing(Passport::getNumber));
            log.info("Восстановлено {} выбранных паспортов", restoredPassports.size());

            // Если не все паспорта найдены, обновляем сохраненное состояние
            if (restoredPassports.size() != savedPassportNumbers.size()) {
                saveSelectedPassportsState();
            }
        } else {
            log.warn("Не найдены паспорта для сохраненных номеров: {}", savedPassportNumbers);
            SelectedPassportsStorage.clearSavedState();
        }
    }

    // ======================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ========================

    /**
     * Обновление вкладки (вызывается при переключении на эту вкладку).
     */
    @Override
    public void updateTab() {
        // Обновляем децимальные группы
        loadDecimalGroups();
        fillAllDecimalGroups();

        // Обновляем список выбранных паспортов
        restoreSelectedPassportsState();

        // Обновляем таблицы без выделения конкретного паспорта
        refreshPassportTables(null);

        log.info("Вкладка журнала регистрации обновлена");
    }
}