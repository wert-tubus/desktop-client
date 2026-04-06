package ru.wert.tubus.chogori.application.cardsbox;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.entities.decimals.Decimal_ACCController;
import ru.wert.tubus.chogori.entities.passports.PassportInfo_Patch;
import ru.wert.tubus.chogori.entities.passports.Passport_PatchController;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.interfaces.UpdatableTabController;
import ru.wert.tubus.winform.enums.EOperation;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

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

    // Компоненты Accordion для группировки Decimal
    @FXML
    private Accordion accDecimalGroups;               // Аккордеон с группами децимальных номеров

    @FXML
    private ListView<Decimal> lvSketches;             // Эскиз (только один элемент)

    @FXML
    private ListView<Decimal> lvDetails700;           // Decimal 700000-744999

    @FXML
    private ListView<Decimal> lvDetails745;           // Decimal 745000-799999

    @FXML
    private ListView<Decimal> lvAssm300;              // Decimal 300000-399999

    @FXML
    private ListView<Decimal> lvAssm400;              // Decimal 400000-499999

    @FXML
    private ListView<Decimal> lvMedicine;             // Decimal 900000-999999

    @FXML
    private ListView<Decimal> lvOther;                // Остальные Decimal

    // ======================== ПОЛЯ ДАННЫХ ========================

    private ObservableList<Passport> selectedPassportsList;  // Список выбранных паспортов

    private PassportContextMenu selectedContextMenu;         // Контекстное меню для списка паспортов

    @Setter
    private Passport_PatchController passportPIKController;    // Контроллер таблицы ПИК паспортов

    @Setter
    private Passport_PatchController passportSketchController; // Контроллер таблицы эскизных паспортов

    // Константы для группировки Decimal
    private static final String SKETCH_NAME = "Эскиз";

    // Диапазоны для группировки
    private static final int DETAILS_700_START = 700000;
    private static final int DETAILS_700_END = 744999;
    private static final int DETAILS_745_START = 745000;
    private static final int DETAILS_745_END = 799999;
    private static final int ASSM_300_START = 300000;
    private static final int ASSM_300_END = 399999;
    private static final int ASSM_400_START = 400000;
    private static final int ASSM_400_END = 499999;
    private static final int MEDICINE_START = 900000;
    private static final int MEDICINE_END = 999999;

    // ======================== ИНИЦИАЛИЗАЦИЯ ========================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Инициализация списка выбранных паспортов
        selectedPassportsList = FXCollections.observableArrayList();
        lvListOFNumbers.setItems(selectedPassportsList);

        // Настройка отображения списка паспортов
        setupNewNumbersListView(lvListOFNumbers);

        // Восстановление ранее выбранных паспортов (по номерам, без загрузки всех паспортов)
        restoreSelectedPassportsState();

        // Загрузка и распределение децимальных групп по спискам
        loadAndDistributeDecimalGroups();

        // Настройка обработчиков кнопок
        setupButtons();
        setupBtnAddDecimalGroup();

        // Настройка обработчиков двойного клика для всех списков децимальных групп
        setupDecimalGroupsDoubleClickHandlers();

        // Настройка контекстного меню для списка паспортов
        setupContextMenus();
    }

    // ======================== НАСТРОЙКА ОТОБРАЖЕНИЯ ========================

    /**
     * Настройка отображения элементов в списке новых номеров.
     *
     * @param listView список для настройки
     */
    private void setupNewNumbersListView(ListView<Passport> listView) {
        listView.setCellFactory(lv -> new ListCell<Passport>() {
            @Override
            protected void updateItem(Passport item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toUsefulString());
                    setOnMouseClicked(e -> {
                        // По двойному клику открывается окно с информацией
                        if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2)
                            PassportInfo_Patch.create(getItem());
                    });
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

    // ======================== ЗАГРУЗКА И ГРУППИРОВКА ДАННЫХ ========================

    /**
     * Загрузка и распределение децимальных групп по соответствующим ListView.
     */
    private void loadAndDistributeDecimalGroups() {
        try {
            List<Decimal> allDecimals = ChogoriServices.CH_DECIMALS.findAll();
            log.info("Загружено {} децимальных групп", allDecimals.size());

            distributeDecimalsToLists(allDecimals);
        } catch (Exception e) {
            log.error("Ошибка при загрузке децимальных групп", e);
            Warning1.create("ОШИБКА!", "Ошибка загрузки данных", "Не удалось загрузить децимальные группы: " + e.getMessage());
            // Инициализируем пустые списки
            initializeEmptyLists();
        }
    }

    /**
     * Распределение децимальных групп по соответствующим ListView.
     *
     * @param allDecimals список всех децимальных групп
     */
    private void distributeDecimalsToLists(List<Decimal> allDecimals) {
        ObservableList<Decimal> sketches = FXCollections.observableArrayList();
        ObservableList<Decimal> details700 = FXCollections.observableArrayList();
        ObservableList<Decimal> details745 = FXCollections.observableArrayList();
        ObservableList<Decimal> assm300 = FXCollections.observableArrayList();
        ObservableList<Decimal> assm400 = FXCollections.observableArrayList();
        ObservableList<Decimal> medicine = FXCollections.observableArrayList();
        ObservableList<Decimal> other = FXCollections.observableArrayList();

        for (Decimal decimal : allDecimals) {
            if (SKETCH_NAME.equals(decimal.getName())) {
                sketches.add(decimal);
            } else {
                Integer numericValue = parseDecimalNameToInt(decimal.getName());
                if (numericValue == null) {
                    other.add(decimal);
                    continue;
                }

                if (numericValue >= DETAILS_700_START && numericValue <= DETAILS_700_END) {
                    details700.add(decimal);
                } else if (numericValue >= DETAILS_745_START && numericValue <= DETAILS_745_END) {
                    details745.add(decimal);
                } else if (numericValue >= ASSM_300_START && numericValue <= ASSM_300_END) {
                    assm300.add(decimal);
                } else if (numericValue >= ASSM_400_START && numericValue <= ASSM_400_END) {
                    assm400.add(decimal);
                } else if (numericValue >= MEDICINE_START && numericValue <= MEDICINE_END) {
                    medicine.add(decimal);
                } else {
                    other.add(decimal);
                }
            }
        }

        // Сортировка всех списков по имени
        sortDecimalList(sketches);
        sortDecimalList(details700);
        sortDecimalList(details745);
        sortDecimalList(assm300);
        sortDecimalList(assm400);
        sortDecimalList(medicine);
        sortDecimalList(other);

        // Установка элементов в ListView
        lvSketches.setItems(sketches);
        lvDetails700.setItems(details700);
        lvDetails745.setItems(details745);
        lvAssm300.setItems(assm300);
        lvAssm400.setItems(assm400);
        lvMedicine.setItems(medicine);
        lvOther.setItems(other);

        // Настройка отображения
        setupListViewDecimalGroups(lvSketches);
        setupListViewDecimalGroups(lvDetails700);
        setupListViewDecimalGroups(lvDetails745);
        setupListViewDecimalGroups(lvAssm300);
        setupListViewDecimalGroups(lvAssm400);
        setupListViewDecimalGroups(lvMedicine);
        setupListViewDecimalGroups(lvOther);
    }

    /**
     * Инициализация пустых списков при ошибке загрузки.
     */
    private void initializeEmptyLists() {
        lvSketches.setItems(FXCollections.observableArrayList());
        lvDetails700.setItems(FXCollections.observableArrayList());
        lvDetails745.setItems(FXCollections.observableArrayList());
        lvAssm300.setItems(FXCollections.observableArrayList());
        lvAssm400.setItems(FXCollections.observableArrayList());
        lvMedicine.setItems(FXCollections.observableArrayList());
        lvOther.setItems(FXCollections.observableArrayList());

        setupListViewDecimalGroups(lvSketches);
        setupListViewDecimalGroups(lvDetails700);
        setupListViewDecimalGroups(lvDetails745);
        setupListViewDecimalGroups(lvAssm300);
        setupListViewDecimalGroups(lvAssm400);
        setupListViewDecimalGroups(lvMedicine);
        setupListViewDecimalGroups(lvOther);
    }

    /**
     * Сортировка списка децимальных групп по имени.
     *
     * @param list список для сортировки
     */
    private void sortDecimalList(ObservableList<Decimal> list) {
        list.sort(Comparator.comparing(Decimal::getName));
    }

    /**
     * Преобразование имени децимальной группы в числовое значение.
     *
     * @param name имя децимальной группы
     * @return числовое значение или null, если преобразование невозможно
     */
    private Integer parseDecimalNameToInt(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return null;
        }
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
     * Настройка обработчиков двойного клика для всех списков децимальных групп.
     * При двойном клике открывается форма создания нового паспорта.
     */
    private void setupDecimalGroupsDoubleClickHandlers() {
        setupDoubleClickHandler(lvSketches);
        setupDoubleClickHandler(lvDetails700);
        setupDoubleClickHandler(lvDetails745);
        setupDoubleClickHandler(lvAssm300);
        setupDoubleClickHandler(lvAssm400);
        setupDoubleClickHandler(lvMedicine);
        setupDoubleClickHandler(lvOther);
    }

    /**
     * Настройка обработчика двойного клика для конкретного ListView.
     *
     * @param listView список для настройки
     */
    private void setupDoubleClickHandler(ListView<Decimal> listView) {
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Decimal selectedDecimal = listView.getSelectionModel().getSelectedItem();
                if (selectedDecimal != null) {
                    if (SKETCH_NAME.equals(selectedDecimal.getName())) {
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
            Warning1.create("ОШИБКА!", "Не удалось создать паспорт ПИК", e.getMessage());
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
            Warning1.create("ОШИБКА!", "Не удалось создать эскизный паспорт", e.getMessage());
        }
    }

    /**
     * Получение следующего доступного номера для паспорта ПИК.
     *
     * @param decimal децимальная группа
     * @return следующий доступный номер
     */
    private String getNextPIKNumber(Decimal decimal) {
        try {
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal == null) {
                throw new RuntimeException("Децимальная группа не найдена");
            }

            List<Passport> allPassports = getAllPassportsFromDatabase();

            int currentLastNumber = freshDecimal.getLastNumber();
            int initialNumber = freshDecimal.getInitialNumber();

            Integer freeNumber = findFreePIKNumber(freshDecimal.getName(), allPassports, initialNumber, currentLastNumber);

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
     * Поиск свободного номера ПИК в диапазоне.
     */
    private Integer findFreePIKNumber(String decimalName, List<Passport> allPassports, int start, int end) {
        try {
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

    private String formatPIKNumber(String decimalName, int number) {
        return String.format("%s.%03d", decimalName, number);
    }

    /**
     * Получение следующего доступного номера для эскизного паспорта.
     */
    private String getNextSketchNumber(Decimal decimal) {
        try {
            Decimal freshDecimal = ChogoriServices.CH_DECIMALS.findById(decimal.getId());
            if (freshDecimal == null) {
                throw new RuntimeException("Децимальная группа не найдена");
            }

            List<Passport> allPassports = getAllPassportsFromDatabase();

            int currentLastNumber = freshDecimal.getLastNumber();
            int initialNumber = freshDecimal.getInitialNumber();

            Integer freeNumber = findFreeSketchNumber(allPassports, initialNumber, currentLastNumber);

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

    private Integer findFreeSketchNumber(List<Passport> allPassports, int start, int end) {
        try {
            Set<Integer> usedNumbers = allPassports.stream()
                    .filter(p -> (p.getPrefix() == null
                            || p.getPrefix().getName() == null
                            || "-".equals(p.getPrefix().getName())))
                    .filter(p -> p.getNumber() != null && p.getNumber().startsWith("Э"))
                    .map(p -> {
                        try {
                            return Integer.parseInt(p.getNumber().substring(1));
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

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

    private String formatSketchNumber(int number) {
        return String.format("Э%05d", number);
    }

    // ======================== ДОБАВЛЕНИЕ ДЕЦИМАЛЬНОЙ ГРУППЫ ========================

    /**
     * Добавление новой децимальной группы.
     * Открывает окно создания, проверяет уникальность и добавляет в соответствующий список.
     */
    private void addDecimalGroup() {
        try {
            // Загружаем FXML форму
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/entities/decimal-form.fxml"));
            Parent parent = loader.load();

            Decimal_ACCController controller = loader.getController();

            // Инициализируем контроллер для операции CREATE
            controller.init(EOperation.ADD, null, null);

            // Открываем окно
            new WindowDecoration("Добавление децимальной группы", parent, false, WF_MAIN_STAGE, true);

            // Проверяем, был ли создан новый объект
            Decimal newDecimal = controller.getNewItem();
            if (newDecimal == null || newDecimal.getName() == null || newDecimal.getName().isEmpty()) {
                log.debug("Создание децимальной группы отменено или не выполнено");
                return;
            }

            // Проверяем уникальность имени в БД
            if (isDecimalNameExists(newDecimal.getName())) {
                Warning1.create("Внимание!",
                        "Децимальная группа с таким именем уже существует",
                        "Введите другое наименование");
                return;
            }

            // Сохраняем в БД
            Decimal savedDecimal = ChogoriServices.CH_DECIMALS.save(newDecimal);
            if (savedDecimal == null || savedDecimal.getId() == null) {
                Warning1.create("ОШИБКА!",
                        "Не удалось сохранить децимальную группу",
                        "Пожалуйста, попробуйте снова");
                return;
            }

            // Добавляем в соответствующий ListView
            addDecimalToAppropriateList(savedDecimal);

            log.info("Децимальная группа {} успешно добавлена", savedDecimal.getName());

            // Показываем сообщение об успехе
            Warning1.create("УСПЕХ!",
                    "Децимальная группа добавлена",
                    "Группа " + savedDecimal.getName() + " успешно создана");

        } catch (IOException e) {
            log.error("Ошибка при открытии формы добавления децимальной группы", e);
            Warning1.create("ОШИБКА!",
                    "Не удалось открыть форму добавления",
                    e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при добавлении децимальной группы", e);
            Warning1.create("ОШИБКА!",
                    "Не удалось добавить децимальную группу",
                    e.getMessage());
        }
    }

    /**
     * Проверка существования децимальной группы с таким именем в БД.
     *
     * @param name имя для проверки
     * @return true если существует
     */
    private boolean isDecimalNameExists(String name) {
        try {
            List<Decimal> allDecimals = ChogoriServices.CH_DECIMALS.findAll();
            return allDecimals.stream()
                    .anyMatch(d -> d.getName() != null && d.getName().equals(name));
        } catch (Exception e) {
            log.error("Ошибка при проверке уникальности имени", e);
            return true; // В случае ошибки считаем, что имя существует
        }
    }

    /**
     * Добавление децимальной группы в соответствующий ListView на основе её имени.
     *
     * @param decimal децимальная группа для добавления
     */
    private void addDecimalToAppropriateList(Decimal decimal) {
        String name = decimal.getName();

        if (SKETCH_NAME.equals(name)) {
            addToListAndSort(lvSketches, decimal);
            return;
        }

        Integer numericValue = parseDecimalNameToInt(name);
        if (numericValue == null) {
            addToListAndSort(lvOther, decimal);
            return;
        }

        if (numericValue >= DETAILS_700_START && numericValue <= DETAILS_700_END) {
            addToListAndSort(lvDetails700, decimal);
        } else if (numericValue >= DETAILS_745_START && numericValue <= DETAILS_745_END) {
            addToListAndSort(lvDetails745, decimal);
        } else if (numericValue >= ASSM_300_START && numericValue <= ASSM_300_END) {
            addToListAndSort(lvAssm300, decimal);
        } else if (numericValue >= ASSM_400_START && numericValue <= ASSM_400_END) {
            addToListAndSort(lvAssm400, decimal);
        } else if (numericValue >= MEDICINE_START && numericValue <= MEDICINE_END) {
            addToListAndSort(lvMedicine, decimal);
        } else {
            addToListAndSort(lvOther, decimal);
        }
    }

    /**
     * Добавление элемента в ListView с последующей сортировкой.
     *
     * @param listView целевой ListView
     * @param decimal элемент для добавления
     */
    private void addToListAndSort(ListView<Decimal> listView, Decimal decimal) {
        ObservableList<Decimal> items = listView.getItems();
        if (items == null) {
            listView.setItems(FXCollections.observableArrayList(decimal));
            setupListViewDecimalGroups(listView);
        } else {
            items.add(decimal);
            items.sort(Comparator.comparing(Decimal::getName));
        }
    }

    // ======================== ДИАЛОГОВЫЕ ОКНА ========================

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
                    refreshPassportTables(savedPassport);
                }
            } else if (controller.isCancelled() && controller.isNumberReserved()) {
                rollbackLastNumber(decimal, controller.getReservedNumber());
            }

        } catch (IOException ex) {
            log.error("Ошибка при открытии формы создания паспорта", ex);
            Warning1.create("ОШИБКА!", "Не удалось открыть форму создания паспорта", ex.getMessage());
        }
    }

    private void editPassport(Passport passport) {
        try {
            Passport freshPassport = getPassportByNumber(passport.getNumber());
            if (freshPassport == null) {
                Warning1.create("ОШИБКА!", "Паспорт не найден в базе данных", "Появится после перезагрузки");
                refreshSelectedList();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/registrationForm.fxml"));
            Parent parent = loader.load();

            RegistrationFormController controller = loader.getController();
            controller.setDataForEdit(freshPassport);

            new WindowDecoration("Редактирование паспорта", parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                refreshPassportTables(freshPassport);

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
            Warning1.create("ОШИБКА!", "Не удалось открыть форму редактирования паспорта", ex.getMessage());
        }
    }

    // ======================== УПРАВЛЕНИЕ СПИСКАМИ ========================

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

    @FXML
    private void exportSelectedListToFile() {
        if (selectedPassportsList.isEmpty()) {
            Warning1.create($ATTENTION, "Список пуст", "Нечего экспортировать");
            return;
        }

        String defaultFileName = "Новые номера.txt";

        boolean exported = SelectedPassportsStorage.exportSelectedPassportsToFile(selectedPassportsList, defaultFileName);

        if (exported) {
            Warning1.create("ОТЛИЧНО!", "Экспорт выполнен", "Список успешно сохранен!");
        }
    }

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

    private void refreshPassportTables() {
        refreshPassportTables(null);
    }

    private void refreshPassportTables(Passport passportToSelect) {
        if (passportPIKController != null && passportPIKController.getPassportsTable() != null) {
            Passport_TableView tvPIK = passportPIKController.getPassportsTable();
            tvPIK.refreshPreservingType();

            if (passportToSelect != null && isPIKPassport(passportToSelect)) {
                selectPassportInTable(tvPIK, passportToSelect);
            }
        }

        if (passportSketchController != null && passportSketchController.getPassportsTable() != null) {
            Passport_TableView tvSketch = passportSketchController.getPassportsTable();
            tvSketch.refreshPreservingType();

            if (passportToSelect != null && isSketchPassport(passportToSelect)) {
                selectPassportInTable(tvSketch, passportToSelect);
            }
        }

        log.debug("Таблицы паспортов обновлены");
    }

    private boolean isPIKPassport(Passport passport) {
        return passport.getPrefix() != null
                && "ПИК".equals(passport.getPrefix().getName())
                && passport.getNumber() != null
                && passport.getNumber().matches("\\d{6}\\.\\d{3}");
    }

    private boolean isSketchPassport(Passport passport) {
        boolean prefixCondition = passport.getPrefix() == null
                || "-".equals(passport.getPrefix().getName());
        return prefixCondition
                && passport.getNumber() != null
                && passport.getNumber().matches("Э\\d{5}");
    }

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

    private int findPassportIndex(Passport_TableView tableView, Passport passport) {
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (tableView.getItems().get(i).getId().equals(passport.getId())) {
                return i;
            }
        }
        return -1;
    }

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

    private void refreshAfterDelete() {
        refreshSelectedList();
        refreshPassportTables(null);
        log.info("Выполнено обновление после удаления");
    }

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

    private void saveSelectedPassportsState() {
        if (selectedPassportsList != null && !selectedPassportsList.isEmpty()) {
            SelectedPassportsStorage.saveSelectedPassports(selectedPassportsList);
        } else {
            SelectedPassportsStorage.clearSavedState();
        }
    }

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

            if (restoredPassports.size() != savedPassportNumbers.size()) {
                saveSelectedPassportsState();
            }
        } else {
            log.warn("Не найдены паспорта для сохраненных номеров: {}", savedPassportNumbers);
            SelectedPassportsStorage.clearSavedState();
        }
    }

    // ======================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ========================

    @Override
    public void updateTab() {
        loadAndDistributeDecimalGroups();
        restoreSelectedPassportsState();
        refreshPassportTables(null);
        log.info("Вкладка журнала регистрации обновлена");
    }
}