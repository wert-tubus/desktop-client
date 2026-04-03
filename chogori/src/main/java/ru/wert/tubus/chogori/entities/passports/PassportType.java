package ru.wert.tubus.chogori.entities.passports;


public enum PassportType {
    ALL,        // Все паспорта
    PIK,        // Только ПИК (префикс "ПИК" и номер по маске "######.###")
    SKETCHES    // Только эскизные (префикс null или "-", номер по маске "Э#####")
}

