package ru.wert.tubus.chogori.application.drafts;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.winform.modal.WaitAMinute;

import java.io.IOException;

import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_TAB_PANE;

@Slf4j
public class OpenDraftsTabTask extends Task<DraftsTabController> {

    DraftsTabController controller = null;

    @Override
    public DraftsTabController call() throws InterruptedException {

        if (isCancelled()) return null;

        Platform.runLater(WaitAMinute::create);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/drafts/draftsEditor.fxml"));
            Parent parent = loader.load();
            controller = loader.getController();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/drafts-dark.css").toString());
            String tabName = "Чертежи";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true, loader.getController());
        } catch (IOException e) {
            log.debug("OpenDraftsEditorTask : Cancelled by user");
        }

        return controller;
    }

    @Override
    protected void done() {
        super.done();
        Platform.runLater(WaitAMinute::close);
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        Platform.runLater(WaitAMinute::close);
    }

}

