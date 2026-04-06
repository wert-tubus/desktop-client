package ru.wert.tubus.chogori.entities.drafts;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import ru.wert.tubus.chogori.common.tableView.TableViewWithResizableColumns;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.Sorting;
import ru.wert.tubus.chogori.common.tableView.RoutineTableView;
import ru.wert.tubus.chogori.entities.drafts.commands._Draft_Commands;
import ru.wert.tubus.chogori.previewer.PreviewerPatchController;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.chogori.statics.Comparators;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.winform.enums.EDraftStatus;
import ru.wert.tubus.winform.enums.EDraftType;

import java.util.*;

import static ru.wert.tubus.chogori.statics.AppStatic.*;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_SEARCH_FIELD;
import static ru.wert.tubus.client.entity.serviceQUICK.DraftQuickService.LOADED_DRAFTS;

public class Draft_TableView extends RoutineTableView<Draft> implements Sorting<Draft> {

    private static final String accWindowRes = "/chogori-fxml/drafts/draftACC.fxml";
    private _Draft_Commands commands;
    private PreviewerPatchController previewerController;
    @Getter@Setter private Draft_PatchController draftPatchController;//нужен для доступа к showSourceOfPassports()
    @Setter private Object modifyingClass;
    @Getter@Setter private Object modifyingItem; //Product или Folder
    private List<Draft> currentItemList = new ArrayList<>(); //Лист чертежей, отображаемых в таблице сейчас
    private Draft_ACCController accController;
    @Getter private Draft_Manipulator manipulator;
    @Getter@Setter private String searchedText = "";

    @Getter@Setter private List<Folder> tempSelectedFolders; //Обнуляется после расчетов
    @Getter@Setter private List<Folder> selectedFolders;//ForContextMenu и не только

    @Getter ListProperty<Draft> preparedList = new SimpleListProperty<>();

    //Фильтр типов документов
    @Getter@Setter private boolean showDraftDocks = true; //Чертежи и 3D
    @Getter@Setter private boolean showDFXDocks; //DFX

    //Фильтр статусов
    @Getter@Setter private boolean showValid = true; //ДЕЙСТВУЮЩИЕ - по умолчанию
    @Getter@Setter private boolean showChanged; //ЗАМЕНЕНННЫЕ
    @Getter@Setter private boolean showAnnulled; //АННУЛИРОВАННЫЕ

    private TableColumn<Draft, String> tcId;
    private TableColumn<Draft, Label> tcDraftNumber; //Номер чертежа
    private TableColumn<Draft, Label> tcDraftName; //Наименование чертежа
    private TableColumn<Draft, Label> tcDraftType; //Тип чертежа
    private TableColumn<Draft, ImageView> tcRemarks; //Тип чертежа
    private TableColumn<Draft, Label> tcStatus; //Статус
    private TableColumn<Draft, String> tcInitialName; //Наименование файла
    private TableColumn<Draft, String> tcCreationTime; //Дата создания
    private TableColumn<Draft, String> tcNote;   //Колонка Комментарии

    //Показывать колонки
    @Getter@Setter private boolean showId; //Идентификатор
    @Getter@Setter private boolean showNumber; //Дец номер
    @Getter@Setter private boolean showName; //Наименование
    @Getter@Setter private boolean showDraftType  = true; //Тип чертежа
    @Getter@Setter private boolean showStatus  = true; //Статус
    @Getter@Setter private boolean showRemarks = true; //Примечания
    @Getter@Setter private boolean showInitialName; //Изначальное имя файла
    @Getter@Setter private boolean showCreationTime; //Время создания
    @Getter@Setter private boolean showNote  = true; //Примечание


    /**
     * Конструктор для таблицы, связанной с предпросмотром чертежей
     * @param promptText String, текст, добавляемый в поисковую строку
     * @param previewerController PreviewerNoTBController контроллер окна предпросмотра
     * @param switchSearch азрешить переключение поиска на ПОИСК ЧЕРТЕЖЕЙ
     */
    public Draft_TableView(String promptText, PreviewerPatchController previewerController, VBox vbox, boolean switchSearch) {
        super(promptText);
        this.previewerController = previewerController;

        new TableViewWithResizableColumns(this, vbox);

        manipulator = new Draft_Manipulator(this);

        commands = new _Draft_Commands(this);

        createContextMenu();

        //Слушатель работает с задержкой 0,5 сек
        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == getSelectionModel().getSelectedItem()) {
                if (newValue == null ||
                        newValue.getId() == null ||
                        previewerController.getCurrentDraft() == null ||
                        previewerController.getCurrentDraft().equals(newValue))
                    return;
                Platform.runLater(() -> {
                    AppStatic.openDraftInPreviewer(newValue, previewerController, false);
                });
            }

        });

        setOnMouseClicked(e -> {
            Draft selectedDraft = getSelectionModel().getSelectedItem();
            if (selectedDraft == null) return;
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                if (e.getClickCount() == 1)
                    Platform.runLater(() -> {
                        AppStatic.openDraftInPreviewer(selectedDraft, previewerController, true);
                    });
                else {
                    //Если приложение открывается сторонним приложением
                    if(OUTER_APP_LIST.contains(EDraftType.getDraftTypeById(selectedDraft.getDraftType())))
                        openInOuterApplication(selectedDraft);
                    else
                        AppStatic.openDraftsInNewTabs(getSelectionModel().getSelectedItems());
                }
                e.consume();
            };
        });

       //Здесь происходит включение поиска по чертежам
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(switchSearch && newValue)
                CH_SEARCH_FIELD.changeSearchedTableView(this, "ЧЕРТЕЖ");
        });


    }


    @Override
    public void setTableColumns() {

        tcId = Draft_Columns.createTcId(); //id Чертежа
        tcDraftNumber = Draft_Columns.createTcDraftNumber(); //Колонка Дец номер
        tcDraftName = Draft_Columns.createTcDraftName(); //Колонка Наименование
        tcDraftType = Draft_Columns.createTcDraftType(); //Тип чертежа
        tcRemarks = Draft_Columns.createTcRemarks(); //Наличие замечаний
        tcStatus = Draft_Columns.createTcStatus(); //Статус
        tcInitialName = Draft_Columns.createTcInitialDraftName(); //Наименование файла
        tcCreationTime = Draft_Columns.createTcCreation(); //Дата создания
        tcNote = Draft_Columns.createTcNote();   //Колонка Комментарии

        getColumns().addAll(tcId, tcDraftNumber, tcDraftName, tcDraftType, tcStatus, tcRemarks, tcInitialName, tcCreationTime, tcNote);

    }

    /**
     * Метод выключает ненужные столбцы
     */
    public void showTableColumns(boolean useTcId, boolean useTcDraftType, boolean useTcStatus,boolean useTcRemarks,
                                 boolean useTcInitialName, boolean useTcCreationTime, boolean useTcNote){
        tcId.setVisible(useTcId);
        showId = useTcId;

        tcDraftType.setVisible(useTcDraftType);
        showDraftType = useTcDraftType;

        tcStatus.setVisible(useTcStatus);
        showStatus = useTcStatus;

        tcRemarks.setVisible(useTcRemarks);
        showRemarks = useTcRemarks;

        tcInitialName.setVisible(useTcInitialName);
        showInitialName = useTcInitialName;

        tcCreationTime.setVisible(useTcCreationTime);
        showCreationTime = useTcCreationTime;

        tcNote.setVisible(useTcNote);
        showNote = useTcNote;

    }


    @Override
    public void createContextMenu() {
        setOnContextMenuRequested(event -> {
            contextMenu = new Draft_ContextMenu(this, commands, accWindowRes);
            contextMenu.show(this.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });

    }

    @Override
    public List<Draft> prepareList() {
        List<Draft> list = new ArrayList<>();
        if(modifyingClass instanceof Folder){
            if(tempSelectedFolders == null || tempSelectedFolders.isEmpty()) {
                if (modifyingItem == null)
                    list = new ArrayList<>(LOADED_DRAFTS);
                else {
                    list = new ArrayList<>(ChogoriServices.CH_QUICK_DRAFTS.findAllByFolder((Folder) modifyingItem));
                }
            } else {
                for(Folder folder: tempSelectedFolders){
                    list.addAll(ChogoriServices.CH_QUICK_DRAFTS.findAllByFolder(folder));
                }
                selectedFolders = tempSelectedFolders;
                tempSelectedFolders = null;
            }
        }

        else if(modifyingClass instanceof Passport){
            if(modifyingItem == null)
                list = new ArrayList<>();
            else {
                list = new ArrayList<>(ChogoriServices.CH_QUICK_DRAFTS.findByPassport((Passport)modifyingItem));
            }
        }

        filterList(list);
        ObservableList<Draft> finalList = FXCollections.observableArrayList(list);
        Platform.runLater(()->{
            preparedList.set(finalList);
        });

        return list;
    }

    /**
     * Метод фильтрует переданный список чертежей по статусу
     * @param items List<Draft>
     */
    public synchronized void filterList(List<Draft> items) {
        if(items.isEmpty()) return;
        Iterator<Draft> i = items.iterator();
        while (i.hasNext()) {
            Draft d = i.next();
            EDraftStatus status = EDraftStatus.getStatusById(d.getStatus());
            EDraftType type = EDraftType.getDraftTypeById(d.getDraftType());
            if (status != null && type != null) {
                if (
                        (status.equals(EDraftStatus.LEGAL) && !isShowValid()) ||
                        (status.equals(EDraftStatus.CHANGED) && !isShowChanged()) ||
                        (status.equals(EDraftStatus.ANNULLED) && !isShowAnnulled()) ||
                        (DRAFT_DOCKS.contains(type) && !isShowDraftDocks()) ||
                        (EXPAND_DOCKS.contains(type) && !isShowDFXDocks())
                )
                    i.remove();
            }
        }
    }

    /**
     * Обновляет таблицу из других мест
     * @param item
     */
    public void updateDraftTableView(Draft item) {
       updateRoutineTableView(Collections.singletonList(item), true);
    }
    
    @Override
    public ItemCommands<Draft> getCommands() {
        return commands;
    }

    @Override
    public String getAccWindowRes() {
        return accWindowRes;
    }


    public PreviewerPatchController getPreviewerController(){
        return previewerController;
    }

    @Override
    public synchronized void sortItemList(List<Draft> list) {
        list.sort(Comparators.draftsComparator());
    }


    @Override //Searchable
    public List<Draft> getCurrentItemSearchedList() {
        return currentItemList;
    }

    @Override //Searchable
    public void setCurrentItemSearchedList(List<Draft> currentItemList) {
        this.currentItemList = currentItemList;
    }


    public void setAccController(FormView_ACCController<Draft> accController){
        this.accController = (Draft_ACCController) accController;
    }

    public Draft_ACCController getAccController(){
        return accController;
    }


}
