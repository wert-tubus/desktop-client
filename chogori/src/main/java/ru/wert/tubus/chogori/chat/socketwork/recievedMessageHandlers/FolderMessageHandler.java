package ru.wert.tubus.chogori.chat.socketwork.recievedMessageHandlers;

import com.google.gson.Gson;
import javafx.scene.control.Tab;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.drafts.DraftsTabController;
import ru.wert.tubus.chogori.tabs.AppTab;
import ru.wert.tubus.client.entity.models.Folder;
import ru.wert.tubus.client.entity.models.Message;
import ru.wert.tubus.client.entity.serviceQUICK.FolderQuickService;
import ru.wert.tubus.client.retrofit.GsonConfiguration;
import ru.wert.tubus.client.utils.MessageType;

import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_TAB_PANE;
import static ru.wert.tubus.client.entity.serviceQUICK.FolderQuickService.LOADED_FOLDERS;

/**
 * Обработчик сообщений о папках.
 */
@Slf4j
public class FolderMessageHandler {
    public static void handle(Message message, MessageType type, StringBuilder str) {
        Gson gson = GsonConfiguration.createGson();
        Folder folder = gson.fromJson(message.getText(), Folder.class);

        switch (type) {
            case ADD_FOLDER:
                processAddFolder(folder, str);
                break;
            case UPDATE_FOLDER:
                processUpdateFolder(folder, str);
                break;
            case DELETE_FOLDER:
                processDeleteFolder(folder, str);
                break;
        }
    }

    private static void processAddFolder(Folder folder, StringBuilder str) {
        str.append("Пользователь добавил комплект чертежей: ");
        str.append(folder.toUsefulString());

        if(!LOADED_FOLDERS.contains(folder))
            LOADED_FOLDERS.add(folder);

        for(Tab tab: CH_TAB_PANE.getTabs()){
            if(((AppTab)tab).getTabController() instanceof DraftsTabController){
                ((DraftsTabController)((AppTab)tab).getTabController()).updateTab();
            }
        }
    }

    private static void processUpdateFolder(Folder folder, StringBuilder str) {
        str.append("Пользователь изменил комплект чертежей: ");
        str.append(folder.toUsefulString());

        // Обновляем чертеж в кеше
        Folder foundFolder = FolderQuickService.getInstance().findById(folder.getId());
        if(foundFolder != null) {
            LOADED_FOLDERS.remove(foundFolder);
            LOADED_FOLDERS.add(folder);
        } else {
            LOADED_FOLDERS.add(folder);
        }

        // Обновляем все открытые вкладки редактора чертежей
        for(Tab tab: CH_TAB_PANE.getTabs()) {
            if(((AppTab)tab).getTabController() instanceof DraftsTabController) {
                ((DraftsTabController)((AppTab)tab).getTabController()).updateTab();
            }
        }
    }

    private static void processDeleteFolder(Folder folder, StringBuilder str) {
        str.append("Пользователь удалил комплект чертежей: ");
        str.append(folder.toUsefulString());

        // Удаляем чертеж из кеша
        LOADED_FOLDERS.remove(folder);

        // Обновляем все открытые вкладки редактора чертежей
        for(Tab tab: CH_TAB_PANE.getTabs()) {
            if(((AppTab)tab).getTabController() instanceof DraftsTabController) {
                ((DraftsTabController)((AppTab)tab).getTabController()).updateTab();
            }
        }
    }
}
