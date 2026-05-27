package ru.wert.tubus.chogori.chat.socketwork.recievedMessageHandlers;

import com.google.gson.Gson;
import javafx.scene.control.Tab;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.application.drafts.DraftsTabController;
import ru.wert.tubus.chogori.tabs.AppTab;
import ru.wert.tubus.client.entity.models.Draft;
import ru.wert.tubus.client.entity.models.Message;
import ru.wert.tubus.client.entity.models.Passport;
import ru.wert.tubus.client.entity.serviceQUICK.DraftQuickService;
import ru.wert.tubus.client.entity.serviceQUICK.PassportQuickService;
import ru.wert.tubus.client.retrofit.GsonConfiguration;
import ru.wert.tubus.client.utils.MessageType;

import static ru.wert.tubus.chogori.statics.UtilStaticNodes.CH_TAB_PANE;
import static ru.wert.tubus.client.entity.serviceQUICK.DraftQuickService.LOADED_DRAFTS;
import static ru.wert.tubus.client.entity.serviceQUICK.PassportQuickService.LOADED_PASSPORTS;

/**
 * Обработчик сообщений о чертежах.
 */
@Slf4j
public class DraftMessageHandler {
    public static void handle(Message message, MessageType type, StringBuilder str) {
        Gson gson = GsonConfiguration.createGson();
        Draft draft = gson.fromJson(message.getText(), Draft.class);

        switch (type) {
            case ADD_DRAFT:
                processAddDraft(draft, str);
                break;
            case UPDATE_DRAFT:
                processUpdateDraft(draft, str);
                break;
            case DELETE_DRAFT:
                processDeleteDraft(draft, str);
                break;
        }
    }

    private static void processAddDraft(Draft draft, StringBuilder str) {
        str.append("Пользователь ");
        str.append(draft.getStatusUser().toUsefulString());
        str.append(" добавил чертеж: ");
        str.append(draft.getPassport().toUsefulString());

        Passport passport = draft.getPassport();
        if(!LOADED_PASSPORTS.contains(passport))
            LOADED_PASSPORTS.add(passport);

        if(!LOADED_DRAFTS.contains(draft))
            LOADED_DRAFTS.add(draft);

        for(Tab tab: CH_TAB_PANE.getTabs()){
            if(((AppTab)tab).getTabController() instanceof DraftsTabController){
                ((DraftsTabController)((AppTab)tab).getTabController()).updateTab();
            }
        }
    }

    private static void processUpdateDraft(Draft draft, StringBuilder str) {
        str.append("Пользователь ");
        str.append(draft.getStatusUser().toUsefulString());
        str.append(" обновил чертеж: ");
        str.append(draft.getPassport().toUsefulString());

        Passport passport = draft.getPassport();
        // Обновляем паспорт в кеше, если он там есть
        Passport foundPassport = PassportQuickService.getInstance().findById(passport.getId());
        if(foundPassport != null) {
            LOADED_PASSPORTS.remove(foundPassport);
            LOADED_PASSPORTS.add(passport);
        }

        Draft foundDraft = DraftQuickService.getInstance().findById(draft.getId());
        // Обновляем чертеж в кеше
        if(foundDraft != null) {
            LOADED_DRAFTS.remove(foundDraft);
            LOADED_DRAFTS.add(draft);
        } else {
            LOADED_DRAFTS.add(draft);
        }

        // Обновляем все открытые вкладки редактора чертежей
        for(Tab tab: CH_TAB_PANE.getTabs()) {
            if(((AppTab)tab).getTabController() instanceof DraftsTabController) {
                ((DraftsTabController)((AppTab)tab).getTabController()).updateTab();
            }
        }
    }

    private static void processDeleteDraft(Draft draft, StringBuilder str) {
        str.append("Пользователь ");
        str.append(draft.getStatusUser().toUsefulString());
        str.append(" удалил чертеж: ");
        str.append(draft.getPassport().toUsefulString());

        // Удаляем чертеж из кеша
        LOADED_DRAFTS.remove(draft);

        // Обновляем все открытые вкладки редактора чертежей
        for(Tab tab: CH_TAB_PANE.getTabs()) {
            if(((AppTab)tab).getTabController() instanceof DraftsTabController) {
                ((DraftsTabController)((AppTab)tab).getTabController()).updateTab();
            }
        }
    }
}
