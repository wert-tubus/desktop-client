package ru.wert.tubus.chogori.application.cardsbox;


import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.utils.CommonUnits;
import ru.wert.tubus.chogori.components.BtnDouble;
import ru.wert.tubus.chogori.entities.catalogOfFolders.CatalogOfFoldersPatch;
import ru.wert.tubus.chogori.entities.drafts.Draft_Patch;
import ru.wert.tubus.chogori.entities.drafts.Draft_PatchController;
import ru.wert.tubus.chogori.entities.drafts.Draft_TableView;
import ru.wert.tubus.chogori.entities.folders.Folder_TableView;
import ru.wert.tubus.chogori.entities.passports.Passport_Patch;
import ru.wert.tubus.chogori.entities.passports.Passport_PatchController;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.chogori.entities.product_groups.ProductGroup_TreeView;
import ru.wert.tubus.chogori.images.BtnImages;
import ru.wert.tubus.chogori.previewer.PreviewerPatchController;
import ru.wert.tubus.chogori.registrationBook.RegistrationBookController;
import ru.wert.tubus.chogori.registrationBook.RegistrationBook_Patch;
import ru.wert.tubus.chogori.setteings.ChogoriSettings;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.ProductGroup;
import ru.wert.tubus.client.interfaces.Item;
import ru.wert.tubus.client.interfaces.SearchableTab;
import ru.wert.tubus.client.interfaces.UpdatableTabController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_QUICK_DRAFTS;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_QUICK_FOLDERS;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_SEARCH_FIELD;


@Slf4j
public class CardsboxController implements SearchableTab, UpdatableTabController {


    @FXML
    private SplitPane sppHorizontal;

    @FXML
    private StackPane stpRegistrationBook;

    @FXML
    private StackPane stpPassports;


    private Passport_TableView passportsTable;
    private PreviewerPatchController previewerPatchController;
    private Draft_TableView draftsTable;
    private Draft_Patch draftPatch;
    private Draft_PatchController draftPatchController;
    private Passport_Patch passportsPatch;
    private RegistrationBook_Patch registrationBook_patch;

    private Folder_TableView folderTableView;
    private ProductGroup_TreeView<Folder> productGroupsTreeView;

    private ChangeListener<Item> folderTableSelectedItemChangeListener;


    @FXML
    void initialize() {

        loadStackPanePassports(); //Пасспорта

        loadStackPaneCatalog(); //Каталог



    }

    private BtnDouble createCatalogOrTableButton(){
        BtnDouble btnCatalogOrTable = new BtnDouble(
                BtnImages.BTN_CATALOG_IMG, "Открыть каталог",
                BtnImages.BTN_TABLE_VIEW_IMG, "Открыть входящие чертежи",
                false);
        btnCatalogOrTable.setOnAction(e->{
//            if(btnCatalogOrTable.getStateProperty().get()) {
//                stpDrafts.getChildren().clear();
//                Parent cat = catalogPatch.getCatalogOfFoldersPatch();
//                stpDrafts.getChildren().add(0, cat);
//            } else {
//                stpDrafts.getChildren().clear();
//                stpDrafts.getChildren().add(0, draftPatch.getParent());
//            }
        });
        return btnCatalogOrTable;
    }

    /**
     * Создаем таблицу ИДЕНТИФИКАТОРОВ
     */
    private void loadStackPanePassports() {

        passportsPatch = new Passport_Patch().create();

        Passport_PatchController passportPatchController = passportsPatch.getPassportPatchController();
        passportPatchController.initPassportsTableView(previewerPatchController, new Passport(), SelectionMode.SINGLE, false);
        passportsTable = passportPatchController.getPassportsTable();
        passportsTable.showTableColumns(false, true, true, true, true);
        passportsTable.setModifyingClass(new Folder());
        //Инструментальную панель инициируем в последнюю очередь
        passportPatchController.initPassportsToolBar(true, true);
        passportPatchController.getHboxPassportsButtons().getChildren().addAll(createCatalogOrTableButton(),
                CommonUnits.createHorizontalDividerButton(sppHorizontal, 0.8, 0.4));

        stpPassports.getChildren().add(passportsPatch.getParent());

    }

    /**
     * Создаем каталог ИЗДЕЛИЙ
     */
    private void loadStackPaneCatalog() {

        registrationBook_patch = new RegistrationBook_Patch().create();
        RegistrationBookController registrationBookController = registrationBook_patch.getRegistrationBookController();

//        registrationBookController.

        stpRegistrationBook.getChildren().add(registrationBook_patch.getParent());

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
