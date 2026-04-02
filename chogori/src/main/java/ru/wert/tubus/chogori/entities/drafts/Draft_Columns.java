package ru.wert.tubus.chogori.entities.drafts;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ru.wert.tubus.chogori.images.BtnImages;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.components.HBoxPassport;
import ru.wert.tubus.chogori.popups.HintPopup;
import ru.wert.tubus.chogori.setteings.ChogoriSettings;
import ru.wert.tubus.winform.enums.EDraftStatus;
import ru.wert.tubus.winform.enums.EDraftType;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.*;
import static ru.wert.tubus.chogori.statics.AppStatic.EXTENSIONS_DOCKS;
import static ru.wert.tubus.chogori.statics.Comparators.createLabelComparator;
import static ru.wert.tubus.winform.statics.WinformStatic.parseLDTtoNormalDate;

public class Draft_Columns {

    /**
     * ID
     */
    public static TableColumn<Draft, String> createTcId(){
        TableColumn<Draft, String> tcId = new TableColumn<>("ID");
        tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcId.setStyle("-fx-alignment: CENTER;");

        tcId.setMinWidth(50);
        tcId.setPrefWidth(80);
        tcId.setMaxWidth(80);
        tcId.setResizable(false);
//        tcId.setComparator(createIntegerComparator(tcId));
        return tcId;

    };

    /**
     * ПАССПОРТ - ИДЕНТИФИКАТОР
     */
    public static TableColumn<Draft, HBox> createTcPassport(){
        TableColumn<Draft, HBox> tcPassport = new TableColumn<>("Идентификатор");
        //Passport выводится в виде label
        tcPassport.setCellValueFactory(cd -> {

            Passport passport = cd.getValue().getPassport();
            HBoxPassport vBoxPassport = new HBoxPassport(passport, "00");
            return new ReadOnlyObjectWrapper<>(vBoxPassport);

        });

        tcPassport.setComparator((o1, o2) -> {
            String num1 = ((Label)o1.lookup("#number")).getText();
            String num2 = ((Label)o2.lookup("#number")).getText();

            return num1.compareTo(num2);
        });

        tcPassport.setMinWidth(150);
        tcPassport.setPrefWidth(150);
        tcPassport.setMaxWidth(5000);
        tcPassport.setResizable(true);
        return tcPassport;
    };

    /**
     * ДЕЦИМАЛЬНЫЙ НОМЕР
     */
    public static TableColumn<Draft, Label> createTcDraftNumber() {
        TableColumn<Draft, Label> tcDraftNumber = new TableColumn<>("Дец.номер");

        tcDraftNumber.setCellValueFactory(cd -> {
            final Passport passport = cd.getValue().getPassport();
            String prefix = passport.getPrefix().getName().equals("-") ? "" : passport.getPrefix().getName() + ".";

            if(!ChogoriSettings.CH_SHOW_PREFIX && passport.getPrefix().equals(ChogoriSettings.CH_DEFAULT_PREFIX))
                prefix = "";

            String decNumber = prefix + passport.getNumber();

            final EDraftType type = EDraftType.getDraftTypeById(cd.getValue().getDraftType());
            if(EXTENSIONS_DOCKS.contains(type))
                decNumber = decNumber + "-" + String.format("%02d", cd.getValue().getPageNumber());

            Label lblNumber = new Label(decNumber);

            // Устанавливаем цвет в зависимости от типа чертежа или первой цифры номера
            if(type == EDraftType.IMAGE_DXF) {
                lblNumber.setStyle("-fx-text-fill: #7322a3; -fx-font-size: 14; -fx-font-weight: bold;");
            } else {
                switch(passport.getNumber().substring(0,1)){
                    case "7":
                        lblNumber.setStyle("-fx-text-fill: darkgreen; -fx-font-size: 14; -fx-font-weight: bold;");
                        break;
                    case "3":
                        lblNumber.setStyle("-fx-text-fill: darkblue; -fx-font-size: 14; -fx-font-weight: bold;");
                        break;
                    case "4":
                        lblNumber.setStyle("-fx-text-fill: saddlebrown; -fx-font-size: 14; -fx-font-weight: bold;");
                        break;
                    default:
                        lblNumber.setStyle("-fx-text-fill: black; -fx-font-size: 14; -fx-font-weight: bold;");
                }
            }

            return new ReadOnlyObjectWrapper<>(lblNumber);
        });

        tcDraftNumber.setComparator(createLabelComparator(tcDraftNumber));

        tcDraftNumber.setMinWidth(130);
        tcDraftNumber.setPrefWidth(130);
        tcDraftNumber.setMaxWidth(5000);
        tcDraftNumber.setResizable(true);
        return tcDraftNumber;
    }

    /**
     * НАИМЕНОВАНИЕ
     */
    public static TableColumn<Draft, Label> createTcDraftName() {
        TableColumn<Draft, Label> tcDraftName = new TableColumn<>("Наименование");

        tcDraftName.setCellValueFactory(cd -> {
            Passport passport = cd.getValue().getPassport();
            String name = passport.getName();

            Label lblName = new Label(name);

            return new ReadOnlyObjectWrapper<>(lblName);

        });
        tcDraftName.setComparator(createLabelComparator(tcDraftName));

        tcDraftName.setMinWidth(100);
        tcDraftName.setPrefWidth(120);
        tcDraftName.setMaxWidth(5000);
        tcDraftName.setResizable(true);
        return tcDraftName;
    }

    /**
     * СТАТУС
     */
    public static TableColumn<Draft, Label> createTcStatus(){
        TableColumn<Draft, Label> tcStatus = new TableColumn<>("Статус");
        tcStatus.setCellValueFactory(cd->{
            HintPopup hint;
            Draft draft = cd.getValue();
            Integer statusId = draft.getStatus();
            EDraftStatus status = EDraftStatus.getStatusById(statusId);
            String str = "";
            Label lblStatus = new Label();
            lblStatus.setMouseTransparent(true); //Подсказка не работает
            if(status != null){
//                if(status.equals(EDraftStatus.LEGAL)){

                    lblStatus.setText(status.getStatusName());
                    String hintText = draft.getStatusUser().getName() + "\n" + parseLDTtoNormalDate(draft.getStatusTime());

                hint = new HintPopup(lblStatus, hintText, 3.0);

                lblStatus.setOnMouseEntered(e->{
                    hint.showHint();

                });
                lblStatus.setOnMouseExited(e->{
                    hint.closeHint();
                });

                switch(status){
                    case LEGAL:lblStatus.setStyle("-fx-text-fill: darkblue"); break;
                    case CHANGED:lblStatus.setStyle("-fx-text-fill: saddlebrown");break;
                    case ANNULLED:lblStatus.setStyle("-fx-text-fill: darkred");break;
                    default:lblStatus.setStyle("-fx-text-fill: black");
                }

            }

            return new ReadOnlyObjectWrapper<>(lblStatus);
        });
        tcStatus.setComparator(createLabelComparator(tcStatus));
        tcStatus.setStyle("-fx-alignment: CENTER;");

        tcStatus.setMinWidth(100);//100
        tcStatus.setPrefWidth(100);//100
        tcStatus.setMaxWidth(100);
        tcStatus.setResizable(false);
        return tcStatus;
    };

    /**
     * ПАПКА ХРАНЕНИЯ
     */
    public static TableColumn<Draft, String> createTcFolder(){
        TableColumn<Draft, String> tcFolder = new TableColumn<>("Папка хранения");
        tcFolder.setCellValueFactory(new PropertyValueFactory<>("folder"));
        tcFolder.setStyle("-fx-alignment: CENTER;");

        tcFolder.setMinWidth(100);//120
        tcFolder.setPrefWidth(120);//120
        tcFolder.setMaxWidth(5000);
        tcFolder.setResizable(true);
        return tcFolder;
    };

    /**
     * ИСХОДНОЕ НАЗВАНИЕ ФАЙЛА
     */
    public static TableColumn<Draft, String> createTcInitialDraftName(){
        TableColumn<Draft, String> tcInitialDraftName = new TableColumn<>("Имя файла");
        tcInitialDraftName.setCellValueFactory(new PropertyValueFactory<>("initialDraftName"));
        tcInitialDraftName.setStyle("-fx-alignment: CENTER;");

        tcInitialDraftName.setMinWidth(100);//120
        tcInitialDraftName.setPrefWidth(120);//120
        tcInitialDraftName.setMaxWidth(5000);
        tcInitialDraftName.setResizable(true);

        return tcInitialDraftName;
    };

    /**
     * ТИП ДОКУМЕНТА И СТРАНИЦА
     */
    public static TableColumn<Draft, Label> createTcDraftType(){
        TableColumn<Draft, Label> tcDraftType = new TableColumn<>("Тип/стр");
        tcDraftType.setCellValueFactory(cd->{
            Draft draft = cd.getValue();
            EDraftType type = EDraftType.getDraftTypeById(draft.getDraftType());
            assert type != null;
            String str = type.getShortName() + "-" + draft.getPageNumber();
            Label label = new Label(str);
            switch(type){
                case DETAIL: label.setStyle("-fx-text-fill: green"); break;
                case ASSEMBLE: label.setStyle("-fx-text-fill: darkblue"); break;
                case SPECIFICATION: label.setStyle("-fx-text-fill: brown"); break;
                case IMAGE_DXF: label.setStyle("-fx-text-fill: #7322a3"); break;
                default: label.setStyle("-fx-text-fill: black");
            }


            return new ReadOnlyObjectWrapper<>(label);
        });
        tcDraftType.setSortable(false);
        tcDraftType.setStyle("-fx-alignment: CENTER;");

        tcDraftType.setMinWidth(80);//80
        tcDraftType.setPrefWidth(80);//80
        tcDraftType.setMaxWidth(80);
        tcDraftType.setResizable(false);
        return tcDraftType;
    };

    /**
     * КОММЕНТАРИИ
     */
    public static TableColumn<Draft, ImageView> createTcRemarks(){
        TableColumn<Draft, ImageView> tcRemarks = new TableColumn<>("K");
        tcRemarks.setCellValueFactory(cd->{
            Draft draft = cd.getValue();
            ImageView image = new ImageView();
            if(CH_QUICK_DRAFTS.hasRemarks(draft)){
                image = new ImageView(BtnImages.BTN_REMARKS_IMG);
            }
            return new ReadOnlyObjectWrapper(image);
        });

        tcRemarks.setStyle("-fx-alignment: CENTER;");
        tcRemarks.setMinWidth(40);//40
        tcRemarks.setPrefWidth(40);//40
        tcRemarks.setMaxWidth(40);
        tcRemarks.setResizable(false);
        return tcRemarks;
    };

    /**
     * ИНФОРМАЦИЯ О СОЗДАНИИ
     */
    public static TableColumn<Draft, String> createTcCreation(){
        TableColumn<Draft, String> tcCreation = new TableColumn<>("Создан");
        tcCreation.setCellValueFactory(cd->{
            Draft draft = cd.getValue();
            String str = parseLDTtoNormalDate(draft.getCreationTime());
            return new ReadOnlyStringWrapper(str);
        });

        tcCreation.setStyle("-fx-alignment: CENTER;");
        tcCreation.setMinWidth(150);//120
        tcCreation.setPrefWidth(150);//120
        tcCreation.setMaxWidth(150);
        tcCreation.setResizable(false);
        return tcCreation;
    };



    /**
     * ПРИМЕЧАНИЕ
     */
    public static TableColumn<Draft, String> createTcNote(){
        TableColumn<Draft, String> tcNote = new TableColumn<>("Примечание");
        tcNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        tcNote.setMinWidth(120);//120
        tcNote.setMaxWidth(5000);
        tcNote.setResizable(true);
        tcNote.setSortable(false);
        return tcNote;
    };
}
