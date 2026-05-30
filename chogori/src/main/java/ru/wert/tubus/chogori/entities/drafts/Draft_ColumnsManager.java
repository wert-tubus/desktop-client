// Draft_ColumnsManager.java
package ru.wert.tubus.chogori.entities.drafts;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.wert.tubus.chogori.AppPropsSettings;
import ru.wert.tubus.client.entity.models.Draft;

/**
 * Класс для управления состоянием колонок таблицы чертежей
 */
public class Draft_ColumnsManager {

    // Порядок колонок соответствует порядку в Draft_TableView.setTableColumns()
    private static final int TOTAL_COLUMNS = 9; // ID, Дец.номер, Наименование, Тип/стр, Статус, K, Имя файла, Создан, Примечание

    /**
     * Восстанавливает видимость колонок таблицы чертежей из сохраненных настроек
     * @param tableView таблица чертежей
     */
    public static void restoreColumnState(TableView<Draft> tableView) {
        String visibleColumns = AppPropsSettings.getInstance().getDraftsVisibleColumns();

        if (visibleColumns == null || visibleColumns.isEmpty()) {
            visibleColumns = "111111111"; // 9 колонок, все видимы
        }

        // Если строка короче, дополняем единицами
        while (visibleColumns.length() < TOTAL_COLUMNS) {
            visibleColumns += "1";
        }

        final String finalVisibleColumns = visibleColumns;

        Platform.runLater(() -> {
            int columnCount = tableView.getColumns().size();
            for (int i = 0; i < columnCount && i < finalVisibleColumns.length(); i++) {
                tableView.getColumns().get(i).setVisible(finalVisibleColumns.charAt(i) == '1');
            }
        });
    }

    /**
     * Сохраняет текущее состояние видимости колонок таблицы чертежей
     * @param tableView таблица чертежей
     */
    public static void saveCurrentColumnState(TableView<Draft> tableView) {
        StringBuilder sb = new StringBuilder();
        for (TableColumn<Draft, ?> column : tableView.getColumns()) {
            sb.append(column.isVisible() ? "1" : "0");
        }

        // Убеждаемся, что строка имеет нужную длину
        while (sb.length() < TOTAL_COLUMNS) {
            sb.append("0");
        }

        AppPropsSettings.getInstance().setDraftsVisibleColumns(sb.toString());
        AppPropsSettings.getInstance().saveParams();
    }

    /**
     * Настраивает слушатели для автосохранения состояния колонок
     * @param tableView таблица чертежей
     */
    public static void setupColumnStateListener(TableView<Draft> tableView) {
        tableView.getColumns().forEach(column -> {
            column.visibleProperty().addListener((obs, oldVal, newVal) ->
                    saveCurrentColumnState(tableView)
            );
        });
    }
}
