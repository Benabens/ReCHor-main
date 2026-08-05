package ch.epfl.rechor.timetable;

import java.time.LocalDate;

/**
 * Représente l'horaire complet du transport public, qui rassemble :
 * - les gares (Stations) et leurs noms alternatifs (StationAliases),
 * - les voies/quais (Platforms),
 * - les lignes (Routes),
 * - les changements (Transfers),
 * - les courses (Trips) et les liaisons (Connections) propres à une date donnée.
 *
 * Les méthodes par défaut (isStationId, isPlatformId, stationId, platformName)
 * facilitent la distinction entre gare et quai/voie.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface TimeTable {

    /** Retourne l'ensemble des gares. */
    Stations stations();

    /** Retourne les noms alternatifs des gares. */
    StationAliases stationAliases();

    /** Retourne les voies/quais. */
    Platforms platforms();

    /** Retourne les lignes de transport. */
    Routes routes();

    /** Retourne les changements. */
    Transfers transfers();

    /**
     * Retourne les courses actives pour une date donnée.
     *
     * @param date la date
     * @return les courses pour ce jour
     */
    Trips tripsFor(LocalDate date);

    /**
     * Retourne les liaisons actives pour une date donnée.
     *
     * @param date la date
     * @return les liaisons pour ce jour
     */
    Connections connectionsFor(LocalDate date);

    // Méthodes par défaut

    /**
     * Indique si l'index d'arrêt correspond à une gare (et non à un quai/voie).
     *
     * @param stopId l'index d'arrêt
     * @return true si stopId correspond à une gare, false sinon
     */
    default boolean isStationId(int stopId) {
        return stopId < stations().size();
    }

    /**
     * Indique si l'index d'arrêt correspond à un quai/voie (et non à une gare).
     *
     * @param stopId l'index d'arrêt
     * @return true si stopId correspond à un quai/voie, false sinon
     */
    default boolean isPlatformId(int stopId) {
        return stopId >= stations().size();
    }

    /**
     * Retourne l'index de la gare associée à l'arrêt.
     * Si l'arrêt désigne déjà une gare, retourne l'index lui-même ;
     * sinon, retourne l'index de la gare correspondante (stopId - stations().size()).
     *
     * @param stopId l'index d'arrêt
     * @return l'index de la gare associée
     */
    default int stationId(int stopId) {
        if (isStationId(stopId)) {
            return stopId;
        } else {
            return platforms().stationId(stopId - stations().size());
        }
    }

    /**
     * Retourne le nom du quai/voie correspondant à l'arrêt, ou null s'il s'agit d'une gare.
     *
     * @param stopId l'index d'arrêt
     * @return le nom du quai/voie, ou null si l'arrêt correspond à une gare
     */
    default String platformName(int stopId) {
        if (isStationId(stopId)) {
            return null;
        } else {
            return platforms().name(stopId - stations().size());
        }
    }
}
