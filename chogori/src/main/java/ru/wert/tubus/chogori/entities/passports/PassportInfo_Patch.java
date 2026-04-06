package ru.wert.tubus.chogori.entities.passports;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.io.IOException;

import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;

public class PassportInfo_Patch {

    public static void create(Passport passport){
        try {
            FXMLLoader loader = new FXMLLoader(PassportInfo_Patch.class.getResource("/chogori-fxml/passports/passportInfo.fxml"));
            Parent parent = loader.load();
            PassportInfo_Controller controller = loader.getController();
            controller.init(passport);

            // Всегда используем главное окно как owner
            // Это самый простой и надежный способ
            new WindowDecoration("Информация о номере", parent, false, WF_MAIN_STAGE, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
