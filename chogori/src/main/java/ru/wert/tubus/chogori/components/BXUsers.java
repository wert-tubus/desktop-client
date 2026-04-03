package ru.wert.tubus.chogori.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ru.wert.tubus.client.entity.models.User;
import ru.wert.tubus.client.entity.models.UserGroup;
import ru.wert.tubus.client.entity.serviceREST.UserService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BXUsers {

    public BXUsers(ComboBox<User> bxUsers, UserGroup userGroup) {

        //!!!!!!!!!!!!!!!!!!!
        bxUsers.setEditable(false);

        bxUsers.setStyle("-fx-font-size: 24; -fx-background-color: white");
        
        List<User> allUsers = new ArrayList<>();
        for(User u: UserService.getInstance().findAll())
            //Если userGroup == null добавляем всех пользователей
            //Если userGroup == null добавляем пользователей определенной группы даже если они неактивны
            if(u.getUserGroup().equals(userGroup) & u.isActive()) allUsers.add(u);

        ObservableList<User> activeUsers = FXCollections.observableArrayList(allUsers);
        activeUsers.sort(Comparator.comparing(User::getName));
        bxUsers.setItems(activeUsers);

        bxUsers.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user.getName();
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
    }


}
