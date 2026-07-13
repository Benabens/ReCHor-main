package ch.epfl.rechor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe permettant de construire un document iCalendar, en gérant les composants
 * (VCALENDAR, VEVENT) et les lignes (VERSION, PRODID, UID, DTSTAMP, DTSTART, etc.).
 *
 * Cette classe gère aussi le pliage des lignes au-delà de 75 caractères
 * en insérant un retour suivi d'un espace, pour être conforme à la norme iCalendar.
 * Plus précisément, si la ligne logique dépasse 75 caractères, elle est pliée de la manière suivante :
 * - La première ligne physique contient jusqu'à 75 caractères.
 * - Les lignes physiques suivantes débutent par un espace et contiennent jusqu'à 74 caractères.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class IcalBuilder {

    public enum Component {
        VCALENDAR, VEVENT;
    }

    public enum Name {
        BEGIN, END, PRODID, VERSION, UID, DTSTAMP, DTSTART, DTEND, SUMMARY, DESCRIPTION;
    }

    private final List<Component> openComponents;
    private final StringBuilder sb;

    /**
     * Formatteur partagé pour les dates au format iCalendar (ex. 20240517T153000).
     */
    private static final DateTimeFormatter ICAL_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter();

    public IcalBuilder() {
        openComponents = new ArrayList<>();
        sb = new StringBuilder();
    }

    /**
     * Ajoute une ligne de type "NAME:valeur" au document iCalendar, puis plie la ligne si nécessaire.
     *
     * @param name  nom du champ iCalendar
     * @param value valeur du champ
     * @return this, pour chaîner les appels
     * @throws NullPointerException si l'un des arguments est null
     */
    public IcalBuilder add(Name name, String value) {
        if (name == null || value == null) {
            throw new NullPointerException();
        }
        String line = name.name() + ":" + value;
        sb.append(foldLine(line)).append("\r\n");
        return this;
    }

    /**
     * Ajoute une ligne "NAME:YYYYMMDDTHHMMSS" à partir d'un {@link LocalDateTime} donné.
     *
     * @param name     nom du champ iCalendar
     * @param dateTime la date/heure à formater
     * @return this, pour chaîner les appels
     */
    public IcalBuilder add(Name name, LocalDateTime dateTime) {
        return add(name, ICAL_DATE_FORMATTER.format(dateTime));
    }

    /**
     * Démarre un composant iCalendar et l'ajoute à la liste des composants ouverts.
     *
     * @param comp le composant à commencer
     * @return this, pour chaîner les appels
     */
    public IcalBuilder begin(Component comp) {
        add(Name.BEGIN, comp.name());
        openComponents.add(comp);
        return this;
    }

    /**
     * Termine le dernier composant ouvert.
     *
     * @return this, pour chaîner les appels
     * @throws IllegalArgumentException si aucun composant n'est actuellement ouvert
     */
    public IcalBuilder end() {
        Preconditions.checkArgument(!openComponents.isEmpty());
        Component comp = openComponents.removeLast();
        add(Name.END, comp.name());
        return this;
    }

    /**
     * Construit la chaîne iCalendar finale.
     *
     * @return la représentation textuelle iCalendar
     * @throws IllegalArgumentException si un composant n'a pas été fermé
     */
    public String build() {
        Preconditions.checkArgument(openComponents.isEmpty());
        return sb.toString();
    }

    /**
     * Plie la ligne si elle dépasse 75 caractères, en insérant "\r\n " conformément à la norme iCalendar.
     * La première ligne physique contient jusqu'à 75 caractères.
     * Les lignes physiques suivantes débutent par un espace et contiennent jusqu'à 74 caractères.
     *
     * @param line la ligne à plier si nécessaire
     * @return la ligne éventuellement pliée
     */
    private String foldLine(String line) {
        if (line.length() <= 75) {
            return line;
        }
        StringBuilder folded = new StringBuilder();
        folded.append(line, 0, 75);
        int index = 75;
        while (index < line.length()) {
            int end = Math.min(index + 74, line.length());
            folded.append("\r\n ");
            folded.append(line, index, end);
            index = end;
        }
        return folded.toString();
    }
}
