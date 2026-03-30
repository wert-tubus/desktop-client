package ru.wert.tubus.chogori.entities.decimals;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import ru.wert.tubus.client.entity.models.Decimal;

/**
 * Класс для создания колонок таблицы десятичных классификаторов
 * Содержит статические методы для настройки каждой колонки
 */
public class Decimal_Columns {

    /**
     * Создает колонку "ID"
     * @return TableColumn<Decimal, String>
     */
    public static TableColumn<Decimal, String> createTcId() {
        TableColumn<Decimal, String> tcId = new TableColumn<>("ID");
        tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcId.setStyle("-fx-alignment: CENTER;");
        return tcId;
    }

    /**
     * Создает колонку "Наименование" для отображения имени десятичного классификатора
     * @return TableColumn<Decimal, String>
     */
    public static TableColumn<Decimal, String> createTcName() {
        TableColumn<Decimal, String> tcName = new TableColumn<>("Наименование");
        tcName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tcName.setStyle("-fx-alignment: CENTER;");
        tcName.setPrefWidth(150);
        tcName.setMaxWidth(150);
        tcName.setMinWidth(150);
        return tcName;
    }

    /**
     * Создает колонку "Описание" для отображения описания классификатора
     * Текст переносится по строкам
     * @return TableColumn<Decimal, String>
     */
    public static TableColumn<Decimal, String> createTcDescription() {
        TableColumn<Decimal, String> tcDescription = new TableColumn<>("Описание");
        tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        tcDescription.setPrefWidth(400);

        // Настройка переноса текста
        tcDescription.setCellFactory(tc -> {
            TableCell<Decimal, String> cell = new TableCell<Decimal, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                    }
                }
            };
            cell.setStyle("-fx-padding: 5px; -fx-alignment: TOP-LEFT;");
            return cell;
        });

        return tcDescription;
    }

    /**
     * Создает колонку "Начальный номер" для отображения начального значения диапазона
     * Заголовок в две строки, ширина 80px, центрирование по центру
     * @return TableColumn<Decimal, Integer>
     */
    public static TableColumn<Decimal, Integer> createTcInitialNumber() {
        TableColumn<Decimal, Integer> tcInitialNumber = new TableColumn<>("Начальный\nномер");
        tcInitialNumber.setCellValueFactory(new PropertyValueFactory<>("initialNumber"));
        tcInitialNumber.setStyle("-fx-alignment: CENTER;");
        tcInitialNumber.setPrefWidth(120);
        tcInitialNumber.setMaxWidth(120);
        tcInitialNumber.setMinWidth(120);

        // Форматирование отображения чисел
        tcInitialNumber.setCellFactory(tc -> new TableCell<Decimal, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%03d", item));
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        return tcInitialNumber;
    }

    /**
     * Создает колонку "Конечный номер" для отображения конечного значения диапазона
     * Заголовок в две строки, ширина 80px, центрирование по центру
     * @return TableColumn<Decimal, Integer>
     */
    public static TableColumn<Decimal, Integer> createTcLastNumber() {
        TableColumn<Decimal, Integer> tcLastNumber = new TableColumn<>("Последний\nномер");
        tcLastNumber.setCellValueFactory(new PropertyValueFactory<>("lastNumber"));
        tcLastNumber.setStyle("-fx-alignment: CENTER;");
        tcLastNumber.setPrefWidth(120);
        tcLastNumber.setMaxWidth(120);
        tcLastNumber.setMinWidth(120);

        // Форматирование отображения чисел
        tcLastNumber.setCellFactory(tc -> new TableCell<Decimal, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%03d", item));
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        return tcLastNumber;
    }

}
