// Draft_ColumnsManager.java
package ru.wert.tubus.chogori.entities.drafts;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.AppPropsSettings;
import ru.wert.tubus.client.entity.models.Draft;

@Slf4j
public class Draft_ColumnsManager {

    private static final int TOTAL_COLUMNS = 9;

    public static void restoreColumnState(TableView<Draft> tableView) {
        String visibleColumns = AppPropsSettings.getInstance().getDraftsVisibleColumns();

        log.debug("restoreColumnState: загружена строка '{}'", visibleColumns);

        if (visibleColumns == null || visibleColumns.isEmpty()) {
            visibleColumns = "111111111";
            log.debug("restoreColumnState: используем значение по умолчанию '{}'", visibleColumns);
        }

        while (visibleColumns.length() < TOTAL_COLUMNS) {
            visibleColumns += "1";
        }

        final String finalVisibleColumns = visibleColumns;

        Platform.runLater(() -> {
            int columnCount = tableView.getColumns().size();
            log.debug("restoreColumnState: кол-во колонок = {}, применяем строку '{}'", columnCount, finalVisibleColumns);

            for (int i = 0; i < columnCount && i < finalVisibleColumns.length(); i++) {
                boolean visible = finalVisibleColumns.charAt(i) == '1';
                tableView.getColumns().get(i).setVisible(visible);
                log.debug("Колонка {} установлена visible={}", i, visible);
            }

            syncShowFieldsFromColumns(tableView);
        });
    }

    public static void saveCurrentColumnState(TableView<Draft> tableView) {
        StringBuilder sb = new StringBuilder();
        for (TableColumn<Draft, ?> column : tableView.getColumns()) {
            sb.append(column.isVisible() ? "1" : "0");
        }

        while (sb.length() < TOTAL_COLUMNS) {
            sb.append("0");
        }

        String state = sb.toString();
        log.debug("saveCurrentColumnState: сохраняем строку '{}'", state);

        AppPropsSettings.getInstance().setDraftsVisibleColumns(state);
        AppPropsSettings.getInstance().saveParams();

        // Синхронизируем поля после сохранения
        syncShowFieldsFromColumns(tableView);
    }

    public static void setupColumnStateListener(TableView<Draft> tableView) {
        tableView.getColumns().forEach(column -> {
            column.visibleProperty().addListener((obs, oldVal, newVal) -> {
                log.debug("Колонка изменила видимость: {} -> {}", oldVal, newVal);
                saveCurrentColumnState(tableView);
            });
        });
    }

    private static void syncShowFieldsFromColumns(TableView<Draft> tableView) {
        if (tableView instanceof Draft_TableView && tableView.getColumns().size() >= TOTAL_COLUMNS) {
            Draft_TableView draftTable = (Draft_TableView) tableView;
            draftTable.setShowId(tableView.getColumns().get(0).isVisible());
            draftTable.setShowDraftType(tableView.getColumns().get(3).isVisible());
            draftTable.setShowStatus(tableView.getColumns().get(4).isVisible());
            draftTable.setShowRemarks(tableView.getColumns().get(5).isVisible());
            draftTable.setShowInitialName(tableView.getColumns().get(6).isVisible());
            draftTable.setShowCreationTime(tableView.getColumns().get(7).isVisible());
            draftTable.setShowNote(tableView.getColumns().get(8).isVisible());

            log.debug("syncShowFieldsFromColumns: showId={}, showDraftType={}, showStatus={}, showRemarks={}, showInitialName={}, showCreationTime={}, showNote={}",
                    draftTable.isShowId(), draftTable.isShowDraftType(), draftTable.isShowStatus(),
                    draftTable.isShowRemarks(), draftTable.isShowInitialName(), draftTable.isShowCreationTime(), draftTable.isShowNote());
        }
    }
}
