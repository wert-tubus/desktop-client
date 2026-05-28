package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import ru.wert.tubus.client.entity.models.Passport;

public class RegisteredPassportItem {

    private final SimpleObjectProperty<Passport> passport;
    private final BooleanProperty hasDrafts;
    private final StringProperty note;

    public RegisteredPassportItem(Passport passport, boolean hasDrafts) {
        this.passport = new SimpleObjectProperty<>(passport);
        this.hasDrafts = new SimpleBooleanProperty(hasDrafts);
        this.note = new SimpleStringProperty(passport != null ? passport.getNote() : "");

        // Слушаем изменения паспорта и обновляем note
        this.passport.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                note.set(newVal.getNote());
            }
        });
    }

    public Passport getPassport() {
        return passport.get();
    }

    public void setPassport(Passport passport) {
        this.passport.set(passport);
        if (passport != null) {
            this.note.set(passport.getNote());
        }
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

    public String getNote() {
        return note.get();
    }

    public void setNote(String note) {
        this.note.set(note);
    }

    public StringProperty noteProperty() {
        return note;
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
