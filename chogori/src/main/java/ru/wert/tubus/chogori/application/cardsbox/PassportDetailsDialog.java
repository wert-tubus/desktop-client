package ru.wert.tubus.chogori.application.cardsbox;


import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.wert.tubus.client.entity.models.Passport;

import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;

public class PassportDetailsDialog {

    private final Stage stage;
    private final Passport passport;

    public PassportDetailsDialog(Passport passport) {
        this.passport = passport;
        this.stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(WF_MAIN_STAGE);
        stage.setTitle("Просмотр паспорта");

        stage.setScene(createScene());
    }

    private Scene createScene() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setVgap(10);
        grid.setHgap(10);

        int row = 0;

        // Номер
        grid.add(new Label("Номер:"), 0, row);
        TextField tfNumber = new TextField(passport.getNumber());
        tfNumber.setEditable(false);
        tfNumber.setPrefWidth(300);
        grid.add(tfNumber, 1, row++);

        // Наименование
        grid.add(new Label("Наименование:"), 0, row);
        TextField tfName = new TextField(passport.getName());
        tfName.setEditable(false);
        grid.add(tfName, 1, row++);

        // Изделие
        grid.add(new Label("Изделие:"), 0, row);
        TextField tfNote = new TextField(passport.getNote() != null ? passport.getNote() : "");
        tfNote.setEditable(false);
        grid.add(tfNote, 1, row++);

        // Разработчик
        grid.add(new Label("Разработчик:"), 0, row);
        TextField tfUser = new TextField(passport.getUserName() != null ? passport.getUserName() : "");
        tfUser.setEditable(false);
        grid.add(tfUser, 1, row++);

        // Дата
        grid.add(new Label("Дата:"), 0, row);
        TextField tfDate = new TextField(passport.getDate() != null ? passport.getDate() : "");
        tfDate.setEditable(false);
        grid.add(tfDate, 1, row++);

        Button btnOk = new Button("OK");
        btnOk.setOnAction(e -> stage.close());

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(15));
        mainLayout.getChildren().addAll(grid, btnOk);

        return new Scene(mainLayout, 450, 300);
    }

    public void showAndWait() {
        stage.showAndWait();
    }
}
