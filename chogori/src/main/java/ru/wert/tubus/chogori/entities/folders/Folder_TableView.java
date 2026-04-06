package ru.wert.tubus.chogori.entities.folders;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import lombok.Getter;
import lombok.Setter;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.ProductGroup;
import ru.wert.tubus.client.interfaces.CatalogGroup;
import ru.wert.tubus.client.interfaces.Item;
import ru.wert.tubus.client.interfaces.ItemService;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.interfaces.IFormView;
import ru.wert.tubus.chogori.common.tableView.CatalogableTable;
import ru.wert.tubus.chogori.common.tableView.RoutineTableView;
import ru.wert.tubus.chogori.entities.drafts.Draft_TableView;
import ru.wert.tubus.chogori.entities.folders.commands._Folder_Commands;
import ru.wert.tubus.chogori.entities.product_groups.ProductGroup_TreeView;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.images.AppImages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.wert.tubus.chogori.statics.AppStatic.UPWARD;
import static ru.wert.tubus.chogori.statics.Comparators.usefulStringComparator;

/**
 * Комплекты чертежей
 */
public class Folder_TableView extends RoutineTableView<Item> implements IFormView<Item>, CatalogableTable<ProductGroup> {

    @Getter private final ObjectProperty<TreeItem<ProductGroup>> upwardRowProperty = new SimpleObjectProperty<>();//Верхняя строка в таблице
    public TreeItem<ProductGroup> getUpwardRow(){return this.upwardRowProperty.get();}
    public void setUpwardRow(TreeItem<ProductGroup> upwardRowProperty){this.upwardRowProperty.set(upwardRowProperty);}

    @Getter private String accWindowRes = "/chogori-fxml/folders/folderACC.fxml";
    private ItemCommands commands;
    private FormView_ACCController<Item> accController;
    @Getter private ProductGroup_TreeView<Item> catalogTree;
    @Getter private Folder_Manipulator manipulator;
    @Getter@Setter private Draft_TableView draftTable;

    private List<Item> shownList = new ArrayList<>(); //Лист чертежей, отображаемых в таблице сейчас
    @Getter@Setter private String searchedText = "";

    private Folder_ContextMenu contextMenu;
    @Getter private final boolean useContextMenu;

    public Folder_TableView(String prompt, boolean useContextMenu) {
        super(prompt);
        this.useContextMenu = useContextMenu;

//        ПОИСК ПЕРЕНЕСЕН В ПОИСК ПО КНОПКЕ ПО КОМПЛЕКТАМ НЕВОЗМОЖЕН!

//        focusedProperty().addListener((observable, oldValue, newValue) -> {
//            if(newValue) CH_SEARCH_FIELD.changeSearchedTableView(this, "КОМПЛЕКТ ЧЕРТЕЖЕЙ");
//        });



    }

    public List<ProductGroup> findMultipleProductGroups(ProductGroup productGroup){
        List<ProductGroup> foundProductGroups =
                catalogTree.findAllGroupChildren(catalogTree.findTreeItemById(productGroup.getId()));
        foundProductGroups.add(productGroup);
        return foundProductGroups;

    }

    /**
     * Метод подключает Folder_Manipulator и RowFactory к таблице
     * @param catalogTree ProductGroup_TreeView
     */
    public void plugContextMenuAndFolderManipulators(ProductGroup_TreeView catalogTree){
        this.catalogTree = catalogTree;
        this.upwardRowProperty.set(catalogTree.getRoot());//Инициализируем upwardRow

        manipulator = new Folder_Manipulator(this, catalogTree);

        commands = new _Folder_Commands(this);

        if(useContextMenu)
            createContextMenu();

        //При двойном клике на верхнюю строку, поднимаемся по списку выше
        //При двойном клике на папку открываем папку
        //При клике правой кнопку по пустой строке снимаем всякое выделение

        setRowFactory( tv -> {
            TableRow<Item> row = new TableRow<>();

            row.setOnDragDetected(e -> manipulator.createOnDragDetected(e));
            row.setOnDragOver(e -> manipulator.createOnDragOver(e, row, useContextMenu));
            row.setOnDragDropped(e -> manipulator.createOnDragDropped(e, useContextMenu));

            row.setOnMouseClicked(event -> {
                Item prevRowData = null;
                Item rowData = row.getItem();

                if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    if(rowData instanceof ProductGroup){
                        if(rowData.equals(upwardRowProperty.get().getValue())){ //Верхняя строка
                            prevRowData = rowData;
                            upwardRowProperty.set(catalogTree.findTreeItemById(((ProductGroup) rowData).getParentId()));
                        } else {
                            upwardRowProperty.set(catalogTree.findTreeItemById(rowData.getId()));
                        }
                        updateNow((ProductGroup) prevRowData);
                    }
                }
                //Снимаем всякое выделение, если клик по пустому месту
                if (row.isEmpty()) {
                    getSelectionModel().clearSelection();
                }

            });


            return row ;
        });
    }


    /**
     * Обновление с учетом выделенного элемента в TREE VIEW
     * Метод вызывается извне этого класса
     */
    @Override
    public void updateTableView() {
        if(globalOffProperty.get()) updateWithGlobalOn();
        else {
            //Находим выделенный элемент в дереве каталогов
            upwardRowProperty.set(catalogTree.getSelectionModel().getSelectedItem());
            updateNow(null);
        }
    }

    /**
     * Обновление таблицы с учетом нажатой кнопки Global
     */
    private void updateWithGlobalOn(){
        Platform.runLater(()->{
            ObservableList<Folder> folders = FXCollections.observableArrayList(ChogoriServices.CH_QUICK_FOLDERS.findAll());
            shownList = new ArrayList<>();
            for(Folder folder: folders){
                shownList.add((Item)folder);
            }
            ObservableList<Item> items = FXCollections.observableArrayList(shownList);
            getItems().clear();
            setItems(items);
        });
    }

    /**
     * Нестандартное обновление таблицы
     */
    @Override
    public void updateSearchedView() {
        super.updateSearchedView();

        List<Folder> foundFolders = new ArrayList<>();

        List<ProductGroup> groups = catalogTree.findAllGroupChildren(upwardRowProperty.get());
        groups.add(upwardRowProperty.get().getValue());

        for(ProductGroup gr: groups){
            foundFolders.addAll(ChogoriServices.CH_QUICK_FOLDERS.findAllByGroupId(gr.getId()));
        }

        ObservableList<Item> items = FXCollections.observableArrayList();
        for(Folder folder: foundFolders){
            if (folder.toUsefulString().toLowerCase().contains(searchedText.toLowerCase())) {
                items.add(folder);
            }
        }
        getItems().clear();
        setItems(items);
    }

    /**
     * Обновление без учета выделенного элемента в TREE VIEW
     * @param prevGroupToBeSelected группа TreeView - верхняя строка в таблице, учитывается припереходе назад
     */
    public void updateNow(ProductGroup prevGroupToBeSelected) {
        shownList = new ArrayList<>();
        if (upwardRowProperty.get() == null) upwardRowProperty.set(catalogTree.getRoot());
        ProductGroup selectedGroup = upwardRowProperty.get().getValue();
        //Добавим верхнюю строку в список, потом она превратится в троеточие
        //Корневой элемент в список не добавляем
        if(upwardRowProperty.get() != catalogTree.getRoot())
            shownList.add(upwardRowProperty.get().getValue());
        List<TreeItem<ProductGroup>> children = upwardRowProperty.get().getChildren();
        for (TreeItem<ProductGroup> ti : children) {
            shownList.add(ti.getValue());
        }

        List<Folder> folders = ChogoriServices.CH_QUICK_FOLDERS.findAllByGroupId(selectedGroup.getId());
        folders.sort(usefulStringComparator());
        shownList.addAll(folders);

        getItems().clear();
        refresh();
        getItems().addAll(shownList);

        //TODO:Выделяем родительскую группу
        if(prevGroupToBeSelected != null){
            getSelectionModel().select(prevGroupToBeSelected);
        }
    }

    /**
     * Обновляет таблицу независимо от выделения в TreeView
     */
    @Override
    public void updateVisibleLeafOfTableView(CatalogGroup selectedProductGroup) {

        List<Item> selectedFolders =  new ArrayList<>(getSelectionModel().getSelectedItems());

        upwardRowProperty.set(catalogTree.findTreeItemById(selectedProductGroup.getId()));

        List<Item> items = new ArrayList<>();
        ProductGroup selectedGroup = upwardRowProperty.get().getValue();
        //Добавим верхнюю строку в список, потом она превратится в троеточие
        //Корневой элемент в список не добавляем
        if(upwardRowProperty.get() != catalogTree.getRoot())
            items.add(upwardRowProperty.get().getValue());
        List<TreeItem<ProductGroup>> children = upwardRowProperty.get().getChildren();
        for (TreeItem<ProductGroup> ti : children) {
            items.add(ti.getValue());
        }

        List<Folder> folders = ChogoriServices.CH_QUICK_FOLDERS.findAllByGroupId(selectedGroup.getId());
        folders.sort(usefulStringComparator());
        items.addAll(folders);

        getItems().clear();
        refresh();
        getItems().addAll(items);

        if (!selectedFolders.isEmpty())
            for (Item item : selectedFolders) {
                getSelectionModel().select(item);
            }
    }


    @Override
    public void createContextMenu() {
        setOnContextMenuRequested(event -> {
            contextMenu = new Folder_ContextMenu(this, catalogTree, (_Folder_Commands) commands, accWindowRes);
            contextMenu.show(this.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    @Override
    public List<Item> prepareList() {
        return null;
    }

    @Override
    public void setTableColumns() {
        TableColumn<Item, Label> tcFolder = new TableColumn<>();
        tcFolder.setCellValueFactory(cd ->{
            Label label = new Label();
            Item item = cd.getValue();
            if(item instanceof ProductGroup) {
                if(item.equals(upwardRowProperty.get().getValue())){
                    label.setText(UPWARD);
                    label.setId("upward"); //На случай применения компаратора
                }else {
                    label.setText(item.toUsefulString());
                    label.setGraphic(new ImageView(AppImages.TREE_NODE_IMG));
                    label.setId("pg");//На случай применения компаратора
                }
            } else {
                label.setText(item.getName());
                label.setId("f");//На случай применения компаратора
            }
            return new ReadOnlyObjectWrapper<>(label);
        });

        getColumns().add(tcFolder);

    }

    @Override
    public ItemCommands<Item> getCommands() {
        return commands;
    }

    @Override
    public void setModifyingItem(Object item) {

    }

    @Override
    public void setCurrentItemSearchedList(List<Item> currentItemList) {
        this.shownList = currentItemList;
    }

    @Override
    public List<Item> getCurrentItemSearchedList() {
        return shownList;
    }


    @Override
    public void easyUpdate(ItemService<Item> service) {
        //No use
    }

    @Override
    public List<Item> getAllSelectedItems() {
        return getSelectionModel().getSelectedItems();
    }

    @Override
    public void setAccController(FormView_ACCController<Item> accController) {
        this.accController = accController;
    }

    @Override
    public FormView_ACCController<Item> getAccController() {
        return accController;
    }

    @Override
    public TreeItem<ProductGroup> getChosenCatalogItem() {
        return catalogTree.getSelectionModel().getSelectedItem();
    }

    @Override
    public TreeItem<ProductGroup> getRootItem() {
        return catalogTree.getRoot();
    }

}
