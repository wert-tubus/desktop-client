package ru.wert.tubus.chogori.application.cardsbox;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.utils.CommonUnits;
import ru.wert.tubus.chogori.entities.passports.PassportType;
import ru.wert.tubus.chogori.entities.passports.Passport_Patch;
import ru.wert.tubus.chogori.entities.passports.Passport_PatchController;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.interfaces.Item;
import ru.wert.tubus.client.interfaces.SearchableTab;
import ru.wert.tubus.client.interfaces.UpdatableTabController;

import java.util.*;

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


    @FXML
    void initialize() {
        loadStackPanePIKPassports(); //Паспорта PIK
        loadStackPaneSketchPassports(); //Паспорта эскизных чертежей

        loadRegistrationBook(); //Журнал регистрации
    }

    /**
     * Создаем таблицу ИДЕНТИФИКАТОРОВ
     */
    private void loadStackPanePIKPassports() {

        passportsPIKPatch = new Passport_Patch().create();

        Passport_PatchController passportPatchController = passportsPIKPatch.getPassportPatchController();
        passportPatchController.initPassportsTableView(null, new Passport(), SelectionMode.SINGLE, true);
        tvPIK = passportPatchController.getPassportsTable();
        tvPIK.showTableColumns(false, true, false, false, false);
        // Устанавливаем тип паспортов для таблицы PIK
        tvPIK.setPassportType(PassportType.PIK);
        tvPIK.setShowPrefix(false);

        //Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(false, true);

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
        tvSketch.showTableColumns(false, true, false, false, false);
        // Устанавливаем тип паспортов для таблицы эскизов
        tvSketch.setPassportType(PassportType.SKETCHES);
        tvSketch.setShowPrefix(false);

        //Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(false, true);

        spSketch.getChildren().add(passportsSketchPatch.getParent());

    }

    /**
     * Создаем каталог ИЗДЕЛИЙ
     */
    /**
     * Создаем журнал регистрации
     */
    private void loadRegistrationBook() {
        registrationBookPatch = new RegistrationBook_Patch().create();
        RegistrationBookController registrationBookController = registrationBookPatch.getRegistrationBookController();
        registrationBookController.setPassportPIKController(passportsPIKPatch.getPassportPatchController());
        registrationBookController.setPassportSketchController(passportsSketchPatch.getPassportPatchController());

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
        log.debug("CardsBoxController.updateTab() вызван");

        // Обновляем таблицу ПИК паспортов
        if (tvPIK != null) {
            tvPIK.updateTableView();
            log.debug("Таблица ПИК обновлена");
        }

        // Обновляем таблицу эскизных паспортов
        if (tvSketch != null) {
            tvSketch.updateTableView();
            log.debug("Таблица эскизов обновлена");
        }

        // Обновляем журнал регистрации
        if (registrationBookPatch != null) {
            RegistrationBookController registrationBookController = registrationBookPatch.getRegistrationBookController();
            if (registrationBookController != null) {
                registrationBookController.updateTab();
                log.debug("Журнал регистрации обновлен");
            }
        }
    }

}
