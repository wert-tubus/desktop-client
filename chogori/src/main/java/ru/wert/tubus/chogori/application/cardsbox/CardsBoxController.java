package ru.wert.tubus.chogori.application.cardsbox;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.cardsbox.registrationBook.RegistrationBookController;
import ru.wert.tubus.chogori.entities.passports.*;
import ru.wert.tubus.chogori.tabs.AppTab;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.interfaces.ITabController;
import ru.wert.tubus.client.interfaces.SearchableTab;
import ru.wert.tubus.client.interfaces.UpdatableTabController;

import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_DEFAULT_PREFIX;
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

    @FXML@Getter
    private Tab tabPIK;

    @FXML@Getter
    private Tab tabSketch;

    @FXML@Getter
    private TabPane tabPane;

    @FXML@Getter
    private ProgressIndicator progressbarPIK;

    @FXML@Getter
    private ProgressIndicator progressbarSketch;

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

        // Восстанавливаем состояние колонок ПОСЛЕ того, как таблицы полностью загружены
        // и showTableColumns() уже вызван с параметрами по умолчанию
        restoreColumnStates();

        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab.equals(tabPIK))
                CH_SEARCH_FIELD.changeSearchedTableView(tvPIK, CH_DEFAULT_PREFIX.getName());
            else if (newTab.equals(tabSketch))
                CH_SEARCH_FIELD.changeSearchedTableView(tvSketch, "ЭСКИЗ");
        });

        tabPane.getSelectionModel().select(tabPIK);
    }

    /**
     * Восстанавливает состояние колонок для обеих таблиц
     */
    private void restoreColumnStates() {
        if (tvPIK != null) {
            Passport_ColumnsManager.restorePIKColumnState(tvPIK);
            Passport_ColumnsManager.setupPIKColumnStateListener(tvPIK);
        }
        if (tvSketch != null) {
            Passport_ColumnsManager.restoreSketchColumnState(tvSketch);
            Passport_ColumnsManager.setupSketchColumnStateListener(tvSketch);
        }
    }

    public void notifyPassportDeleted(Passport deletedPassport) {
        if (registrationBookPatch != null) {
            RegistrationBookController controller = registrationBookPatch.getRegistrationBookController();
            if (controller != null) {
                controller.handlePassportDeleted(deletedPassport);
            }
        }
    }

    public void notifyPassportUpdated(Passport updatedPassport) {
        if (registrationBookPatch != null) {
            RegistrationBookController controller = registrationBookPatch.getRegistrationBookController();
            if (controller != null) {
                controller.handlePassportUpdated(updatedPassport);
            }
        }
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

        passportPatchController.getPassportsTable().setCardsBoxController(this);

        // Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(true, true);

        // ВАЖНО: Добавляем таблицу в StackPane ПЕРВОЙ (индекс 0), чтобы ProgressIndicator был поверх
        spPIK.getChildren().add(0, passportsPIKPatch.getParent());

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

        // Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(false, true);

        // ВАЖНО: Добавляем таблицу в StackPane ПЕРВОЙ (индекс 0), чтобы ProgressIndicator был поверх
        spSketch.getChildren().add(0, passportsSketchPatch.getParent());

    }

    /**
     * Создаем журнал регистрации
     */
    private void loadRegistrationBook() {
        registrationBookPatch = new RegistrationBook_Patch().create();
        RegistrationBookController registrationBookController = registrationBookPatch.getRegistrationBookController();
        registrationBookController.setPassportPIKController(passportsPIKPatch.getPassportPatchController());
        registrationBookController.setPassportSketchController(passportsSketchPatch.getPassportPatchController());
        registrationBookController.setCardsBoxController(this);

        stpRegistrationBook.getChildren().add(registrationBookPatch.getParent());
    }

    @Override//SearchableTab
    public void tuneSearching() {
        Platform.runLater(()-> tvPIK.requestFocus());
        CH_SEARCH_FIELD.changeSearchedTableView(tvPIK, CH_DEFAULT_PREFIX.getName());
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
                registrationBookController.updateRegistrationBook();
                log.debug("Журнал регистрации обновлен");
            }
        }
    }

}
