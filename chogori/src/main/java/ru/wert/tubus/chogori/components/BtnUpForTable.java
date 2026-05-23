package ru.wert.tubus.chogori.components;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import ru.wert.tubus.chogori.application.cardsbox.registrationBook.RegisteredPassportItem;

import static ru.wert.tubus.chogori.images.BtnImages.BTN_UP_IMG;

/**
 * Кнопка для перемещения выбранного элемента в TableView вверх.
 * Специально для RegisteredPassportItem.
 */
public class BtnUpForTable {

    private final TableView<RegisteredPassportItem> tableView;
    private final Runnable onOrderChanged;

    public BtnUpForTable(Button btnUp, TableView<RegisteredPassportItem> tableView) {
        this(btnUp, tableView, null);
    }

    public BtnUpForTable(Button btnUp, TableView<RegisteredPassportItem> tableView, Runnable onOrderChanged) {
        this.tableView = tableView;
        this.onOrderChanged = onOrderChanged;

        btnUp.setId("patchButton");
        btnUp.setGraphic(new ImageView(BTN_UP_IMG));
        btnUp.setTooltip(new Tooltip("Поднять"));
        btnUp.setOnAction(event -> moveSelectedItemUp());
    }

    /**
     * Перемещает выбранный элемент в таблице на одну позицию вверх.
     */
    private void moveSelectedItemUp() {
        ObservableList<RegisteredPassportItem> items = tableView.getItems();
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();

        if (selectedIndex <= 0 || items.isEmpty()) {
            return;
        }

        RegisteredPassportItem selectedItem = items.get(selectedIndex);
        RegisteredPassportItem upperItem = items.get(selectedIndex - 1);

        items.set(selectedIndex - 1, selectedItem);
        items.set(selectedIndex, upperItem);

        tableView.getSelectionModel().select(selectedIndex - 1);
        tableView.scrollTo(selectedIndex - 1);

        // Вызываем callback если он установлен
        if (onOrderChanged != null) {
            onOrderChanged.run();
        }
    }
}
