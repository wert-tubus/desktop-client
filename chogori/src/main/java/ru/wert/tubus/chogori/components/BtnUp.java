package ru.wert.tubus.chogori.components;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import ru.wert.tubus.client.interfaces.Item;

import static ru.wert.tubus.chogori.images.BtnImages.*;

public class BtnUp<T extends Item> {

    private final ListView<T> listView;

    public BtnUp(Button btnUp, ListView<T> listView) {
        this.listView = listView;

        btnUp.setId("patchButton");
        btnUp.setGraphic(new ImageView(BTN_UP_IMG));
        btnUp.setTooltip(new Tooltip("Поднять"));
        btnUp.setOnAction(event -> moveSelectedItemUp());
        
    }

    /**
     * Перемещает выбранный элемент в списке на одну позицию вверх.
     */
    private void moveSelectedItemUp() {
        ObservableList<T> items = listView.getItems();
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();

        if (selectedIndex <= 0 || items.isEmpty()) {
            return; // Нельзя переместить выше первого элемента или нет выбранного элемента
        }

        T selectedItem = items.get(selectedIndex);
        T upperItem = items.get(selectedIndex - 1);

        // Меняем местами элементы
        items.set(selectedIndex - 1, selectedItem);
        items.set(selectedIndex, upperItem);

        // Сохраняем выделение на перемещённом элементе
        listView.getSelectionModel().select(selectedIndex - 1);

        // Прокручиваем к новому положению элемента
        listView.scrollTo(selectedIndex - 1);

    }
}
