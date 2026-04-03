package ru.wert.tubus.chogori.details;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ru.wert.tubus.client.entity.models.Detail;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.components.HBoxPassport;

public class DetailColumns {

    /**
     * ID
     */
    public static TableColumn<Detail, String> createTcId(){
        TableColumn<Detail, String> tcId = new TableColumn<>("ID");
        tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcId.setStyle("-fx-alignment: CENTER;");
        return tcId;
    };

    /**
     * KRP
     */
    public static TableColumn<Detail, String> createTcKRP(){
        TableColumn<Detail, String> tcId = new TableColumn<>("ID");
        tcId.setCellValueFactory(new PropertyValueFactory<>("krp"));
        tcId.setStyle("-fx-alignment: CENTER;");
        return tcId;
    };

    /**
     * ПАССПОРТ - ИДЕНТИФИКАТОР
     */
    public static TableColumn<Detail, HBox> createTcPassport(){
        TableColumn<Detail, HBox> tcPassport = new TableColumn<>("Деталь");
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
     * ДЕЦИМАЛЬНЫЙ НОМЕР
     */
    public static TableColumn<Detail, Label> createTcDecNumber(){
        TableColumn<Detail, Label> tcDecNumber = new TableColumn<>("Децимальный\nномер");
        tcDecNumber.setCellValueFactory(cd->{
            Detail detail = cd.getValue();
            String variant = detail.getVariant();
            Passport passport = detail.getPassport();

            String decNumber = passport.getNumber();
            if(!variant.equals("00")) decNumber += variant;
            String prefix = passport.getPrefix().getName();
            if(!prefix.equals("-"))
                decNumber = prefix + "." + decNumber;

            Label lblDecNumber = new Label(decNumber);
            lblDecNumber.setStyle("-fx-text-fill: black; -fx-font-size: 4mm; -fx-font-weight: bold");

            return new ReadOnlyObjectWrapper(lblDecNumber);
        });
        tcDecNumber.setMinWidth(120);
        return tcDecNumber;
    };

    /**
     * НАИМЕНОВАНИЕ
     */
    public static TableColumn<Detail, Label> createTcName(){
        TableColumn<Detail, Label> tcName = new TableColumn<>("Наименование");
        tcName.setCellValueFactory(cd->{
            Detail detail = cd.getValue();
            Passport passport = detail.getPassport();

            String name = passport.getName();
            Label lblName = new Label(name);
            lblName.setStyle("-fx-text-fill: black; -fx-font-size: 4mm; -fx-font-weight: bold");

            return new ReadOnlyObjectWrapper(lblName);
        });
        tcName.setMinWidth(120);
        return tcName;
    };

    /**
     * ПОКРЫТИЕ
     */
    public static TableColumn<Detail, String> createTcCoat(){
        TableColumn<Detail, String> tcСoat = new TableColumn<>("Покрытие");
        tcСoat.setCellValueFactory(new PropertyValueFactory<>("coat"));
        tcСoat.setStyle("-fx-alignment: CENTER;");
        tcСoat.setMinWidth(120);
        return tcСoat;
    };

    /**
     * ПАПКА ХРАНЕНИЯ
     */
    public static TableColumn<Detail, String> createTcFolder(){
        TableColumn<Detail, String> tcFolder = new TableColumn<>("Папка\nхранения");
        tcFolder.setCellValueFactory(new PropertyValueFactory<>("folder"));
        tcFolder.setStyle("-fx-alignment: CENTER;");
        tcFolder.setMinWidth(120);
        return tcFolder;
    };

    /**
     * МАТЕРИАЛ
     */
    public static TableColumn<Detail, String> createTcMaterial(){
        TableColumn<Detail, String> tcMaterial = new TableColumn<>("Материал");
        tcMaterial.setCellValueFactory(new PropertyValueFactory<>("material"));
        tcMaterial.setStyle("-fx-alignment: CENTER;");
        tcMaterial.setMinWidth(120);
        return tcMaterial;
    };

    /**
     * ПАРАМЕТР A
     */
    public static TableColumn<Detail, String> createTcParamA(){
        TableColumn<Detail, String> tcParamA = new TableColumn<>("A");
        tcParamA.setCellValueFactory(new PropertyValueFactory<>("paramA"));
        tcParamA.setStyle("-fx-alignment: CENTER;");
        tcParamA.setMinWidth(60);
        return tcParamA;
    };

    /**
     * ПАРАМЕТР B
     */
    public static TableColumn<Detail, String> createTcParamB(){
        TableColumn<Detail, String> tcParamB = new TableColumn<>("B");
        tcParamB.setCellValueFactory(new PropertyValueFactory<>("paramB"));
        tcParamB.setStyle("-fx-alignment: CENTER;");
        tcParamB.setMinWidth(60);
        return tcParamB;
    };

    /**
     * ЧЕРТЕЖ
     */
    public static TableColumn<Detail, String> createTcDraft(){
        TableColumn<Detail, String> tcDraft = new TableColumn<>("Чертеж");
        tcDraft.setCellValueFactory(cd -> {

            try{
                cd.getValue().getDraft().getId();
            } catch(Exception ex) {
                return new ReadOnlyStringWrapper("НЕТ");
            }
            return new ReadOnlyStringWrapper("*");

        });
        tcDraft.setMinWidth(60);
        return tcDraft;
    };

    /**
     * ПРИМЕЧАНИЕ
     */
    public static TableColumn<Detail, String> createTcNote(){
        TableColumn<Detail, String> tcNote = new TableColumn<>("Примечание");
        tcNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        tcNote.setMinWidth(120);
        return tcNote;
    };
}
