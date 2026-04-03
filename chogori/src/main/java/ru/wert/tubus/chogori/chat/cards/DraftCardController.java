package ru.wert.tubus.chogori.chat.cards;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import ru.wert.tubus.chogori.components.HBoxPassport;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.winform.enums.EDraftType;

import java.util.Collections;

public class DraftCardController {

    @FXML
    private VBox vbDraftsName;

    @FXML
    private AnchorPane draftChatCard;

    public void init(String strId){
        Draft draft = ChogoriServices.CH_DRAFTS.findById(Long.valueOf(strId));
        Passport passport = draft.getPassport();
        VBox box = new VBox();
        HBoxPassport vBoxPassport = new HBoxPassport(passport, true, "00");
        String type = EDraftType.getDraftTypeById(draft.getDraftType()).getTypeName();
        String page = String.valueOf(draft.getPageNumber());
        Label lblTypeAndPage = new Label(type + ", стр." + page);
        box.getChildren().addAll(vBoxPassport, lblTypeAndPage);
        box.setId("draftInChat");
        vbDraftsName.getChildren().add(box);
        vbDraftsName.setOnMouseClicked(e->{
            if(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2){
                AppStatic.openDraftsInNewTabs(Collections.singletonList(draft));
            }
        });

    }
}
