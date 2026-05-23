package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import ru.wert.tubus.client.entity.models.Passport;

/**
 * Обертка для паспорта в таблице зарегистрированных паспортов.
 * Содержит паспорт и информацию о наличии чертежей.
 */
public class RegisteredPassportItem {

    private final SimpleObjectProperty<Passport> passport;
    private final BooleanProperty hasDrafts;

    public RegisteredPassportItem(Passport passport, boolean hasDrafts) {
        this.passport = new SimpleObjectProperty<>(passport);
        this.hasDrafts = new SimpleBooleanProperty(hasDrafts);
    }

    public Passport getPassport() {
        return passport.get();
    }

    public void setPassport(Passport passport) {
        this.passport.set(passport);
    }

    public SimpleObjectProperty<Passport> passportProperty() {
        return passport;
    }

    public boolean hasDrafts() {
        return hasDrafts.get();
    }

    public void setHasDrafts(boolean hasDrafts) {
        this.hasDrafts.set(hasDrafts);
    }

    public BooleanProperty hasDraftsProperty() {
        return hasDrafts;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RegisteredPassportItem that = (RegisteredPassportItem) obj;
        Passport p1 = getPassport();
        Passport p2 = that.getPassport();
        return p1 != null && p2 != null && p1.getId().equals(p2.getId());
    }

    @Override
    public int hashCode() {
        Passport p = getPassport();
        return p != null ? p.getId().hashCode() : 0;
    }
}
