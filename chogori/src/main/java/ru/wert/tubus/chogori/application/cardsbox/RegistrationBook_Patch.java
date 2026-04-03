package ru.wert.tubus.chogori.application.cardsbox;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.Getter;

import java.io.IOException;

public class RegistrationBook_Patch {

    @Getter private Parent parent;
    @Getter private RegistrationBookController registrationBookController;

    public RegistrationBook_Patch create(){

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/registrationBook.fxml"));
            parent = loader.load();
            registrationBookController = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

}
