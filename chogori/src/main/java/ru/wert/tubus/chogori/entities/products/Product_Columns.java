package ru.wert.tubus.chogori.entities.products;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ru.wert.tubus.client.entity.models.AnyPart;
import ru.wert.tubus.client.entity.models.Product;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.components.HBoxPassport;

public class Product_Columns {

    /**
     * ID
     */
    public static TableColumn<Product, String> createTcId(){
        TableColumn<Product, String> tcId = new TableColumn<>("ID");
        tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcId.setStyle("-fx-alignment: CENTER;");
        return tcId;
    };

    /**
     * ПАССПОРТ - ИДЕНТИФИКАТОР НЕ ИСПОЛЬЗОВАТЬ
     */
    @Deprecated
    public static TableColumn<Product, HBox> createTcPassportVBoxDontUse(){
        TableColumn<Product, HBox> tcPassport = new TableColumn<>("Изделие");
        //Passport выводится в виде label
        tcPassport.setCellValueFactory(cd -> {

            Passport passport = cd.getValue().getPassport();
            HBoxPassport vBoxPassport = new HBoxPassport(passport, true, cd.getValue().getVariant());
            return new ReadOnlyObjectWrapper<>(vBoxPassport);

        });
        tcPassport.setMinWidth(150);
        return tcPassport;
    };

    /**
     * ПАССПОРТ - ИДЕНТИФИКАТОР
     */
    public static TableColumn<Product, String> createTcPartOneRow(){
        TableColumn<Product, String> tcPart = new TableColumn<>("Изделие");

        tcPart.setCellValueFactory(cd -> {

            AnyPart part = cd.getValue().getAnyPart();
            String s = part.toUsefulString();
            return new ReadOnlyStringWrapper(s);

        });
        tcPart.setMinWidth(200);
        return tcPart;
    };

    /**
     * ДЕЦИМАЛЬНЫЙ НОМЕР
     */
    public static TableColumn<Product, Label> createTcDecNumber(){
        TableColumn<Product, Label> tcDecNumber = new TableColumn<>("Децимальный\nномер");
        tcDecNumber.setCellValueFactory(cd->{
            Product detail = cd.getValue();
            String variant = detail.getVariant();
            Passport passport = detail.getPassport();

            String decNumber = passport.getNumber();
            if(!variant.equals("00")) decNumber += variant;
            String prefix = passport.getPrefix().getName();
            if(!prefix.equals("-"))
                decNumber = prefix + "." + decNumber;

            Label lblDecNumber = new Label(decNumber);
//            lblDecNumber.setStyle("-fx-text-fill: black; -fx-font-size: 4mm; -fx-font-weight: bold");

            return new ReadOnlyObjectWrapper(lblDecNumber);
        });
        tcDecNumber.setMinWidth(120);
        return tcDecNumber;
    };

    /**
     * НАИМЕНОВАНИЕ
     */
    public static TableColumn<Product, Label> createTcName(){
        TableColumn<Product, Label> tcName = new TableColumn<>("Наименование");
        tcName.setCellValueFactory(cd->{
            Product detail = cd.getValue();
            Passport passport = detail.getPassport();

            String name = passport.getName();
            Label lblName = new Label(name);
//            lblName.setStyle("-fx-text-fill: black; -fx-font-size: 4mm; -fx-font-weight: bold");

            return new ReadOnlyObjectWrapper(lblName);
        });
        tcName.setMinWidth(120);
        return tcName;
    };


    /**
     * ПАПКА ХРАНЕНИЯ
     */
    public static TableColumn<Product, String> createTcFolder(){
        TableColumn<Product, String> tcFolder = new TableColumn<>("Папка\nхранения");
        tcFolder.setCellValueFactory(new PropertyValueFactory<>("folder"));
        tcFolder.setStyle("-fx-alignment: CENTER;");
        tcFolder.setMinWidth(120);
        return tcFolder;
    };


    /**
     * ПРИМЕЧАНИЕ
     */
    public static TableColumn<Product, String> createTcNote(){
        TableColumn<Product, String> tcNote = new TableColumn<>("Примечание");
        tcNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        tcNote.setMinWidth(120);
        return tcNote;
    };
}
