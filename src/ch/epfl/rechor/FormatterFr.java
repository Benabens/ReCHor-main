package ch.epfl.rechor;

import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.Stop;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Fournit des méthodes de formatage pour les durées, les heures, les noms de plateformes,
 * ainsi que pour le formatage des étapes de trajet (legs) et des itinéraires en transport public.
 * Toutes les méthodes de cette classe sont statiques et permettent de convertir des données
 * en chaînes de caractères au format français.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class FormatterFr {

    // Empêche l’instanciation de la classe utilitaire
    private FormatterFr() {
        // Cette classe ne doit pas être instanciée
    }

    // Formateur réutilisé pour les heures au format "HhMM"
    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY)
            .appendLiteral("h")
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();

    /**
     * Formate une durée en minutes et heures.
     * Si la durée est inférieure à 60 minutes, renvoie "x min".
     * Sinon, renvoie "y h z min" où y représente les heures et z les minutes restantes.
     *
     * @param duration la durée à formater
     * @return la durée formatée en chaîne de caractères
     */
    public static String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long remaining = minutes % 60;
        return hours + " h " + remaining + " min";
    }

    /**
     * Formate une date/heure locale pour l'afficher sous la forme "HhMM".
     *
     * @param dateTime la date et l'heure à formater
     * @return l'heure formatée en chaîne de caractères
     */
    public static String formatTime(LocalDateTime dateTime) {
        return TIME_FORMATTER.format(dateTime);
    }

    /**
     * Formate le nom de la plateforme d'un arrêt.
     * Si le nom de la plateforme est null ou vide, renvoie une chaîne vide.
     * Si le premier caractère est un chiffre, renvoie "voie " suivi du nom,
     * sinon renvoie "quai " suivi du nom.
     *
     * @param stop l'arrêt dont le nom de plateforme doit être formaté
     * @return la chaîne formatée pour le nom de plateforme
     */
    public static String formatPlatformName(Stop stop) {
        String platformName = stop.platformName();
        if (platformName == null || platformName.isEmpty()) {
            return "";
        }
        char first = platformName.charAt(0);
        if (Character.isDigit(first)) {
            return "voie " + platformName;
        } else {
            return "quai " + platformName;
        }
    }

    /**
     * Formate une étape à pied.
     * Si l'étape représente un transfert (les arrêts de départ et d'arrivée ont le même nom et les mêmes coordonnées),
     * renvoie "changement", sinon renvoie "trajet à pied". Ajoute ensuite la durée formatée de l'étape entre parenthèses.
     *
     * @param footLeg l'étape à pied à formater
     * @return la chaîne formatée pour l'étape à pied
     */
    public static String formatLeg(Journey.Leg.Foot footLeg) {
        StringBuilder sb = new StringBuilder();
        if (footLeg.isTransfer()) {
            sb.append("changement");
        } else {
            sb.append("trajet à pied");
        }
        sb.append(" (").append(formatDuration(footLeg.duration())).append(")");
        return sb.toString();
    }

    /**
     * Formate une étape en transport public.
     * Construit une chaîne comprenant l'heure de départ, le nom de l'arrêt de départ (et sa plateforme si disponible),
     * suivi d'une flèche indiquant la direction, et enfin le nom de l'arrêt d'arrivée (et sa plateforme si disponible).
     * L'heure d'arrivée est affichée dans la partie plateforme ou directement après le nom de l'arrêt d'arrivée.
     *
     * @param leg l'étape en transport public à formater
     * @return la chaîne formatée pour l'étape en transport
     */
    public static String formatLeg(Journey.Leg.Transport leg) {
        StringBuilder sb = new StringBuilder();
        String depTimeStr = formatTime(leg.depTime());
        String arrTimeStr = formatTime(leg.arrTime());
        String depStopName = leg.depStop().name();
        String arrStopName = leg.arrStop().name();
        String depPlatform = formatPlatformName(leg.depStop());
        String arrPlatform = formatPlatformName(leg.arrStop());

        sb.append(depTimeStr).append(" ").append(depStopName);
        if (!depPlatform.isEmpty()) {
            sb.append(" (").append(depPlatform).append(")");
        }
        sb.append(" → ").append(arrStopName);
        sb.append(" (arr. ").append(arrTimeStr);
        if (!arrPlatform.isEmpty()) {
            sb.append(" ").append(arrPlatform);
        }
        sb.append(")");

        return sb.toString();
    }

    /**
     * Formate la ligne et la destination d'un trajet en transport public.
     *
     * @param transportLeg l'étape en transport public dont on veut formater la route et la destination
     * @return la chaîne formatée combinant la ligne et la destination
     */
    public static String formatRouteDestination(Journey.Leg.Transport transportLeg) {
        return transportLeg.route() + " Direction " + transportLeg.destination();
    }
}
