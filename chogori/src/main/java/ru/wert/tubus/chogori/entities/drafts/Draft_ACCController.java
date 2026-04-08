package ru.wert.tubus.chogori.entities.drafts;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import ru.wert.tubus.chogori.statics.validators.NameValidator;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormViewACCWindow;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.IFormView;
import ru.wert.tubus.chogori.entities.drafts.commands.*;
import ru.wert.tubus.chogori.entities.folders.Folder_Chooser;
import ru.wert.tubus.chogori.popups.HintPopup;
import ru.wert.tubus.chogori.previewer.PreviewerPatchController;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.components.*;
import ru.wert.tubus.chogori.setteings.ChogoriSettings;
import ru.wert.tubus.winform.enums.EDraftStatus;
import ru.wert.tubus.winform.enums.EDraftType;
import ru.wert.tubus.winform.enums.EOperation;
import ru.wert.tubus.winform.enums.ESolution;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.warnings.Warning2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_QUICK_PASSPORTS;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CORRECT_DET_TO_ASSM;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;
import static ru.wert.tubus.chogori.statics.AppStatic.*;
import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;
import static ru.wert.tubus.winform.warnings.WarningMessages.*;

/**
 * Класс описывает форму добавления и замены чертежей
 * Вилка : можно просто добавить или добавить сразу папку.
 * Добавить : делится на добавить один чертеж и несколько, зависит от того,
 * сколько выбрал в папке пользователь
 * List<DraftFileAndId> draftsList - список добавляемых чертежей, DraftFileAndId - Draft + Id
 * если Id = null, значит чертеж еще не добавлен в БД
 */
@Slf4j
public class Draft_ACCController extends FormView_ACCController<Draft> {

    @FXML
    private Label lblNumFile;

    @FXML
    private Label lblFileName;

    @FXML
    private Label lblStatus;

    @FXML
    private Button btnNext;

    @FXML
    private Button btnPrevious;

    @FXML
    private Slider sliderCurrentPosition;

    @FXML
    private StackPane spPreviewer;

    @FXML
    private ComboBox<Prefix> bxPrefix;

    @FXML
    private TextField txtNumber;

    @FXML
    private Label lblDraftNameInDB;

    @FXML
    private TextField txtName;

    @FXML
    private ComboBox<EDraftType> bxType;

    @FXML
    private ComboBox<Integer> bxPage;

    @FXML
    private ComboBox<Folder> bxFolder;

    @FXML
    private Button btnSearchFolder;

    @FXML
    private RadioButton rbAutopilot;

    @FXML
    private RadioButton rbAsk;

    @FXML
    private RadioButton rbSkip;

    @FXML
    private RadioButton rbChange;

    @FXML
    private RadioButton rbDelete;

    @FXML
    private TextArea txtAreaNote;

    @FXML
    private Button btnOk;

    @FXML
    private Button btnCancelOperation;

    private static File lastFile = new File("C:/test");

    private Draft_TableView tableView;
    private PreviewerPatchController previewerController;

    private Event okEvent;

    private Folder currentFolder;
    private HintPopup hintPopup;
    private ObjectProperty<EOperation> operationProperty ;

    private StackPane spIndicator;//Панель с расположенным на ней индикатором прогресса, опявляется при длительных процессах
    private ICommand currentCommand; //Команда исполняемая в текущее время.
    private Service<?> manipulation;//Текущая выполняемая задача

    private List<DraftFileAndId> draftsList = new ArrayList<>(); //Список чертежей <Файл - ID>
    private IntegerProperty currentPosition; //Порядковый номер файла в draftsList
    private File currentFilePath; //Текущий путь к файлу, хранящийся в draftsList
    private String currentFileName;
    private Draft currentDraft; //Текущий чертеж для которого нажали OK - id = null, так как он не сохранен
    private Passport currentPassport; //Пасспорт найденный для омера чертежа в txtNumber
    private ChangeListener<Number> currentPosListener;

    private boolean askMe, skipMe, changeMe, deleteMe;
    private List<File> droppedFiles;

    @FXML
    void initialize(){

        rbAsk.setTooltip(new Tooltip("Действие с чертежом походу добавления"));
        rbSkip.setTooltip(new Tooltip("Новый чертеж НЕ сохраняется,\nстарый чертеж НЕ меняется"));
        rbChange.setTooltip(new Tooltip("Новый чертеж становится ДЕЙСТВУЮЩИМ,\nстарый чертеж меняет статус на ЗАМЕНЕННЫЙ"));
        rbDelete.setTooltip(new Tooltip("Новый чертеж становится ДЕЙСТВУЮЩИМ,\nстарый чертеж УДАЛЯЕТСЯ "));

        lblDraftNameInDB.setStyle("-fx-text-fill: #FFFF99");

        initAutopilot();

//        initBtnCancelOperation();

        initRadioGroup();

        Platform.runLater(()->txtNumber.requestFocus());

    }


    public void addDroppedFiles(EOperation operation, IFormView<Draft> formView, ItemCommands<Draft> commands, List<File> droppedFiles){
        this.droppedFiles = droppedFiles;
        init(operation, formView, commands);

    }


    /**
     * Предварительно происходит выбор чертежа или папки с чертежами
     */
    @Override
    public void init(EOperation operation, IFormView<Draft> formView, ItemCommands<Draft> commands) {
        super.initSuper(operation, formView, commands, ChogoriServices.CH_QUICK_DRAFTS);
        this.tableView = (Draft_TableView) formView;

        currentPosition = new SimpleIntegerProperty();

        initOperationProperty(operation);

        if(operationProperty.get().equals(EOperation.REPLACE)) loadOneDraft();
        else if(operationProperty.get().equals(EOperation.ADD)) loadManyDrafts();
        else if(operationProperty.get().equals(EOperation.ADD_FOLDER))loadFolder();


        if(operationProperty.get().equals(EOperation.CHANGE)){
            draftsList.add(new DraftFileAndId(null, getOldItem().getId()));
        }

        sliderCurrentPosition.setMin(0);
        sliderCurrentPosition.setMax(draftsList.size()-1);
        sliderCurrentPosition.setValue(0);
        sliderCurrentPosition.valueProperty().addListener((observable) -> {
            double newPos = Math.round(sliderCurrentPosition.getValue());
            sliderCurrentPosition.setValue(newPos);
            currentPosition.set((int)newPos);
            fillForm((int)newPos);
        });

        //Если ничего не выбрано, выходим без создания окна
        if((operationProperty.get().equals(EOperation.ADD) || operationProperty.get().equals(EOperation.ADD_FOLDER)
                || operationProperty.get().equals(EOperation.REPLACE))
                && (draftsList == null || draftsList.isEmpty())){
            FormViewACCWindow.windowCreationAllowed = false;
            return;
        }

        new BXPage().create(bxPage); //СТРАНИЦЫ
        new BXDraftType().create(bxType); //ТИП ЧЕРТЕЖА
        new BXPrefix().create(bxPrefix); //ПРЕФИКСЫ
        new BXFolder().create(bxFolder); //ИЗДЕЛИЯ
        new BtnSearchProduct().create(btnSearchFolder); //НАЙТИ/ДОБАВИТЬ изделие

        createLabelFileName();

        initLabelDraftStatus();

        createPreviewer();

        createTxtNumber();

        createLblDraftNameInDB();

        btnSearchFolder.setOnAction(this::findFolder);

        //Устанавливаем начальные значения полей в зависимости от operation
        setInitialValues();

        if(operationProperty.get().equals(EOperation.ADD_FOLDER)){
            setSettingsForOperationAddFolder();
        } else
            lblNumFile.setText("Файлов: 1");

    }

    /**
     * Настройка АВТОПИЛОТА
     */
    private void initAutopilot() {
        rbAutopilot.setTooltip(new Tooltip("Включает/выключает авто добавление чертежей"));
        rbAutopilot.setId("rbAutopilot");
        rbAutopilot.setSelected(false);
        rbAutopilot.setOnAction(e -> {
            checkUppAutopilot();
        });
    }

    /**
     * Запускает АВТОПИЛОТ, если он активен
     */
    public void autopilotWork() {
        if(rbAutopilot.isSelected())
            btnOk.fire();
        checkUppAutopilot();
    }

    /**
     * Проводит проверку АТОПИЛОТА и выключает его, если необходимо
     */
    private void checkUppAutopilot() {
        if (rbAutopilot.isSelected())
            if (draftsList == null ||
                    draftsList.size() <= 1 ||
                    currentPosition.get() >=(draftsList.size() - 1) ||
                    operationProperty.get().equals(EOperation.REPLACE) ||
                    operationProperty.get().equals(EOperation.CHANGE))
                rbAutopilot.setSelected(false);
    }

    private void createLblDraftNameInDB() {
        lblDraftNameInDB.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
                Draft_RenameCommand command = new Draft_RenameCommand(tableView, currentPassport);
                command.execute();
                if (currentPassport != null)
                    lblDraftNameInDB.setText(currentPassport.getName());
            }
        });

        lblDraftNameInDB.setOnMouseEntered(event ->{
            String hint = currentPassport.getName();
            hintPopup = new HintPopup(lblDraftNameInDB, hint, 0.0);
            hintPopup.showHint();
        });

        lblDraftNameInDB.setOnMouseExited(event ->{
            if(hintPopup != null)
                hintPopup.closeHint();
        });

        lblDraftNameInDB.setOnMousePressed(event->{
            if(hintPopup != null)
                hintPopup.closeHint();       });
    }

    private void createTxtNumber() {
        txtNumber.textProperty().addListener(observable -> {
            String name = txtNumber.getText().trim();
            Platform.runLater(()->{
                currentPassport = CH_QUICK_PASSPORTS.findByPrefixIdAndNumber(bxPrefix.getValue(), name);
                if(currentPassport != null)
                    lblDraftNameInDB.setText("=> " + currentPassport.getName());
                else
                    lblDraftNameInDB.setText("");
            });

        });
    }

    /**
     * Метод инициализирует кнопку Отмена
     */
    @FXML
    public void cancelPressed(Event event) {
        if (manipulation != null && manipulation.isRunning()) {
            manipulation.cancel();
        } else {
            super.cancelPressed(event);
        }
    }

    /**
     * Радио группа определяет действие при сопадении добавляемого чертежа с имеющимся
     * По умолчанию - полагается спросить, что делать
     */
    private void initRadioGroup() {
        ToggleGroup radioGroup = new ToggleGroup();
        radioGroup.getToggles().addAll(rbAsk, rbSkip, rbChange, rbDelete);
        radioGroup.selectedToggleProperty().addListener(observable -> {
            askMe = rbAsk.isSelected();
            skipMe = rbSkip.isSelected();
            changeMe = rbChange.isSelected();
            deleteMe = rbDelete.isSelected();

        });
        rbAsk.setSelected(true);
    }

    /**
     * Метод устанавливает начальное значение кнопки ОК, количество файлов в папке
     * Начальную позицию добавляемых чертежей в папке и вешает слушателя на изменение этой позиции
     * Метод не срабатывает только на последней позиции, когда нет смены значерния currentPosition
     */
    private void setSettingsForOperationAddFolder() {
        //Показываем изначальное число файлов
        lblNumFile.setText("Файлов: " + draftsList.size());

        currentPosListener = (observable, oldValue, newValue) -> {
            lblNumFile.setText(format("Файл %d из %d", currentPosition.get() +1, draftsList.size()));
            Long id = draftsList.get(currentPosition.get()).draftId;
            if(id == null) {
                btnOk.setText("ДОБАВИТЬ");
                btnOk.setStyle("-fx-background-color: #8bc8ff;");
                manipulation = addDraftTask();
            } else {
                btnOk.setText("ИЗМЕНИТЬ");
                btnOk.setStyle("-fx-background-color: #ffd4a3;");
                manipulation = changeDraftTask();
            }
        };

        currentPosition.addListener(currentPosListener);

        //Инициируем
        manipulation = addDraftTask();
    }

    /**
     * Метод инициирует надпись со статусом чертежа - ДЕЙСТВУЕТ, ЗАМЕНЕН, АННУЛИРОВАН
     */
    private void initLabelDraftStatus() {
        if(operationProperty.get().equals(EOperation.ADD) || operationProperty.get().equals(EOperation.ADD_FOLDER))
            setDraftStatus(null);
        else
            setDraftStatus(tableView.getAllSelectedItems() == null ? null : tableView.getAllSelectedItems().get(0));
    }

    /**
     * Метод содержит слушатель для управлением надписью на кнопке btnOk
     * @param operation EOperation
     */
    private void initOperationProperty(EOperation operation) {
        operationProperty = new SimpleObjectProperty<>();
        operationProperty.set(operation);

        switch(operationProperty.get()){
            case REPLACE:
                btnOk.setText("ЗАМЕНИТЬ");
                btnOk.setStyle("-fx-background-color: #70DB55;");
                break;
            case CHANGE:
                btnOk.setText("ИЗМЕНИТЬ");
                btnOk.setStyle("-fx-background-color: #ffd4a3;");
                break;
            default: //ADD, ADD_FOLDER
                btnOk.setText("ДОБАВИТЬ");
                btnOk.setStyle("-fx-background-color: #8bc8ff;");
                break;
        }
    }

    /**
     * Основная развилка класса при нажатии на кнопку ОК.
     * @param event
     */
    @FXML
    void ok(Event event) {
        this.okEvent = event;
        if (notNullFieldEmpty()) {
            Warning1.create($ATTENTION, "Некоторые поля не заполнены!", "Заполните все поля");
            return;
        }
        if(ChogoriSettings.CHECK_ENTERED_NUMBER && !enteredDataCorrect()) return;

        //draftsList == null при изменении
        if (draftsList != null && !draftsList.isEmpty()) {
            if (operationProperty.get().equals(EOperation.ADD_FOLDER)) {
                //При сохранении чертежа, нам нужен id сохраненного чертежа
                currentDraft = getNewItem();
                currentCommand = new Draft_MultipleAddCommand(currentDraft, tableView);
                btnOk.setDisable(true);
                spIndicator.setVisible(true);

                manipulation.restart();

            } else if (operationProperty.get().equals(EOperation.REPLACE)) {
                replaceDraft();
            } else if(operationProperty.get().equals(EOperation.ADD)){ //EOperation.ADD
                currentDraft = getNewItem();
                currentCommand = new Draft_QuickAddCommand(currentDraft, tableView);
                btnOk.setDisable(true);
                spIndicator.setVisible(true);

                manipulation = addDraftTask();
                manipulation.start();
            } else { //CHANGE (Изменить)
                super.okPressed(event, spIndicator, btnOk);
            }
        } else {
            super.okPressed(event, spIndicator, btnOk);
        }
    }

    /**
     * Проверка чертежа на дубликаты
     * Если новый чертеж повторяет имеющийся ДЕЙСТВУЮЩИЙ или АННУЛИРОВАННЫЙ, то пользователю поступит предложение его заменить
     * Предполагается, что метод действует в потоке отличном от главного, поэтому окно с сообщением выводится принудительно
     * в главном потоке. Ожидание ответа от пользователя происходит с помощью класса CountDownLatch и связывания BooleanProperty
     */
    public boolean draftIsDuplicated(Draft newDraft, Draft oldDraft){
        //Так как пасспорт нового чертежа еще фактически не существует, то ищем такой же пасспорт в базе по косвенным признакам
        Passport passport = CH_QUICK_PASSPORTS.findByPrefixIdAndNumber(newDraft.getPassport().getPrefix(), newDraft.getPassport().getNumber());
        if (passport == null) {
            log.debug("draftIsDuplicated : пасспорта {} не найдено", newDraft.getPassport().toUsefulString());
            return false; //Если пасспорта в базе нет - чертеж новый
        } else
            log.debug("draftIsDuplicated : найден пасспорт {}", passport.toUsefulString());

        List<Draft> drafts = ChogoriServices.CH_QUICK_DRAFTS.findByPassport(passport);
        drafts.remove(oldDraft);
        log.debug("draftIsDuplicated : найдено {} чертежей с пасспортом {}", drafts.size(), passport.toUsefulString());
        if (drafts.isEmpty()) return false;

        for (Draft draft : drafts) {
            if (draft.equals(newDraft)) {
                if (draft.getStatus().equals(EDraftStatus.LEGAL.getStatusId())) {
                    if (skipMe) {
                        Platform.runLater(this::showNextDraft);
                        return true;
                    } else
                        return foundDuplicatedLegalDraft(draft); //Иначе возвращаем на доработку
                } else if (draft.getStatus().equals(EDraftStatus.ANNULLED.getStatusId())) {
                    if (skipMe) {
                        Platform.runLater(this::showNextDraft);
                        return true;
                    } else return foundDuplicatedAnnulledDraft(draft); //Иначе возвращаем на доработку
                }
            }
        }

        return false;
    }

    /**
     * Метод выясняет, хочет ли пользователь восстановить ранее аннулированный чертеж
     * @param draft Draft
     * @param changeDraft BooleanProperty
     */
    private void askIfAnnulledDraftMustBeSurvived(Draft draft, ObjectProperty<ESolution> changeDraft) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            changeDraft.set(new Draft_DuplicateDraftFound().create(draft, "'АННУЛИРОВАННЫЙ'"));
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод выясняет, хочет ли пользователь изменить действующий чертеж на новый чертеж
     * @param draft Draft
     * @param changeDraft BooleanProperty
     */
    private void askIfLegalDraftMustBeChanged(Draft draft, ObjectProperty<ESolution> changeDraft) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            changeDraft.set(new Draft_DuplicateDraftFound().create(draft, "'ДЕЙСТВУЮЩИЙ'"));
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод выясняет восстанавливает ранее аннулированный чертеж
     * @param oldDraft Draft
     * @return true - дублируется
     */
    private boolean foundDuplicatedAnnulledDraft(Draft oldDraft) {
        ObjectProperty<ESolution> changeDraft = new SimpleObjectProperty<>();
        askIfAnnulledDraftMustBeSurvived(oldDraft, changeDraft);

        if (changeDraft.getValue().equals(ESolution.CHANGE)) {
            Platform.runLater(()->changeStatusForAnnulledDraft(oldDraft));
        }
        else if (changeDraft.getValue().equals(ESolution.DELETE))
            Platform.runLater(()->{
                deleteAnnulledDraft(oldDraft);
            });
        else {
            log.debug("draftIsDuplicated : пользователь отказался менять статус чертежа {} на АННУЛИРОВАННЫЙ", oldDraft.toUsefulString());
            return true;
        }
        return false;
    }

    /**
     * Метод пакетом меняет статус аннулированный на ЗАМЕННЕННЫЙ
     * @param draft Draft
     */
    private void changeStatusForAnnulledDraft(Draft draft) {
        //Так как аннулируется не одна страница, а весь документ, то собираем с базы все записи относящиеся к данному чертежу
        //и меняем их статус на замененный, таким образом их реанимировав
        List<Draft> annulledDrafts = ChogoriServices.CH_QUICK_DRAFTS.findByPassport(draft.getPassport());
        for (Draft annulledDraft : annulledDrafts) {
            draft.setStatus(EDraftStatus.LEGAL.getStatusId());
            draft.setStatusTime(LocalDateTime.now().toString());
            draft.setStatusUser(CH_CURRENT_USER);
            log.debug("draftIsDuplicated : меняем статус чертежа {} на АННУЛИРОВАННЫЙ", draft.toUsefulString());
            ChogoriServices.CH_QUICK_DRAFTS.update(annulledDraft);
        }
    }

    /**
     * Метод пакетом удаляет все аннулированные чертежи
     * @param draft Draft
     */
    private void deleteAnnulledDraft(Draft draft) {
        //Так как аннулируется не одна страница, а весь документ, то собираем с базы все записи относящиеся к данному чертежу
        //и меняем их статус на замененный, таким образом их реанимировав
        List<Draft> annulledDrafts = new ArrayList<>(ChogoriServices.CH_QUICK_DRAFTS.findByPassport(draft.getPassport()));
        currentCommand = new Draft_DeleteCommand(annulledDrafts, tableView);
        currentCommand.execute();

    }


    /**
     * Метод после выяснения , хочет ли пользователь изменить копию черетежа на новый чертеж
     * @return boolean, True  - изменение отменено или не произошло,
     *                  False  - изменение произошло либо необходим переход к следующему
     */
    private boolean foundDuplicatedLegalDraft(Draft oldDraft) {
        ObjectProperty<ESolution> changeDraft = new SimpleObjectProperty<>();
        if (askMe) {
            //Метод меняет переменную changeDraft
            askIfLegalDraftMustBeChanged(oldDraft, changeDraft);
        } else {
            if (deleteMe) {
                Platform.runLater(() -> deleteOldDraft(oldDraft));
            } else if (changeMe)
//                if(draftIsDuplicated(getNewItem()))
                Platform.runLater(() -> changeOldDraft(oldDraft));

            return false;
        }

        //Переменная changeDraft изменилась, пора действовать
        if (changeDraft.getValue().equals(ESolution.CHANGE))
//            if(draftIsDuplicated(getNewItem()))
            Platform.runLater(() -> changeOldDraft(oldDraft));
        else if (changeDraft.getValue().equals(ESolution.DELETE))
            Platform.runLater(() -> deleteOldDraft(oldDraft));
        else {
            log.debug("draftIsDuplicated : пользователь отказался менять статус чертежа {} на ЗАМЕНЕННЫЙ", oldDraft.toUsefulString());
            return true;
        }

        return false;
    }

    /**
     * Метод удаляет старый чертеж из БД перед сохранением нового
     * @param oldDraft Draft
     */
    private void deleteOldDraft(Draft oldDraft) {
        log.debug(format("deleteOldDraft() - deleting draft with id %s", oldDraft.getId()));
//        ICommand command = new Draft_DeleteCommand(Collections.singletonList(oldDraft), tableView);
//        command.execute();

        boolean res = ChogoriServices.CH_DRAFTS.delete(oldDraft);
        if(res) tableView.getItems().remove(oldDraft);
    }

    /**
     * Метод изменяет старый чертеж в БД перед сохранением нового
     * @param oldDraft Draft
     */
    private void changeOldDraft(Draft oldDraft) {

        oldDraft.setStatus(EDraftStatus.CHANGED.getStatusId());
        oldDraft.setStatusTime(LocalDateTime.now().toString());
        log.debug("draftIsDuplicated : меняем статус чертежа {} на ЗАМЕНЕННЫЙ", oldDraft.toUsefulString());
        ICommand updateCommand = new Draft_ChangeCommand(oldDraft, tableView);
        updateCommand.execute();
    }


    @NotNull
    private Service<Draft> addDraftTask() {
        Service<Draft> task = new Service<Draft>() {
            @Override
            protected Task<Draft> createTask() {
                return new Task<Draft>() {
                    @Override
                    protected Draft call() throws Exception {
                        spIndicator.setVisible(true); //////////
                        if (draftIsDuplicated(currentDraft, null)) {
                            //TRUE -->
                            return null;
                        }
                        //FALSE -->
                        if (operation.equals(EOperation.ADD)) {
                            return ((Draft_QuickAddCommand) currentCommand).executeWithReturn();
                        } else //operation.equals(EOperation.ADD_FOLDER)
                            return ((Draft_MultipleAddCommand) currentCommand).addDraft();
                    }
                };
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                btnOk.setDisable(false);
                spIndicator.setVisible(false);
            }

            @Override
            protected void failed() {
                super.failed();
                btnOk.setDisable(false);
                spIndicator.setVisible(false);
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                btnOk.setDisable(false);
                spIndicator.setVisible(false);
                Draft savedDraft = this.getValue();
                if (savedDraft != null) {
                    draftsList.get(currentPosition.get()).setDraftId(savedDraft.getId());
                    tableView.updateRoutineTableView(Collections.singletonList(getValue()), false);
                    if(operation.equals(EOperation.ADD) && draftsList.size() == 1)
                        btnOk.getScene().getWindow().hide();
                    else { //EOperation.ADD_FOLDER
                        if (rbAutopilot.isSelected()) {
                            int next = currentPosition.get() + 1;
                            sliderCurrentPosition.setValue(next);
                            Platform.runLater(()->{
                                autopilotWork();
                            });
                        } else
                            showNextDraft();
                    }
                }
            }
        };

        return task;

    }


    @NotNull
    private Service<Draft> changeDraftTask() {
        return new Service<Draft>() {

            @Override
            protected Task<Draft> createTask() {
                return new Task<Draft>() {
                    @Override
                    protected Draft call() throws Exception {
                        spIndicator.setVisible(true); //////////
                        if(draftIsDuplicated(getNewItem(), currentDraft)){
                            if (askMe)
                                Platform.runLater(() -> Warning1.create($ATTENTION, $ITEM_EXISTS, $USE_ORIGINAL_ITEM));
                            return null;
                        }
                        Draft oldDraft = ChogoriServices.CH_QUICK_DRAFTS.findById(draftsList.get(currentPosition.get()).draftId);

                        changeOldItemFields(oldDraft);
                        commands.change(null, oldDraft);

                        return oldDraft;
                    }
                };
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                btnOk.setDisable(false);
                spIndicator.setVisible(false);
            };

            @Override
            protected void failed() {
                super.failed();
                btnOk.setDisable(false);
                spIndicator.setVisible(false);
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                btnOk.setDisable(false);
                spIndicator.setVisible(false);
                if(getValue() != null) {
                    showNextDraft();
                    checkUppAutopilot();
                }
            }
        };
    }


    /**
     * Метод показывает следущий в списке чертеж, если активна нопка NEXT
     */
    private void showNextDraft() {
        //Если мы дошли до конца списка (кнопка активна)
        if (!btnNext.isDisable())
            btnNext.fire(); //Эмулируем нажатие кнопки
        else {
            //Иначе перезаполняем форму с обновленными данными
            fillForm(currentPosition.get());
            currentPosListener.changed(currentPosition, currentPosition.get(), currentPosition.get());
        }
    }


    /**
     * ЗАМЕНИТЬ ЧЕРТЕЖ
     */
    private void replaceDraft() {
        Draft oldDraft = tableView.getSelectionModel().getSelectedItem();
        oldDraft.setStatus(EDraftStatus.CHANGED.getStatusId());
        oldDraft.setStatusUser(CH_CURRENT_USER);
        oldDraft.setStatusTime(LocalDateTime.now().toString());

        manipulation = replaceDraftTask(oldDraft);

        manipulation.restart();
    }

    @NotNull
    private Service<Draft> replaceDraftTask(Draft oldDraft) {
        return new Service<Draft>() {
            @Override
            protected Task<Draft> createTask() {
                return new Task<Draft>() {
                    @Override
                    protected Draft call() throws Exception {
                        spIndicator.setVisible(true); //////////

                        if (deleteMe) {
                            currentCommand = new Draft_DeleteCommand(Arrays.asList(oldDraft), tableView);
                            currentCommand.execute();
                        } else {
                            oldDraft.setStatus(EDraftStatus.CHANGED.getStatusId());
                            oldDraft.setStatusTime(LocalDateTime.now().toString());
                            currentCommand = new Draft_ChangeCommand(oldDraft, tableView);
                            currentCommand.execute();
                        }

                        //Сохраняем новый чертеж
                        currentCommand = new Draft_MultipleAddCommand(getNewItem(), tableView);
                        return ((Draft_MultipleAddCommand) currentCommand).addDraft();

                    }

                };
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                if (!operationProperty.get().equals(EOperation.ADD_FOLDER))
                    closeWindow(okEvent);
                else
                    showNextDraft();
            }

            @Override
            protected void failed() {
                super.failed();
                log.error("При замене чертежа произошла ошибка:\n");
                getException().printStackTrace();
                Warning1.create($ATTENTION, "При замене чертежа произошла ошибка",
                        "Повторите операцию позже \nили обратитесь к администратору");
                if (!operationProperty.get().equals(EOperation.ADD_FOLDER))
                    closeWindow(okEvent);
                else
                    showNextDraft();

            }

            @Override
            protected void succeeded() {
                super.succeeded();
                if(!operationProperty.get().equals(EOperation.ADD_FOLDER))
                    closeWindow(okEvent);
                else {
                    draftsList.get(currentPosition.get()).setDraftId(getValue().getId());
                    showNextDraft();
                }

                //К отображаемому списку добавляем вновь созданный чертеж
                tableView.getItems().add(getValue());
                tableView.updateRoutineTableView(Collections.singletonList(getValue()), true);
            }
        };
    }

    /**
     * Метод описывает действие при нажатии на кнопку Добавить пакет
     */
    void findFolder(ActionEvent event) {

        Folder chosenFolder = Folder_Chooser.create(((Node) event.getSource()).getScene().getWindow());
        if(chosenFolder != null){
            bxFolder.setValue(chosenFolder);
        }
    }

    /**
     * Устанавливается поле статуса согласно БД
     * @param draft Draft
     */
    private void setDraftStatus(Draft draft) {
        EDraftStatus status;
        if(draft == null)
            status = EDraftStatus.UNKNOWN;
        else
            status = EDraftStatus.getStatusById(draft.getStatus());
        lblStatus.setText(status.getStatusName());
        switch (status) {
            case LEGAL:
                lblStatus.setStyle("-fx-background-color: white; -fx-font-weight: bold;-fx-text-fill: green; "); break;//ДЕЙСТВУЕТ
            case CHANGED:
                lblStatus.setStyle("-fx-background-color: white; -fx-font-weight: bold;-fx-text-fill: yellow; "); break;//ЗАМЕНЕН
            case ANNULLED:
                lblStatus.setStyle("-fx-background-color: white; -fx-font-weight: bold;-fx-text-fill: red; "); break;//АННУЛИРОВАН
            case UNKNOWN:
                lblStatus.setStyle("-fx-background-color: white; -fx-font-weight: bold;-fx-text-fill: black; "); break;//НЕОПРЕДЕЛЕН
        }
    }

    /**
     * Загружаем папку с чертежами
     */
    private void loadFolder(){

        try {
//            draftsList = new ArrayList<>();

            File folder = null;
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setInitialDirectory(lastFile);
            dirChooser.setTitle("Выберите папку...");
            try { //Хак для
                folder = dirChooser.showDialog(WF_MAIN_STAGE);
            } catch (Exception e) {
                lastFile = new File("C:/");
                dirChooser.setInitialDirectory(lastFile);
                folder = dirChooser.showDialog(WF_MAIN_STAGE);
                log.debug("loadFolder : Hack has been successfully used!");
            }
            if(folder == null || !folder.isDirectory()) return;

            List<Path> filesInFolder = Files.walk(folder.toPath())
                    .filter(file ->
                            file.toString().toLowerCase().endsWith(".pdf") ||
                            file.toString().toLowerCase().endsWith(".png") ||
                            file.toString().toLowerCase().endsWith(".jpg") ||
                            file.toString().toLowerCase().endsWith(".jpeg") ||
                            file.toString().toLowerCase().endsWith(".dxf") ||
                            file.toString().toLowerCase().endsWith(".stl") ||
                            file.toString().toLowerCase().endsWith(".step") ||
                            file.toString().toLowerCase().endsWith(".eprt") ||
                            file.toString().toLowerCase().endsWith(".easm"))
                    .collect(Collectors.toList());


            for(Path p : filesInFolder)
                draftsList.add(new DraftFileAndId(p.toFile(), null));
            lastFile = folder.getParentFile();

            currentFilePath = draftsList.get(0).getDraftFile();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Загружаем несколько выбранных чертежей
     */
    private void loadManyDrafts() {
//        draftsList = new ArrayList<>();
        List<File> chosenList = null;
        if(droppedFiles == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(lastFile);
            fileChooser.setTitle("Выберите чертежи...");
            fileChooser.getExtensionFilters().add(ALLOWED_EXTENSIONS);

            try {
                chosenList = fileChooser.showOpenMultipleDialog(WF_MAIN_STAGE);
            } catch (Exception e) {
                lastFile = new File("C:/");
                fileChooser.setInitialDirectory(lastFile);
                chosenList = fileChooser.showOpenMultipleDialog(WF_MAIN_STAGE);
                log.debug("loadManyDrafts : Hack has been successfully used!");
            }
            if (chosenList == null) return;
        } else
            chosenList = droppedFiles;
        lastFile = chosenList.get(0).getParentFile();
        //Формируем список файлов из выбранных чертежей
        for (File file : chosenList) {
            draftsList.add(new DraftFileAndId(file, null));
        }

        //За текущий файл берем самый первый в списке
        currentFilePath = draftsList.get(0).getDraftFile();

        //Если чертежей для добавления больше одного, считаем, что добавляется папка
        if(draftsList.size() > 1)
            operationProperty.set(EOperation.ADD_FOLDER);
    }

    /**
     * Метод для замены одного чертежа другим
     */
    private void loadOneDraft(){
//        draftsList = new ArrayList<>();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(lastFile);
        fileChooser.setTitle("Выберите чертеж...");
        fileChooser.getExtensionFilters().add(ALLOWED_EXTENSIONS);

        File chosenFile = null;
        try {
            chosenFile = fileChooser.showOpenDialog(WF_MAIN_STAGE);
        } catch (Exception e) {
            lastFile = new File("C:/");
            fileChooser.setInitialDirectory(lastFile);
            chosenFile = fileChooser.showOpenDialog(WF_MAIN_STAGE);
            log.debug("loadOneDraft : Hack has been successfully used!");
        }
        if(chosenFile == null) return;
        lastFile = chosenFile.getParentFile();
        //Формируем список файлов из выбранных чертежей

        draftsList.add(new DraftFileAndId(chosenFile, null));

        //За текущий файл берем единственный в списке
        currentFilePath = draftsList.get(0).getDraftFile();
    }

    /**
     * Выводит ниже комбобокса подсказку - это же наименование, но мелким шрифтом
     */
    private void createLabelFileName() {
        lblFileName.setStyle("-fx-background-color: white; -fx-text-fill: saddlebrown");
        lblFileName.setOnMouseEntered(event ->{
            String hint = lblFileName.getText();
            hintPopup = new HintPopup(lblFileName, hint, 0.0);
            hintPopup.showHint();
        });

        lblFileName.setOnMouseExited(event ->{
            if(hintPopup != null)
                hintPopup.closeHint();
        });

        lblFileName.setOnMousePressed(event->{
            if(hintPopup != null)
                hintPopup.closeHint();
        });

        if (operationProperty.get().equals(EOperation.ADD) || operationProperty.get().equals(EOperation.ADD_FOLDER))
            //При изменении файла меняется дец номер и наименование
            lblFileName.textProperty().addListener(((observable, oldValue, newValue) -> {
                Platform.runLater(this::setDecNumberAndName);
            }));

    }

    /**
     * Метод создает кнопки NEXT и PREVIOUS
     */
    private void createNextAndPreviousButtons() {
        currentPosition = new SimpleIntegerProperty(0);

        btnPrevious.disableProperty().bind(currentPosition.lessThanOrEqualTo(0));
        btnPrevious.setOnAction(event -> {
            int prev = currentPosition.get() - 1;
            if (prev >= 0) {
                sliderCurrentPosition.setValue(prev);
            }
        });

        btnNext.disableProperty().bind(currentPosition.greaterThanOrEqualTo(draftsList.size() - 1));
        btnNext.setOnAction(event -> {
            int next = currentPosition.get() + 1;
            if (next < draftsList.size()) {
                sliderCurrentPosition.setValue(next);
            }
        });

    }

    /**
     * Метод заполняет форму в зависимости от того, файл уже сохранен или еще не сохранен в БД
     * @param num int - порядковый номер файла в списке выбранных файлов
     */
    private void fillForm(int num) {
        //Если документ #num еще не сохранен, то форму заполняем из файла
        if (draftsList.get(num).getDraftId() == null) {

            currentFilePath = draftsList.get(currentPosition.get()).getDraftFile(); //File
            previewerController.showDraft(null, currentFilePath);
            currentFileName = draftsList.get(currentPosition.get()).getDraftFile().getName(); //String
            lblFileName.setText(currentFileName);

//            bxPage.getSelectionModel().select(0); //Устанавливаем страницу в "1"
            txtAreaNote.setText("");//Комментарий пустой
            setDraftStatus(null);
        } else {
            //Если документ уже сохранен, то форму заполняем значениями БД
            Draft draft = ChogoriServices.CH_QUICK_DRAFTS.findById(draftsList.get(num).getDraftId());
            fillFieldsOnTheForm(draft);
        }

    }

    /**
     * Метод создает панель предпросмотра, состоящий и двух панелей - собственно предпросмотра
     * и панели с индикатором прогресса. Последняя панель находится в скрытом состоянии и появляется только
     * на время асинхронных операций
     */
    private void createPreviewer(){

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/previewer/previewerPatch.fxml"));
            Parent previewer = loader.load();
            //Помещаем панель с previewer в шаблонное окно WindowDecoration
            previewerController = loader.getController();
            previewerController.initPreviewer(ChogoriSettings.CH_PDF_VIEWER, WF_MAIN_STAGE.getScene());
            previewerController.initPreviewerToolBar(
                    false,
                    false,
                    false,
                    true, //Распечатывать чертежи
                    false,
                    false);

            //Создаем прозрачную панель с индикатором
            spIndicator = new StackPane();
            spIndicator.setAlignment(Pos.CENTER);
            spIndicator.setStyle("-fx-background-color: rgb(225, 225,225, 0.5)");
            //создаем сам индикатор
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setMaxSize(90.0, 90.0);
            spIndicator.getChildren().addAll(progressIndicator);
            spIndicator.setVisible(false);

            //На панели размещаем предпросмотрщик и панель с индикатором
            spPreviewer.getChildren().addAll(previewer, spIndicator);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public ArrayList<String> getNotNullFields() {
        ArrayList<String> notNullFields = new ArrayList<>();
        notNullFields.add(txtNumber.getText());
        notNullFields.add(txtName.getText());
        return notNullFields;
    }

    @Override
    public Draft getNewItem() {

        Draft newDraft = new Draft();

        Passport newPassport = new Passport(bxPrefix.getValue(),
                txtNumber.getText().trim(),
                txtName.getText().trim(),
                bxFolder.getValue().getName(),
                CH_CURRENT_USER.getName(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                new ArrayList<>());
        Passport foundPassport = CH_QUICK_PASSPORTS.findByPrefixIdAndNumber(newPassport.getPrefix(), newPassport.getNumber());
        if(foundPassport == null){
            Passport savedPassport = CH_QUICK_PASSPORTS.save(newPassport);
            newDraft.setPassport(savedPassport);
        } else
            newDraft.setPassport(foundPassport);

        newDraft.setFolder(bxFolder.getValue());
        newDraft.setInitialDraftName(lblFileName.getText());
        if(operationProperty.get().equals(EOperation.ADD) || operationProperty.get().equals(EOperation.ADD_FOLDER))
            newDraft.setExtension(FilenameUtils.getExtension(currentFilePath.toString()).toLowerCase());
        else
            //Получениее расширения при ИЗМЕНЕНИИ полей записи
            newDraft.setExtension(FilenameUtils.getExtension(lblFileName.getText()).toLowerCase());
        //
        EDraftType type = bxType.getValue();
        String num = newPassport.getNumber();
        if (CH_CORRECT_DET_TO_ASSM && // Если допускается исправлять ДЕТ на СБ для номеров 3ХХХХХ.ХХХ и 4ХХХХХ.ХХХ
                type.equals(EDraftType.DETAIL) &&
                (num.matches("^[34]\\d{5}\\.\\d{3}$"))) {
            type = EDraftType.ASSEMBLE;
        }
        newDraft.setDraftType(type.getTypeId());
        newDraft.setPageNumber(bxPage.getValue());
        // СТАТУС
        newDraft.setStatus(EDraftStatus.LEGAL.getStatusId());
        newDraft.setStatusUser(CH_CURRENT_USER);
        newDraft.setStatusTime(LocalDateTime.now().toString());
        //СОЗДАНИЕ
        newDraft.setCreationUser(CH_CURRENT_USER);
        newDraft.setCreationTime(LocalDateTime.now().toString());

        newDraft.setNote(txtAreaNote.getText());

        return newDraft;
    }

    @Override
    public Draft getOldItem() {
        return tableView.getSelectionModel().getSelectedItems().get(0);
    }

    /**
     * Метод заполняет поля формы СОХРАНЕННОГО РАНЕЕ в БД чертежа
     * @param oldItem Draft
     */
    @Override
    public void fillFieldsOnTheForm(Draft oldItem) {

        //При выборе единственного чертежа для изменения
        if (draftsList == null || draftsList.size() == 1) {
            //Кнопки далее и предыдущее нужно загасить
            if(!btnNext.isDisable()) btnNext.setDisable(true);
            if(!btnPrevious.isDisable())  btnPrevious.setDisable(true);
        }
//        AppStatic.openDraftInPreviewer(oldItem, previewerController);
        lblFileName.setText(oldItem.getInitialDraftName());

        bxPrefix.setValue(oldItem.getPassport().getPrefix());
        txtNumber.setText(oldItem.getPassport().getNumber());
        txtName.setText(oldItem.getName());
        bxType.setValue(EDraftType.getDraftTypeById(oldItem.getDraftType()));
        bxPage.setValue(oldItem.getPageNumber());
        bxFolder.setValue(oldItem.getFolder());
        txtAreaNote.setText(oldItem.getNote());
        setDraftStatus(oldItem);

        if(operationProperty.get().equals(EOperation.REPLACE)){
            lblFileName.setText(currentFilePath.getName());
            previewerController.showDraft(currentFilePath);

            bxPrefix.setDisable(true);
            txtNumber.setDisable(true);
            txtName.setDisable(true);
            bxType.setDisable(true);
            bxPage.setDisable(true);
            bxFolder.setDisable(true);
            btnSearchFolder.setDisable(true);

        }
        else {
            AppStatic.openDraftInPreviewer(oldItem, previewerController, false);
        }
    }

    @Override
    public void changeOldItemFields(Draft oldItem) {
        if (!oldItem.getPassport().getPrefix().equals(bxPrefix.getValue()) ||
                !oldItem.getPassport().getNumber().equals(txtNumber.getText()) ||
                !oldItem.getPassport().getName().equals(txtName.getText())) {
            //Проверяем наличие готового пасспорта в базе
            Passport p = CH_QUICK_PASSPORTS.findByPrefixIdAndNumber(bxPrefix.getValue(), txtNumber.getText().trim());
            if(p == null) {
                oldItem.setPassport(new Passport(
                        bxPrefix.getValue(),
                        txtNumber.getText().trim(),
                        txtName.getText(),
                        bxFolder.getValue().getName(),
                        CH_CURRENT_USER.getName(),
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                        new ArrayList<>())
                );
            } else {
                oldItem.setPassport(p);
            }
        }

        oldItem.setDraftType(bxType.getValue().getTypeId());
        oldItem.setPageNumber(bxPage.getValue());
        oldItem.setFolder(bxFolder.getValue());
        //Статус не может меняться
//        oldItem.setStatus(EDraftStatus.findStatusIdByStatusName(lblStatus.getText()));
        oldItem.setNote(txtAreaNote.getText());
    }

    @Override
    public void showEmptyForm() {

        currentFileName = draftsList.get(0).draftFile.getName();
        lblFileName.setText(currentFileName);

        createNextAndPreviousButtons();
        currentFilePath = draftsList.get(0).draftFile;
        previewerController.showDraft(currentFilePath);

        bxFolder.setValue((Folder) tableView.getModifyingItem());

        Platform.runLater(this::setDecNumberAndName);

    }

    @Override
    public boolean enteredDataCorrect() {
        String enteredNumber = txtNumber.getText().trim();
        List<String> patterns = Arrays.asList(DEC_NUMBER, SKETCH_NUMBER);

        for (String pat : patterns) {
            if (enteredNumber.matches(pat))
                return true;
        }
        return Warning2.create("ВНИМАНИЕ!",
                format("Введенный номер %s не типичен.", enteredNumber),
                "Желаете его оставить?"); //ОК, true - оставить

    }

    /**
     * Метод преобразовавает наименование файла в строки дец номера и наименования
     * Вызывается так же при изменении lblFileName в методе createLabelFileName()
     */
    private void setDecNumberAndName() {
        String var = "";
        String extension = FilenameUtils.getExtension(currentFileName).toLowerCase();
        //Обрезаем расширение файла
        String initialFileName = currentFileName.substring(0, currentFileName.lastIndexOf("."));
        log.debug("setDecNumberAndName : initial file name is '{}'", initialFileName);
        //Определяем наличие префикса
        Prefix prefix = null;
        int index1 = initialFileName.indexOf(".");
        if(index1 > 0)
            prefix = ChogoriServices.CH_QUICK_PREFIXES.findByName(initialFileName.substring(0, index1));
        //Если префикс присутствует, отрезаем его от строки
        if(prefix != null) {
            initialFileName = initialFileName.substring(index1 + 1);
            bxPrefix.setValue(prefix);
        }

        //Если пробел разделяет номер группами по три
        if(initialFileName.indexOf(" ") == 3) initialFileName = initialFileName.replaceFirst(" ", "");

        //Предположительно на этом этапе мы имеем что-то вроде 745222.255 какая-то деталь
        //Вычленим децимальный номер из оставшейся части получим наименование
        //Если децимального номера нет, то это будет только наименованием
        //Маска децимального номера XXXXXX.XXX, где X число

        Pattern p = Pattern.compile("\\d{3}.?\\d{3}\\.\\d{3}"); //Децимальный номер xxxxxx.xxx
        Matcher m = p.matcher(initialFileName);
        String decNumber = "";
        while(m.find()){
            decNumber = initialFileName.substring(m.start(), m.end());
        }
        log.debug("setDecNumberAndName : found decimal number '{}'",  decNumber);

        String partName = initialFileName.replace(decNumber, "").trim();
        log.debug("setDecNumberAndName : part name is '{}'", partName);

        int page = 1;
        EDraftType type = EDraftType.DETAIL;
        if(extension.equals("step")) {
            type = EDraftType.IMAGE_STEP;
        } else if(extension.equals("dxf")){
            type = EDraftType.IMAGE_DXF;
            page = Integer.parseInt(parsVariant(initialFileName));
        } else if(extension.equals("stl")){
            type = EDraftType.IMAGE_STL;
            page = Integer.parseInt(parsVariant(initialFileName));
        } else {
            String[] nameParts = partName.split(" ", -1);
            outer_loop:
            for(String s : nameParts){
                for (String shortName : EDraftType.getShortNames()) {
                    String numbers;
                    if(s.equals(shortName) || s.equals(shortName + ",") || s.equals(shortName + ".")){
                        type = EDraftType.getTypeByShortName(shortName);
                        numbers = "1";
                    } else {
                        Pattern pat1 = Pattern.compile(shortName + "\\d"); //сб1, сб2 ...
                        Matcher m1 = pat1.matcher(s);
                        String typeAndPage = "";
                        while (m1.find()) {
                            typeAndPage = s.substring(m1.start(), m1.end());
                        }
                        if (typeAndPage.equals("")) continue;

                        type = EDraftType.getTypeByShortName(shortName);
                        numbers = s.replace(shortName, "");
                    }

                    page = Integer.parseInt(numbers);
                    partName = partName.replace(s, "").trim();
                    break outer_loop;
                }
            }
        }


        bxType.getSelectionModel().select(type);
        bxPage.getSelectionModel().select(page);
        txtNumber.setText(decNumber);
        txtName.setText(NameValidator.createValidName(partName));

    }

    /**
     * Метод возвращает исполнение, если оно указано в наименовании чертежа
     * По умолчанию возвращается значение "0"
     * @param initialFileName
     * @return
     */
    private String parsVariant(String initialFileName) {
        String variant = "";

        Pattern p0 = Pattern.compile("-\\d{2}"); //-xx
        Matcher m0 = p0.matcher(initialFileName);
        while(m0.find()){
            variant = initialFileName.substring(m0.start(), m0.end());
        }
        if(!variant.equals("")){
            variant = variant.split("-", -1)[1];
        } else {
            variant = "0";
        }
        return variant;
    }


    /**
     * Возвращает путь к сохраняемому в данный момент файлу
     * Используется в методе execute() класса AddCommand
     */
    public File getCurrentFilePath(){
        return currentFilePath;
    }
}

/**
 * Класс для использования в списке загруженных чертежей для сохранения
 * С помощью списка контролируется состояние чертежа, загружен/не загружен
 */
class DraftFileAndId {

    @Getter@Setter File draftFile; //Сохраняемый чертеж (путь)
    @Getter@Setter Long draftId; //Если id есть, то чертеж загружен

    public DraftFileAndId(File draftFile, Long draftId) {
        this.draftFile = draftFile;
        this.draftId = draftId;
    }

    @Override
    public String toString() {
        return "DraftFileAndId{" +
                "draftFile=" + draftFile +
                ", draftId=" + draftId +
                '}';
    }
}
