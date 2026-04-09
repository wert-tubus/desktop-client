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
    private final Runnable onOrderChanged; // Добавляем callback

    public BtnUp(Button btnUp, ListView<T> listView) {
        this(btnUp, listView, null);
    }

    public BtnUp(Button btnUp, ListView<T> listView, Runnable onOrderChanged) {
        this.listView = listView;
        this.onOrderChanged = onOrderChanged;

        btnUp.setId("patchButton");
        btnUp.setGraphic(new ImageView(BTN_UP_IMG));
        btnUp.setTooltip(new Tooltip("Поднять"));
        btnUp.setOnAction(event -> moveSelectedItemUp());
    }

    private void moveSelectedItemUp() {
        ObservableList<T> items = listView.getItems();
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();

        if (selectedIndex <= 0 || items.isEmpty()) {
            return;
        }

        T selectedItem = items.get(selectedIndex);
        T upperItem = items.get(selectedIndex - 1);

        items.set(selectedIndex - 1, selectedItem);
        items.set(selectedIndex, upperItem);

        listView.getSelectionModel().select(selectedIndex - 1);
        listView.scrollTo(selectedIndex - 1);

        // Вызываем callback если он установлен
        if (onOrderChanged != null) {
            onOrderChanged.run();
        }
    }
}
