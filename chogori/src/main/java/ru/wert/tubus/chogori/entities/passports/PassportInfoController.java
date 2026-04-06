package ru.wert.tubus.chogori.entities.passports;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Passport;

public class PassportInfoController {

    @FXML private AnchorPane apInfo;

    @FXML private TextField tfId;
    @FXML private TextField tfNumber;
    @FXML private TextField tfName;
    @FXML private TextField tfDate;
    @FXML private TextField tfUser;
    @FXML private TextArea taNote;

    @FXML private Button btnOK;

    @FXML
    private void initialize() {
        // Базовый стиль
        taNote.setStyle(
                "-fx-background-color: white; " +
                        "-fx-control-inner-background: white; " +
                        "-fx-text-fill: black;"
        );

        // Отложенная установка стиля для внутренних элементов
        taNote.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(() -> {
                    if (taNote.lookup(".content") != null) {
                        taNote.lookup(".content").setStyle("-fx-background-color: white;");
                    }
                });
            }
        });
    }

    void init(Passport passport) {
        if (passport == null) {
            clearAllFields();
            btnOK.setOnAction(AppStatic::closeWindow);
            return;
        }

        tfId.setText(passport.getId() != null ? String.valueOf(passport.getId()) : "");
        tfNumber.setText(passport.getNumber() != null ? String.valueOf(passport.getNumber()) : "");
        tfName.setText(passport.getName() != null ? passport.getName() : "");
        tfDate.setText(passport.getDate() != null ? String.valueOf(passport.getDate()) : "");
        tfUser.setText(passport.getUserName() != null ? passport.getUserName() : "");
        taNote.setText(passport.getNote() != null ? passport.getNote() : "");

        btnOK.setOnAction(AppStatic::closeWindow);
    }

    private void clearAllFields() {
        tfId.clear();
        tfNumber.clear();
        tfName.clear();
        tfDate.clear();
        tfUser.clear();
        taNote.clear();
    }
}
