package ch.epfl.rechor.journey;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.IcalBuilder;
import ch.epfl.rechor.journey.Journey;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Classe permettant de convertir un {@link Journey} en un événement iCalendar.
 * Le code iCalendar généré contient un objet VCALENDAR (avec VERSION et PRODID),
 * puis un VEVENT décrivant le voyage (UID, DTSTAMP, DTSTART, DTEND, SUMMARY, DESCRIPTION).
 * La DESCRIPTION inclut chaque étape du voyage, séparées par "\\n".
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class JourneyIcalConverter {

    /**
     * Constructeur privé, pas d'instanciation.
     */
    private JourneyIcalConverter() {
    }

    /**
     * Convertit le voyage spécifié en iCalendar, avec un VCALENDAR et un VEVENT.
     * L'événement contient les heures de départ et d'arrivée, un UID, un DTSTAMP,
     * et un résumé basé sur le premier et le dernier arrêt du {@link Journey}.
     * La description est formée en concaténant la représentation de chaque étape.
     *
     * @param journey le voyage à exporter (non null)
     * @return le texte iCalendar correspondant
     * @throws NullPointerException si {@code journey} est null
     */
    public static String toIcalendar(Journey journey) {
        Objects.requireNonNull(journey);
        IcalBuilder builder = new IcalBuilder();
        builder.begin(IcalBuilder.Component.VCALENDAR)
                .add(IcalBuilder.Name.VERSION, "2.0")
                .add(IcalBuilder.Name.PRODID, "ReCHor")
                .begin(IcalBuilder.Component.VEVENT)
                .add(IcalBuilder.Name.UID, UUID.randomUUID().toString())
                .add(IcalBuilder.Name.DTSTAMP, LocalDateTime.now())
                .add(IcalBuilder.Name.DTSTART, journey.depTime())
                .add(IcalBuilder.Name.DTEND, journey.arrTime())
                .add(IcalBuilder.Name.SUMMARY, journey.depStop().name() + " → " + journey.arrStop().name());

        StringJoiner sj = new StringJoiner("\\n");
        for (Journey.Leg leg : journey.legs()) {
            String legStr = switch (leg) {
                case Journey.Leg.Foot f       -> FormatterFr.formatLeg(f);
                case Journey.Leg.Transport t  -> FormatterFr.formatLeg(t);
            };
            sj.add(legStr);
        }
        builder.add(IcalBuilder.Name.DESCRIPTION, sj.toString());
        builder.end(); // VEVENT
        builder.end(); // VCALENDAR
        return builder.build();
    }
}
