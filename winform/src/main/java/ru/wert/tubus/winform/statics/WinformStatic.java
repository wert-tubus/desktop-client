package ru.wert.tubus.winform.statics;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.winform.warnings.Warning2;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Slf4j
public class WinformStatic {

    public static String PROGRAM_NAME = "TUBUS"; //Наименование программы
    public static String CURRENT_PROJECT_VERSION = "7.8"; //Версия приложения обновляется вручную
    public static String VERSION_CREATED_DATE = "27.03.2026"; //Дата выпуска версии
    public static boolean TEST_VERSION = true; //тестовая версия - работает с тестовым сервером
    public static boolean USE_HEARTBEAT = true; //Использовать heartbeat, логирование которого очень мешает
    public static boolean USE_CHAT_SERVER = false; //Использовать server для чата

    public static String LAST_VERSION_IN_DB; //Последняя доступная версия в базе данных
    public static Stage WF_MAIN_STAGE;
    public static File WF_TEMPDIR; //Директория временного хранения
    public static String HOME_DIRECTORY = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local" + File.separator + "Tubus";

    /**
     * Метод обеспечивает закрытие любого окна
     * В случае, если это окно главного приложения, то осуществляется выход из приложения
     */
    public static void closeWindow(Event event) {
        Window window = ((Node) event.getSource()).getScene().getWindow();
        if (window.equals(WinformStatic.WF_MAIN_STAGE)) {
            exitApplication(event);
        } else
            window.hide();
    }

    /**
     * Метод обеспечивает выход из приложения
     */
    public static void exitApplication(Event event){
        if(Warning2.create("ВНИМАНИЕ!", "Вы уверены, что хотите выйти", "из программы?")) {
            clearTempDir();
            System.exit(0);
        }
    }

    /**
     * Метод удаляет все файлы из папки, где кэшируются данные
     */
    public static void clearTempDir() {
        if (WF_TEMPDIR.exists()) {
            for (File file : WF_TEMPDIR.listFiles()) {
                if (file.isFile())
                    file.delete();
            }
            log.debug("AppStatic : folder with cash has been cleared");
        }
    }

    public static void centerWindow(Stage window, Boolean fullScreen, int mainMonitor){

        List<Screen> screenList = Screen.getScreens();
        //Если всего один монитор, то открываем на нем
        int monitor = Math.min(mainMonitor, screenList.size() - 1);

        if(fullScreen) {
            window.setWidth(screenList.get(monitor).getVisualBounds().getWidth());
            window.setHeight(screenList.get(monitor).getVisualBounds().getHeight());
            window.setX(screenList.get(monitor).getVisualBounds().getMinX());
            window.setY(screenList.get(monitor).getVisualBounds().getMinY());
        } else {
            double screenMinX = screenList.get(monitor).getVisualBounds().getMinX();
            double screenMinY = screenList.get(monitor).getVisualBounds().getMinY();
            double screenWidth = screenList.get(monitor).getVisualBounds().getWidth();
            double screenHeight = screenList.get(monitor).getVisualBounds().getHeight();

            window.setX(screenMinX + ((screenWidth - window.getWidth()) / 2));
            window.setY(screenMinY + ((screenHeight - window.getHeight()) / 2));
        }

    }

    /**
     * Метод парсит LocalDateTime к читаемому виду
     */
    public static String parseLDTtoNormalDate(String localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        LocalDateTime ldt = LocalDateTime.parse(localDateTime);
        return ldt.format(formatter);
    }



}
