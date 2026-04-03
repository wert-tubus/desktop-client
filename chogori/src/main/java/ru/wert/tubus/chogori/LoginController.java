package ru.wert.tubus.chogori;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.chogori.chat.socketwork.ServiceMessaging;
import ru.wert.tubus.chogori.chat.socketwork.socketservice.SocketService;
import ru.wert.tubus.chogori.components.BXUsers;
import ru.wert.tubus.chogori.statics.AppStatic;
import ru.wert.tubus.client.entity.models.AppSettings;
import ru.wert.tubus.client.entity.models.User;
import ru.wert.tubus.client.retrofit.AppProperties;
import ru.wert.tubus.winform.enums.EPDFViewer;
import ru.wert.tubus.winform.warnings.Warning1;

import java.io.File;
import java.io.IOException;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_SETTINGS;
import static ru.wert.tubus.chogori.application.services.ChogoriServices.CH_USERS;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.*;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_DECORATION_ROOT_PANEL;
import static ru.wert.tubus.winform.warnings.WarningMessages.*;


@Slf4j
public class LoginController {

    @FXML
    private ComboBox<User> bxUsers;

    //Поле ввода пароля
    @FXML
    private PasswordField passwordField;


    @FXML
    void initialize() {

        log.debug("initialize : login window is initializing ...");
        new BXUsers(bxUsers, null);

        long userId = AppProperties.getInstance().getLastUser();
        User lastUser = userId == 0L ? null : CH_USERS.findById(userId);

        //Если последнего пользователя успели заблокировать
        if (lastUser != null && !lastUser.isActive()) {
            bxUsers.getSelectionModel().select(null);
        } else {
            bxUsers.getSelectionModel().select(lastUser);
        }

        CH_CURRENT_USER = null;
        CH_CURRENT_USER_GROUP = null;

        passwordField.setOnAction(this::welcomeUser);

        Platform.runLater(()->passwordField.requestFocus());

        log.debug("initialize : login window has been initialized well!");

    }

    private void welcomeUser(Event e){
        User user = bxUsers.getValue();
        String pass = passwordField.getText();

        if (user.getPassword().equals(pass)) {
            // Инициализируем быстрые сервисы перед началом работы
            if(ChogoriServices.CH_QUICK_DRAFTS == null) {
                ChogoriServices.initQuickServices();
            }

            CH_CURRENT_USER = user;

            startSocketServerWithChats();

            loadApplicationSettings();
            showTabPaneWindow();
            AppProperties.getInstance().setLastUser(user.getId());
            AppStatic.createLog(true, "Подключился к серверу");
            ServiceMessaging.sendMessageUserIn(CH_CURRENT_USER.getId());

            if(CH_CURRENT_USER_SETTINGS.isOpenDraftsTabOnStart())
                openDrafts();
        } else {
            passwordField.setText("");
            Warning1.create($ATTENTION, $NO_SUCH_USER, $TRY_MORE);
        }
    }

    private void startSocketServerWithChats() {
        // Добавляем shutdown hook для корректного завершения работы при завершении приложения
        Runtime.getRuntime().addShutdownHook(new Thread(SocketService::stop));
        // Запускаем сервис
        SocketService.start();
    }

    private void loadApplicationSettings() {
        log.debug("loadApplicationSettings : загружаются настройки приложения...");
        CH_CURRENT_USER_GROUP = CH_CURRENT_USER.getUserGroup();

        CH_CURRENT_USER_SETTINGS = CH_SETTINGS.findByName(CH_CURRENT_USER.getName());
        if(CH_CURRENT_USER_SETTINGS == null){
            AppSettings defaultSettings = CH_SETTINGS.findByName("default");
            AppSettings newUserSettings = defaultSettings.makeCopy();
            newUserSettings.setName(CH_CURRENT_USER.getName());
            newUserSettings.setUser(CH_CURRENT_USER);
            CH_CURRENT_USER_SETTINGS = CH_SETTINGS.save(newUserSettings);
        }

        CH_SHOW_PREFIX = CH_CURRENT_USER_SETTINGS.isShowPrefixes();
        CH_SHOW_NOTIFICATION_LINE = CH_CURRENT_USER_SETTINGS.isShowNotificationLine();
        CH_CORRECT_DET_TO_ASSM = CH_CURRENT_USER_SETTINGS.isCorrectDetToAssm();
        CH_DEFAULT_PREFIX = CH_CURRENT_USER_SETTINGS.getDefaultPrefix();
        CH_OPEN_DRAFTS_TAB_ON_START = CH_CURRENT_USER_SETTINGS.isOpenDraftsTabOnStart();
        CH_VALIDATE_DEC_NUMBERS = CH_CURRENT_USER_SETTINGS.isValidateDecNumbers();
        CH_PDF_VIEWER = EPDFViewer.values()[CH_CURRENT_USER_SETTINGS.getPdfViewer()];
        CH_DEFAULT_PATH_TO_NORMY_MK = new File(CH_CURRENT_USER_SETTINGS.getPathToNormyMK());

        log.debug("loadApplicationSettings : настройки приложения успешно загружены");

    }

    private void openDrafts(){
        Platform.runLater(()->{
            StartChogori.APP_MENU_CONTROLLER.openDrafts(null);
        });

    }


    private void showTabPaneWindow() {
        log.info("В приложение вошел по паролю {} в качестве {}", CH_CURRENT_USER.getName(), CH_CURRENT_USER_GROUP);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/appWindow/application.fxml"));
            Parent parent = loader.load();

            CH_DECORATION_ROOT_PANEL.getChildren().clear();
            CH_DECORATION_ROOT_PANEL.getChildren().add(parent);


            log.debug("На панель CH_DECORATION_ROOT_PANEL добавлена application.fxml");
            log.info("ПРИЛОЖЕНИЕ ЗАПУЩЕНО!!!");

        } catch (IOException ioException) {
            log.info("application.fxml не была создана");
            ioException.printStackTrace();
        }
    }


}
