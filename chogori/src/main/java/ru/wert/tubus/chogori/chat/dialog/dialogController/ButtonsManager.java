package ru.wert.tubus.chogori.chat.dialog.dialogController;

import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.drafts.OpenDraftsTabTask;
import ru.wert.tubus.chogori.images.BtnImages;

@Slf4j
public class ButtonsManager {

    private final DialogController controller;

    public ButtonsManager(DialogController controller) {
        this.controller = controller;
    }

    /**
     * Настройка кнопки отправки текстового сообщения.
     */
    public void createBtnSendText() {
        controller.getBtnSend().setText(null);
        controller.getBtnSend().setGraphic(new ImageView(BtnImages.SEND_MESSAGE_IMG));
        controller.getBtnSend().setTooltip(new Tooltip("Ctrl-Enter"));
        controller.getBtnSend().setOnAction(e -> {
            controller.getDialogListView().sendText(); // Отправка текста при нажатии на кнопку
            log.debug("Отправлено текстовое сообщение");
        });
    }

    /**
     * Настройка кнопки добавления изображений.
     */
    public void createBtnPictures() {
        controller.getBtnAddPicture().setText("");
        controller.getBtnAddPicture().setGraphic(new ImageView(BtnImages.BTN_ADD_CHAT_PIC_IMG));
        controller.getBtnAddPicture().setTooltip(new Tooltip("Добавить изображение"));
        controller.getBtnAddPicture().setOnAction(e -> {
            controller.getDialogListView().sendPicture(e); // Отправка изображения при нажатии на кнопку
            log.debug("Отправлено изображение");
        });
    }

    /**
     * Настройка кнопки добавления чертежей.
     */
    public void createBtnDrafts() {
        controller.getBtnAddDraft().setText("");
        controller.getBtnAddDraft().setGraphic(new ImageView(BtnImages.BTN_ADD_CHAT_DRAFT_IMG));
        controller.getBtnAddDraft().setTooltip(new Tooltip("Добавить чертеж"));
        controller.getBtnAddDraft().setOnAction(e -> {
            // Запуск задачи для открытия редактора чертежей
            Thread t = new Thread(new OpenDraftsTabTask());
            t.setDaemon(true);
            t.start();
            log.debug("Запущена задача для открытия редактора чертежей");
        });
    }

    /**
     * Настройка кнопки открытия списка комнат.
     */
    public void createBtnRooms() {
        controller.getBtnBackToRooms().setOnAction(e -> {
            controller.getChat().showChatGroups(); // Открытие списка комнат при нажатии на кнопку
            log.debug("Открыт список комнат");
        });
    }
}
