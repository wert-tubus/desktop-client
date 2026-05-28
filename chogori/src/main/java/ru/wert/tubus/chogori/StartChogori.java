package ru.wert.tubus.chogori;

import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.app_window.AppMenuController;
import ru.wert.tubus.chogori.components.FileFwdSlash;
import ru.wert.tubus.chogori.images.AppImages;
import ru.wert.tubus.chogori.pdf.ICEpdfGlobalDialogBlocker;
import ru.wert.tubus.chogori.pdf.ICEpdfLoggingBlocker;
import ru.wert.tubus.chogori.search.SearchHistoryFile;
import ru.wert.tubus.chogori.statics.UtilStaticNodes;
import ru.wert.tubus.chogori.tempfile.TempDir;
import ru.wert.tubus.chogori.toolpane.ChogoriToolBar;
import ru.wert.tubus.client.entity.models.VersionDesktop;
import ru.wert.tubus.client.retrofit.AppProperties;
import ru.wert.tubus.winform.statics.WinformStatic;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;
import ru.wert.tubus.winform.window_decoration.WindowDecorationController;
import ru.wert.tubus.winform.winform_settings.WinformSettings;

import java.io.IOException;
import java.util.List;

import static ru.wert.tubus.chogori.application.services.ChogoriServices.*;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.MAIN_CLOSE_BUTTON;
import static ru.wert.tubus.winform.statics.WinformStatic.PROGRAM_NAME;
import static ru.wert.tubus.winform.statics.WinformStatic.TEST_VERSION;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

@Slf4j
public class StartChogori extends Application {

    private boolean initStatus = true;
    public static AppMenuController APP_MENU_CONTROLLER;

    @Override
    public void init(){

        try {
            // 1. Инициализация настроек приложения
            AppPropsSettings appSettings = AppPropsSettings.getInstance();
            log.info("AppPropsSettings инициализирован. Файл настроек: {}", appSettings.getConfigFilePath());
            // 2. Инициализация сервисов
            initServices();
            initQuickServicesWithCache();

            log.debug("init : DATA from server got well!");
        } catch (Exception e) {
            log.error("init : couldn't get DATA from server");
            initStatus = false;
        }

        //Определяем последнюю доступную версию программы в Базе данных
        List<VersionDesktop> allVersions = CH_VERSIONS_DESKTOP.findAll();
        WinformStatic.LAST_VERSION_IN_DB = allVersions.get(allVersions.size()-1).getName();

        new ChogoriToolBar();
        //Создадим папку временного хранения файлов чертежей
        FileFwdSlash tempDir = TempDir.createTempDirectory("temp-tubus");
        log.info("Temp folder has been created : {}", tempDir.toString());

        WinformStatic.WF_TEMPDIR = tempDir;
        log.info("WinformStatic.WF_TEMPDIR = tempDir; passed" );

        //Если монитор, заявленный в настройках отсутствует, то при запуске он меняется на основной
        int targetMonitor = AppProperties.getInstance().getMonitor();
        List<Screen> screenList = Screen.getScreens();
        if(targetMonitor > screenList.size()-1){
            AppProperties.getInstance().setMonitor(0);
        }
        WinformSettings.CH_MONITOR = AppProperties.getInstance().getMonitor();


        log.info("AppProperties.getInstance().getMonitor() passed");

    }

    @Override
    public void start(Stage stage) throws Exception {

        if (!initStatus) {
            Warning1.create($ATTENTION, "Не удалось загрузить чертежи с сервера", "Работа программы будет прекращена" +
                    "\nдля перезагрузки сервера обратитесь к администратору");
            System.exit(0);
        }
        WinformStatic.WF_MAIN_STAGE = stage;

        try {
            //Загружаем WindowDecoration
            FXMLLoader decorationLoader = new FXMLLoader(WindowDecoration.class.getResource("/winform-fxml/window_decoration/window_decoration.fxml"));
            Parent decoration = decorationLoader.load();
            WindowDecorationController controller = decorationLoader.getController();

            //Загружаем loginWindow
            FXMLLoader loginWindowLoader = new FXMLLoader(getClass().getResource("/chogori-fxml/login/login.fxml"));
            Parent loginWindow = loginWindowLoader.load();

            //loginWindow помещаем в WindowDecoration
            UtilStaticNodes.CH_DECORATION_ROOT_PANEL = (StackPane)decoration.lookup("#mainPane");
            UtilStaticNodes.CH_DECORATION_ROOT_PANEL.getChildren().add(loginWindow);
            MAIN_CLOSE_BUTTON = (ImageView) decoration.lookup("#imgBtnClose");
            MAIN_CLOSE_BUTTON.setOnMousePressed(e->{
                SearchHistoryFile.getInstance().save();
            });

            //Меняем заголовок окна
            Label programName = (Label)decoration.lookup("#programName");
            Label windowName = (Label)decoration.lookup("#windowName");

            programName.setText(!TEST_VERSION ? PROGRAM_NAME : PROGRAM_NAME + " ТЕСТ");
            windowName.setText("");

            Scene scene = new Scene(decoration);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            scene.getStylesheets().add(this.getClass().getResource("/chogori-css/pik-dark.css").toString());


            stage.sizeToScene();
            stage.setResizable(true);
            stage.getIcons().add(AppImages.LOGO_ICON);

            stage.show();

            controller.centerInitialWindow(stage, true, WinformSettings.CH_MONITOR);

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //Блокируем любые сообщения из ICEpdf библиотеки
        ICEpdfGlobalDialogBlocker.blockAllDialogs();
        ICEpdfLoggingBlocker.disableAllLogging();

        //Запускаем приложение
        if(!TEST_VERSION) SentryConfig.initialize();
        LauncherImpl.launchApplication(StartChogori.class, AppPreloader.class, args);
    }
}
