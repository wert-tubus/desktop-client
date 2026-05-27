package ru.wert.tubus.chogori.chat.cards;

import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import ru.wert.tubus.chogori.application.drafts.DraftsTabController;
import ru.wert.tubus.chogori.application.drafts.OpenDraftsTabTask;
import ru.wert.tubus.chogori.tabs.AppTab;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.chat.dialog.dialogController.DialogController;

import static ru.wert.tubus.chogori.statics.AppStatic.CHAT_WIDTH;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_TAB_PANE;

/**
 * Controller описывает поведение карточки с наименованием комплекта чертежей, передаваемой через ЧАТ
 */
public class FolderCardController {

    @FXML
    private VBox vbFoldersName;

    @FXML
    private AnchorPane folderChatCard;


    public void init(String strId) {
        Folder folder = ChogoriServices.CH_FOLDERS.findById(Long.valueOf(strId));
        String folderName = folder.getName();
        Label label = new Label(folderName);
        label.setStyle("-fx-padding: 0 10 0 10;");
        label.setWrapText(true);
        label.setPrefWidth(CHAT_WIDTH * DialogController.MESSAGE_WIDTH);
        label.setId("draftInChat");

        vbFoldersName.getChildren().add(label);


         /* При двойном клике на карточку, производится проверка
         * если вкладка с чертежами уже открыта, то данные карточки передаются в существующий контроллер
         * если вкладка закрыта, то контроллер вкладки создается вновь*/
        vbFoldersName.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {

                AppTab pane = CH_TAB_PANE.tabIsAvailable("Чертежи");
                if(pane != null){
                    DraftsTabController controller = (DraftsTabController) pane.getTabController();
                    controller.openFolderByName(folder, null);
                    for(Tab t : CH_TAB_PANE.getTabs()){
                        if(t.getId().equals("Чертежи") && !t.isSelected())
                            CH_TAB_PANE.getSelectionModel().select(t);
                    }
                } else {
                    OpenDraftsTabTask openDraftsTask = new OpenDraftsTabTask();
                    openDraftsTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                            t -> {
                                DraftsTabController controller = openDraftsTask.getValue();
                                controller.openFolderByName(folder, null);
                            });

                    Thread thread = new Thread(openDraftsTask);
                    thread.setDaemon(true);
                    thread.start();
                }

            }

        });

    }
}
