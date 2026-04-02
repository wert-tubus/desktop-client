package ru.wert.tubus.chogori.chat.cards;

import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import ru.wert.tubus.chogori.application.passports.OpenPassportsEditorTask;
import ru.wert.tubus.chogori.application.passports.PassportsEditorController;
import ru.wert.tubus.chogori.components.HBoxPassport;
import ru.wert.tubus.chogori.tabs.AppTab;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.chat.dialog.dialogController.DialogController;

import static ru.wert.tubus.chogori.statics.AppStatic.CHAT_WIDTH;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_TAB_PANE;

/**
 * Controller писывает поведение карточки с наименованием комплекта чертежей, передаваемой через ЧАТ
 */
public class PassportCardController {

    @FXML
    private VBox vbPassportsName;


    public void init(String strId) {
        Passport passport = ChogoriServices.CH_PASSPORTS.findById(Long.valueOf(strId));
        String passportName = passport.getName();
        HBoxPassport vb = new HBoxPassport(passport, "00");
        vb.setStyle("-fx-padding: 0 10 0 10;");
        vb.setPrefWidth(CHAT_WIDTH * DialogController.MESSAGE_WIDTH);
        vb.setId("draftInChat");

        vbPassportsName.getChildren().add(vb);


         /* При двойном клике на карточку, производится проверка
         * если вкладка с чертежами уже открыта, то данные карточки передаются в существующий контроллер
         * если вкладка закрыта, то контроллер вкладки создается вновь*/
        vbPassportsName.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {

                AppTab pane = CH_TAB_PANE.tabIsAvailable("Картотека");
                if(pane != null){
                    PassportsEditorController controller = (PassportsEditorController) pane.getTabController();
                    controller.openPassportFromChat(passport);
                } else {
                    OpenPassportsEditorTask openPassportsTask = new OpenPassportsEditorTask();
                    openPassportsTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                            t -> {
                                PassportsEditorController controller = openPassportsTask.getValue();
                                controller.openPassportFromChat(passport);
                            });

                    Thread thread = new Thread(openPassportsTask);
                    thread.setDaemon(true);
                    thread.start();
                }

            }

        });

    }
}
