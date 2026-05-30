// Passport_ColumnsManager.java

package ru.wert.tubus.chogori.entities.passports;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.wert.tubus.chogori.AppPropsSettings;
import ru.wert.tubus.client.entity.models.Passport;

/**
 * Класс для управления состоянием колонок таблиц паспортов
 */
public class Passport_ColumnsManager {

    /**
     * Восстанавливает видимость колонок таблицы паспортов PIK
     * @param tableView таблица паспортов PIK
     */
    public static void restorePIKColumnState(TableView<Passport> tableView) {
        String visibleColumns = AppPropsSettings.getInstance().getPassportsPIKVisibleColumns();
        restoreColumnState(tableView, visibleColumns);

        // Синхронизируем поля showXxx в Passport_TableView
        if (tableView instanceof Passport_TableView) {
            ((Passport_TableView) tableView).syncShowFieldsFromColumns();
        }
    }

    /**
     * Сохраняет текущее состояние видимости колонок таблицы паспортов PIK
     * @param tableView таблица паспортов PIK
     */
    public static void savePIKColumnState(TableView<Passport> tableView) {
        String state = saveCurrentColumnState(tableView);
        AppPropsSettings.getInstance().setPassportsPIKVisibleColumns(state);
        AppPropsSettings.getInstance().saveParams();
    }

    /**
     * Восстанавливает видимость колонок таблицы паспортов SKETCH
     * @param tableView таблица паспортов SKETCH
     */
    public static void restoreSketchColumnState(TableView<Passport> tableView) {
        String visibleColumns = AppPropsSettings.getInstance().getPassportsSketchVisibleColumns();
        restoreColumnState(tableView, visibleColumns);

        // Синхронизируем поля showXxx в Passport_TableView
        if (tableView instanceof Passport_TableView) {
            ((Passport_TableView) tableView).syncShowFieldsFromColumns();
        }
    }

    /**
     * Сохраняет текущее состояние видимости колонок таблицы паспортов SKETCH
     * @param tableView таблица паспортов SKETCH
     */
    public static void saveSketchColumnState(TableView<Passport> tableView) {
        String state = saveCurrentColumnState(tableView);
        AppPropsSettings.getInstance().setPassportsSketchVisibleColumns(state);
        AppPropsSettings.getInstance().saveParams();
    }

    /**
     * Универсальный метод восстановления состояния колонок
     */
    private static void restoreColumnState(TableView<Passport> tableView, String visibleColumns) {
        if (visibleColumns == null || visibleColumns.isEmpty()) {
            visibleColumns = "11111"; // 5 колонок по умолчанию
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
     * Универсальный метод сохранения состояния колонок
     */
    private static String saveCurrentColumnState(TableView<Passport> tableView) {
        StringBuilder sb = new StringBuilder();
        for (TableColumn<Passport, ?> column : tableView.getColumns()) {
            sb.append(column.isVisible() ? "1" : "0");
        }
        return sb.toString();
    }

    /**
     * Настраивает слушатели для автосохранения состояния колонок таблицы PIK
     * @param tableView таблица паспортов PIK
     */
    public static void setupPIKColumnStateListener(TableView<Passport> tableView) {
        tableView.getColumns().forEach(column -> {
            column.visibleProperty().addListener((obs, oldVal, newVal) -> {
                savePIKColumnState(tableView);
                // Синхронизируем поля showXxx при любом изменении
                if (tableView instanceof Passport_TableView) {
                    ((Passport_TableView) tableView).syncShowFieldsFromColumns();
                }
            });
        });
    }

    /**
     * Настраивает слушатели для автосохранения состояния колонок таблицы SKETCH
     * @param tableView таблица паспортов SKETCH
     */
    public static void setupSketchColumnStateListener(TableView<Passport> tableView) {
        tableView.getColumns().forEach(column -> {
            column.visibleProperty().addListener((obs, oldVal, newVal) -> {
                saveSketchColumnState(tableView);
                // При изменении видимости через UI (меню) обновляем поля showXxx
                if (tableView instanceof Passport_TableView) {
                    ((Passport_TableView) tableView).syncShowFieldsFromColumns();
                }
            });
        });
    }
}
