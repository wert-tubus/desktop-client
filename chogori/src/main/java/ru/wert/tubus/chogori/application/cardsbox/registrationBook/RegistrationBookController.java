package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.cardsbox.CardsBoxController;
import ru.wert.tubus.chogori.application.cardsbox.DecimalFormController;
import ru.wert.tubus.chogori.application.cardsbox.RegistrationBookContextMenu;
import ru.wert.tubus.chogori.application.cardsbox.RegistrationFormController;
import ru.wert.tubus.chogori.components.BtnDownForTable;
import ru.wert.tubus.chogori.components.BtnUpForTable;
import ru.wert.tubus.chogori.entities.passports.PassportInfo_Patch;
import ru.wert.tubus.chogori.entities.passports.Passport_PatchController;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.chogori.setteings.ChogoriSettings;
import ru.wert.tubus.client.entity.models.Decimal;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.warnings.Warning2;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_DECIMALS;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_PASSPORTS;
import static ru.wert.tubus.chogori.images.BtnImages.BTN_EDIT_IMG;
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
public class RegistrationBookController {

    // ======================== КОНСТАНТЫ ========================
    private static final Image DRAFTS_EXIST_IMG = new Image(
            RegistrationBookController.class.getResourceAsStream("/chogori-pics/btns/view(32x32).png"),
            20, 20, true, true
    );
    private static final Image NO_DRAFTS_IMG = new Image(
            RegistrationBookController.class.getResourceAsStream("/chogori-pics/btns/close.png"),
            20, 20, true, true
    );

    // ======================== FXML КОМПОНЕНТЫ ========================

    @FXML @Getter private TableView<RegisteredPassportItem> lvRegisteredPassports; // Таблица зарегистрированных чертежей
    @FXML private TableColumn<RegisteredPassportItem, Passport> colPassport;
    @FXML private TableColumn<RegisteredPassportItem, Boolean> colHasDrafts;
    @FXML private TableColumn<RegisteredPassportItem, String> colNote;
    @FXML private Button btnAddDecimalGroup;
    @FXML private Button btnClear;
    @FXML private Button btnSave;
    @FXML private Button btnLoad;
    @FXML private Button btnPrint;
    @FXML private Accordion accDecimalGroups;

    @FXML private TextArea taDescriptionESKD;

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

    @FXML private Button btnAddSeparator;
    @FXML private Button btnUp;
    @FXML private Button btnDown;
    @FXML private Button btnEditDescription;

    private final Map<TitledPane, ListView<Decimal>> paneToListViewMap = new HashMap<>();

    // ======================== СЕРВИСЫ И МЕНЕДЖЕРЫ ========================

    private final RegistrationService registrationService = new RegistrationService();
    @Getter private RegisteredPassportsManager registeredPassportsManager;
    private final Map<DecimalGroupingService.DecimalGroup, ListView<Decimal>> groupToListViewMap = new EnumMap<>(DecimalGroupingService.DecimalGroup.class);
    private final RegistrationBookPrintService printService = new RegistrationBookPrintService();
    private PassportListFileManager fileManager;
    private RegistrationBookContextMenu registrationBookContextMenu;

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
        initializeRegisteredPassportsTable();
        initializeDecimalGroupsLists();
        setupButtonHandlers();
        setupContextMenus();

        new BtnUpForTable(btnUp, lvRegisteredPassports, () -> registeredPassportsManager.saveState());
        new BtnDownForTable(btnDown, lvRegisteredPassports, () -> registeredPassportsManager.saveState());

        // Восстановление состояния
        registeredPassportsManager.restoreState();

        Platform.runLater(() -> {
            setupSketchListView();
            setupPIKListViews();
            expandGroup745();
        });

        new RegistrationBookManipulator(this);
    }

    /**
     * Инициализация таблицы зарегистрированных паспортов.
     * Настраивает колонки и обработчики двойного клика.
     */
    private void initializeRegisteredPassportsTable() {
        ObservableList<RegisteredPassportItem> registeredItems = FXCollections.observableArrayList();
        lvRegisteredPassports.setItems(registeredItems);
        registeredPassportsManager = new RegisteredPassportsManager(registeredItems, registrationService);

        // Инициализация менеджера файлов
        fileManager = new PassportListFileManager(
                registrationService,
                passport -> registeredPassportsManager.addPassport(passport),  // для одиночных добавлений
                () -> registeredPassportsManager.clear(),
                () -> refreshTablesPreservingState(),
                this::showLoadingCursorAndDisableControls,
                this::hideLoadingCursorAndEnableControls
        );

        // Настройка колонки с паспортом
        colPassport.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPassport()));
        colPassport.setCellFactory(column -> new TableCell<RegisteredPassportItem, Passport>() {
            @Override
            protected void updateItem(Passport item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                    setGraphic(null);
                } else {
                    setText(item.toUsefulString());

                    // Устанавливаем цвет в зависимости от типа чертежа
                    String number = item.getNumber();
                    if (number != null && !number.isEmpty()) {
                        if (number.startsWith("Э")) {
                            setStyle("-fx-text-fill: #7322a3; -fx-font-size: 14; -fx-font-weight: bold;");
                        } else {
                            String firstChar = number.substring(0, 1);
                            switch (firstChar) {
                                case "7":
                                    setStyle("-fx-text-fill: darkgreen; -fx-font-size: 14; -fx-font-weight: bold;");
                                    break;
                                case "3":
                                    setStyle("-fx-text-fill: darkblue; -fx-font-size: 14; -fx-font-weight: bold;");
                                    break;
                                case "4":
                                    setStyle("-fx-text-fill: saddlebrown; -fx-font-size: 14; -fx-font-weight: bold;");
                                    break;
                                default:
                                    setStyle("-fx-text-fill: black; -fx-font-size: 14; -fx-font-weight: bold;");
                            }
                        }
                    }
                }
            }
        });

        // Настройка колонки с наличием чертежей
        colHasDrafts.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().hasDrafts()));
        colHasDrafts.setCellFactory(column -> new TableCell<RegisteredPassportItem, Boolean>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(20);
                imageView.setFitHeight(20);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Boolean hasDrafts, boolean empty) {
                super.updateItem(hasDrafts, empty);
                if (empty || hasDrafts == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    imageView.setImage(hasDrafts ? DRAFTS_EXIST_IMG : NO_DRAFTS_IMG);
                    setGraphic(imageView);
                    setText(null);
                    // Добавляем всплывающую подсказку
                    setTooltip(new Tooltip(hasDrafts ? "Есть чертежи" : "Нет чертежей"));
                }
            }
        });
        colHasDrafts.setStyle("-fx-alignment: CENTER;");

        // Устанавливаем фиксированную ширину для колонки с иконкой
        colHasDrafts.setPrefWidth(50);
        colHasDrafts.setMaxWidth(50);
        colHasDrafts.setMinWidth(50);

// Настройка колонки с описанием (note) - используем noteProperty из RegisteredPassportItem
        colNote.setCellValueFactory(cellData -> cellData.getValue().noteProperty());
        colNote.setCellFactory(column -> new TableCell<RegisteredPassportItem, String>() {
            @Override
            protected void updateItem(String note, boolean empty) {
                super.updateItem(note, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else if (note == null || note.isEmpty()) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(note);
                    setStyle("-fx-font-size: 13;");
                }
            }
        });
        colNote.setStyle("-fx-alignment: CENTER-LEFT;");

        // Настройка растяжения колонки описания на все свободное место
        colPassport.prefWidthProperty().bind(lvRegisteredPassports.widthProperty().multiply(0.35));
        colHasDrafts.prefWidthProperty().bind(lvRegisteredPassports.widthProperty().multiply(0.05));
        colNote.prefWidthProperty().bind(lvRegisteredPassports.widthProperty().multiply(0.60));

        // Установка минимальных ширин
        colPassport.setMinWidth(150);
        colHasDrafts.setMinWidth(50);
        colNote.setMinWidth(100);

        // Добавляем обработчик двойного клика
        lvRegisteredPassports.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
                RegisteredPassportItem selected = lvRegisteredPassports.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getPassport() != null) {
                    PassportInfo_Patch.create(selected.getPassport());
                }
            }
        });

        // Добавляем возможность перемещения с помощью стрелок и копирование в буфер обмена
        lvRegisteredPassports.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                // Стрелки уже обрабатываются стандартным поведением
                Platform.runLater(() -> registeredPassportsManager.saveState());
            } else if (event.isControlDown() && event.getCode() == KeyCode.C) {
                // Ctrl+C для копирования
                if (registrationBookContextMenu != null) {
                    boolean copied = registrationBookContextMenu.copyPassportNameToClipboard();
                    if (copied) {
                        event.consume();
                    }
                }
            }
        });

        // Сохраняем состояние при изменении порядка (после перетаскивания)
        lvRegisteredPassports.getColumns().addListener((javafx.collections.ListChangeListener.Change<? extends TableColumn<RegisteredPassportItem, ?>> change) -> {
            while (change.next()) {
                if (change.wasReplaced()) {
                    registeredPassportsManager.saveState();
                }
            }
        });
    }

    /**
     * Настраивает списки децимальных групп для эскизных номеров.
     * Эскизы НЕ фильтруются по decimal, всегда показываются все эскизные номера (маска ЭXXXXX).
     * Выделение строки - обновление описания.
     * Enter или двойной клик - создание эскизного паспорта.
     * Одинарный клик - открытие вкладки с эскизами.
     */
    private void setupSketchListView() {
        lvSketches.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showDescriptionESKD(newSelection);
            }
        });

        // Добавляем обработку нажатия клавиш
        lvSketches.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Decimal selected = lvSketches.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    createSketchPassport(selected);
                    event.consume();
                }
            }
        });

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
     * Выделение строки - обновление описания.
     * Enter или двойной клик - создание нового паспорта.
     * Одинарный клик - фильтрация таблицы по выбранной decimal.
     * Правый клик - открытие контекстного меню для редактирования.
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
                        String number = item.getName();
                        setText(number);
                    }
                }
            });

            // Добавляем контекстное меню для правой кнопки мыши
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Изменить");
            editItem.setOnAction(event -> {
                Decimal selected = getSelectedDecimal();
                if (selected != null) {
                    editDescription();
                }
            });

            MenuItem deleteItem = new MenuItem("Удалить");
            deleteItem.setOnAction(event -> {
                Decimal selected = getSelectedDecimal();
                if (selected != null) {
                    deleteDecimalGroup();
                }
            });
            contextMenu.getItems().addAll(editItem, deleteItem);
            listView.setContextMenu(contextMenu);

            // Добавляем слушатель выделения для обновления описания
            listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showDescriptionESKD(newSelection);
                }
            });

            // Добавляем обработку нажатия клавиш
            listView.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    Decimal selected = listView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        createPIKPassport(selected);
                        event.consume();
                    }
                }
            });

            // Добавляем обработчик клика мыши для фильтрации таблицы
            listView.setOnMouseClicked(event -> {
                Decimal selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (event.getClickCount() == 2 && event.getButton().equals(MouseButton.PRIMARY)) {
                        // Двойной клик - создание нового паспорта
                        createPIKPassport(selected);
                    } else if (event.getClickCount() == 1 && event.getButton().equals(MouseButton.PRIMARY)) {
                        // Одинарный левый клик - фильтрация таблицы
                        openPIKTab();
                        currentPIKFilterDecimal = selected;
                        filterPIKTableByDecimal(selected);
                    }
                    // Правый клик обрабатывается контекстным меню, ничего не делаем
                }
            });
        }
    }

    /**
     * Удаление выбранной децимальной группы.
     * Проверяет наличие связанных паспортов перед удалением.
     */
    private void deleteDecimalGroup() {
        // Получаем выбранный Decimal из открытой панели аккордеона
        Decimal selected = getSelectedDecimal();

        // Проверяем, выбран ли элемент
        if (selected == null) {
            Warning1.create($ATTENTION, "Ничего не выбрано",
                    "Пожалуйста, выберите децимальную группу для удаления");
            return;
        }

        // Проверяем наличие связанных паспортов
        if (hasLinkedPassports(selected)) {
            Warning1.create($ATTENTION, "Невозможно удалить децимальную группу",
                    String.format("Децимальная группа '%s' не может быть удалена,\n" +
                            "так как с ней связаны существующие паспорта.", selected.getName()));
            log.warn("Попытка удаления Decimal {} отклонена: существуют связанные паспорта", selected.getName());
            return;
        }

        // Подтверждение удаления
        boolean confirmed = Warning2.create("Подтверждение удаления",
                "Удаление децимальной группы",
                String.format("Вы действительно хотите удалить децимальную группу '%s'?\n\n" +
                        "ВНИМАНИЕ: Операция необратима!", selected.getName()));

        if (!confirmed) {
            return;
        }

        // Блокируем контролы перед операцией
        showLoadingCursorAndDisableControls();

        // Выполняем удаление в отдельном потоке
        Decimal finalSelected = selected;
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Удаляем через сервис
                boolean deleted = CH_DECIMALS.delete(finalSelected);
                if (deleted) {
                    log.info("Децимальная группа успешно удалена: {}", finalSelected.toUsefulString());
                    return true;
                } else {
                    log.error("Не удалось удалить децимальную группу: {}", finalSelected.getName());
                    return false;
                }
            } catch (Exception e) {
                log.error("Ошибка при удалении децимальной группы {}: {}", finalSelected.getName(), e.getMessage(), e);
                return false;
            }
        });

        future.thenAcceptAsync(success -> {
            Platform.runLater(() -> {
                hideLoadingCursorAndEnableControls();

                if (success) {
                    // Перезагружаем все списки, чтобы обновить их состояние
                    loadAndDistributeDecimalGroups();

                    // Очищаем описание, если удалена выбранная группа
                    if (taDescriptionESKD != null && getSelectedDecimal() == null) {
                        taDescriptionESKD.clear();
                    }

                    Warning1.create("УСПЕШНО!", "Децимальная группа удалена",
                            String.format("Группа '%s' успешно удалена", finalSelected.getName()));
                } else {
                    Warning1.create("ОШИБКА!", "Не удалось удалить децимальную группу",
                            "Проверьте подключение к базе данных и повторите попытку");
                }
            });
        }, Platform::runLater);
    }

    /**
     * Проверяет, есть ли у Decimal связанные Passport с префиксом по умолчанию.
     *
     * @param decimal проверяемый десятичный классификатор
     * @return true - если есть хотя бы один связанный Passport, false - если нет
     */
    private boolean hasLinkedPassports(Decimal decimal) {
        String decimalName = decimal.getName(); // Например "745222"

        // Ищем все паспорта, содержащие в номере этот decimal
        List<Passport> passports = CH_PASSPORTS.findAllByText(decimalName);

        if (passports == null || passports.isEmpty()) {
            return false;
        }

        // Проверяем, есть ли паспорт с нужным префиксом и номером, начинающимся с decimalName
        for (Passport passport : passports) {
            if (passport.getPrefix() != null &&
                    passport.getNumber() != null &&
                    ChogoriSettings.CH_DEFAULT_PREFIX.equals(passport.getPrefix()) &&
                    passport.getNumber().startsWith(decimalName + ".")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Открытие панели с группой 745 при загрузке контроллера.
     * Приоритетно раскрывается DETAILS_745, если данные уже загружены.
     */
    private void expandGroup745() {
        if (accDecimalGroups != null && tpDetails745 != null) {
            accDecimalGroups.setExpandedPane(tpDetails745);
            log.debug("Панель DETAILS_745 раскрыта при загрузке");
        }
    }

    /**
     * Метод выводит описание децимальной группы по ЕСКД
     */
    private void showDescriptionESKD(Decimal decimal) {
        if (taDescriptionESKD != null && decimal != null) {
            taDescriptionESKD.setText(decimal.getDescription());
        }
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
        if (passportPIKController != null && cardsBoxController != null && cardsBoxController.getTabPIK() != null) {
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
        if (passportSketchController != null && cardsBoxController != null && cardsBoxController.getTabSketch() != null) {
            // Активируем вкладку с эскизами
            cardsBoxController.getTabSketch().getTabPane().getSelectionModel()
                    .select(cardsBoxController.getTabSketch());
            TableView<Passport> tvSketches = passportSketchController.getPassportsTable();
            if (tvSketches != null && !tvSketches.getItems().isEmpty()) {
                tvSketches.scrollTo(tvSketches.getItems().size() - 1);
            }
        }
    }

    // ======================== НАСТРОЙКА КОМПОНЕНТОВ ========================

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

        // Инициализация маппинга панелей к спискам
        paneToListViewMap.put(tpSketches, lvSketches);
        paneToListViewMap.put(tpDetails700, lvDetails700);
        paneToListViewMap.put(tpDetails745, lvDetails745);
        paneToListViewMap.put(tpAssm300, lvAssm300);
        paneToListViewMap.put(tpAssm400, lvAssm400);
        paneToListViewMap.put(tpMedicine, lvMedicine);
        paneToListViewMap.put(tpOther, lvOther);

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
     * Возвращает выбранный Decimal из открытой (развернутой) панели аккордеона.
     */
    public Decimal getSelectedDecimal() {
        if (accDecimalGroups == null || paneToListViewMap.isEmpty()) {
            return null;
        }

        TitledPane expandedPane = accDecimalGroups.getExpandedPane();
        if (expandedPane == null) {
            return null;
        }

        ListView<Decimal> targetListView = paneToListViewMap.get(expandedPane);
        if (targetListView == null) {
            return null;
        }

        return targetListView.getSelectionModel().getSelectedItem();
    }

    /**
     * Настройка обработчиков кнопок.
     */
    private void setupButtonHandlers() {
        if (btnAddDecimalGroup != null) {
            btnAddDecimalGroup.setOnAction(e -> addDecimalGroup());
        }
        if (btnClear != null) {
            btnClear.setOnAction(e -> clearRegisteredList());
        }
        if (btnSave != null) {
            btnSave.setOnAction(e -> exportRegisteredListToFile());
        }
        if (btnLoad != null) {
            btnLoad.setOnAction(e -> loadRegisteredList());
        }
        if (btnPrint != null) {
            btnPrint.setOnAction(e -> printList());
        }
        if (btnEditDescription != null) {
            btnEditDescription.setId("patchButton");
            btnEditDescription.setTooltip(new Tooltip("Редактировать описание"));
            btnEditDescription.setGraphic(new ImageView(BTN_EDIT_IMG));
            btnEditDescription.setOnAction(e -> editDescription());
        }
    }

    /**
     * Печать списка новых чертежей
     */
    private void printList() {
        if (registeredPassportsManager.isEmpty()) {
            Warning1.create($ATTENTION, "Список пуст", "Нечего печатать");
            return;
        }

        // Получаем список RegisteredPassportItem и преобразуем в список для печати
        List<RegisteredPassportPrintItem> printItems = registeredPassportsManager.getList().stream()
                .map(item -> new RegisteredPassportPrintItem(
                        item.getPassport(),
                        item.hasDrafts()
                ))
                .collect(java.util.stream.Collectors.toList());

        printService.printPassportsList(printItems);
    }

    /**
     * Изменение описания децимальной группы. Вызывается полное окно для редактирования Decimal
     */
    private void editDescription() {
        // Получаем выбранный Decimal из открытой панели аккордеона
        Decimal selected = getSelectedDecimal();

        // Проверяем, выбран ли элемент
        if (selected == null) {
            Warning1.create($ATTENTION, "Ничего не выбрано",
                    "Пожалуйста, выберите децимальную группу для редактирования");
            return;
        }

        // Находим ListView, в котором находится выбранный элемент
        ListView<Decimal> sourceListView = null;
        TitledPane expandedPane = accDecimalGroups.getExpandedPane();
        if (expandedPane != null) {
            sourceListView = paneToListViewMap.get(expandedPane);
        }

        // Если не нашли через открытую панель (страховочный вариант), ищем по всем спискам
        if (sourceListView == null) {
            List<ListView<Decimal>> listViews = Arrays.asList(
                    lvSketches, lvDetails700, lvDetails745, lvAssm300, lvAssm400, lvMedicine, lvOther
            );
            for (ListView<Decimal> listView : listViews) {
                if (listView.getSelectionModel().getSelectedItem() != null) {
                    sourceListView = listView;
                    break;
                }
            }
        }

        final ListView<Decimal> finalSourceListView = sourceListView;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/decimalsForm.fxml"));
            Parent parent = loader.load();

            DecimalFormController controller = loader.getController();

            // Устанавливаем данные для редактирования через setDataToEdit
            controller.setDataToEdit(selected);
            Platform.runLater(() -> {
                controller.getTfName().setEditable(false);
                controller.getTfName().setStyle("-fx-background-color: #d4d2d2");
                controller.getTaDescription().selectAll();
                controller.getTaDescription().requestFocus();
            });

            new WindowDecoration("Редактирование описания", parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                Decimal updatedDecimal = controller.getSavedDecimal();
                if (updatedDecimal != null) {
                    // Обновляем элемент в исходном списке
                    if (finalSourceListView != null) {
                        int index = finalSourceListView.getItems().indexOf(selected);
                        if (index != -1) {
                            finalSourceListView.getItems().set(index, updatedDecimal);
                            sortDecimalList(finalSourceListView.getItems());
                        }
                    } else {
                        // Если список не найден, обновляем все списки перезагрузкой
                        log.warn("Не удалось найти исходный список для обновления Decimal, выполняем перезагрузку");
                        loadAndDistributeDecimalGroups();
                    }

                    // Обновляем отображение описания, если редактируемая группа была выбрана
                    if (taDescriptionESKD != null) {
                        // Проверяем, не изменился ли выбранный элемент после редактирования
                        Decimal currentSelected = getSelectedDecimal();
                        if (currentSelected != null && currentSelected.getId().equals(updatedDecimal.getId())) {
                            taDescriptionESKD.setText(updatedDecimal.getDescription());
                        }
                    }

                    log.info("Децимальная группа успешно обновлена: {}", updatedDecimal.toUsefulString());
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при открытии формы редактирования децимальной группы", e);
            Warning1.create("ОШИБКА!", "Не удалось открыть форму редактирования", e.getMessage());
        }
    }

    /**
     * Возвращает текущий выбранный Decimal из любого списка групп.
     */
    private Decimal getCurrentlySelectedDecimal() {
        List<ListView<Decimal>> listViews = Arrays.asList(
                lvSketches, lvDetails700, lvDetails745, lvAssm300, lvAssm400, lvMedicine, lvOther
        );

        for (ListView<Decimal> listView : listViews) {
            Decimal selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                return selected;
            }
        }
        return null;
    }

    /**
     * Настройка контекстного меню для таблицы выбранных паспортов.
     */
    private void setupContextMenus() {
        registrationBookContextMenu = new RegistrationBookContextMenu(
                lvRegisteredPassports,
                this::editPassport,
                this::refreshPassportTables,
                () -> registeredPassportsManager.saveState(),
                this::showLoadingCursorAndDisableControls,
                this::hideLoadingCursorAndEnableControls
        );
        registrationBookContextMenu.setOnDeleteCallback(this::refreshAfterDelete);
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
        // Показываем индикацию загрузки
        showLoadingCursorAndDisableControls();

        CompletableFuture.supplyAsync(() -> {
            try {
                return registrationService.getNextPIKNumber(decimal);
            } catch (Exception e) {
                log.error("Ошибка при получении следующего номера ПИК", e);
                return null;
            }
        }).thenAccept(nextNumber -> {
            Platform.runLater(() -> {
                // Скрываем индикацию загрузки
                hideLoadingCursorAndEnableControls();

                if (nextNumber != null && !nextNumber.isEmpty()) {
                    openRegistrationDialog("PIK", "Номер ПИК", nextNumber, decimal);
                } else {
                    Warning1.create("ОШИБКА!", "Не удалось получить следующий номер",
                            "Проверьте подключение к базе данных");
                }
            });
        }).exceptionally(ex -> {
            log.error("Ошибка в асинхронной операции получения номера ПИК", ex);
            Platform.runLater(() -> {
                hideLoadingCursorAndEnableControls();
                Warning1.create("ОШИБКА!", "Не удалось создать паспорт ПИК",
                        ex.getMessage() != null ? ex.getMessage() : "Неизвестная ошибка");
            });
            return null;
        });
    }

    /**
     * Создание нового эскизного паспорта для указанной децимальной группы.
     *
     * @param decimal децимальная группа
     */
    private void createSketchPassport(Decimal decimal) {
        // Показываем индикацию загрузки
        showLoadingCursorAndDisableControls();

        CompletableFuture.supplyAsync(() -> {
            try {
                return registrationService.getNextSketchNumber(decimal);
            } catch (Exception e) {
                log.error("Ошибка при получении следующего эскизного номера", e);
                return null;
            }
        }).thenAccept(nextNumber -> {
            Platform.runLater(() -> {
                // Скрываем индикацию загрузки
                hideLoadingCursorAndEnableControls();

                if (nextNumber != null && !nextNumber.isEmpty()) {
                    openRegistrationDialog("SKETCH", "Эскизный номер", nextNumber, decimal);
                } else {
                    Warning1.create("ОШИБКА!", "Не удалось получить следующий эскизный номер",
                            "Проверьте подключение к базе данных");
                }
            });
        }).exceptionally(ex -> {
            log.error("Ошибка в асинхронной операции получения эскизного номера", ex);
            Platform.runLater(() -> {
                hideLoadingCursorAndEnableControls();
                Warning1.create("ОШИБКА!", "Не удалось создать эскизный паспорт",
                        ex.getMessage() != null ? ex.getMessage() : "Неизвестная ошибка");
            });
            return null;
        });
    }

    /**
     * Показывает курсор загрузки и блокирует все контролы на форме
     */
    public void showLoadingCursorAndDisableControls() {
        if (WF_MAIN_STAGE != null && WF_MAIN_STAGE.getScene() != null) {
            Scene scene = WF_MAIN_STAGE.getScene();
            scene.setCursor(javafx.scene.Cursor.WAIT);
        }

        // Блокируем все контролы
        disableAllControls(true);
    }

    /**
     * Скрывает курсор загрузки и разблокирует все контролы на форме
     */
    public void hideLoadingCursorAndEnableControls() {
        if (WF_MAIN_STAGE != null && WF_MAIN_STAGE.getScene() != null) {
            Scene scene = WF_MAIN_STAGE.getScene();
            scene.setCursor(javafx.scene.Cursor.DEFAULT);
        }

        // Разблокируем все контролы
        disableAllControls(false);
    }

    /**
     * Блокирует/разблокирует все контролы на форме
     *
     * @param disable true - блокировать, false - разблокировать
     */
    private void disableAllControls(boolean disable) {
        // Кнопки
        if (btnAddDecimalGroup != null) btnAddDecimalGroup.setDisable(disable);
        if (btnClear != null) btnClear.setDisable(disable);
        if (btnSave != null) btnSave.setDisable(disable);
        if (btnLoad != null) btnLoad.setDisable(disable);
        if (btnPrint != null) btnPrint.setDisable(disable);
        if (btnUp != null) btnUp.setDisable(disable);
        if (btnDown != null) btnDown.setDisable(disable);
        if (btnEditDescription != null) btnEditDescription.setDisable(disable);

        // Таблица зарегистрированных паспортов
        if (lvRegisteredPassports != null) lvRegisteredPassports.setDisable(disable);

        // Списки децимальных групп
        if (lvSketches != null) lvSketches.setDisable(disable);
        if (lvDetails700 != null) lvDetails700.setDisable(disable);
        if (lvDetails745 != null) lvDetails745.setDisable(disable);
        if (lvAssm300 != null) lvAssm300.setDisable(disable);
        if (lvAssm400 != null) lvAssm400.setDisable(disable);
        if (lvMedicine != null) lvMedicine.setDisable(disable);
        if (lvOther != null) lvOther.setDisable(disable);

        // Аккордеон
        if (accDecimalGroups != null) accDecimalGroups.setDisable(disable);

        // Текстовая область
        if (taDescriptionESKD != null) taDescriptionESKD.setDisable(disable);

        log.debug("Все контролы {}блокированы", disable ? "за" : "раз");
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

            // Передаем callback для управления индикацией
            controller.setLoadingCallbacks(
                    this::showLoadingCursorAndDisableControls,
                    this::hideLoadingCursorAndEnableControls
            );

            controller.setData(passportType, number, decimal);

            new WindowDecoration(windowTitle, parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                Passport savedPassport = controller.getSavedPassport();
                if (savedPassport != null) {
                    registeredPassportsManager.addPassport(savedPassport);
                    refreshAfterPassportCreation(savedPassport, decimal);
                }
            } else if (controller.isCancelled() && controller.isNumberReserved()) {
                registrationService.rollbackLastNumber(decimal, controller.getReservedNumber());
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
        // Раскрываем панель с нужной группой
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
            Passport freshPassport = registrationService.findPassportByNumberFast(passport.getNumber());
            if (freshPassport == null) {
                Warning1.create("ОШИБКА!", "Номер не найден в базе данных", "Появится после перезагрузки");
                registeredPassportsManager.refresh();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/registrationForm.fxml"));
            Parent parent = loader.load();

            RegistrationFormController controller = loader.getController();

            // Передаем callback для управления индикацией
            controller.setLoadingCallbacks(
                    this::showLoadingCursorAndDisableControls,
                    this::hideLoadingCursorAndEnableControls
            );

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
        // Блокируем контролы перед операцией
        showLoadingCursorAndDisableControls();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/decimalsForm.fxml"));
            Parent parent = loader.load();

            DecimalFormController controller = loader.getController();
            controller.setData(null);

            // Закрываем окно загрузки перед открытием формы
            hideLoadingCursorAndEnableControls();

            new WindowDecoration("Добавление", parent, false, WF_MAIN_STAGE, true);

            if (controller.isAccepted()) {
                Decimal savedDecimal = controller.getSavedDecimal();
                if (savedDecimal != null) {
                    // Снова блокируем контролы для операции добавления в БД
                    showLoadingCursorAndDisableControls();

                    // Выполняем добавление в отдельном потоке
                    CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            // Сохраняем в БД через сервис
                            Decimal result = CH_DECIMALS.save(savedDecimal);
                            return result != null;
                        } catch (Exception e) {
                            log.error("Ошибка при сохранении децимальной группы в БД", e);
                            return false;
                        }
                    });

                    future.thenAcceptAsync(success -> {
                        if (success) {
                            addDecimalToAppropriateList(savedDecimal);
                            log.info("Децимальная группа успешно добавлена: {}", savedDecimal.toUsefulString());
                        } else {
                            Warning1.create("ОШИБКА!", "Не удалось сохранить децимальную группу",
                                    "Проверьте подключение к базе данных");
                        }
                        hideLoadingCursorAndEnableControls();
                    }, Platform::runLater);
                } else {
                    hideLoadingCursorAndEnableControls();
                }
            } else {
                hideLoadingCursorAndEnableControls();
            }
        } catch (IOException e) {
            log.error("Ошибка при открытии формы добавления децимальной группы", e);
            hideLoadingCursorAndEnableControls();
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
    private void clearRegisteredList() {
        if (registeredPassportsManager.isEmpty()) {
            Warning1.create($ATTENTION, "Список уже пуст", "Нечего очищать");
            return;
        }

        boolean confirmed = Warning2.create("Подтверждение очистки",
                "Очистка списка выбранных чертежей",
                "Вы действительно хотите очистить весь список?\n" +
                        "Всего чертежей в списке: " + registeredPassportsManager.size());

        if (confirmed) {
            registeredPassportsManager.clear();
        }
    }

    /**
     * Экспорт списка выбранных паспортов в файл.
     */
    @FXML
    private void exportRegisteredListToFile() {
        if (fileManager == null) {
            log.error("FileManager не инициализирован");
            Warning1.create("ОШИБКА!", "Системная ошибка", "Менеджер файлов не инициализирован");
            return;
        }

        // Получаем список паспортов для экспорта
        List<Passport> passports = registeredPassportsManager.getList().stream()
                .map(RegisteredPassportItem::getPassport)
                .collect(java.util.stream.Collectors.toList());

        fileManager.exportToFile(passports, "Новые номера.txt");
    }

    /**
     * Загрузка списка выбранных паспортов из файла.
     */
    @FXML
    private void loadRegisteredList() {
        if (fileManager == null) {
            log.error("FileManager не инициализирован");
            Warning1.create("ОШИБКА!", "Системная ошибка", "Менеджер файлов не инициализирован");
            return;
        }
        fileManager.loadFromFile();
    }

    /**
     * Обновление информации о наличии чертежей для всех элементов в таблице.
     * Вызывается при обновлении вкладки или после изменений.
     */
    public void updateDraftsStatus() {
        if (registeredPassportsManager != null) {
            registeredPassportsManager.updateAllDraftsStatus();
        }
    }

    // ======================== ОБНОВЛЕНИЕ ВКЛАДКИ ========================

    /**
     * Обновление содержимого вкладки при её активации.
     * Загружает свежие данные из базы данных, но сохраняет текущие фильтры.
     */
    public void updateRegistrationBook() {
        // Обновляем списки децимальных групп
        loadAndDistributeDecimalGroups();

        // Восстанавливаем состояние выбранных паспортов
        registeredPassportsManager.restoreState();

        // Обновляем статус чертежей
        updateDraftsStatus();

        // Обновляем таблицы с сохранением текущих фильтров
        refreshTablesPreservingState();

        log.info("Вкладка журнала регистрации обновлена (фильтры сохранены)");
    }

    /**
     * Обработка удаления паспорта.
     * Удаляет паспорт из таблицы зарегистрированных паспортов, если он там присутствует.
     *
     * @param deletedPassport удаленный паспорт
     */
    public void handlePassportDeleted(Passport deletedPassport) {
        if (deletedPassport == null || deletedPassport.getNumber() == null) {
            log.warn("Попытка обработать удаление null паспорта или паспорта без номера");
            return;
        }

        // Ищем и удаляем паспорт из списка зарегистрированных
        RegisteredPassportItem toRemove = registeredPassportsManager.getList().stream()
                .filter(item -> {
                    Passport p = item.getPassport();
                    return p != null && p.getNumber() != null &&
                            p.getNumber().equals(deletedPassport.getNumber());
                })
                .findFirst()
                .orElse(null);

        if (toRemove != null) {
            registeredPassportsManager.getList().remove(toRemove);
            registeredPassportsManager.saveState();
            log.info("Паспорт {} удален из журнала регистрации", deletedPassport.getNumber());
        } else {
            log.debug("Паспорт {} не найден в журнале регистрации для удаления", deletedPassport.getNumber());
        }
    }

    /**
     * Обработка обновления паспорта.
     * Обновляет информацию о паспорте в таблице зарегистрированных паспортов.
     *
     * @param updatedPassport обновленный паспорт
     */
    public void handlePassportUpdated(Passport updatedPassport) {
        if (updatedPassport == null || updatedPassport.getNumber() == null) {
            log.warn("Попытка обработать обновление null паспорта или паспорта без номера");
            return;
        }

        // Ищем паспорт в списке зарегистрированных
        for (RegisteredPassportItem item : registeredPassportsManager.getList()) {
            Passport p = item.getPassport();
            if (p != null && p.getNumber() != null && p.getNumber().equals(updatedPassport.getNumber())) {
                // Обновляем данные паспорта
                item.setPassport(updatedPassport);
                item.setNote(updatedPassport.getNote());

                // Обновляем статус наличия чертежей
                registeredPassportsManager.updateDraftsStatusForPassport(updatedPassport);

                // Принудительно обновляем строку таблицы
                int index = registeredPassportsManager.getList().indexOf(item);
                if (index >= 0) {
                    registeredPassportsManager.getList().set(index, item);
                }

                registeredPassportsManager.saveState();
                log.info("Паспорт {} обновлен в журнале регистрации", updatedPassport.getNumber());
                return;
            }
        }

        log.debug("Паспорт {} не найден в журнале регистрации для обновления", updatedPassport.getNumber());
    }
}