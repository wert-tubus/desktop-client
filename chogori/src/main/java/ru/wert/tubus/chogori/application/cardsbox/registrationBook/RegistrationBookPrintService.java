package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.geometry.Insets;
import javafx.print.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.winform.warnings.Warning1;
import ru.wert.tubus.winform.window_decoration.WindowDecoration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static ru.wert.tubus.winform.statics.WinformStatic.WF_MAIN_STAGE;
import static ru.wert.tubus.winform.warnings.WarningMessages.$ATTENTION;

/**
 * Сервис для печати списка чертежей с двумя столбцами
 */
@Slf4j
public class RegistrationBookPrintService {

    private static final String TITLE = "ЖУРНАЛ РЕГИСТРАЦИИ ЧЕРТЕЖЕЙ";
    private static final String SEPARATOR = "=============================================================";
    private static final Font CONTENT_FONT = Font.font("Consolas", 12);
    private static final Font PREVIEW_FONT = Font.font("Consolas", 11);

    /**
     * Печать списка чертежей
     *
     * @param printItems список элементов для печати (паспорт + статус чертежей)
     */
    public void printPassportsList(List<RegisteredPassportPrintItem> printItems) {
        if (printItems == null || printItems.isEmpty()) {
            Warning1.create($ATTENTION, "Список пуст", "Нечего печатать");
            return;
        }

        try {
            String content = buildPrintContent(printItems);
            showPrintPreview(content, printItems.size());
        } catch (Exception e) {
            log.error("Ошибка при подготовке к печати", e);
            Warning1.create("ОШИБКА!", "Не удалось подготовить данные для печати", e.getMessage());
        }
    }

    /**
     * Формирует содержимое для печати в виде таблицы с двумя столбцами
     *
     * @param printItems список элементов для печати
     * @return отформатированная строка для печати
     */
    private String buildPrintContent(List<RegisteredPassportPrintItem> printItems) {
        StringBuilder content = new StringBuilder();

        // Заголовок
        content.append(TITLE).append("\n");
        content.append(SEPARATOR).append("\n");
        content.append("Дата печати: ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                .format(new Date())).append("\n");
        content.append(SEPARATOR).append("\n\n");

        // Заголовки столбцов
        content.append(String.format("%-4s %-55s %-12s\n", "№", "Новый чертеж", "Чертежи"));
        content.append(formatTableSeparator());

        // Данные
        int counter = 1;
        for (RegisteredPassportPrintItem item : printItems) {
            content.append(formatTableRow(counter++, item));
        }

        content.append(formatTableSeparator());
        content.append("\n").append("Всего чертежей: ").append(printItems.size());

        return content.toString();
    }

    /**
     * Форматирует разделитель таблицы
     */
    private String formatTableSeparator() {
        return "----------------------------------------------------------------------\n";
    }

    /**
     * Форматирует строку таблицы с двумя столбцами
     *
     * @param number   порядковый номер
     * @param item     элемент для печати (паспорт + статус чертежей)
     * @return отформатированная строка
     */
    private String formatTableRow(int number, RegisteredPassportPrintItem item) {
        String displayString = item.getPassport().toUsefulString();
        String draftsSymbol = item.getDraftsSymbol();

        return String.format("%-4d %-55s %-12s\n", number, displayString, draftsSymbol);
    }

    /**
     * Показывает диалог предварительного просмотра
     *
     * @param content    содержимое для печати
     * @param totalCount общее количество чертежей
     */
    private void showPrintPreview(String content, int totalCount) {
        VBox mainPane = new VBox();

        // Текст для предварительного просмотра
        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setFont(PREVIEW_FONT);
        textArea.setStyle("-fx-control-inner-background: white;");

        // Кнопки управления
        Button btnPrintWithSettings = new Button("Печать с настройками");
        btnPrintWithSettings.setStyle("-fx-font-size: 12px; -fx-padding: 5px 15px;");
        btnPrintWithSettings.setOnAction(e -> {
            performPrint(content);
            mainPane.getScene().getWindow().hide();
        });

        Button btnCancel = new Button("Отмена");
        btnCancel.setStyle("-fx-font-size: 12px; -fx-padding: 5px 15px;");
        btnCancel.setOnAction(e -> {
            mainPane.getScene().getWindow().hide();
        });

        Button btnPrint = new Button("Печать");
        btnPrint.setStyle("-fx-font-size: 12px; -fx-padding: 5px 15px;");
        btnPrint.setOnAction(e -> {
            printDirect(content);
            mainPane.getScene().getWindow().hide();
        });

        // Панель кнопок
        javafx.scene.layout.HBox buttonBar = new javafx.scene.layout.HBox(10);
        buttonBar.setPadding(new Insets(10));
        buttonBar.setStyle("-fx-background-color: #f0f0f0;");
        buttonBar.getChildren().addAll(btnPrint, btnPrintWithSettings, btnCancel);

        // Основная панель
        mainPane.setPrefSize(650.0, 800.0);
        mainPane.setPadding(new Insets(10));
        VBox.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        mainPane.getChildren().addAll(textArea, buttonBar);

        new WindowDecoration("Печать", mainPane, false, WF_MAIN_STAGE);
    }

    /**
     * Выполняет печать с диалогом настроек (принудительно портретный режим)
     *
     * @param content содержимое для печати
     */
    private void performPrint(String content) {
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob != null) {
            VBox printNode = createPrintNode(content);

            // Принудительно устанавливаем портретный режим
            setupPortraitOrientation(printerJob);

            // Показываем диалог настроек печати
            boolean proceed = printerJob.showPrintDialog(WF_MAIN_STAGE);

            if (proceed) {
                executePrint(printerJob, printNode);
            } else {
                printerJob.cancelJob();
            }
        } else {
            Warning1.create("ОШИБКА!", "Принтер не найден",
                    "Настройте принтер в системе и попробуйте снова");
        }
    }

    /**
     * Выполняет прямую печать без диалога настроек (принудительно портретный режим)
     *
     * @param content содержимое для печати
     */
    private void printDirect(String content) {
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob != null) {
            VBox printNode = createPrintNode(content);

            // Принудительно устанавливаем портретный режим
            setupPortraitOrientation(printerJob);

            executePrint(printerJob, printNode);
        } else {
            Warning1.create("ОШИБКА!", "Принтер не найден",
                    "Настройте принтер в системе и попробуйте снова");
        }
    }

    /**
     * Настраивает портретный режим печати
     *
     * @param printerJob задание печати
     */
    private void setupPortraitOrientation(PrinterJob printerJob) {
        Printer printer = printerJob.getPrinter();
        if (printer != null) {
            // Получаем стандартную страницу A4
            Paper paper = Paper.A4;
            // Создаем PageLayout с портретной ориентацией
            PageLayout pageLayout = printer.createPageLayout(paper, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            printerJob.getJobSettings().setPageLayout(pageLayout);
        }
    }

    /**
     * Создает узел для печати
     *
     * @param content содержимое для печати
     */
    private VBox createPrintNode(String content) {
        Text textNode = new Text(content);
        textNode.setFont(CONTENT_FONT);

        // Оборачиваем текст, чтобы он не выходил за границы страницы
        textNode.wrappingWidthProperty().set(600);

        VBox vbox = new VBox(textNode);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: white;");

        return vbox;
    }

    /**
     * Выполняет непосредственную печать
     *
     * @param printerJob задание печати
     * @param printNode  узел для печати
     */
    private void executePrint(PrinterJob printerJob, VBox printNode) {
        try {
            boolean printed = printerJob.printPage(printNode);

            if (printed) {
                printerJob.endJob();
                log.info("Печать успешно выполнена");
                Warning1.create("УСПЕШНО!", "Печать выполнена", "Список отправлен на печать");
            } else {
                printerJob.cancelJob();
                Warning1.create("ОШИБКА!", "Не удалось выполнить печать", "Попробуйте еще раз");
            }
        } catch (Exception e) {
            log.error("Ошибка при печати", e);
            printerJob.cancelJob();
            Warning1.create("ОШИБКА!", "Не удалось выполнить печать", e.getMessage());
        }
    }
}
