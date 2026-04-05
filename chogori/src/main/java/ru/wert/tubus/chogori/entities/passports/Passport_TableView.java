package ru.wert.tubus.chogori.entities.passports;

import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.Sorting;
import ru.wert.tubus.chogori.common.tableView.RoutineTableView;
import ru.wert.tubus.chogori.entities.passports.commands._Passport_Commands;
import ru.wert.tubus.chogori.previewer.PreviewerPatchController;
import ru.wert.tubus.chogori.application.services.ChogoriServices;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_SEARCH_FIELD;

@Slf4j
public class Passport_TableView extends RoutineTableView<Passport> implements Sorting<Passport> {

    private static final String accWindowRes = "/chogori-fxml/passports/passportsACC.fxml";
    private final _Passport_Commands commands;
    @Getter private PreviewerPatchController previewerController;

    private Object modifyingItem; //Product или Folder
    @Getter private Passport_ACCController accController;
    @Setter private Object modifyingClass;

    private List<Passport> shownList = new ArrayList<>(); //Лист карточек, отображаемых в таблице сейчас
    @Getter@Setter private String searchedText = "";

    @Getter@Setter private List<Folder> selectedFolders;

    // Добавляем поле для типа паспортов
    @Getter@Setter private PassportType passportType = PassportType.ALL;
    @Getter@Setter private boolean showPrefix = true;

    // Паттерны для фильтрации (аналогичны CardsBoxController)
    private static final Pattern PIK_PATTERN = Pattern.compile("\\d{6}\\.\\d{3}");
    private static final Pattern SKETCH_PATTERN = Pattern.compile("Э\\d{5}");

    // Контекстное меню
    private Passport_ContextMenu contextMenu;

    private TableColumn<Passport, String> tcId;
    private TableColumn<Passport, HBox> tcPassport; //Колонка Идентификатор
    private TableColumn<Passport, Label> tcPassportNumber; //Номер чертежа
    private TableColumn<Passport, Label> tcPassportName; //Наименование чертежа
    private TableColumn<Passport, Label> tcPassportNote; //Изделие, комментарий
    private TableColumn<Passport, Label> tcPassportUser; //Пользователь
    private TableColumn<Passport, Label> tcPassportDate; //Дата создания

    //Показывать колонки
    @Getter@Setter private boolean showId; //Идентификатор
    @Getter@Setter private boolean showIdentity; //Идентификатор
    @Getter@Setter private boolean showNumber; //Дец номер
    @Getter@Setter private boolean showName; //Наименование
    @Getter@Setter private boolean showNote; //Изделие
    @Getter@Setter private boolean showUser; //Пользователь
    @Getter@Setter private boolean showDate; //Дата

    /**
     * Конструктор для таблицы, связанной с предпросмотром чертежей
     * @param promptText String, текст, добавляемый в поисковую строку
     * @param previewerController PreviewerNoTBController контроллер окна предпросмотра
     */
    public Passport_TableView(String promptText, PreviewerPatchController previewerController, boolean useContextMenu, boolean switchSearch) {
        this(promptText);
        this.previewerController = previewerController;

//        new Passport_Manipulator(this);

        //Создаем контекстное меню
        if (useContextMenu) {
            createContextMenu();
        }

        //Здесь происходит включение поиска по чертежам
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(switchSearch && newValue)
                CH_SEARCH_FIELD.changeSearchedTableView(this, "КАРТОЧКА");
        });

    }

    /**
     * Конструктор для таблицы без предпросмотра
     * @param promptText String, текст, добавляемый в поисковую строку
     */
    public Passport_TableView(String promptText) {
        super(promptText);
        commands = new _Passport_Commands(this);
    }

    @Override
    public void setTableColumns() {
        tcId = Passport_Columns.createTcId();
        tcPassport = Passport_Columns.createTcPassport(showPrefix);
        tcPassportNumber = Passport_Columns.createTcPassportNumber();
        tcPassportName = Passport_Columns.createTcPassportName();
        tcPassportNote = Passport_Columns.createTcNote();
        tcPassportUser = Passport_Columns.createTcUserName();
        tcPassportDate = Passport_Columns.createTcDate();

        getColumns().addAll(tcId, tcPassport, tcPassportNumber, tcPassportName, tcPassportNote, tcPassportUser, tcPassportDate);
    }

    /**
     * Метод выключает ненужные столбцы
     */
    public void showTableColumns(boolean showTcId, boolean showPassportInOneColumn, boolean showNote, boolean showUser, boolean showDate){
        tcId.setVisible(showTcId);
        this.showId = showTcId;

        tcPassport.setVisible(showPassportInOneColumn); //Показывает дец номер и наименование в одном столбце
        this.showIdentity = showPassportInOneColumn;

        tcPassportNumber.setVisible(!showPassportInOneColumn);
        this.showNumber = !showPassportInOneColumn;

        tcPassportName.setVisible(!showPassportInOneColumn);
        this.showName = !showPassportInOneColumn;

        tcPassportNote.setVisible(showNote);
        this.showNote = showNote;

        tcPassportUser.setVisible(showUser);
        this.showUser = showUser;

        tcPassportDate.setVisible(showDate);
        this.showDate = showDate;
    }

    @Override
    public void createContextMenu() {
        // Создаем контекстное меню
        contextMenu = new Passport_ContextMenu(this, commands, accWindowRes);

        // Устанавливаем обработчик для показа контекстного меню
        setOnContextMenuRequested(event -> {
            // Получаем позицию клика
            javafx.scene.control.TablePosition<Passport, ?> pos = getSelectionModel()
                    .getSelectedCells()
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (pos == null) {
                // Клик на пустом месте - снимаем выделение
                getSelectionModel().clearSelection();
            }

            // Показываем контекстное меню
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    @Override
    public List<Passport> prepareList() {
        List<Passport> list = new ArrayList<>();

        // Получаем базовый список в зависимости от modifyingClass
        List<Passport> baseList;

        if (modifyingClass instanceof Folder) {
            if (selectedFolders == null || selectedFolders.isEmpty()) {
                if (modifyingItem == null) {
                    // Если нет ни selectedFolders, ни modifyingItem - загружаем ВСЕ паспорта
                    baseList = ChogoriServices.CH_QUICK_PASSPORTS.findAll();
                    log.debug("Загружены все паспорты (нет папок для фильтрации), всего: {}", baseList.size());
                } else {
                    // Загружаем паспорта из конкретной папки
                    baseList = new ArrayList<>(findPassportsInFolder((Folder) modifyingItem));
                    log.debug("Загружены паспорта из папки: {}, всего: {}", ((Folder) modifyingItem).getName(), baseList.size());
                }
            } else {
                // Загружаем паспорта из выбранных папок
                Set<Passport> combinedSet = new HashSet<>();
                for (Folder folder : selectedFolders) {
                    combinedSet.addAll(findPassportsInFolder(folder));
                }
                baseList = new ArrayList<>(combinedSet);
                selectedFolders = null;
                log.debug("Загружены паспорта из {} выбранных папок, всего: {}", combinedSet.size());
            }
        } else {
            // Если modifyingClass не Folder, возвращаем все паспорта
            baseList = ChogoriServices.CH_QUICK_PASSPORTS.findAll();
            log.debug("Загружены все паспорта (modifyingClass не Folder), всего: {}", baseList.size());
        }

        // Применяем фильтрацию по типу паспорта
        list = filterPassportsByType(baseList);
        list.sort(Comparator.comparing(Passport::getNumber));

        return list;
    }

    /**
     * Обновляет таблицу с сохранением фильтрации по типу и сортировки
     */
    public void refreshPreservingType() {
        log.debug("refreshPreservingType() - тип: {}", passportType);

        // Получаем базовый список
        List<Passport> baseList;
        if (modifyingItem instanceof Folder) {
            baseList = new ArrayList<>(findPassportsInFolder((Folder) modifyingItem));
        } else {
            baseList = ChogoriServices.CH_QUICK_PASSPORTS.findAll();
        }

        // Применяем фильтрацию по типу
        List<Passport> filteredList = filterPassportsByType(baseList);

        // Сортируем
        filteredList.sort(passportsComparator());

        // Обновляем таблицу
        getItems().setAll(filteredList);
    }

    /**
     * Фильтрует список паспортов в зависимости от текущего типа
     * @param passports исходный список паспортов
     * @return отфильтрованный список
     */
    private List<Passport> filterPassportsByType(List<Passport> passports) {
        if (passportType == null || passportType == PassportType.ALL) {
            return passports;
        }

        return passports.stream()
                .filter(this::matchesPassportType)
                .collect(Collectors.toList());
    }

    /**
     * Проверяет, соответствует ли паспорт текущему типу
     * @param passport проверяемый паспорт
     * @return true если соответствует
     */
    private boolean matchesPassportType(Passport passport) {
        Prefix prefix = passport.getPrefix();
        String number = passport.getNumber();

        if (number == null) return false;

        switch (passportType) {
            case PIK:
                return prefix != null
                        && "ПИК".equals(prefix.getName())
                        && PIK_PATTERN.matcher(number).matches();

            case SKETCHES:
                boolean prefixCondition = prefix == null || "-".equals(prefix.getName());
                return prefixCondition && SKETCH_PATTERN.matcher(number).matches();

            case ALL:
            default:
                return true;
        }
    }

    /**
     * Метод находит пасспорта в нужной папке, пасспорта не должны повторяться, поэтому используется Set<Passport>
     * @param folder Folder
     * @return Set<Passport>
     */
    private Set<Passport> findPassportsInFolder(Folder folder){
        Set<Passport> foundPassports = new HashSet<>();
        List<Draft> listOfDrafts = ChogoriServices.CH_QUICK_DRAFTS.findAllByFolder(folder);
        for(Draft d : listOfDrafts){
            foundPassports.add(d.getPassport());
        }
        return foundPassports;
    }

    @Override
    public ItemCommands<Passport> getCommands() {
        return commands;
    }

    @Override
    public String getAccWindowRes() {
        return accWindowRes;
    }

    /**
     * Устанавливает выделенный на данный момент элемент
     * @param modifyingItem Product или Folder
     */
    public void setModifyingItem(Object modifyingItem) {
        this.modifyingItem = modifyingItem;
    }

    /**
     * Возвращает выделенный на данный момент элемент Product или Folder
     */
    public Object getModifyingItem() {
        return modifyingItem;
    }

    @Override
    public void sortItemList(List<Passport> list) {
        list.sort(passportsComparator());
    }

    /**
     * Компаратор сравнивает чертеж по НОМЕРУ -> ТИПУ -> СТРАНИЦЕ
     */
    public static Comparator<Passport> passportsComparator() {
        return (o1, o2) -> {
            int result = o1.toUsefulString()
                    .compareTo(o2.toUsefulString());
            return result;
        };
    }

    @Override //Searchable
    public List<Passport> getCurrentItemSearchedList() {
        return shownList;
    }

    @Override //Searchable
    public void setCurrentItemSearchedList(List<Passport> currentItemList) {
        this.shownList = currentItemList;
    }

    public void setAccController(FormView_ACCController<Passport> accController){
        this.accController = (Passport_ACCController) accController;
    }

    /**
     * Обновить контекстное меню (вызывается при изменении выделения)
     */
    public void updateContextMenu() {
        if (contextMenu != null) {
            contextMenu.createMainMenuItems();
        }
    }

    /**
     * Принудительное обновление таблицы с очисткой кэша.
     */
    public void forceRefresh() {
        log.debug("Принудительное обновление таблицы паспортов, тип: {}", passportType);

        // Сбрасываем фильтры и выделения
        setSelectedFolders(null);
        setModifyingItem(null);

        // Обновляем таблицу
        updateTableView();
    }
}
