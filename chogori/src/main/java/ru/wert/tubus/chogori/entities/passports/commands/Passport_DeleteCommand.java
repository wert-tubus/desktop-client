package ru.wert.tubus.chogori.entities.passports.commands;

import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.chogori.common.commands.ICommand;
import ru.wert.tubus.chogori.entities.passports.Passport_TableView;
import ru.wert.tubus.chogori.application.services.ChogoriServices;
import ru.wert.tubus.winform.warnings.Warning1;

import java.util.List;

import static ru.wert.tubus.winform.warnings.WarningMessages.*;

/**
 * Команда для удаления одного или нескольких паспортов.
 * Перед удалением проверяет, нет ли у паспорта связанных чертежов (Draft).
 * Если чертежи существуют, удаление блокируется с соответствующим предупреждением.
 */
@Slf4j
public class Passport_DeleteCommand implements ICommand {

    private final List<Passport> items;          // Список паспортов для удаления
    private final Passport_TableView tableView;  // Таблица, из которой удаляются паспорты

    /**
     * Конструктор команды удаления.
     *
     * @param items     список паспортов, подлежащих удалению
     * @param tableView таблица с паспортами (нужна для обновления и выделения строки)
     */
    public Passport_DeleteCommand(List<Passport> items, Passport_TableView tableView) {
        this.items = items;
        this.tableView = tableView;
    }

    /**
     * Выполняет удаление паспортов.
     * Алгоритм:
     * 1. Показывает индикатор загрузки
     * 2. Запоминает текущую выделенную позицию
     * 3. Для каждого паспорта проверяет наличие связанных чертежов
     * 4. Если чертежов нет — удаляет паспорт, иначе показывает предупреждение и пропускает
     * 5. Обновляет таблицу с сохранением текущего фильтра
     * 6. Восстанавливает выделение (на новом месте, если были успешные удаления, или на старом, если не было)
     * 7. Скрывает индикатор загрузки
     */
    @Override
    public void execute() {
        // Запоминаем текущую выделенную позицию до начала удаления
        int originalSelectedIndex = tableView.getSelectionModel().getSelectedIndex();
        Passport originalSelectedItem = originalSelectedIndex >= 0
                ? tableView.getItems().get(originalSelectedIndex)
                : null;

        // Флаг, было ли хоть одно успешное удаление
        boolean[] anyDeleted = {false};

        // Показываем индикатор загрузки
        showProgressIndicator(true);

        try {
            // Запоминаем элемент для выделения после удаления (до изменения данных)
            Passport itemToSelect = findItemToSelectAfterDeletion();

            // Удаляем элементы с предварительной проверкой
            for (Passport passport : items) {
                // Проверка: есть ли у паспорта чертежи?
                if (hasDrafts(passport)) {
                    String message = String.format(
                            "Невозможно удалить номер '%s' — существуют связанные чертежи.",
                            passport.toUsefulString()
                    );
                    Warning1.create($ATTENTION, message, "Сначала удалите или переместите чертежи");
                    log.warn("Попытка удаления паспорта {} заблокирована: имеются чертежи", passport.toUsefulString());
                    continue; // Пропускаем удаление этого паспорта
                }

                // Нет чертежов — можно удалять
                try {
                    ChogoriServices.CH_QUICK_PASSPORTS.delete(passport);
                    anyDeleted[0] = true;
                    log.info("Удалён паспорт {}", passport.toUsefulString());
                } catch (Exception e) {
                    Warning1.create($ATTENTION, $ERROR_WHILE_DELETING_ITEM, $ITEM_IS_BUSY_MAYBE);
                    log.error("При удалении паспорта {} произошла ошибка", passport.toUsefulString(), e);
                }
            }

            // Если были успешные удаления, обновляем таблицу
            if (anyDeleted[0]) {
                // Обновляем таблицу, сохраняя текущий тип/фильтр
                tableView.refreshPreservingType();

                // Восстанавливаем выделение на новом месте
                restoreSelection(itemToSelect);
            } else {
                // Если не было ни одного успешного удаления, восстанавливаем выделение на прежней позиции
                restoreOriginalSelection(originalSelectedItem, originalSelectedIndex);
            }

        } finally {
            // Скрываем индикатор загрузки
            showProgressIndicator(false);
        }
    }

    /**
     * Показывает или скрывает индикатор загрузки на таблице.
     *
     * @param show true - показать индикатор, false - скрыть
     */
    private void showProgressIndicator(boolean show) {
        Platform.runLater(() -> {
            if (show) {
                // Устанавливаем прозрачный плейсхолдер с индикатором загрузки
                ProgressIndicator progressIndicator = new ProgressIndicator();
                progressIndicator.setMaxSize(50, 50);
                tableView.setPlaceholder(progressIndicator);
            } else {
                // Восстанавливаем стандартный плейсхолдер
                tableView.setPlaceholder(null);
                // Принудительно обновляем отображение
                tableView.refresh();
            }
        });
    }

    /**
     * Восстанавливает выделение на исходной позиции (при неудачном удалении).
     *
     * @param originalSelectedItem исходный выделенный элемент
     * @param originalSelectedIndex исходный индекс выделенного элемента
     */
    private void restoreOriginalSelection(Passport originalSelectedItem, int originalSelectedIndex) {
        if (originalSelectedItem == null || originalSelectedIndex < 0) {
            return;
        }

        Platform.runLater(() -> {
            // Проверяем, существует ли ещё исходный элемент в таблице
            int currentIndex = findItemIndex(originalSelectedItem);
            if (currentIndex >= 0) {
                // Элемент всё ещё в таблице - выделяем его
                tableView.scrollTo(currentIndex);
                tableView.getSelectionModel().select(currentIndex);
            } else if (originalSelectedIndex < tableView.getItems().size()) {
                // Элемента нет, но пытаемся выделить ту же позицию
                tableView.scrollTo(originalSelectedIndex);
                tableView.getSelectionModel().select(originalSelectedIndex);
            }
        });
    }

    /**
     * Проверяет, есть ли у паспорта связанные чертежи (Draft).
     *
     * @param passport проверяемый паспорт
     * @return true, если хотя бы один чертеж привязан к паспорту
     */
    private boolean hasDrafts(Passport passport) {
        if (passport == null || passport.getId() == null) {
            return false;
        }
        // Используем сервис для поиска чертежов по паспорту
        return !ChogoriServices.CH_QUICK_DRAFTS.findByPassport(passport).isEmpty();
    }

    /**
     * Определяет, какой паспорт нужно будет выделить в таблице после удаления.
     * Логика выбора:
     * - Следующий элемент после первого удаляемого (если он не удаляется)
     * - Предыдущий элемент перед первым удаляемым (если он не удаляется)
     * - Любой другой неудаляемый элемент из таблицы
     * - null, если таблица станет пустой
     *
     * @return паспорт для выделения, либо null, если выделять нечего
     */
    private Passport findItemToSelectAfterDeletion() {
        if (tableView.getItems().isEmpty()) return null;

        // Находим индекс первого удаляемого
        int firstDeletedIndex = -1;
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (items.contains(tableView.getItems().get(i))) {
                firstDeletedIndex = i;
                break;
            }
        }

        if (firstDeletedIndex < 0) return null;

        // Пытаемся взять следующий
        if (firstDeletedIndex + 1 < tableView.getItems().size()) {
            Passport next = tableView.getItems().get(firstDeletedIndex + 1);
            if (!items.contains(next)) return next;
        }

        // Пытаемся взять предыдущий
        if (firstDeletedIndex - 1 >= 0) {
            Passport prev = tableView.getItems().get(firstDeletedIndex - 1);
            if (!items.contains(prev)) return prev;
        }

        // Ищем любой неудаляемый
        for (Passport p : tableView.getItems()) {
            if (!items.contains(p)) return p;
        }

        return null;
    }

    /**
     * Находит индекс паспорта в таблице по его идентификатору.
     *
     * @param passport искомый паспорт
     * @return индекс в списке элементов таблицы, либо -1 если не найден
     */
    private int findItemIndex(Passport passport) {
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (tableView.getItems().get(i).getId().equals(passport.getId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Восстанавливает выделение в таблице после успешного удаления.
     *
     * @param itemToSelect элемент, который нужно выделить (может быть null)
     */
    private void restoreSelection(Passport itemToSelect) {
        if (itemToSelect != null) {
            int rowToSelect = findItemIndex(itemToSelect);
            if (rowToSelect >= 0) {
                final int finalRow = rowToSelect;
                Platform.runLater(() -> {
                    tableView.scrollTo(finalRow);
                    tableView.getSelectionModel().select(finalRow);
                });
            }
        } else if (!tableView.getItems().isEmpty()) {
            // Если нет конкретного элемента для выделения, выделяем первую строку
            Platform.runLater(() -> {
                tableView.scrollTo(0);
                tableView.getSelectionModel().select(0);
            });
        }
    }
}
