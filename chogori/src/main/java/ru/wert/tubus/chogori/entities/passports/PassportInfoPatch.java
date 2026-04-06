package ru.wert.tubus.chogori.entities.passports;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.io.IOException;

import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;

public class PassportInfoPatch {

    public static void create(Passport passport, Event event){
        try {
            FXMLLoader loader = new FXMLLoader(PassportInfoPatch.class.getResource("/chogori-fxml/passports/passportInfo.fxml"));
            Parent parent = loader.load();
            PassportInfoController controller = loader.getController();
            controller.init(passport);

            Stage owner = (event == null)?
                    WF_MAIN_STAGE: (Stage)((Node)event.getSource()).getScene().getWindow();

            new WindowDecoration("Информация о номере", parent, false, owner, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
