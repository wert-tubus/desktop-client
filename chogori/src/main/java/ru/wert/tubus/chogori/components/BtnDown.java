package ru.wert.tubus.chogori.components;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import ru.wert.tubus.client.interfaces.Item;

import static ru.wert.tubus.chogori.images.BtnImages.BTN_DOWN_IMG;
import static ru.wert.tubus.chogori.images.BtnImages.BTN_ROLLDOWN_IMG;

public class BtnDown<T extends Item> {

    private final ListView<T> lvListOFNumbers;

    public BtnDown(Button btnDown, ListView<T> lvListOFNumbers) {
        this.lvListOFNumbers = lvListOFNumbers;

        btnDown.setId("patchButton");
        btnDown.setGraphic(new ImageView(BTN_DOWN_IMG));
        btnDown.setTooltip(new Tooltip("Опустить"));
        btnDown.setOnAction(event -> moveSelectedItemDown());

    }

    /**
     * Перемещает выбранный элемент в списке на одну позицию вниз.
     */
    private void moveSelectedItemDown() {
        ObservableList<T> items = lvListOFNumbers.getItems();
        int selectedIndex = lvListOFNumbers.getSelectionModel().getSelectedIndex();

        if (selectedIndex < 0 || selectedIndex >= items.size() - 1 || items.isEmpty()) {
            return; // Нельзя переместить ниже последнего элемента или нет выбранного элемента
        }

        T selectedItem = items.get(selectedIndex);
        T lowerItem = items.get(selectedIndex + 1);

        // Меняем местами элементы
        items.set(selectedIndex, lowerItem);
        items.set(selectedIndex + 1, selectedItem);

        // Сохраняем выделение на перемещённом элементе
        lvListOFNumbers.getSelectionModel().select(selectedIndex + 1);

        // Прокручиваем к новому положению элемента
        lvListOFNumbers.scrollTo(selectedIndex + 1);

    }
}
