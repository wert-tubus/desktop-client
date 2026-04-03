package ru.wert.tubus.chogori.application.cardsbox;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.List;
import java.util.function.Consumer;

import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

@Slf4j
public class PassportContextMenu {

    private final ListView<Passport> listView;
    private final Consumer<Passport> onEdit;
    private final Runnable onRefreshLists;
    private final Runnable onUpdateSelectedList;

    private ContextMenu contextMenu;
    private MenuItem miShow;
    private MenuItem miEdit;
    private MenuItem miDelete;

    public PassportContextMenu(ListView<Passport> listView,
                               Consumer<Passport> onEdit,
                               Runnable onRefreshLists,
                               Runnable onUpdateSelectedList) {
        this.listView = listView;
        this.onEdit = onEdit;
        this.onRefreshLists = onRefreshLists;
        this.onUpdateSelectedList = onUpdateSelectedList;

        createMenuItems();
        setupContextMenu();
        setupDoubleClickHandler();
    }

    /**
     * Получить контекстное меню
     */
    public ContextMenu getContextMenu() {
        return contextMenu;
    }

    private void createMenuItems() {
        contextMenu = new ContextMenu();

        miShow = new MenuItem("Показать");
        miShow.setOnAction(event -> {
            Passport selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showPassportDetails(selected);
            }
        });

        miEdit = new MenuItem("Изменить");
        miEdit.setOnAction(event -> {
            Passport selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onEdit.accept(selected);
            }
        });

        miDelete = new MenuItem("Удалить");
        miDelete.setOnAction(event -> {
            Passport selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deletePassport(selected);
            }
        });

        contextMenu.getItems().addAll(miShow, miEdit, miDelete);
    }

    private void setupContextMenu() {
        // Устанавливаем cellFactory с учетом контекстного меню
        listView.setCellFactory(lv -> new ListCell<Passport>() {
            @Override
            protected void updateItem(Passport item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText(item.toUsefulString());
                    // Устанавливаем контекстное меню только для непустых элементов
                    setContextMenu(contextMenu);
                }
            }
        });
    }

    private void setupDoubleClickHandler() {
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Passport selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showPassportDetails(selected);
                }
            }
        });
    }

    /**
     * Показать детали паспорта в диалоговом окне
     */
    private void showPassportDetails(Passport passport) {
        PassportDetailsDialog dialog = new PassportDetailsDialog(passport);
        dialog.showAndWait();
    }

    /**
     * Удаление паспорта
     */
    private void deletePassport(Passport passport) {
        // Проверяем, есть ли чертежи у паспорта
        List<Draft> drafts = ChogoriServices.CH_DRAFTS.findByPassport(passport);

        if (drafts != null && !drafts.isEmpty()) {
            Warning1.create($ATTENTION,
                    "Нельзя удалить паспорт, так как у него есть связанные чертежи (" + drafts.size() + " шт.)",
                    "Сначала удалите все чертежи данного паспорта");
            return;
        }

        // Подтверждение удаления
        javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение удаления");
        confirmAlert.setHeaderText("Удаление паспорта");
        confirmAlert.setContentText("Вы действительно хотите удалить паспорт?\n" +
                passport.toUsefulString() + "\n\nЭто действие необратимо!");

        java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                boolean deleted = ChogoriServices.CH_QUICK_PASSPORTS.delete(passport);
                if (deleted) {
                    log.info("Паспорт удален: {}", passport.toUsefulString());

                    // Удаляем из текущего списка
                    listView.getItems().remove(passport);

                    // Обновляем списки
                    if (onRefreshLists != null) {
                        onRefreshLists.run();
                    }
                    if (onUpdateSelectedList != null) {
                        onUpdateSelectedList.run();
                    }
                } else {
                    Warning1.create($ATTENTION,
                            "Не удалось удалить паспорт",
                            "Ошибка при удалении из базы данных");
                }
            } catch (Exception e) {
                log.error("Ошибка при удалении паспорта", e);
                Warning1.create($ATTENTION,
                        "Ошибка при удалении паспорта",
                        e.getMessage());
            }
        }
    }

    /**
     * Обновить состояние пунктов меню в зависимости от выбранного элемента
     */
    public void updateMenuState() {
        Passport selected = listView.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null;
        miShow.setDisable(!hasSelection);
        miEdit.setDisable(!hasSelection);
        miDelete.setDisable(!hasSelection);
    }
}