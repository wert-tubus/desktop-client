package ru.wert.tubus.chogori.components;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import ru.wert.tubus.chogori.application.cardsbox.registrationBook.RegisteredPassportItem;

import static ru.wert.tubus.chogori.images.BtnImages.BTN_DOWN_IMG;

/**
 * Кнопка для перемещения выбранного элемента в TableView вниз.
 * Специально для RegisteredPassportItem.
 */
public class BtnDownForTable {

    private final TableView<RegisteredPassportItem> tableView;
    private final Runnable onOrderChanged;

    public BtnDownForTable(Button btnDown, TableView<RegisteredPassportItem> tableView) {
        this(btnDown, tableView, null);
    }

    public BtnDownForTable(Button btnDown, TableView<RegisteredPassportItem> tableView, Runnable onOrderChanged) {
        this.tableView = tableView;
        this.onOrderChanged = onOrderChanged;

        btnDown.setId("patchButton");
        btnDown.setGraphic(new ImageView(BTN_DOWN_IMG));
        btnDown.setTooltip(new Tooltip("Опустить"));
        btnDown.setOnAction(event -> moveSelectedItemDown());
    }

    /**
     * Перемещает выбранный элемент в таблице на одну позицию вниз.
     */
    private void moveSelectedItemDown() {
        ObservableList<RegisteredPassportItem> items = tableView.getItems();
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();

        if (selectedIndex < 0 || selectedIndex >= items.size() - 1 || items.isEmpty()) {
            return;
        }

        RegisteredPassportItem selectedItem = items.get(selectedIndex);
        RegisteredPassportItem lowerItem = items.get(selectedIndex + 1);

        // Меняем местами элементы
        items.set(selectedIndex, lowerItem);
        items.set(selectedIndex + 1, selectedItem);

        // Сохраняем выделение на перемещённом элементе
        tableView.getSelectionModel().select(selectedIndex + 1);

        // Прокручиваем к новому положению элемента
        tableView.scrollTo(selectedIndex + 1);

        // Вызываем callback если он установлен
        if (onOrderChanged != null) {
            onOrderChanged.run();
        }
    }
}
