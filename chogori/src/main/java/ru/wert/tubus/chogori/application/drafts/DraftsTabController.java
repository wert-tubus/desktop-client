package ru.wert.tubus.chogori.application.drafts;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.utils.CommonUnits;
import ru.wert.tubus.chogori.entities.catalogOfFolders.CatalogOfFoldersPatch;
import ru.wert.tubus.chogori.entities.drafts.Draft_Patch;
import ru.wert.tubus.chogori.entities.drafts.Draft_PatchController;
import ru.wert.tubus.chogori.entities.drafts.Draft_TableView;
import ru.wert.tubus.chogori.entities.folders.Folder_TableView;
import ru.wert.tubus.chogori.entities.product_groups.ProductGroup_TreeView;
import ru.wert.tubus.chogori.previewer.PreviewerPatchController;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.ProductGroup;
import ru.wert.tubus.client.interfaces.Item;
import ru.wert.tubus.client.interfaces.SearchableTab;
import ru.wert.tubus.client.interfaces.UpdatableTabController;

import java.util.*;

import static ru.wert.tubus.chogori.search.SearchField.SEARCHING_NOW;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_SEARCH_FIELD;
import static ru.wert.tubus.winform.statics.WinformStatic.clearTempDir;


@Slf4j
public class DraftsTabController implements SearchableTab, UpdatableTabController {


    @FXML
    private StackPane spDrafts;

    @FXML
    private StackPane spPreviewer;

    @FXML
    private StackPane spCatalog;

    @FXML
    private SplitPane sppVertical;

    @FXML
    private SplitPane sppHorizontal;

    @Getter private Draft_TableView draftsTable;
    private Draft_PatchController draftPatchController;
    private PreviewerPatchController previewerPatchController;
//    private Label lblDraftInfo;

    @Getter private Folder_TableView folderTableView;
    private ProductGroup_TreeView<Folder> productGroupsTreeView;

    @FXML
    void initialize() {

        createCatalogOfFolders(); //Каталог пакетов

        createPreviewer(); //Предпросмотр

        createDraftsTable(); //ЧЕРТЕЖИ

        folderTableView.setDraftTable(draftsTable);

    }

    //=========== РАБОТА С ЧАТОМ   ====================================================

    /**
     * Открывает нужную папку во вкладке ЧЕРТЕЖИ.
     * Вызывается из чата и из контекстного меню чертежа "Открыть комплект с этим чертежом"
     */
    public void openFolderByName(Folder folder, Draft draftToBeSelected) {
        ProductGroup group = folder.getProductGroup();
        folderTableView.updateVisibleLeafOfTableView(group);
        folderTableView.scrollTo(folder);
        folderTableView.getSelectionModel().select(folder);
        draftsTable.setModifyingItem(folder);
        draftsTable.updateTableView();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (draftToBeSelected != null) {
                    Platform.runLater(() -> {
                        draftsTable.getSelectionModel().select(draftToBeSelected);
                        draftsTable.scrollTo(draftToBeSelected);
                    });
                }
            }
        }, 1000);

    }


    /**
     * ПРЕДПРОСМОТРЩИК
     */
    private void createPreviewer() {
        previewerPatchController =
                CommonUnits.loadStpPreviewer(spPreviewer, sppHorizontal, sppVertical, false); //Предпросмотр
    }

    /**
     * таблица с ЧЕРТЕЖАМИ
     */
    private void createDraftsTable() {

        Draft_Patch draftPatch = new Draft_Patch().create();
        draftPatchController = draftPatch.getDraftPatchController();

        draftPatchController.initDraftsTableView(previewerPatchController, new Folder(), SelectionMode.MULTIPLE, true);
        draftsTable = draftPatchController.getDraftsTable();
        draftsTable.showTableColumns(false, true, true, true, false,
                false, true);
        //Инструментальную панель инициируем в последнюю очередь
        draftPatchController.initDraftsToolBar(true, true, true, true);
        draftPatchController.getHboxDraftsButtons().getChildren().add(CommonUnits.createHorizontalDividerButton(sppHorizontal, 0.8, 0.4));

        //Сообщаем Previewer ссылку на tableView
        previewerPatchController.setDraftsTableView(draftsTable);

        //Монтируем
        spDrafts.getChildren().add(draftPatch.getParent());

    }

    /**
     * Создаем каталог ИЗДЕЛИЙ
     */
    private void createCatalogOfFolders() {
        CatalogOfFoldersPatch catalogPatch = new CatalogOfFoldersPatch().create();
        //Подключаем слушатель
        folderTableView = catalogPatch.getFolderTableView();
        productGroupsTreeView = catalogPatch.getProductGroupsTreeView();

        folderTableView.setOnMouseClicked(e->{
            if(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2){
                Item selectedItem = folderTableView.getSelectionModel().getSelectedItem();
                if ( selectedItem instanceof Folder) {
                    CH_SEARCH_FIELD.updateSearchHistory("компл: " + selectedItem.getName());
                    clearTempDir();
                    Platform.runLater(() -> {
                        updateListOfDrafts(selectedItem);
                        SEARCHING_NOW = false;
                        draftsTable.requestFocus();
                    });
                }
            }
        });

        catalogPatch.getFoldersButtons().getChildren().add(CommonUnits.createVerticalDividerButton(sppVertical, 0.8, 0.4));

        //Монтируем каталог в панель
        Parent cat = catalogPatch.getCatalogOfFoldersPatch();
        spCatalog.getChildren().add(cat);

    }

    public void updateListOfDrafts(Item newValue) {
        draftPatchController.showSourceOfPassports(newValue);
        draftsTable.setTempSelectedFolders(Collections.singletonList((Folder) newValue));
//        draftsTable.setSearchedText(""); //обнуляем поисковую строку
        draftsTable.setModifyingItem(newValue);
        draftsTable.updateView();
    }

    @Override//SearchableTab
    public void tuneSearching() {
        Platform.runLater(()->draftsTable.requestFocus());
        CH_SEARCH_FIELD.changeSearchedTableView(draftsTable, "ЧЕРТЕЖ");
    }

    @Override //UpdatableTabController
    public void updateTab() {
        productGroupsTreeView.updateView();
        folderTableView.updateVisibleLeafOfTableView(folderTableView.getUpwardRow().getValue());
        if (!SEARCHING_NOW) draftsTable.updateTableView();
        previewerPatchController.updatePreviewer();
    }

}
