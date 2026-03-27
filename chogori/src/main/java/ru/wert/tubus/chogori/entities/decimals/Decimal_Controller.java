package ru.wert.tubus.chogori.entities.decimals;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ru.wert.tubus.client.interfaces.SearchableTab;
import ru.wert.tubus.client.interfaces.UpdatableTabController;

import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;
import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_SEARCH_FIELD;

/**
 * Контроллер для управления вкладкой децимальных классификаторов (Decimal)
 * Обеспечивает отображение таблицы и панели инструментов
 */
public class Decimal_Controller implements SearchableTab, UpdatableTabController {

    @FXML
    private AnchorPane apMainPatch;

    @FXML
    private VBox vbHeader;

    @FXML
    private HBox controlButtons;

    private Decimal_TableView tableView;

    @FXML
    void initialize() {
        // Создаем панели инструментов
        createDecimals_ToolBar();

        // Создаем таблицу децимальных классификаторов
        createDecimals_TableView();
    }

    /**
     * Возвращает панель с кнопками управления
     * @return HBox с кнопками управления
     */
    public HBox getControlButtons() {
        return controlButtons;
    }

    /**
     * Создает таблицу децимальных классификаторов
     */
    private void createDecimals_TableView() {
        boolean useContextMenu = CH_CURRENT_USER.getUserGroup().isEditDrafts();
        tableView = new Decimal_TableView("ДЕЦИМАЛЬНЫЙ КЛАССИФИКАТОР", useContextMenu);
        tableView.updateView();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        vbHeader.getChildren().add(tableView);
    }

    /**
     * Создает инструментальную панель для децимальных классификаторов
     * В текущей версии кнопки управления отсутствуют
     */
    private void createDecimals_ToolBar() {
        // КНОПОК УПРАВЛЕНИЯ НЕТ
    }

    /**
     * Возвращает корневую панель AnchorPane
     * @return AnchorPane
     */
    public AnchorPane getApMainPatch() {
        return apMainPatch;
    }

    /**
     * Возвращает таблицу децимальных классификаторов
     * @return Decimal_TableView
     */
    public Decimal_TableView getDecimalTableView() {
        return tableView;
    }

    /**
     * Настраивает поиск по таблице
     */
    @Override
    public void tuneSearching() {
        Platform.runLater(() -> tableView.requestFocus());
        CH_SEARCH_FIELD.changeSearchedTableView(tableView, "ДЕЦИМАЛЬНЫЙ КЛАССИФИКАТОР");
    }

    /**
     * Обновляет содержимое вкладки
     */
    @Override
    public void updateTab() {
        tableView.updateTableView();
    }
}
