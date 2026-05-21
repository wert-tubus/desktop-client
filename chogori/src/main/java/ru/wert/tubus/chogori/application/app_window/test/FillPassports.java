package ru.wert.tubus.chogori.application.app_window.test;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.serviceREST.DraftService;
import ru.wert.tubus.client.entity.serviceREST.PassportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс заполняет пасспорта данными из чертежей:
 * дата создания, разработчик
 */
@Slf4j
public class FillPassports {

    public static void fill(ActionEvent event) {
        // Получаем сервисы для прямого доступа к БД
        PassportService passportService = PassportService.getInstance();
        DraftService draftService = DraftService.getInstance();

        // Получаем все паспорта напрямую из БД
        List<Passport> allPassports = passportService.findAll();
        if (allPassports == null || allPassports.isEmpty()) {
            showAlert("Нет данных", "В базе данных нет паспортов для обработки");
            return;
        }

        int totalPassports = allPassports.size();

        // Создаем диалоговое окно с прогресс-баром
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Обновление паспортов");
        dialog.setHeaderText("Перенос данных из чертежей в паспорта");
        dialog.setResizable(true);
        dialog.setWidth(650);

        // Создаем содержимое диалога
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(600);

        Label progressLabel = new Label("0 / " + totalPassports);
        Label currentPassportLabel = new Label("Обработка паспорта...");
        currentPassportLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
        currentPassportLabel.setWrapText(true);

        // TextArea для отображения ошибок
        TextArea errorArea = new TextArea();
        errorArea.setEditable(false);
        errorArea.setPrefHeight(200);
        errorArea.setStyle("-fx-font-size: 11px; -fx-font-family: monospace; -fx-text-fill: #cc0000;");
        errorArea.setPromptText("Ошибки будут отображаться здесь...");

        TitledPane errorPane = new TitledPane("Лог ошибок", errorArea);
        errorPane.setExpanded(false);
        errorPane.setAnimated(true);

        // TextArea для лога успешных операций
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-font-size: 11px; -fx-font-family: monospace; -fx-text-fill: #008000;");
        logArea.setPromptText("Лог операций...");

        TitledPane logPane = new TitledPane("Лог операций", logArea);
        logPane.setExpanded(false);
        logPane.setAnimated(true);

        content.getChildren().addAll(progressBar, progressLabel, currentPassportLabel, errorPane, logPane);
        dialog.getDialogPane().setContent(content);

        // Добавляем кнопку отмены
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelButton);

        // Счетчики
        List<String> errors = new ArrayList<>();
        List<String> logs = new ArrayList<>();

        // Запускаем обработку в отдельном потоке
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int processed = 0;
                int errorCount = 0;
                int updatedCount = 0;

                for (Passport passport : allPassports) {
                    if (isCancelled()) {
                        updateMessage("Операция отменена");
                        break;
                    }

                    try {
                        String passportInfo = passport.toUsefulString();
                        updateMessage(passportInfo);
                        log.info("Обработка паспорта: {}", passportInfo);

                        // Прямой запрос к БД для получения чертежей по паспорту
                        List<Draft> drafts = draftService.findByPassport(passport);

                        if (drafts != null && !drafts.isEmpty()) {
                            // Находим последний добавленный чертеж (по creationTime)
                            Draft lastDraft = drafts.stream()
                                    .max((d1, d2) -> {
                                        if (d1.getCreationTime() == null && d2.getCreationTime() == null) return 0;
                                        if (d1.getCreationTime() == null) return -1;
                                        if (d2.getCreationTime() == null) return 1;
                                        return d1.getCreationTime().compareTo(d2.getCreationTime());
                                    })
                                    .orElse(null);

                            if (lastDraft != null) {
                                boolean needUpdate = false;
                                StringBuilder changes = new StringBuilder();

                                // Проверяем и сохраняем изменения
                                String oldUserName = passport.getUserName();
                                String oldNote = passport.getNote();
                                String oldDate = passport.getDate();

                                // 1. statusUser.getName() -> passport.userName
                                if (lastDraft.getStatusUser() != null && lastDraft.getStatusUser().getName() != null) {
                                    String newUserName = lastDraft.getStatusUser().getName();
                                    if (!newUserName.equals(oldUserName)) {
                                        passport.setUserName(newUserName);
                                        needUpdate = true;
                                        changes.append("userName: '").append(oldUserName).append("' -> '").append(newUserName).append("'; ");
                                        log.debug("Установлен user: {}", newUserName);
                                    }
                                }

                                // 2. folder.getName() -> passport.note
                                if (lastDraft.getFolder() != null && lastDraft.getFolder().getName() != null) {
                                    String newNote = lastDraft.getFolder().getName();
                                    if (!newNote.equals(oldNote)) {
                                        passport.setNote(newNote);
                                        needUpdate = true;
                                        changes.append("note: '").append(oldNote).append("' -> '").append(newNote).append("'; ");
                                        log.debug("Установлена папка: {}", newNote);
                                    }
                                }

                                // 3. creationTime -> passport.date (формат dd.MM.yy)
                                if (lastDraft.getCreationTime() != null) {
                                    try {
                                        String newDate = formatDate(lastDraft.getCreationTime());
                                        if (!newDate.equals(oldDate)) {
                                            passport.setDate(newDate);
                                            needUpdate = true;
                                            changes.append("date: '").append(oldDate).append("' -> '").append(newDate).append("'; ");
                                            log.debug("Установлена дата: {}", newDate);
                                        }
                                    } catch (Exception e) {
                                        String errorMsg = String.format("Ошибка форматирования даты для паспорта %s: %s, raw date: %s",
                                                passportInfo, e.getMessage(), lastDraft.getCreationTime());
                                        log.error(errorMsg, e);
                                        errors.add(errorMsg);
                                        errorCount++;
                                    }
                                }

                                // Сохраняем только если были изменения
                                if (needUpdate) {
                                    try {
                                        boolean success = passportService.update(passport);
                                        if (success) {
                                            updatedCount++;
                                            String logMsg = String.format("✓ Обновлен паспорт %s: %s", passportInfo, changes.toString());
                                            logs.add(logMsg);
                                            log.info(logMsg);

                                            // Обновляем лог в UI
                                            final List<String> currentLogs = new ArrayList<>(logs);
                                            Platform.runLater(() -> {
                                                logArea.setText(String.join("\n", currentLogs));
                                                if (!currentLogs.isEmpty()) {
                                                    logPane.setExpanded(true);
                                                }
                                            });
                                        } else {
                                            String errorMsg = String.format("✗ Не удалось обновить паспорт %s", passportInfo);
                                            errors.add(errorMsg);
                                            errorCount++;
                                            log.error(errorMsg);
                                        }
                                    } catch (Exception e) {
                                        String errorMsg = String.format("✗ Ошибка сохранения паспорта %s: %s",
                                                passportInfo, e.getMessage());
                                        log.error(errorMsg, e);
                                        errors.add(errorMsg);
                                        errorCount++;
                                    }
                                } else {
                                    log.debug("Нет изменений для паспорта: {}", passportInfo);
                                }
                            } else {
                                log.debug("Не найден последний чертеж для паспорта: {}", passportInfo);
                            }
                        } else {
                            log.debug("Нет чертежей для паспорта: {}", passportInfo);
                        }

                    } catch (Exception e) {
                        String errorMsg = String.format("✗ Критическая ошибка при обработке паспорта %s: %s",
                                passport.toUsefulString(), e.getMessage());
                        log.error(errorMsg, e);
                        errors.add(errorMsg);
                        errorCount++;
                    }

                    processed++;
                    updateProgress(processed, totalPassports);
                    updateTitle(String.format("Обработано: %d / %d (обновлено: %d, ошибок: %d)",
                            processed, totalPassports, updatedCount, errorCount));

                    // Периодически обновляем UI с ошибками
                    final List<String> currentErrors = new ArrayList<>(errors);
                    Platform.runLater(() -> {
                        if (!currentErrors.isEmpty()) {
                            errorArea.setText(String.join("\n", currentErrors));
                            errorPane.setExpanded(true);
                        }
                    });
                }

                // Финальное сообщение
                String finalMessage = String.format("Обработка завершена. Обновлено: %d, Ошибок: %d",
                        updatedCount, errorCount);
                updateMessage(finalMessage);
                log.info(finalMessage);

                return null;
            }
        };

        // Привязываем прогресс-бар к задаче
        progressBar.progressProperty().bind(task.progressProperty());

        // Обновляем текстовые метки
        task.messageProperty().addListener((obs, old, newMsg) -> {
            if (newMsg != null) {
                Platform.runLater(() -> currentPassportLabel.setText("Текущий паспорт: " + newMsg));
            }
        });

        task.titleProperty().addListener((obs, old, newTitle) -> {
            if (newTitle != null) {
                Platform.runLater(() -> progressLabel.setText(newTitle));
            }
        });

        // Обработка завершения задачи
        task.setOnSucceeded(e -> {
            dialog.close();
            String resultMessage = task.getTitle() != null ? task.getTitle() : "Операция завершена";
            if (!errors.isEmpty()) {
                showAlert("Завершено с ошибками", resultMessage + "\n\nДетали ошибок:\n" +
                        String.join("\n", errors.subList(0, Math.min(15, errors.size()))));
            } else {
                showAlert("Завершено успешно", resultMessage);
            }
        });

        task.setOnFailed(e -> {
            dialog.close();
            Throwable exception = task.getException();
            String errorDetails = exception != null ? exception.getMessage() : "Неизвестная ошибка";
            log.error("Ошибка выполнения задачи", exception);
            showAlert("Ошибка", "Произошла ошибка при обработке:\n" + errorDetails);
        });

        task.setOnCancelled(e -> {
            dialog.close();
            showAlert("Отменено", "Операция была отменена пользователем");
        });

        // Запускаем задачу в отдельном потоке
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

        // Показываем диалог и ждем завершения
        dialog.showAndWait();
    }

    /**
     * Форматирует дату из строки в формат dd.MM.yy
     * Поддерживает форматы: "2021-10-12 11:45:21.392+03" и "2022-03-21T08:36:21.492"
     */
    private static String formatDate(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return "";
        }

        try {
            // Убираем миллисекунды и часовой пояс
            String cleanedDate = dateTimeString.split("\\.")[0];
            // Заменяем 'T' на пробел если есть
            cleanedDate = cleanedDate.replace('T', ' ');

            // Парсим дату и время
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(cleanedDate, inputFormatter);

            // Форматируем в dd.MM.yy
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            log.error("Ошибка форматирования даты: {}", dateTimeString, e);
            return "";
        }
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(Math.min(400, textArea.getScrollTop() + 200));
        textArea.setStyle("-fx-font-size: 12px; -fx-font-family: monospace;");

        alert.getDialogPane().setContent(textArea);
        alert.setResizable(true);
        alert.showAndWait();
    }

}
