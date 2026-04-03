package ru.wert.tubus.chogori.application.cardsbox;


import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.common.utils.CommonUnits;
import ru.wert.tubus.chogori.entities.drafts.Draft_Patch;
import ru.wert.tubus.chogori.entities.drafts.Draft_PatchController;
import ru.wert.tubus.chogori.entities.drafts.Draft_TableView;
import ru.wert.tubus.chogori.entities.folders.Folder_TableView;
import ru.wert.tubus.chogori.entities.passports.Passport_Patch;
import ru.wert.tubus.chogori.entities.passports.Passport_PatchController;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.chogori.entities.product_groups.ProductGroup_TreeView;
import ru.wert.tubus.chogori.previewer.PreviewerPatchController;
import ru.wert.tubus.chogori.registrationBook.RegistrationBookController;
import ru.wert.tubus.chogori.registrationBook.RegistrationBook_Patch;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.Prefix;
import ru.wert.tubus.client.interfaces.Item;
import ru.wert.tubus.client.interfaces.SearchableTab;
import ru.wert.tubus.client.interfaces.UpdatableTabController;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_SEARCH_FIELD;


@Slf4j
public class CardsboxController implements SearchableTab, UpdatableTabController {


    @FXML
    private SplitPane sppHorizontal;

    @FXML
    private StackPane stpRegistrationBook;

    @FXML
    private StackPane stpPassports;

    @FXML
    private StackPane spPIK;

    @FXML
    private StackPane stpSketch;


    private Passport_TableView passportsTable;
    private PreviewerPatchController previewerPatchController;
    private Draft_TableView draftsTable;
    private Draft_Patch draftPatch;
    private Draft_PatchController draftPatchController;
    private Passport_Patch passportsPatch;
    private RegistrationBook_Patch registrationBookPatch;

    private Folder_TableView folderTableView;
    private ProductGroup_TreeView<Folder> productGroupsTreeView;

    private ChangeListener<Item> folderTableSelectedItemChangeListener;

    private ObservableList<Passport> allPassportsList;
    private ObservableList<Passport> pikPassportsList;
    private ObservableList<Passport> sketchPassportsList;

    private TableView<Passport> tvPIK;
    private TableView<Passport> tvSketch;


    @FXML
    void initialize() {

        loadStackPanePIKPassports(); //Пасспорта PIK
        loadStackPaneSketchPassports(); //Пасспорта эскизных чертежей

        // Загружаем все паспорта
        loadAllPassports();

        // Заполняем списки
        fillPIKListView();
        fillSketchesListView();



        loadRegistrationBook(); //Журнал регистрации



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
//            showError("Ошибка загрузки данных", "Не удалось загрузить список паспортов: " + e.getMessage());
            allPassportsList = FXCollections.observableArrayList();
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
        selectedPassportsList.removeAll(toRemove);
    }

    /**
     * Заполняет список паспортов ПИК
     * Фильтрует паспорта с префиксом "ПИК" и номером по маске "######.###"
     */
    private void fillPIKListView() {
        Pattern pikPattern = Pattern.compile("\\d{6}\\.\\d{3}");

        List<Passport> pikPassports = allPassportsList.stream()
                .filter(passport -> {
                    Prefix prefix = passport.getPrefix();
                    String number = passport.getNumber();
                    return prefix != null
                            && "ПИК".equals(prefix.getName())
                            && number != null
                            && pikPattern.matcher(number).matches();
                })
                .sorted(Comparator.comparing(Passport::getNumber))
                .collect(Collectors.toList());

        pikPassportsList = FXCollections.observableArrayList(pikPassports);
        tvPIK.setItems(pikPassportsList);
    }

    /**
     * Заполняет список эскизных паспортов
     * Фильтрует паспорта с префиксом "-" или null и номером по маске "Э#####"
     */
    private void fillSketchesListView() {
        Pattern sketchPattern = Pattern.compile("Э\\d{5}");

        List<Passport> sketchPassports = allPassportsList.stream()
                .filter(passport -> {
                    Prefix prefix = passport.getPrefix();
                    String number = passport.getNumber();
                    boolean prefixCondition = prefix == null || "-".equals(prefix.getName());
                    return prefixCondition
                            && number != null
                            && sketchPattern.matcher(number).matches();
                })
                .sorted(Comparator.comparing(Passport::getNumber))
                .collect(Collectors.toList());

        sketchPassportsList = FXCollections.observableArrayList(sketchPassports);
        tvSketch.setItems(sketchPassportsList);
    }

    /**
     * Создаем таблицу ИДЕНТИФИКАТОРОВ
     */
    private void loadStackPanePIKPassports() {

        passportsPatch = new Passport_Patch().create();

        Passport_PatchController passportPatchController = passportsPatch.getPassportPatchController();
        passportPatchController.initPassportsTableView(null, new Passport(), SelectionMode.SINGLE, true);
        passportsTable = passportPatchController.getPassportsTable();
        passportsTable.showTableColumns(false, true, true, true, true);
        tvPIK = passportPatchController.getPassportsTable();

        //Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(true, true);
        passportPatchController.getHboxPassportsButtons().getChildren().
                addAll(CommonUnits.createHorizontalDividerButton(sppHorizontal, 0.8, 0.4));

        spPIK.getChildren().add(passportsPatch.getParent());

    }

    /**
     * Создаем таблицу ИДЕНТИФИКАТОРОВ эскизных чертежей
     */
    private void loadStackPaneSketchPassports() {

        passportsPatch = new Passport_Patch().create();

        Passport_PatchController passportPatchController = passportsPatch.getPassportPatchController();
        passportPatchController.initPassportsTableView(null, new Passport(), SelectionMode.SINGLE, true);
        passportsTable = passportPatchController.getPassportsTable();
        passportsTable.showTableColumns(false, true, true, true, true);
        tvSketch = passportPatchController.getPassportsTable();

        //Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(true, true);
        passportPatchController.getHboxPassportsButtons().getChildren().
                addAll(CommonUnits.createHorizontalDividerButton(sppHorizontal, 0.8, 0.4));

        stpSketch.getChildren().add(passportsPatch.getParent());

    }

    /**
     * Создаем каталог ИЗДЕЛИЙ
     */
    private void loadRegistrationBook() {

        registrationBookPatch = new RegistrationBook_Patch().create();
        RegistrationBookController registrationBookController = registrationBookPatch.getRegistrationBookController();

//        registrationBookController.

        stpRegistrationBook.getChildren().add(registrationBookPatch.getParent());

    }




    private void updateListOfPassports(Item newValue) {
        passportsPatch.getPassportPatchController().showSourceOfPassports(newValue);

        passportsTable.setSelectedFolders(Collections.singletonList((Folder) newValue));
        passportsTable.setSearchedText(""); //обнуляем поисковую строку
        passportsTable.setModifyingItem(newValue);
        passportsTable.updateView();
    }

    @Override//SearchableTab
    public void tuneSearching() {
        Platform.runLater(()->passportsTable.requestFocus());
        CH_SEARCH_FIELD.changeSearchedTableView(passportsTable, "КАРТОЧКА");
    }


    @Override
    public void updateTab() {
        productGroupsTreeView.updateView();
        folderTableView.updateVisibleLeafOfTableView(folderTableView.getUpwardRow().getValue());
        draftsTable.updateTableView();
        passportsTable.updateTableView();
    }
}
