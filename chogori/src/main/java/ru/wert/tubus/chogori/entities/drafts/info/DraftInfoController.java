package ru.wert.tubus.chogori.entities.drafts.info;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.winform.enums.EDraftStatus;
import ru.wert.tubus.winform.enums.EDraftType;

import static ru.wert.tubus.winform.statics.WinformStatic.parseLDTtoNormalDate;

public class DraftInfoController {

    @FXML private AnchorPane apInfo;

    @FXML private TextField tfId;
    @FXML private TextField tfDecNumber;
    @FXML private TextField tfName;
    @FXML private TextField tfTypeStr;
    @FXML private TextField tfStatus;
    @FXML private TextField tfStatusTime;
    @FXML private TextField tfSourceFileName;
    @FXML private TextField tfCreationTime;
    @FXML private TextField tfSource;
    @FXML private TextArea taNote;

    @FXML private Button btnOK;

    @FXML
    void initialize() {

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

        btnOK.setOnAction(AppStatic::closeWindow);
    }

    public void init(Draft draft) {
        if (draft == null) {
            clearAllFields();
            return;
        }

        // ID
        tfId.setText(String.valueOf(draft.getId()));

        // Дец. номер
        tfDecNumber.setText(draft.getDecimalNumber() != null ? draft.getDecimalNumber() : "");

        // Наименование
        tfName.setText(draft.getName() != null ? draft.getName() : "");

        // Имя файла
        tfSourceFileName.setText(draft.getInitialDraftName() != null ? draft.getInitialDraftName() : "");

        // Время создания (с пользователем)
        String creationTime = "";
        if (draft.getCreationTime() != null) {
            creationTime = parseLDTtoNormalDate(draft.getCreationTime());
        }
        String creationUser = draft.getCreationUser() != null ? draft.getCreationUser().getName() : "";
        tfCreationTime.setText(creationTime + (creationUser.isEmpty() ? "" : ", " + creationUser));

        // Тип/стр
        EDraftType type = EDraftType.getDraftTypeById(draft.getDraftType());
        String typeStr = "";
        String pageStr = "";
        if (type != null) {
            typeStr = type.getShortName() != null ? type.getShortName() : "";
        }
        if (draft.getPageNumber() != null) {
            pageStr = "-" + draft.getPageNumber();
        }
        tfTypeStr.setText(typeStr + pageStr);

        // Статус
        if (draft.getStatus() != null) {
            String statusName = EDraftStatus.getStatusById(draft.getStatus()).getStatusName();
            tfStatus.setText(statusName != null ? statusName : "");
        } else {
            tfStatus.setText("");
        }

        // Время статуса
        tfStatusTime.setText(draft.getStatusTime() != null ? parseLDTtoNormalDate(draft.getStatusTime()) : "");

        // Комплект (папка)
        tfSource.setText(draft.getFolder() != null ? draft.getFolder().toUsefulString() : "");

        // Примечание
        taNote.setText(draft.getNote() != null ? draft.getNote() : "");

        // Устанавливаем фокус
        Platform.runLater(() -> apInfo.requestFocus());
    }

    private void clearAllFields() {
        tfId.clear();
        tfDecNumber.clear();
        tfName.clear();
        tfTypeStr.clear();
        tfStatus.clear();
        tfStatusTime.clear();
        tfSourceFileName.clear();
        tfCreationTime.clear();
        tfSource.clear();
        taNote.clear();
    }
}
