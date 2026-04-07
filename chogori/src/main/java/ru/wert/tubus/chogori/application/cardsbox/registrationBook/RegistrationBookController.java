package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.cardsbox.DecimalFormController;
import ru.wert.tubus.chogori.application.cardsbox.PassportContextMenu;
import ru.wert.tubus.chogori.application.cardsbox.RegistrationFormController;
import ru.wert.tubus.chogori.application.cardsbox.RegisteredPassportsStorage;
import ru.wert.tubus.chogori.entities.passports.PassportInfo_Patch;
import ru.wert.tubus.chogori.entities.passports.Passport_PatchController;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.interfaces.UpdatableTabController;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.io.IOException;
import java.util.*;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;
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
public class RegistrationBookController implements UpdatableTabController {

    // ======================== FXML КОМПОНЕНТЫ ========================

    @FXML private ListView<Passport> lvListOFNumbers;
    @FXML private Button btnAddDecimalGroup;
    @FXML private Button btnClear;
    @FXML private Button btnSave;
    @FXML private Accordion accDecimalGroups;

    // Списки децимальных групп
    @FXML private ListView<Decimal> lvSketches;
    @FXML private ListView<Decimal> lvDetails700;
    @FXML private ListView<Decimal> lvDetails745;
    @FXML private ListView<Decimal> lvAssm300;
    @FXML private ListView<Decimal> lvAssm400;
    @FXML private ListView<Decimal> lvMedicine;
    @FXML private ListView<Decimal> lvOther;

    // Панели аккордеона (для управления раскрытием)
    @FXML private TitledPane tpSketches;
    @FXML private TitledPane tpDetails700;
    @FXML private TitledPane tpDetails745;
    @FXML private TitledPane tpAssm300;
    @FXML private TitledPane tpAssm400;
    @FXML private TitledPane tpMedicine;
    @FXML private TitledPane tpOther;

    // ======================== СЕРВИСЫ И МЕНЕДЖЕРЫ ========================

    private final PassportService passportService = new PassportService();
    private RegisteredPassportsManager registeredPassportsManager;
    private final Map<DecimalGroupingService.DecimalGroup, ListView<Decimal>> groupToListViewMap = new EnumMap<>(DecimalGroupingService.DecimalGroup.class);

    @Setter private Passport_PatchController passportPIKController;
    @Setter private Passport_PatchController passportSketchController;

    // ======================== ИНИЦИАЛИЗАЦИЯ ========================

    /**
     * Инициализация контроллера после загрузки FXML.
     * Настраивает все компоненты, загружает данные и восстанавливает состояние.
     */
    @FXML
    public void initialize() {
        initializeSelectedPassportsList();
        initializeDecimalGroupsLists();
        setupButtonHandlers();
        setupDoubleClickHandlers();
        setupContextMenus();

        // Восстановление состояния
        registeredPassportsManager.restoreState();
    }

    /**
     * Инициализация списка выбранных паспортов.
     * Настраивает отображение элементов и обработчики двойного клика.
     */
    private void initializeSelectedPassportsList() {
        ObservableList<Passport> selectedList = FXCollections.observableArrayList();
        lvListOFNumbers.setItems(selectedList);
        registeredPassportsManager = new RegisteredPassportsManager(selectedList, passportService);

        lvListOFNumbers.setCellFactory(lv -> new ListCell<Passport>() {
            @Override
            protected void updateItem(Passport item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toUsefulString());
                    setOnMouseClicked(e -> {
                        if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
                            PassportInfo_Patch.create(item);
                        }
                    });
                }
            }
        });
    }

    /**
     * Инициализация списков децимальных групп.
     * Создает маппинг групп, настраивает отображение и загружает данные.
     */
    private void initializeDecimalGroupsLists() {
        // Инициализация маппинга групп
        groupToListViewMap.put(DecimalGroupingService.DecimalGroup.SKETCH, lvSketches);
        groupToListViewMap.put(DecimalGroupingService.DecimalGroup.DETAILS_700, lvDetails700);
        groupToListViewMap.put(DecimalGroupingService.DecimalGroup.DETAILS_745, lvDetails745);
        groupToListViewMap.put(DecimalGroupingService.DecimalGroup.ASSM_300, lvAssm300);
        groupToListViewMap.put(DecimalGroupingService.DecimalGroup.ASSM_400, lvAssm400);
        groupToListViewMap.put(DecimalGroupingService.DecimalGroup.MEDICINE, lvMedicine);
        groupToListViewMap.put(DecimalGroupingService.DecimalGroup.OTHER, lvOther);

        // Настройка отображения для всех списков
        for (ListView<Decimal> listView : groupToListViewMap.values()) {
            setupDecimalListView(listView);
        }

        // Загрузка данных
        loadAndDistributeDecimalGroups();
    }

    /**
     * Настройка отображения элементов в списке децимальных групп.
     *
     * @param listView список для настройки
     */
    private void setupDecimalListView(ListView<Decimal> listView) {
        listView.setCellFactory(lv -> new ListCell<Decimal>() {
            @Override
            protected void updateItem(Decimal item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });
    }

    /**
     * Настройка обработчиков кнопок.
     */
    private void setupButtonHandlers() {
        if (btnAddDecimalGroup != null) {
            btnAddDecimalGroup.setOnAction(e -> addDecimalGroup());
        }
        if (btnClear != null) {
            btnClear.setOnAction(e -> clearSelectedList());
        }
        if (btnSave != null) {
            btnSave.setOnAction(e -> exportSelectedListToFile());
        }
    }

    /**
     * Настройка обработчиков двойного клика для всех списков децимальных групп.
     * При двойном клике открывается форма создания нового паспорта.
     */
    private void setupDoubleClickHandlers() {
        for (ListView<Decimal> listView : groupToListViewMap.values()) {
            listView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Decimal selected = listView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        if (DecimalGroupingService.isSketch(selected)) {
                            createSketchPassport(selected);
                        } else {
                            createPIKPassport(selected);
                        }
                    }
                }
            });
        }
    }

    /**
     * Настройка контекстного меню для списка выбранных паспортов.
     */
    private void setupContextMenus() {
        PassportContextMenu contextMenu = new PassportContextMenu(
                lvListOFNumbers,
                this::editPassport,
                this::refreshPassportTables,
                () -> registeredPassportsManager.refresh()
        );
        contextMenu.setOnDeleteCallback(this::refreshAfterDelete);
    }

    // ======================== ЗАГРУЗКА ДАННЫХ ========================

    /**
     * Загрузка и распределение децимальных групп по соответствующим ListView.
     */
    private void loadAndDistributeDecimalGroups() {
        try {
            List<Decimal> allDecimals = CH_DECIMALS.findAll();
            log.info("Загружено {} децимальных групп", allDecimals.size());
            distributeDecimalsToLists(allDecimals);
        } catch (Exception e) {
            log.error("Ошибка при загрузке децимальных групп", e);
            Warning1.create("ОШИБКА!", "Ошибка загрузки данных", "Не удалось загрузить децимальные группы: " + e.getMessage());
            clearAllDecimalLists();
        }
    }

    /**
     * Распределение децимальных групп по соответствующим ListView.
     *
     * @param allDecimals список всех децимальных групп
     */
    private void distributeDecimalsToLists(List<Decimal> allDecimals) {
        // Очищаем все списки
        for (ListView<Decimal> listView : groupToListViewMap.values()) {
            listView.getItems().clear();
        }

        // Распределяем по группам
        for (Decimal decimal : allDecimals) {
            DecimalGroupingService.DecimalGroup group = DecimalGroupingService.determineGroup(decimal);
            ListView<Decimal> targetListView = groupToListViewMap.get(group);
            if (targetListView != null) {
                targetListView.getItems().add(decimal);
            }
        }

        // Сортируем все списки
        for (ListView<Decimal> listView : groupToListViewMap.values()) {
            sortDecimalList(listView.getItems());
        }
    }

    /**
     * Сортировка списка децимальных групп по имени.
     *
     * @param list список для сортировки
     */
    private void sortDecimalList(ObservableList<Decimal> list) {
        if (list != null) {
            list.sort(Comparator.comparing(Decimal::getName, Comparator.nullsLast(Comparator.naturalOrder())));
        }
    }

    /**
     * Очистка всех списков децимальных групп при ошибке загрузки.
     */
    private void clearAllDecimalLists() {
        for (ListView<Decimal> listView : groupToListViewMap.values()) {
            listView.setItems(FXCollections.observableArrayList());
            setupDecimalListView(listView);
        }
    }

    // ======================== ОПЕРАЦИИ С ПАСПОРТАМИ ========================

    /**
     * Создание нового паспорта ПИК для указанной децимальной группы.
     *
     * @param decimal децимальная группа
     */
    private void createPIKPassport(Decimal decimal) {
        try {
            String nextNumber = passportService.getNextPIKNumber(decimal);
            openRegistrationDialog("PIK", "Номер ПИК", nextNumber, decimal);
        } catch (Exception e) {
            log.error("Ошибка при создании паспорта ПИК", e);
            Warning1.create("ОШИБКА!", "Не удалось создать паспорт ПИК", e.getMessage());
        }
    }

    /**
     * Создание нового эскизного паспорта для указанной децимальной группы.
     *
     * @param decimal децимальная группа
     */
    private void createSketchPassport(Decimal decimal) {
        try {
            String nextNumber = passportService.getNextSketchNumber(decimal);
            openRegistrationDialog("SKETCH", "Эскизный номер", nextNumber, decimal);
        } catch (Exception e) {
            log.error("Ошибка при создании эскизного паспорта", e);
            Warning1.create("ОШИБКА!", "Не удалось создать эскизный паспорт", e.getMessage());
        }
    }

    /**
     * Открытие диалогового окна регистрации нового паспорта.
     *
     * @param passportType тип паспорта ("PIK" или "SKETCH")
     * @param windowTitle заголовок окна
     * @param number предварительно сгенерированный номер
     * @param decimal децимальная группа
     */
    private void openRegistrationDialog(String passportType, String windowTitle, String number, Decimal decimal) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/registrationForm.fxml"));
            Parent parent = loader.load();

            RegistrationFormController controller = loader.getController();
            controller.setData(passportType, number, decimal);

            new WindowDecoration(windowTitle, parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                Passport savedPassport = controller.getSavedPassport();
                if (savedPassport != null) {
                    registeredPassportsManager.addPassport(savedPassport);
                    refreshPassportTables(savedPassport);
                }
            } else if (controller.isCancelled() && controller.isNumberReserved()) {
                passportService.rollbackLastNumber(decimal, controller.getReservedNumber());
            }
        } catch (IOException ex) {
            log.error("Ошибка при открытии формы создания паспорта", ex);
            Warning1.create("ОШИБКА!", "Не удалось открыть форму создания паспорта", ex.getMessage());
        }
    }

    /**
     * Редактирование существующего паспорта.
     *
     * @param passport паспорт для редактирования
     */
    private void editPassport(Passport passport) {
        try {
            Passport freshPassport = passportService.getPassportByNumber(passport.getNumber());
            if (freshPassport == null) {
                Warning1.create("ОШИБКА!", "Номер не найден в базе данных", "Появится после перезагрузки");
                registeredPassportsManager.refresh();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/registrationForm.fxml"));
            Parent parent = loader.load();

            RegistrationFormController controller = loader.getController();
            controller.setDataForEdit(freshPassport);

            new WindowDecoration("Редактирование номера", parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                refreshPassportTables(freshPassport);

                Passport updatedPassport = controller.getSavedPassport();
                if (updatedPassport != null) {
                    registeredPassportsManager.updatePassport(passport, updatedPassport);
                }
            }
        } catch (IOException ex) {
            log.error("Ошибка при открытии формы редактирования паспорта", ex);
            Warning1.create("ОШИБКА!", "Не удалось открыть форму редактирования номера", ex.getMessage());
        }
    }

    // ======================== ДОБАВЛЕНИЕ ДЕЦИМАЛЬНОЙ ГРУППЫ ========================

    /**
     * Добавление новой децимальной группы.
     * Открывает форму создания и добавляет группу в соответствующий список.
     */
    private void addDecimalGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/decimalsForm.fxml"));
            Parent parent = loader.load();

            DecimalFormController controller = loader.getController();
            controller.setData(null);

            new WindowDecoration("Добавление", parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                Decimal savedDecimal = controller.getSavedDecimal();
                if (savedDecimal != null) {
                    addDecimalToAppropriateList(savedDecimal);
                    log.info("Децимальная группа успешно добавлена: {}", savedDecimal.toUsefulString());
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при открытии формы добавления децимальной группы", e);
            Warning1.create("ОШИБКА!", "Не удалось открыть форму добавления", e.getMessage());
        }
    }

    /**
     * Добавление децимальной группы в соответствующий ListView с последующим выделением.
     *
     * @param decimal децимальная группа для добавления
     */
    private void addDecimalToAppropriateList(Decimal decimal) {
        DecimalGroupingService.DecimalGroup group = DecimalGroupingService.determineGroup(decimal);
        ListView<Decimal> targetListView = groupToListViewMap.get(group);

        if (targetListView == null) {
            log.warn("Не удалось определить целевой список для децимальной группы: {}", decimal.getName());
            loadAndDistributeDecimalGroups();
            return;
        }

        ObservableList<Decimal> currentItems = targetListView.getItems();

        // Проверка на дубликат
        boolean exists = currentItems.stream().anyMatch(d -> d.getId().equals(decimal.getId()));
        if (exists) {
            log.warn("Децимальная группа {} уже существует в списке", decimal.getName());
            Warning1.create($ATTENTION, "Такая группа уже существует",
                    "Децимальная группа '" + decimal.getName() + "' уже присутствует в списке");
            return;
        }

        // Добавление и сортировка
        currentItems.add(decimal);
        sortDecimalList(currentItems);

        // Поиск индекса по ID
        int newIndex = findDecimalIndexById(currentItems, decimal.getId());

        // Раскрытие панели и выделение
        expandTitledPane(group);
        selectDecimalInListView(targetListView, newIndex, decimal);
    }

    /**
     * Поиск индекса децимальной группы в списке по ID.
     *
     * @param items список элементов
     * @param id ID для поиска
     * @return индекс элемента или -1, если не найден
     */
    private int findDecimalIndexById(ObservableList<Decimal> items, Long id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Раскрывает TitledPane в Accordion для указанной группы.
     *
     * @param group группа, панель которой нужно раскрыть
     */
    private void expandTitledPane(DecimalGroupingService.DecimalGroup group) {
        TitledPane pane = getTitledPaneForGroup(group);
        if (pane != null && !pane.isExpanded()) {
            pane.setExpanded(true);
            log.debug("Раскрыта панель для группы: {}", group.getTitle());
        }
    }

    /**
     * Возвращает TitledPane для указанной группы.
     *
     * @param group группа
     * @return соответствующая TitledPane или null
     */
    private TitledPane getTitledPaneForGroup(DecimalGroupingService.DecimalGroup group) {
        switch (group) {
            case SKETCH: return tpSketches;
            case DETAILS_700: return tpDetails700;
            case DETAILS_745: return tpDetails745;
            case ASSM_300: return tpAssm300;
            case ASSM_400: return tpAssm400;
            case MEDICINE: return tpMedicine;
            case OTHER: return tpOther;
            default: return null;
        }
    }

    /**
     * Выделяет децимальную группу в указанном ListView.
     *
     * @param listView список, в котором нужно выделить элемент
     * @param index индекс элемента
     * @param decimal децимальная группа для логирования
     */
    private void selectDecimalInListView(ListView<Decimal> listView, int index, Decimal decimal) {
        if (index >= 0) {
            Platform.runLater(() -> {
                listView.scrollTo(index);
                listView.getSelectionModel().select(index);
                listView.requestFocus();
                log.debug("Выделена децимальная группа {} в списке, индекс: {}", decimal.getName(), index);
            });
        } else {
            log.warn("Не удалось найти индекс для выделения децимальной группы: {}", decimal.getName());
        }
    }

    // ======================== ОБНОВЛЕНИЕ ТАБЛИЦ ========================

    /**
     * Обновление таблиц паспортов (ПИК и эскизы).
     * Вызывается из контекстного меню и после создания/редактирования паспорта.
     */
    private void refreshPassportTables() {
        refreshPassportTables(null);
    }

    /**
     * Обновление таблиц паспортов (ПИК и эскизы).
     *
     * @param passportToSelect паспорт для выделения после обновления (может быть null)
     */
    private void refreshPassportTables(Passport passportToSelect) {
        refreshPIKPassportsTable(passportToSelect);
        refreshSketchPassportsTable(passportToSelect);
        log.debug("Таблицы паспортов обновлены");
    }

    /**
     * Обновление таблицы ПИК паспортов.
     *
     * @param passportToSelect паспорт для выделения
     */
    private void refreshPIKPassportsTable(Passport passportToSelect) {
        if (passportPIKController != null && passportPIKController.getPassportsTable() != null) {
            Passport_TableView tvPIK = passportPIKController.getPassportsTable();
            tvPIK.refreshPreservingType();
            if (passportToSelect != null && passportService.isPIKPassport(passportToSelect)) {
                selectPassportInTable(tvPIK, passportToSelect);
            }
        }
    }

    /**
     * Обновление таблицы эскизных паспортов.
     *
     * @param passportToSelect паспорт для выделения
     */
    private void refreshSketchPassportsTable(Passport passportToSelect) {
        if (passportSketchController != null && passportSketchController.getPassportsTable() != null) {
            Passport_TableView tvSketch = passportSketchController.getPassportsTable();
            tvSketch.refreshPreservingType();
            if (passportToSelect != null && passportService.isSketchPassport(passportToSelect)) {
                selectPassportInTable(tvSketch, passportToSelect);
            }
        }
    }

    /**
     * Выделяет паспорт в таблице.
     *
     * @param tableView таблица для выделения
     * @param passport паспорт для выделения
     */
    private void selectPassportInTable(Passport_TableView tableView, Passport passport) {
        Platform.runLater(() -> {
            for (int i = 0; i < tableView.getItems().size(); i++) {
                if (tableView.getItems().get(i).getId().equals(passport.getId())) {
                    tableView.scrollTo(i);
                    tableView.getSelectionModel().select(i);
                    log.debug("Выделен паспорт {} в таблице, индекс: {}", passport.getNumber(), i);
                    return;
                }
            }
            log.debug("Паспорт {} не найден в таблице после обновления", passport.getNumber());
        });
    }

    /**
     * Обновление после удаления паспорта.
     * Обновляет список выбранных паспортов и таблицы.
     */
    private void refreshAfterDelete() {
        registeredPassportsManager.refresh();
        refreshPassportTables();
        log.info("Выполнено обновление после удаления");
    }

    // ======================== ДЕЙСТВИЯ С КНОПКАМИ ========================

    /**
     * Очистка списка выбранных паспортов.
     * Показывает диалог подтверждения перед очисткой.
     */
    @FXML
    private void clearSelectedList() {
        if (registeredPassportsManager.isEmpty()) {
            Warning1.create($ATTENTION, "Список уже пуст", "Нечего очищать");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение очистки");
        confirmAlert.setHeaderText("Очистка списка выбранных паспортов");
        confirmAlert.setContentText("Вы действительно хотите очистить весь список?\n" +
                "Всего паспортов в списке: " + registeredPassportsManager.size());

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            registeredPassportsManager.clear();
        }
    }

    /**
     * Экспорт списка выбранных паспортов в файл.
     */
    @FXML
    private void exportSelectedListToFile() {
        if (registeredPassportsManager.isEmpty()) {
            Warning1.create($ATTENTION, "Список пуст", "Нечего экспортировать");
            return;
        }

        boolean exported = RegisteredPassportsStorage.exportRegisteredPassportsToFile(
                registeredPassportsManager.getList(), "Новые номера.txt");

        if (exported) {
            Warning1.create("ОТЛИЧНО!", "Экспорт выполнен", "Список успешно сохранен!");
        }
    }

    // ======================== ОБНОВЛЕНИЕ ВКЛАДКИ ========================

    /**
     * Обновление содержимого вкладки при её активации.
     * Загружает свежие данные из базы данных и восстанавливает состояние.
     */
    @Override
    public void updateTab() {
        loadAndDistributeDecimalGroups();
        registeredPassportsManager.restoreState();
        refreshPassportTables();
        log.info("Вкладка журнала регистрации обновлена");
    }
}