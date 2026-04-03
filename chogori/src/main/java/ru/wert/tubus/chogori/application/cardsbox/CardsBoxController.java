package ru.wert.tubus.chogori.application.cardsbox;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.common.utils.CommonUnits;
import ru.wert.tubus.chogori.entities.passports.Passport_Patch;
import ru.wert.tubus.chogori.entities.passports.Passport_PatchController;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
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
public class CardsBoxController implements SearchableTab, UpdatableTabController {


    @FXML
    private SplitPane sppHorizontal;

    @FXML
    private StackPane stpRegistrationBook;

    @FXML
    private StackPane spPassports;

    @FXML
    private StackPane spPIK;

    @FXML
    private StackPane spSketch;


    private Passport_TableView tvPIK;
    private Passport_Patch passportsPIKPatch;

    private Passport_TableView tvSketch;
    private Passport_Patch passportsSketchPatch;

    private RegistrationBook_Patch registrationBookPatch;

    private ObservableList<Passport> allPassportsList;
    private ObservableList<Passport> pikPassportsList;
    private ObservableList<Passport> sketchPassportsList;


    @FXML
    void initialize() {

        loadStackPanePIKPassports(); //Паспорта PIK
        loadStackPaneSketchPassports(); //Паспорта эскизных чертежей

        // Загружаем все паспорта
        loadAllPassports();

        // Заполняем списки
        fillPIKTableView();
        fillSketchesTableView();



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
     * Заполняет список паспортов ПИК
     * Фильтрует паспорта с префиксом "ПИК" и номером по маске "######.###"
     */
    private void fillPIKTableView() {
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
        Platform.runLater(()->{
            tvPIK.getItems().clear();
            tvPIK.setItems(pikPassportsList);
        });
    }

    /**
     * Заполняет список эскизных паспортов
     * Фильтрует паспорта с префиксом "-" или null и номером по маске "Э#####"
     */
    private void fillSketchesTableView() {
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
        Platform.runLater(()->{
            tvSketch.getItems().clear();
            tvSketch.setItems(sketchPassportsList);
        });
    }

    /**
     * Создаем таблицу ИДЕНТИФИКАТОРОВ
     */
    private void loadStackPanePIKPassports() {

        passportsPIKPatch = new Passport_Patch().create();

        Passport_PatchController passportPatchController = passportsPIKPatch.getPassportPatchController();
        passportPatchController.initPassportsTableView(null, new Passport(), SelectionMode.SINGLE, true);
        tvPIK = passportPatchController.getPassportsTable();
        tvPIK.showTableColumns(false, true, true, true, true);

        //Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(false, true);
        passportPatchController.getHboxPassportsButtons().getChildren().
                addAll(CommonUnits.createHorizontalDividerButton(sppHorizontal, 0.8, 0.4));

        spPIK.getChildren().add(passportsPIKPatch.getParent());

    }

    /**
     * Создаем таблицу ИДЕНТИФИКАТОРОВ эскизных чертежей
     */
    private void loadStackPaneSketchPassports() {

        passportsSketchPatch = new Passport_Patch().create();

        Passport_PatchController passportPatchController = passportsSketchPatch.getPassportPatchController();
        passportPatchController.initPassportsTableView(null, new Passport(), SelectionMode.SINGLE, true);
        tvSketch = passportPatchController.getPassportsTable();
        tvSketch.showTableColumns(false, true, true, true, true);

        //Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(false, true);
        passportPatchController.getHboxPassportsButtons().getChildren().
                addAll(CommonUnits.createHorizontalDividerButton(sppHorizontal, 0.8, 0.4));

        spSketch.getChildren().add(passportsSketchPatch.getParent());

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
        passportsPIKPatch.getPassportPatchController().showSourceOfPassports(newValue);

        tvPIK.setSelectedFolders(Collections.singletonList((Folder) newValue));
        tvPIK.setSearchedText(""); //обнуляем поисковую строку
        tvPIK.setModifyingItem(newValue);
        tvPIK.updateView();
    }

    @Override//SearchableTab
    public void tuneSearching() {
        Platform.runLater(()-> tvPIK.requestFocus());
        CH_SEARCH_FIELD.changeSearchedTableView(tvPIK, "КАРТОЧКА");
    }


    @Override
    public void updateTab() {
//        productGroupsTreeView.updateView();
//        folderTableView.updateVisibleLeafOfTableView(folderTableView.getUpwardRow().getValue());
//        draftsTable.updateTableView();
        tvPIK.updateTableView();
    }
}
