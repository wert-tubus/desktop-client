package ru.wert.tubus.chogori.entities.passports;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.ProductGroup;
import ru.wert.tubus.chogori.components.BtnMenuPassportsColumns;
import ru.wert.tubus.chogori.entities.drafts.Draft_TableView;
import ru.wert.tubus.chogori.previewer.PreviewerPatchController;
import ru.wert.tubus.chogori.images.BtnImages;

import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_SEARCH_FIELD;

public class Passport_PatchController {

    @FXML
    @Getter
    private HBox hboxPassportsButtons;

    @FXML
    private VBox vboxPassports;

    @FXML
    private Label lblSourceOfPassports;

    @Getter private Passport_TableView passportsTable;
    private PreviewerPatchController previewerController;
    private Object modifyingClass; //класс, от которого зависит отображаемый список в таблице (Folder, Product, Passport)
    private SelectionMode mode; //SelectionMode.SINGLE, SelectionMode.MULTIPLE
    @Getter private MenuButton btnShowColumns;

    //Кнопки toolbar
    private boolean useBtnPassportsGlobe; //Показать все индентификаторы
    private boolean useBtnShowColumns;
    private boolean useContextMenu;

    public void initPassportsTableView(PreviewerPatchController previewerController, Object modifyingClass, SelectionMode mode, boolean useContextMenu){
        this.previewerController = previewerController;
        this.modifyingClass = modifyingClass;
        this.mode = mode;
        this.useContextMenu = useContextMenu;

        lblSourceOfPassports.setStyle("-fx-font-weight: normal; -fx-font-style: oblique; -fx-text-fill: blue");

        createPassportTableView();

    }

    /**
     * Выводится папка в каталоге или комплект, куда входят найденные записи
     * @param source Object
     */
    public void showSourceOfPassports(Object source){
        if(source != null){
            if(source instanceof Folder)
                lblSourceOfPassports.setText(((Folder) source).getName());
            else if(source instanceof ProductGroup)
                lblSourceOfPassports.setText(((ProductGroup) source).getName());
        } else {
            lblSourceOfPassports.setText("");
        }
    }

    public void initPassportsToolBar(boolean btnPassportsGlobe, boolean btnShowColumns){
        this.useBtnPassportsGlobe = btnPassportsGlobe;
        this.useBtnShowColumns = btnShowColumns;

        createPassportToolBar();
    }

    private void createPassportTableView() {
        //запуск новой версии
        passportsTable = new Passport_TableView("НОМЕР ЧЕРТЕЖА", previewerController, useContextMenu, true);
        passportsTable.setModifyingClass(modifyingClass);
        passportsTable.getSelectionModel().setSelectionMode(mode);
        VBox.setVgrow(passportsTable, Priority.ALWAYS);
        vboxPassports.getChildren().add(passportsTable);

        passportsTable.updateView();

    }

    private void createPassportToolBar() {

        //Кнопка ПОКАЗАТЬ ВСЕ
        Button btnPassportsGlobe = new Button();
        btnPassportsGlobe.setId("patchButton");
        btnPassportsGlobe.setGraphic(new ImageView(BtnImages.BTN_GLOBE_IMG));
        btnPassportsGlobe.setTooltip(new Tooltip("Показать все"));
        btnPassportsGlobe.setOnAction((e) -> {
            lblSourceOfPassports.setText("");
            CH_SEARCH_FIELD.setText("");
            passportsTable.setSearchedText("");
            passportsTable.setModifyingItem(null);
            passportsTable.showAllPassportsOfType();
        });


        //Кнопка ПОКАЗАТЬ КОЛОНКИ
        btnShowColumns = new BtnMenuPassportsColumns(passportsTable);

        if(useBtnShowColumns) hboxPassportsButtons.getChildren().add(btnShowColumns);
        if(useBtnPassportsGlobe) hboxPassportsButtons.getChildren().add(btnPassportsGlobe);

    }



}
