package ru.wert.tubus.chogori.entities.products;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.common.contextMenuACC.FormView_ACCController;
import ru.wert.tubus.chogori.common.commands.ItemCommands;
import ru.wert.tubus.chogori.common.tableView.CatalogTableView;
import ru.wert.tubus.chogori.entities.product_groups.ProductGroup_Chooser;
import ru.wert.tubus.chogori.components.BXPrefix;
import ru.wert.tubus.chogori.components.BXProductGroup;
import ru.wert.tubus.chogori.common.interfaces.IFormView;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.client.entity.models.*;
import ru.wert.tubus.winform.enums.EOperation;

import java.util.ArrayList;

@Slf4j
public class Product_ACCController extends FormView_ACCController<Product> {

    @FXML
    private ComboBox<Prefix> bxPrefix;

    @FXML
    private TextField tfNumber, tfVariant;

    @FXML
    private Button btnFindPGroup;

    @FXML
    private TextField tfName;

    @FXML
    private ComboBox<ProductGroup> bxGroup;

    @FXML
    private TextArea taNote;

    @FXML
    private StackPane spIndicator;

    @FXML
    private Button btnOk;

    private ProductGroup group;
    private static ProductGroup defaultGroup;
    private Product focusedItem;

    @Override
    public void init(EOperation operation, IFormView<Product> formView, ItemCommands<Product> commands) {
        super.initSuper(operation, formView, commands, ChogoriServices.CH_QUICK_PRODUCTS);

        //Create standart buttons on the form
        new BXPrefix().create(bxPrefix); //ПРЕФИКСЫ
        new BXProductGroup().create(bxGroup, null); //ГРУППА ИЗДЕЛИЙ
        defaultGroup = ChogoriServices.CH_PRODUCT_GROUPS.findByName("Разное");
        group = ((CatalogTableView<Product, ProductGroup>)formView).
                findChosenGroup(operation, (CatalogTableView<Product, ProductGroup>) formView, defaultGroup);

        setInitialValues();

    }

    @FXML
    void initialize(){
        AppStatic.createSpIndicator(spIndicator);
    }

    @FXML
    void findProductGroup(ActionEvent event) {
        group = bxGroup.getValue();

        ProductGroup productGroup = ProductGroup_Chooser.create(((Node) event.getSource()).getScene().getWindow());
        if (productGroup != null) {
            this.group = productGroup;
            bxGroup.getSelectionModel().select(group);
        }
    }

    @FXML
    void cancel(ActionEvent event) {
        super.cancelPressed(event);
    }

    @FXML
    void ok(ActionEvent event) {
        super.okPressed(event, spIndicator, btnOk);
    }


    @Override
    public ArrayList<String> getNotNullFields() {
        ArrayList<String> notNullFields = new ArrayList<>();
        notNullFields.add(tfNumber.getText());
        notNullFields.add(tfName.getText());
        return notNullFields;
    }

    @Override
    public Product getNewItem() {

        Passport passport = createPassport();
        AnyPart anyPart = createAnyPart(passport);

        return new Product(
                anyPart,
                group,
                passport,
                tfVariant.getText().trim(),
                null,
                null,
                taNote.getText()
        );
    }

    @Override
    public Product getOldItem() {
        return formView.getAllSelectedItems().get(0);
    }


    @Override
    public void fillFieldsOnTheForm(Product oldItem) {

        bxPrefix.setValue(oldItem.getPassport().getPrefix());
        tfNumber.setText(oldItem.getPassport().getNumber());
        tfVariant.setText(oldItem.getVariant());
        tfName.setText(oldItem.getName());
        bxGroup.setValue(oldItem.getProductGroup());
//        tfInitialExcelFile.setValue(oldItem.getInitialExcelName());
        taNote.setText(oldItem.getNote());
    }

    @Override
    public void changeOldItemFields(Product oldItem) {

        Prefix newPrefix = bxPrefix.getValue();
        String newNumber = tfNumber.getText().trim();
        String newName = tfName.getText().trim();
        String newVariant = tfVariant.getText().trim();

        Passport oldPass = oldItem.getPassport();

        if(oldPass.getPrefix().equals(newPrefix) ||                         //Изменился ПРЕФИКС
                oldPass.getNumber().equals(newNumber) ||                    //Изменился НОМЕР
                oldItem.getName().equals(newName) ||                        //Изменилось НАИМЕНОВАНИЕ
                oldItem.getVariant().equals(newVariant))                    //Изменилось ИСПОЛНЕНИЕ
        {

            Passport newPassport = createPassport();
            AnyPart newAnyPart = createAnyPart(newPassport);

            oldItem.setAnyPart(newAnyPart);
            oldItem.setPassport(newPassport);
        }

        oldItem.setProductGroup(bxGroup.getValue());
        oldItem.setNote(taNote.getText());
    }

    @Override
    public void showEmptyForm() {

        setComboboxPrefixValue(bxPrefix);
        setComboboxProductGroupValue(bxGroup);

    }

    @Override
    public boolean enteredDataCorrect() {
        return true;
    }

    /**
     * Создает AnyPart для изделия
     */
    private AnyPart createAnyPart(Passport pass) {
        //Формируем anyPart
        String fullDecNumber = bxPrefix.getValue().getName() + "." + tfNumber.getText().trim();
        if (!tfVariant.getText().equals(""))
            fullDecNumber = fullDecNumber + "-" + tfVariant.getText().trim();

        AnyPart anyPart = new AnyPart(fullDecNumber, tfName.getText().trim(), ChogoriServices.CH_ANY_PART_GROUPS.findById(4L));


        return anyPart;
    }

    /**
     * Создает Passport для изделия и, если такого пасспорта еще не существует, то сохраняет в БД
     */
    private Passport createPassport() {
        //Создаем пасспорт
        Prefix prefix = bxPrefix.getValue();
        String number = tfNumber.getText();
        String name = tfName.getText();
        String note = "";

        return new Passport(prefix, number, name, note, new ArrayList<>());
    }
}
