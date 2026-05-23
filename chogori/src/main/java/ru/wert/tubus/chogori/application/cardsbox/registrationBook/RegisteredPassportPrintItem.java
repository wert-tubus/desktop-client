package ru.wert.tubus.chogori.application.cardsbox.registrationBook;

import ru.wert.tubus.client.entity.models.Passport;

/**
 * Обертка для печати паспорта с информацией о наличии чертежей.
 */
public class RegisteredPassportPrintItem {

    private final Passport passport;
    private final boolean hasDrafts;

    public RegisteredPassportPrintItem(Passport passport, boolean hasDrafts) {
        this.passport = passport;
        this.hasDrafts = hasDrafts;
    }

    public Passport getPassport() {
        return passport;
    }

    public boolean hasDrafts() {
        return hasDrafts;
    }

    public String getDraftsSymbol() {
        return hasDrafts ? "[X]" : "[ ]";
    }

    public String getDraftsText() {
        return hasDrafts ? "Есть чертежи" : "Нет чертежей";
    }
}