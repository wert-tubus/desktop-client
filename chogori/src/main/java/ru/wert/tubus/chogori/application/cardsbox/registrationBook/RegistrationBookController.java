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
import ru.wert.tubus.chogori.application.cardsbox.*;
import ru.wert.tubus.chogori.components.BtnDown;
import ru.wert.tubus.chogori.components.BtnUp;
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


    @FXML
    TextArea taDescriptionESKD;

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

    @FXML private Button btnUp;
    @FXML private Button btnDown;

    // ======================== СЕРВИСЫ И МЕНЕДЖЕРЫ ========================

    private final PassportService passportService = new PassportService();
    private RegisteredPassportsManager registeredPassportsManager;
    private final Map<DecimalGroupingService.DecimalGroup, ListView<Decimal>> groupToListViewMap = new EnumMap<>(DecimalGroupingService.DecimalGroup.class);

    @Setter private Passport_PatchController passportPIKController;
    @Setter private Passport_PatchController passportSketchController;
    @Setter private CardsBoxController cardsBoxController;

    // Хранение текущего фильтра для ПИК таблицы (для эскизов фильтрации нет)
    private Decimal currentPIKFilterDecimal = null;

    // ======================== ИНИЦИАЛИЗАЦИЯ ========================

    /**
     * Инициализация контроллера после загрузки FXML.
     * Настраивает все компоненты, загружает данные и восстанавливает состояние.
     */
    @FXML
    public void initialize() {
        new BtnUp<>(btnUp, lvListOFNumbers, () -> registeredPassportsManager.saveState());
        new BtnDown<>(btnDown, lvListOFNumbers, () -> registeredPassportsManager.saveState());

        initializeSelectedPassportsList();
        initializeDecimalGroupsLists();
        setupButtonHandlers();
        setupContextMenus();

        // Восстановление состояния
        registeredPassportsManager.restoreState();

        Platform.runLater(this::setupSketchListViews);
        Platform.runLater(this::setupPIKListViews);
    }

    /**
     * Настраивает списки децимальных групп для эскизных номеров.
     * Эскизы НЕ фильтруются по decimal, всегда показываются все эскизные номера (маска ЭXXXXX).
     * Двойной клик - создание эскизного паспорта.
     * Одинарный клик - открытие вкладки с эскизами.
     */
    private void setupSketchListViews() {
        lvSketches.setOnMouseClicked(event -> {
            Decimal selected = lvSketches.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (event.getClickCount() == 2) {
                    // Двойной клик - создание эскизного паспорта
                    createSketchPassport(selected);
                } else if (event.getClickCount() == 1) {
                    // Одинарный клик - открытие вкладки с эскизами (без фильтрации)
                    openSketchTab();
                }
            }
        });
    }

    /**
     * Настраивает списки децимальных групп для ПИК-номеров.
     * Одинарный клик - фильтрация таблицы по выбранной decimal.
     * Двойной клик - создание нового паспорта.
     */
    private void setupPIKListViews() {
        List<ListView<Decimal>> listViews = Arrays.asList(
                lvDetails700, lvDetails745, lvAssm300, lvAssm400, lvMedicine, lvOther
        );

        for (ListView<Decimal> listView : listViews) {
            listView.setCellFactory(lv -> new ListCell<Decimal>() {
                @Override
                protected void updateItem(Decimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });

            // Добавляем обработчик клика мыши для фильтрации таблицы
            listView.setOnMouseClicked(event -> {
                Decimal selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (event.getClickCount() == 2) {
                        // Двойной клик - создание нового паспорта
                        createPIKPassport(selected);
                    } else if (event.getClickCount() == 1) {
                        // Одинарный клик - фильтрация таблицы
                        openPIKTab();
                        currentPIKFilterDecimal = selected;
                        showDescriptionESKD(selected);
                        filterPIKTableByDecimal(selected);
                    }
                }
            });
        }
    }

    /**
     * Метод выводит описание децимальной группы по ЕСКД
     */
    private void showDescriptionESKD(Decimal decimal) {
        taDescriptionESKD.setText(decimal.getDescription());
    }

    /**
     * Фильтрация ПИК таблицы по децимальной группе.
     *
     * @param decimal децимальная группа для фильтрации
     */
    private void filterPIKTableByDecimal(Decimal decimal) {
        if (passportPIKController != null && passportPIKController.getPassportsTable() != null) {
            Passport_TableView tvPIK = passportPIKController.getPassportsTable();

            // Устанавливаем внешний фильтр в таблице
            tvPIK.setExternalFilterAndRefresh(decimal);

            // Сохраняем текущий фильтр
            currentPIKFilterDecimal = decimal;

            // Прокручиваем к последнему элементу
            if (!tvPIK.getItems().isEmpty()) {
                tvPIK.scrollTo(tvPIK.getItems().size() - 1);
            }

            log.debug("ПИК таблица отфильтрована по децимальной группе: {}", decimal.getName());
        }
    }

    /**
     * Обновление ПИК таблицы с сохранением текущего фильтра.
     */
    private void refreshPIKTablePreservingFilter() {
        if (passportPIKController != null && passportPIKController.getPassportsTable() != null) {
            Passport_TableView tvPIK = passportPIKController.getPassportsTable();

            if (currentPIKFilterDecimal != null) {
                // Устанавливаем фильтр заново (он сам обновит таблицу)
                tvPIK.setExternalFilterAndRefresh(currentPIKFilterDecimal);
                log.debug("ПИК таблица обновлена с сохранением фильтра по: {}", currentPIKFilterDecimal.getName());
            } else {
                // Сбрасываем внешний фильтр и обновляем
                tvPIK.setExternalFilterAndRefresh(null);
                log.debug("ПИК таблица обновлена без фильтра");
            }
        }
    }

    /**
     * Обновление эскизной таблицы (всегда показывает все эскизы, без фильтрации).
     */
    private void refreshSketchTable() {
        if (passportSketchController != null && passportSketchController.getPassportsTable() != null) {
            Passport_TableView tvSketch = passportSketchController.getPassportsTable();
            tvSketch.refreshPreservingType();
            log.debug("Эскизная таблица обновлена (все эскизы)");
        }
    }

    /**
     * Открывает вкладку с ПИК-номерами и прокручивает таблицу к последнему элементу.
     */
    private void openPIKTab() {
        if (passportPIKController != null && cardsBoxController.getTabPIK() != null) {
            // Активируем вкладку с ПИК-паспортами
            cardsBoxController.getTabPIK().getTabPane().getSelectionModel()
                    .select(cardsBoxController.getTabPIK());

            TableView<Passport> tvPIK = passportPIKController.getPassportsTable();
            if (tvPIK != null && !tvPIK.getItems().isEmpty()) {
                tvPIK.scrollTo(tvPIK.getItems().size() - 1);
            }
        }
    }

    /**
     * Открывает вкладку с эскизными номерами (все эскизы, без фильтрации).
     */
    private void openSketchTab() {
        if (passportSketchController != null && cardsBoxController.getTabSketch() != null) {
            // Активируем вкладку с эскизами
            cardsBoxController.getTabSketch().getTabPane().getSelectionModel()
                    .select(cardsBoxController.getTabSketch());
            TableView<Passport> tvSketches = passportSketchController.getPassportsTable();
            if (!tvSketches.getItems().isEmpty()) {
                tvSketches.scrollTo(tvSketches.getItems().size() - 1);
            }
        }
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
                    refreshAfterPassportCreation(savedPassport, decimal);
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
     * Обновление после создания паспорта.
     *
     * @param savedPassport созданный паспорт
     * @param decimal децимальная группа
     */
    private void refreshAfterPassportCreation(Passport savedPassport, Decimal decimal) {
        // Просто раскрываем панель с нужной группой
        expandTitledPane(DecimalGroupingService.determineGroup(decimal));

        // Обновляем таблицы
        refreshTablesPreservingState();

        log.info("Паспорт {} успешно создан", savedPassport.getNumber());
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
                refreshTablesPreservingState();

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

    /**
     * Обновление таблиц с сохранением текущего состояния.
     * Для ПИК - сохраняет фильтр, для эскизов - просто обновляет.
     */
    private void refreshTablesPreservingState() {
        refreshPIKTablePreservingFilter();
        refreshSketchTable();
        log.debug("Таблицы обновлены с сохранением состояния");
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
     * Добавление децимальной группы в соответствующий ListView.
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

        // Раскрываем панель
        expandTitledPane(group);

        log.debug("Децимальная группа {} добавлена в список", decimal.getName());
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

    // ======================== ОБНОВЛЕНИЕ ТАБЛИЦ ========================

    /**
     * Обновление таблиц паспортов (ПИК и эскизы).
     * Вызывается из контекстного меню.
     */
    private void refreshPassportTables() {
        refreshTablesPreservingState();
    }

    /**
     * Обновление после удаления паспорта.
     * Обновляет список выбранных паспортов и таблицы с сохранением состояния.
     */
    private void refreshAfterDelete() {
        registeredPassportsManager.refresh();
        refreshTablesPreservingState();
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
     * Загружает свежие данные из базы данных, но сохраняет текущие фильтры.
     */
    @Override
    public void updateTab() {
        // Обновляем списки децимальных групп
        loadAndDistributeDecimalGroups();

        // Восстанавливаем состояние выбранных паспортов
        registeredPassportsManager.restoreState();

        // Обновляем таблицы с сохранением текущих фильтров
        refreshTablesPreservingState();

        log.info("Вкладка журнала регистрации обновлена (фильтры сохранены)");
    }
}