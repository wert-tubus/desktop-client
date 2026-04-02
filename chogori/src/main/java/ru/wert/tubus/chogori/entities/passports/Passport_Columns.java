package ru.wert.tubus.chogori.entities.passports;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.components.HBoxPassport;
import ru.wert.tubus.chogori.setteings.ChogoriSettings;

import static ru.wert.tubus.chogori.statics.Comparators.createIntegerComparatorForStringColumn;
import static ru.wert.tubus.chogori.statics.Comparators.createLabelComparator;

public class Passport_Columns {

    /**
     * ID
     */
    public static TableColumn<Passport, String> createTcId(){
        TableColumn<Passport, String> tcId = new TableColumn<>("ID");
        tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcId.setStyle("-fx-alignment: CENTER;");
        tcId.setComparator(createIntegerComparatorForStringColumn(tcId));
        tcId.setPrefWidth(60);
        tcId.setMinWidth(50);
        tcId.setMaxWidth(80);
        return tcId;
    };

    /**
     * ПАССПОРТ - ИДЕНТИФИКАТОР
     */
    public static TableColumn<Passport, HBox> createTcPassport(){
        TableColumn<Passport, HBox> tcPassport = new TableColumn<>("Идентификатор");
        //Passport выводится в виде label
        tcPassport.setCellValueFactory(cd -> {

            Passport passport = cd.getValue();
            HBoxPassport hBoxPassport = new HBoxPassport(passport, "");
            return new ReadOnlyObjectWrapper<>(hBoxPassport);

        });

        tcPassport.setComparator((o1, o2) -> {
            String num1 = ((Label)o1.lookup("#number")).getText();
            String num2 = ((Label)o2.lookup("#number")).getText();

            return num1.compareTo(num2);
        });

        tcPassport.setMinWidth(150);
        tcPassport.setPrefWidth(200);
        return tcPassport;
    };

    /**
     * ДЕЦИМАЛЬНЫЙ НОМЕР
     */
    public static TableColumn<Passport, Label> createTcPassportNumber() {
        TableColumn<Passport, Label> tcPassportNumber = new TableColumn<>("Дец.номер");

        tcPassportNumber.setCellValueFactory(cd -> {
            Passport passport = cd.getValue();
            String prefix = passport.getPrefix().getName().equals("-") ? "" : passport.getPrefix().getName() + ".";
            if(!ChogoriSettings.CH_SHOW_PREFIX && passport.getPrefix().equals(ChogoriSettings.CH_DEFAULT_PREFIX)) prefix = "";
            String decNumber = prefix + passport.getNumber();

            Label lblNumber = new Label(decNumber);
            lblNumber.setTooltip(new Tooltip(decNumber));

            switch(passport.getNumber().substring(0,1)){
                case "7" :
                    lblNumber.setStyle("-fx-text-fill: darkgreen; -fx-font-size: 14;  -fx-font-weight: bold;");
                    break;
                case "3" :
                    lblNumber.setStyle("-fx-text-fill: darkblue; -fx-font-size: 14;  -fx-font-weight: bold;");
                    break;
                case "4" :
                    lblNumber.setStyle("-fx-text-fill: saddlebrown; -fx-font-size: 14;  -fx-font-weight: bold;");
                    break;
                default :
                    lblNumber.setStyle("-fx-text-fill: black; -fx-font-size: 14;  -fx-font-weight: bold;");
            }

            return new ReadOnlyObjectWrapper<>(lblNumber);

        });
        tcPassportNumber.setComparator(createLabelComparator(tcPassportNumber));
        tcPassportNumber.setMinWidth(120);
        tcPassportNumber.setPrefWidth(150);
        return tcPassportNumber;
    }

    /**
     * НАИМЕНОВАНИЕ
     */
    public static TableColumn<Passport, Label> createTcPassportName() {
        TableColumn<Passport, Label> tcPassportName = new TableColumn<>("Наименование");

        tcPassportName.setCellValueFactory(cd -> {
            Passport passport = cd.getValue();
            String name = passport.getName();

            Label lblName = new Label(name);
            lblName.setTooltip(new Tooltip(name));
            lblName.setWrapText(true);

            return new ReadOnlyObjectWrapper<>(lblName);

        });
        tcPassportName.setComparator(createLabelComparator(tcPassportName));
        tcPassportName.setMinWidth(150);
        tcPassportName.setPrefWidth(250);
        return tcPassportName;
    }

    /**
     * ИЗДЕЛИЕ (NOTE)
     */
    public static TableColumn<Passport, Label> createTcNote() {
        TableColumn<Passport, Label> tcNote = new TableColumn<>("Изделие");

        tcNote.setCellValueFactory(cd -> {
            Passport passport = cd.getValue();
            String note = passport.getNote();
            if (note == null) note = "";

            Label lblNote = new Label(note);
            lblNote.setTooltip(new Tooltip(note));
            lblNote.setWrapText(true);

            return new ReadOnlyObjectWrapper<>(lblNote);
        });
        tcNote.setComparator(createLabelComparator(tcNote));
        tcNote.setMinWidth(150);
        tcNote.setPrefWidth(200);
        return tcNote;
    }

    /**
     * РАЗРАБОТЧИК (USER NAME)
     */
    public static TableColumn<Passport, Label> createTcUserName() {
        TableColumn<Passport, Label> tcUserName = new TableColumn<>("Разработчик");

        tcUserName.setCellValueFactory(cd -> {
            Passport passport = cd.getValue();
            String userName = passport.getUserName();
            if (userName == null) userName = "";

            Label lblUserName = new Label(userName);
            lblUserName.setTooltip(new Tooltip(userName));

            return new ReadOnlyObjectWrapper<>(lblUserName);
        });
        tcUserName.setComparator(createLabelComparator(tcUserName));
        tcUserName.setMinWidth(120);
        tcUserName.setPrefWidth(150);
        return tcUserName;
    }

    /**
     * ДАТА
     */
    public static TableColumn<Passport, Label> createTcDate() {
        TableColumn<Passport, Label> tcDate = new TableColumn<>("Дата");

        tcDate.setCellValueFactory(cd -> {
            Passport passport = cd.getValue();
            String date = passport.getDate();
            if (date == null) date = "";

            Label lblDate = new Label(date);
            lblDate.setTooltip(new Tooltip(date));
            lblDate.setStyle("-fx-alignment: CENTER;");

            return new ReadOnlyObjectWrapper<>(lblDate);
        });
        tcDate.setComparator(createLabelComparator(tcDate));
        tcDate.setMinWidth(80);
        tcDate.setPrefWidth(100);
        tcDate.setMaxWidth(120);
        tcDate.setStyle("-fx-alignment: CENTER;");
        return tcDate;
    }
}
