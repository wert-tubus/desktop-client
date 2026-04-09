package ru.wert.tubus.chogori.application.app_window;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.StartChogori;
import ru.wert.tubus.chogori.application.drafts.OpenDraftsEditorTask;
import ru.wert.tubus.chogori.application.excel.ExcelChooser;
import ru.wert.tubus.chogori.application.passports.OpenPassportsEditorTask;
import ru.wert.tubus.chogori.chat.dialog.dialogListCell.DialogListCell;
import ru.wert.tubus.chogori.chat.socketwork.socketservice.SocketService;
import ru.wert.tubus.chogori.components.BtnDoublePro;
import ru.wert.tubus.chogori.help.About;
import ru.wert.tubus.chogori.images.BtnImages;
import ru.wert.tubus.chogori.search.SearchField;
import ru.wert.tubus.chogori.search.SearchHistoryButton;
import ru.wert.tubus.chogori.search.SearchHistoryFile;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.models.User;
import ru.wert.tubus.client.entity.serviceQUICK.DraftQuickService;
import ru.wert.tubus.client.entity.serviceQUICK.PassportQuickService;
import ru.wert.tubus.client.entity.serviceREST.DraftService;
import ru.wert.tubus.client.entity.serviceREST.PassportService;
import ru.wert.tubus.winform.statics.WinformStatic;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;


import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ru.wert.tubus.chogori.images.BtnImages.BTN_CLOSE_WHITE_IMG;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;
import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER_GROUP;
import static ru.wert.tubus.chogori.statics.AppStatic.KOMPLEKT;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.*;
import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;

@Slf4j
public class AppMenuController {

    @FXML
    MenuBar menuBar;

    @FXML @Getter
    public HBox hbSearch;

    @FXML
    Button btnCloseTab;

    private User tempUser;

    @FXML
    void initialize(){
        log.debug("initialize : запускается блок инициализации ...");

        createMenu();

        StartChogori.APP_MENU_CONTROLLER = this;

        //Создать поле поиска
        SEARCH_CONTAINER = hbSearch;
        PANE_WITH_SEARCH = createSearchField();

        btnCloseTab.setGraphic(new ImageView(BTN_CLOSE_WHITE_IMG));
        btnCloseTab.visibleProperty().bind(CH_TAB_PANE.getEmpty());
        btnCloseTab.setOnAction(event->{
            CH_TAB_PANE.closeThisTab(event);
        });

        log.debug("initialize : блок инициализации успешно выполнен");
    }


    /**
     * СОЗДАТЬ МЕНЮ
     */
    private void createMenu() {
        log.debug("createMenu : запускается создание меню ...");

        menuBar.getMenus().add(createMainMenu());
        //Чертежи
        menuBar.getMenus().add(createDraftsMenu());
        //Изделия
        if(CH_CURRENT_USER_GROUP.isReadProductStructures())
            menuBar.getMenus().add(createEditorMenu());
        //Материалы
        if(CH_CURRENT_USER_GROUP.isReadMaterials())
            menuBar.getMenus().add(createMaterialsMenu());
        //Калькулятор
//        menuBar.getMenus().add(createCalculatorMenu());
        //Изделия
//        if(CH_CURRENT_USER_GROUP.isReadProductStructures())
//            menuBar.getMenus().add(createEditorMenu());
        //Админ
        if(CH_CURRENT_USER_GROUP.isAdministrate())
            menuBar.getMenus().add(createAdminMenu());
        //Помощь
        menuBar.getMenus().add(createHelpMenu());
        log.debug("createMenu : меню успешно создано");
    }

    /**
     * ПОЛЕ ПОИСКА в одной строке с главным меню
     */
    private HBox createSearchField() {

        log.debug("createSearchField : поле поиска создается ...");
        Button searchNowButton = new Button();
        searchNowButton.setText("");
        searchNowButton.setGraphic(new ImageView(BtnImages.BTN_SEARCH_IMG));
        searchNowButton.setOnAction(e->{
            Platform.runLater(()->{
                CH_SEARCH_FIELD.searchNow(true);
                CH_SEARCH_FIELD.requestFocusOnTableView();
            });
        });

        SearchHistoryButton btnHistory = new SearchHistoryButton();
        btnHistory.setStyle("-fx-min-height: 18pt");

        BtnDoublePro doublePro = new BtnDoublePro(true);
        Button btnPro = doublePro.create();
        btnPro.setStyle("-fx-min-height: 18pt");
        doublePro.getStateProperty().bindBidirectional(SearchField.searchProProperty);
        doublePro.getStateProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(()->{
                if(CH_SEARCH_FIELD.getEnteredText().startsWith(KOMPLEKT)) return;
                CH_SEARCH_FIELD.searchNow(true);
                CH_SEARCH_FIELD.requestFocusOnTableView();
            });
        });

        HBox hbox = new HBox();

        CH_SEARCH_FIELD = new SearchField();
        CH_SEARCH_FIELD.setPrefWidth(300);
        CH_SEARCH_FIELD.setStyle("-fx-min-height: 18pt");
        Button btnClean = new Button();
        btnClean.setOnAction((e)->{
            CH_SEARCH_FIELD.setText("");
            CH_SEARCH_FIELD.requestFocus();
        });
        btnClean.setGraphic(new ImageView(BtnImages.BTN_CLEAN_IMG_W));
        hbox.getChildren().addAll(searchNowButton, CH_SEARCH_FIELD, btnHistory, btnPro, btnClean);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setSpacing(2);
        hbox.getStylesheets().add(getClass().getResource("/chogori-css/toolpane-dark.css").toString());

        log.debug("createSearchField : поле поиска успешно создано");

        return hbox;

    }

    //######################   ОБЩЕЕ  #############################

    /**
     * МЕНЮ ОБЩЕЕ
     */
    private Menu createMainMenu() {

        Menu mainMenu = new Menu("Общее");

        MenuItem settings = new MenuItem("Настройки");
        settings.setOnAction(this::openSettings);

        MenuItem changePassword = new MenuItem("Сменить пароль");
        changePassword.setOnAction(this::changePassword);

        MenuItem changeUserItem = new MenuItem("Сменить пользователя");
        changeUserItem.setOnAction(this::changeUser);

//        Заготовка под вкладку чат во весь экран
//        MenuItem chatItem = new MenuItem("Чат");
//        chatItem.setOnAction(this::openChat);

        MenuItem updateData = new MenuItem("Обновить данные");
        updateData.setOnAction(e -> {
            TaskUpdateData task = new TaskUpdateData();
            task.setOnSucceeded(event -> {
                // Дополнительные действия после успешного обновления
                log.info("Данные и кэш успешно обновлены");
            });
            new Thread(task).start();
        });

        MenuItem cleanSearchHistory = new MenuItem("Очистить историю поиска");
        cleanSearchHistory.setOnAction(e-> SearchHistoryFile.getInstance().clear());

        MenuItem exitItem = new MenuItem("Выйти");
        exitItem.setOnAction(this::exit);


        mainMenu.getItems().add(changeUserItem);
        mainMenu.getItems().add(changePassword);
        mainMenu.getItems().add(new SeparatorMenuItem());
        mainMenu.getItems().add(settings);
        mainMenu.getItems().add(new SeparatorMenuItem());
//        if(!CH_CURRENT_USER.getName().equals("Гость"))
//            mainMenu.getItems().add(chatItem);
        mainMenu.getItems().add(updateData);
        mainMenu.getItems().add(cleanSearchHistory);
        mainMenu.getItems().add(new SeparatorMenuItem());
        mainMenu.getItems().add(exitItem);

        return mainMenu;
    }

    /**
     * ОТКРЫТЬ ЧАТ
     */
    private void openChat(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/chat/chat.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Чат";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true, loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateData(Runnable onComplete) {
        Task<Void> updateTask = new TaskUpdateData();
        updateTask.setOnSucceeded(e -> onComplete.run());
        updateTask.setOnFailed(e -> onComplete.run());
        updateTask.setOnCancelled(e -> onComplete.run());

        new Thread(updateTask).start();
    }

    /**
     * СМЕНИТЬ ПАРОЛЬ
     */
    private void changePassword(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/password/changePassword.fxml"));
            Parent parent = loader.load();

            new WindowDecoration("Смена пароля", parent, false, WF_MAIN_STAGE, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * -- ЗАВЕРШЕНИЕ ПРОГРАММЫ
     */
    private void exit(Event e) {
        SearchHistoryFile.getInstance().save(); //Сохраняем историю поиска
        DialogListCell.shutdown();
        WinformStatic.exitApplication(e);

    }

    /**
     * НАСТРОЙКИ
     */
    private void openSettings(Event e){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/settings/settings.fxml"));
            Parent parent = loader.load();

            new WindowDecoration("Настройки", parent, false, WF_MAIN_STAGE, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * -- СМЕНИТЬ ПОЛЬЗОВАТЕЛЯ
     */
    private void changeUser(Event event) {
        //Сохраняем пользователя на случай, если он передумает
        tempUser = CH_CURRENT_USER;


        //Загружаем loginWindow
        try {
            FXMLLoader loginWindowLoader = new FXMLLoader(getClass().getResource("/chogori-fxml/login/login.fxml"));
            Parent loginWindow = loginWindowLoader.load();
            CH_TAB_PANE.getTabs().clear();
            SEARCH_CONTAINER.getChildren().clear();
            menuBar.getMenus().clear();
            CH_DECORATION_ROOT_PANEL.getChildren().add(loginWindow);
            //При нажатии ESCAPE возвращаем прежнего пользователя
            CH_DECORATION_ROOT_PANEL.setOnKeyPressed((event1 -> {
                if(event1.getCode().equals(KeyCode.ESCAPE)){
                    CH_DECORATION_ROOT_PANEL.getChildren().removeAll(loginWindow);
                    CH_CURRENT_USER = tempUser;
                    CH_CURRENT_USER_GROUP = CH_CURRENT_USER.getUserGroup();
                    createMenu();
                }
            }));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   //########################   ЧЕРТЕЖИ   ############################

    /**
     * МЕНЮ ЧЕРТЕЖЕЙ
     */
    private Menu createDraftsMenu() {

        Menu draftsMenu = new Menu("Чертежи");

        MenuItem draftsCabinetItem = new MenuItem("Картотека");
        draftsCabinetItem.setOnAction(this::openFileCabinet);

        MenuItem draftsItem = new MenuItem("Чертежи");
        draftsItem.setOnAction(this::openDrafts);

        MenuItem changeHistoryItem = new MenuItem("История изменений");
        changeHistoryItem.setOnAction(this::openChangeHistory);

        MenuItem registrarItem = new MenuItem("Журнал регистрации");
        registrarItem.setOnAction(this::openRegistrationBook);

//        draftsMenu.getItems().add(draftsCabinetItem);
        draftsMenu.getItems().add(draftsItem);
        draftsMenu.getItems().add(changeHistoryItem);
        draftsMenu.getItems().add(new SeparatorMenuItem());
        draftsMenu.getItems().add(registrarItem);

        return draftsMenu;
    }

    /**
     * -- ИСТОРИЯ ИЗМЕНЕНИЙ
     */
    private void openChangeHistory(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/logging/changeHistory.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "История изменений";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true,  loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * -- КАРТОТЕКА
     */
    private void openFileCabinet(ActionEvent event) {

        Thread t = new Thread(new OpenPassportsEditorTask());
        t.setDaemon(true);
        t.start();
    }

    /**
     * -- ЧЕРТЕЖИ
     */
    public void openDrafts(Event event) {

        Thread t = new Thread(new OpenDraftsEditorTask());
        t.setDaemon(true);
        t.start();
    }


    /**
     * -- ЖУРНАЛ РЕГИСТРАЦИИ
     */
    private void openRegistrationBook(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/cardsBox/cardsbox.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Журнал регистрации";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true,  loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //######################   МАТЕРИАЛЫ   ##########################

    /**
     * МЕНЮ МАТЕРИАЛОВ
     */
    private Menu createMaterialsMenu() {

        Menu materialsMenu = new Menu("Номенклатура");

        MenuItem catalogOfMaterialsItem = new MenuItem("Каталог материалов");
        catalogOfMaterialsItem.setOnAction(this::openCatalogOfMaterials);

        MenuItem densitiesItem = new MenuItem("Плотность материалов");
        densitiesItem.setOnAction(this::openDensities);

        MenuItem prefixesItem = new MenuItem("Префиксы");
        prefixesItem.setOnAction(this::openPrefixes);

        MenuItem decimalsItem = new MenuItem("Децимальный классификатор");
        decimalsItem.setOnAction(this::openDecimals);

        materialsMenu.getItems().addAll(catalogOfMaterialsItem, densitiesItem);
        materialsMenu.getItems().add(new SeparatorMenuItem());
        materialsMenu.getItems().add(prefixesItem);
        materialsMenu.getItems().add(decimalsItem);


        return materialsMenu;
    }

    /**
     * -- МАТЕРИАЛЫ
     */
    private void openCatalogOfMaterials(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/catalogOfMaterials/catalogOfMaterials.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Материалы";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * -- ПЛОТНОСТИ МАТЕРИАЛОВ
     */
    private void openDensities(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/densities/densities.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Плотность материалов";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true,  loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * -- ПРЕФИКСЫ
     */
    private void openPrefixes(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/prefixes/prefixes.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Префиксы";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true,  loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * -- ДЕЦИМАЛЬНЫЙ КЛАССИФИКАТОР
     */
    private void openDecimals(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/decimals/decimals.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Децимальный классификатор";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true,  loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //######################   РЕДАКТОР   ##########################

    /**
     * МЕНЮ ИЗДЕЛИЙ
     */
    private Menu createEditorMenu() {

        Menu editorMenu = new Menu("Изделия");

//        MenuItem catalogOfProductItem = new MenuItem("Каталог изделий");
//        catalogOfProductItem.setOnAction(this::openCatalogOfProducts);

        MenuItem openExcelItem = new MenuItem("Открыть файл Excel");
        openExcelItem.setOnAction(this::openExcelFile);

//        editorMenu.getItems().add(catalogOfProductItem);
        editorMenu.getItems().add(openExcelItem);

        return editorMenu;
    }

    /**
     * -- КАТАЛОГ ИЗДЕЛИЙ
     */
    void openCatalogOfProducts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/catalogOfProducts/catalogOfProducts.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Изделия";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * -- ОТКРЫТЬ ФАЙЛ EXCEL
     */
    public void openExcelFile(ActionEvent event) {
        File chosenFile = new ExcelChooser().choose();
        if(chosenFile == null) return;
        Task<Void> openExcelFile = new TaskOpenExcelFile(chosenFile);

        Thread t = new Thread(openExcelFile);
        t.setDaemon(true);
        t.start();
    }

    //########################   АДМИН    ###########################

    /**
     * МЕНЮ АДМИНИСТРАТОРА
     */
    private Menu createAdminMenu() {

        Menu adminMenu = new Menu("Админ");

        MenuItem usersItem = new MenuItem("Пользователи");
        usersItem.setOnAction(this::openUsers);

        MenuItem userGroupsItem = new MenuItem("Группы пользователей");
        userGroupsItem.setOnAction(this::openUserGroups);

        MenuItem logsItem = new MenuItem("Логи");
        logsItem.setOnAction(this::openLogs);

        MenuItem crashReportsItem = new MenuItem("Отчеты крашей");
        crashReportsItem.setOnAction(this::openCrashReports);

        MenuItem catalogOfFolders = new MenuItem("Каталог папок");
        catalogOfFolders.setOnAction(this::openCatalogOfFolders);

        MenuItem test = new MenuItem("Заполнить паспорта");
        test.setOnAction(this::makeTest);

        adminMenu.getItems().add(usersItem);
        adminMenu.getItems().add(userGroupsItem);
        adminMenu.getItems().add(logsItem);
        adminMenu.getItems().add(crashReportsItem);
        adminMenu.getItems().add(new SeparatorMenuItem());
        adminMenu.getItems().add(catalogOfFolders);
        adminMenu.getItems().add(test);

        return adminMenu;
    }

    /**
     * -- ЛОГИ
     */
    private void openLogs(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/logging/logging.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Логирование";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true, loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * -- ОТЧЕТЫ КРАШЕЙ
     */
    private void openCrashReports(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/crashReports/crashReports.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Краши";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true, loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * -- ПОЛЬЗОВАТЕЛИ
     */
    private void openUsers(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/users/usersPermissions.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Пользователи";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true, loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * -- ГРУППЫ ПОЛЬЗОВАТЕЛЕЙ
     */
    private void openUserGroups(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/users/userGroupsPermissions.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Группы Пользователей";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName,parent, true, loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * -- КАТАЛОГ ПАПОК
     */
    void openCatalogOfFolders(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/catalogOfFolders/catalogOfFolders.fxml"));
            Parent parent = loader.load();
            parent.getStylesheets().add(this.getClass().getResource("/chogori-css/details-dark.css").toString());
            String tabName = "Пакеты";
            String tabId = tabName;
            CH_TAB_PANE.createNewTab(tabId, tabName, parent, true, loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean testVal = false;

    void makeTest(ActionEvent event) {
        // Получаем сервисы для прямого доступа к БД
        PassportService passportService = PassportService.getInstance();
        DraftService draftService = DraftService.getInstance();

        // Получаем все паспорта напрямую из БД
        List<Passport> allPassports = passportService.findAll();
        if (allPassports == null || allPassports.isEmpty()) {
            showAlert("Нет данных", "В базе данных нет паспортов для обработки");
            return;
        }

        int totalPassports = allPassports.size();

        // Создаем диалоговое окно с прогресс-баром
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Обновление паспортов");
        dialog.setHeaderText("Перенос данных из чертежей в паспорта");
        dialog.setResizable(true);
        dialog.setWidth(650);

        // Создаем содержимое диалога
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(600);

        Label progressLabel = new Label("0 / " + totalPassports);
        Label currentPassportLabel = new Label("Обработка паспорта...");
        currentPassportLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
        currentPassportLabel.setWrapText(true);

        // TextArea для отображения ошибок
        TextArea errorArea = new TextArea();
        errorArea.setEditable(false);
        errorArea.setPrefHeight(200);
        errorArea.setStyle("-fx-font-size: 11px; -fx-font-family: monospace; -fx-text-fill: #cc0000;");
        errorArea.setPromptText("Ошибки будут отображаться здесь...");

        TitledPane errorPane = new TitledPane("Лог ошибок", errorArea);
        errorPane.setExpanded(false);
        errorPane.setAnimated(true);

        // TextArea для лога успешных операций
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-font-size: 11px; -fx-font-family: monospace; -fx-text-fill: #008000;");
        logArea.setPromptText("Лог операций...");

        TitledPane logPane = new TitledPane("Лог операций", logArea);
        logPane.setExpanded(false);
        logPane.setAnimated(true);

        content.getChildren().addAll(progressBar, progressLabel, currentPassportLabel, errorPane, logPane);
        dialog.getDialogPane().setContent(content);

        // Добавляем кнопку отмены
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelButton);

        // Счетчики
        List<String> errors = new ArrayList<>();
        List<String> logs = new ArrayList<>();

        // Запускаем обработку в отдельном потоке
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int processed = 0;
                int errorCount = 0;
                int updatedCount = 0;

                for (Passport passport : allPassports) {
                    if (isCancelled()) {
                        updateMessage("Операция отменена");
                        break;
                    }

                    try {
                        String passportInfo = passport.toUsefulString();
                        updateMessage(passportInfo);
                        log.info("Обработка паспорта: {}", passportInfo);

                        // Прямой запрос к БД для получения чертежей по паспорту
                        List<Draft> drafts = draftService.findByPassport(passport);

                        if (drafts != null && !drafts.isEmpty()) {
                            // Находим последний добавленный чертеж (по creationTime)
                            Draft lastDraft = drafts.stream()
                                    .max((d1, d2) -> {
                                        if (d1.getCreationTime() == null && d2.getCreationTime() == null) return 0;
                                        if (d1.getCreationTime() == null) return -1;
                                        if (d2.getCreationTime() == null) return 1;
                                        return d1.getCreationTime().compareTo(d2.getCreationTime());
                                    })
                                    .orElse(null);

                            if (lastDraft != null) {
                                boolean needUpdate = false;
                                StringBuilder changes = new StringBuilder();

                                // Проверяем и сохраняем изменения
                                String oldUserName = passport.getUserName();
                                String oldNote = passport.getNote();
                                String oldDate = passport.getDate();

                                // 1. statusUser.getName() -> passport.userName
                                if (lastDraft.getStatusUser() != null && lastDraft.getStatusUser().getName() != null) {
                                    String newUserName = lastDraft.getStatusUser().getName();
                                    if (!newUserName.equals(oldUserName)) {
                                        passport.setUserName(newUserName);
                                        needUpdate = true;
                                        changes.append("userName: '").append(oldUserName).append("' -> '").append(newUserName).append("'; ");
                                        log.debug("Установлен user: {}", newUserName);
                                    }
                                }

                                // 2. folder.getName() -> passport.note
                                if (lastDraft.getFolder() != null && lastDraft.getFolder().getName() != null) {
                                    String newNote = lastDraft.getFolder().getName();
                                    if (!newNote.equals(oldNote)) {
                                        passport.setNote(newNote);
                                        needUpdate = true;
                                        changes.append("note: '").append(oldNote).append("' -> '").append(newNote).append("'; ");
                                        log.debug("Установлена папка: {}", newNote);
                                    }
                                }

                                // 3. creationTime -> passport.date (формат dd.MM.yy)
                                if (lastDraft.getCreationTime() != null) {
                                    try {
                                        String newDate = formatDate(lastDraft.getCreationTime());
                                        if (!newDate.equals(oldDate)) {
                                            passport.setDate(newDate);
                                            needUpdate = true;
                                            changes.append("date: '").append(oldDate).append("' -> '").append(newDate).append("'; ");
                                            log.debug("Установлена дата: {}", newDate);
                                        }
                                    } catch (Exception e) {
                                        String errorMsg = String.format("Ошибка форматирования даты для паспорта %s: %s, raw date: %s",
                                                passportInfo, e.getMessage(), lastDraft.getCreationTime());
                                        log.error(errorMsg, e);
                                        errors.add(errorMsg);
                                        errorCount++;
                                    }
                                }

                                // Сохраняем только если были изменения
                                if (needUpdate) {
                                    try {
                                        boolean success = passportService.update(passport);
                                        if (success) {
                                            updatedCount++;
                                            String logMsg = String.format("✓ Обновлен паспорт %s: %s", passportInfo, changes.toString());
                                            logs.add(logMsg);
                                            log.info(logMsg);

                                            // Обновляем лог в UI
                                            final List<String> currentLogs = new ArrayList<>(logs);
                                            Platform.runLater(() -> {
                                                logArea.setText(String.join("\n", currentLogs));
                                                if (!currentLogs.isEmpty()) {
                                                    logPane.setExpanded(true);
                                                }
                                            });
                                        } else {
                                            String errorMsg = String.format("✗ Не удалось обновить паспорт %s", passportInfo);
                                            errors.add(errorMsg);
                                            errorCount++;
                                            log.error(errorMsg);
                                        }
                                    } catch (Exception e) {
                                        String errorMsg = String.format("✗ Ошибка сохранения паспорта %s: %s",
                                                passportInfo, e.getMessage());
                                        log.error(errorMsg, e);
                                        errors.add(errorMsg);
                                        errorCount++;
                                    }
                                } else {
                                    log.debug("Нет изменений для паспорта: {}", passportInfo);
                                }
                            } else {
                                log.debug("Не найден последний чертеж для паспорта: {}", passportInfo);
                            }
                        } else {
                            log.debug("Нет чертежей для паспорта: {}", passportInfo);
                        }

                    } catch (Exception e) {
                        String errorMsg = String.format("✗ Критическая ошибка при обработке паспорта %s: %s",
                                passport.toUsefulString(), e.getMessage());
                        log.error(errorMsg, e);
                        errors.add(errorMsg);
                        errorCount++;
                    }

                    processed++;
                    updateProgress(processed, totalPassports);
                    updateTitle(String.format("Обработано: %d / %d (обновлено: %d, ошибок: %d)",
                            processed, totalPassports, updatedCount, errorCount));

                    // Периодически обновляем UI с ошибками
                    final List<String> currentErrors = new ArrayList<>(errors);
                    Platform.runLater(() -> {
                        if (!currentErrors.isEmpty()) {
                            errorArea.setText(String.join("\n", currentErrors));
                            errorPane.setExpanded(true);
                        }
                    });
                }

                // Финальное сообщение
                String finalMessage = String.format("Обработка завершена. Обновлено: %d, Ошибок: %d",
                        updatedCount, errorCount);
                updateMessage(finalMessage);
                log.info(finalMessage);

                return null;
            }
        };

        // Привязываем прогресс-бар к задаче
        progressBar.progressProperty().bind(task.progressProperty());

        // Обновляем текстовые метки
        task.messageProperty().addListener((obs, old, newMsg) -> {
            if (newMsg != null) {
                Platform.runLater(() -> currentPassportLabel.setText("Текущий паспорт: " + newMsg));
            }
        });

        task.titleProperty().addListener((obs, old, newTitle) -> {
            if (newTitle != null) {
                Platform.runLater(() -> progressLabel.setText(newTitle));
            }
        });

        // Обработка завершения задачи
        task.setOnSucceeded(e -> {
            dialog.close();
            String resultMessage = task.getTitle() != null ? task.getTitle() : "Операция завершена";
            if (!errors.isEmpty()) {
                showAlert("Завершено с ошибками", resultMessage + "\n\nДетали ошибок:\n" +
                        String.join("\n", errors.subList(0, Math.min(15, errors.size()))));
            } else {
                showAlert("Завершено успешно", resultMessage);
            }
        });

        task.setOnFailed(e -> {
            dialog.close();
            Throwable exception = task.getException();
            String errorDetails = exception != null ? exception.getMessage() : "Неизвестная ошибка";
            log.error("Ошибка выполнения задачи", exception);
            showAlert("Ошибка", "Произошла ошибка при обработке:\n" + errorDetails);
        });

        task.setOnCancelled(e -> {
            dialog.close();
            showAlert("Отменено", "Операция была отменена пользователем");
        });

        // Запускаем задачу в отдельном потоке
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

        // Показываем диалог и ждем завершения
        dialog.showAndWait();
    }

    /**
     * Форматирует дату из строки в формат dd.MM.yy
     * Поддерживает форматы: "2021-10-12 11:45:21.392+03" и "2022-03-21T08:36:21.492"
     */
    private String formatDate(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return "";
        }

        try {
            // Убираем миллисекунды и часовой пояс
            String cleanedDate = dateTimeString.split("\\.")[0];
            // Заменяем 'T' на пробел если есть
            cleanedDate = cleanedDate.replace('T', ' ');

            // Парсим дату и время
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(cleanedDate, inputFormatter);

            // Форматируем в dd.MM.yy
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            log.error("Ошибка форматирования даты: {}", dateTimeString, e);
            return "";
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(Math.min(400, textArea.getScrollTop() + 200));
        textArea.setStyle("-fx-font-size: 12px; -fx-font-family: monospace;");

        alert.getDialogPane().setContent(textArea);
        alert.setResizable(true);
        alert.showAndWait();
    }


    //########################   ПОМОЩЬ    ###########################

    /**
     * МЕНЮ ПОМОЩЬ
     */
    private Menu createHelpMenu() {

        Menu helpMenu = new Menu("Помощь");

        MenuItem downloadLastVersion = new MenuItem("Скачать последнюю версию");
        downloadLastVersion.setOnAction(this::downloadLastVersion);

        MenuItem openInstruction = new MenuItem("Открыть инструкцию");
        openInstruction.setOnAction(this::openInstruction);

        MenuItem helpVideosOnline = new MenuItem("Обучающее видео");
        helpVideosOnline.setOnAction(this::openHelpVideosOnline);

        MenuItem helpVersionInfo = new MenuItem("Что нового?");
        helpVersionInfo.setOnAction(this::openVersionInfo);

        MenuItem aboutItem = new MenuItem("О программе...");
        aboutItem.setOnAction(this::openAbout);

        MenuItem test = new MenuItem("Заполнить ПАСПОРТА");
        test.setOnAction(this::makeTest);

        helpMenu.getItems().addAll(
                downloadLastVersion,
                openInstruction,
                helpVideosOnline,
                helpVersionInfo);
        helpMenu.getItems().add(new SeparatorMenuItem());
        helpMenu.getItems().addAll(
                aboutItem
        );
//        helpMenu.getItems().add(test);

        return helpMenu;
    }

    /**
     * Метод открывает инструкцию по работе с тубусом
     * @param event
     */
    @SneakyThrows
    private void openInstruction(ActionEvent event) {
        File instruction = new File("//serverhp.ntcpik.com/ntcpik/BazaPIK/TUBUS - инструкция.pdf");
        Desktop.getDesktop().open(instruction);
    }

    /**
     * Метод сохраняет файл новой версии программы в выбранную директори
     * В качествое исходной директории предлагается использовать Рабочиц стол
     */
    private void downloadLastVersion(ActionEvent actionEvent){

        new TaskDownloadNewVersion();
    }

    /**
     * -- О ПРОГРАММЕ...
     */
    private void openAbout(ActionEvent event) {
        new About().create();

    }

    /**
     * ОБУЧАЮЩЕЕ ВИДЕО ОНЛАЙН
     */
    private void openHelpVideosOnline(ActionEvent event) {
        try {
            URI uri = new URI("https://www.youtube.com/playlist?list=PLlXRdu_fwDGUorUERVsuC1JTjQXAeNp0E");
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
                desktop.browse(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ЧТО НОВОГО
     */
    private void openVersionInfo(Event e){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chogori-fxml/help/versionInfo.fxml"));
            Parent parent = loader.load();

            new WindowDecoration("Что нового?", parent, false, WF_MAIN_STAGE, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



}
