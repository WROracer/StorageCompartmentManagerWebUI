package de.wroracer.storagecompartmentui.views.data;

public enum TimeSpan {
    FIVE_MINUTE("Letzten 5 Minuten"),
    TEN_MINUTE("Letzten 10 Minuten"),
    HALF_HOUR("Letzten 30 Minuten"),
    HOUR("Letzte Stunde"),
    TWELVE_HOUR("Letzten 12 Stunden"),
    DAY("Letzten 24 Stunden"),
    WEEK("Letzten 7 Tage"),
    MONTH("Letzen Monat"),
    YEAR("Letztes Jahr"),
    ALL("Alle");
    private final String display;

    TimeSpan(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
