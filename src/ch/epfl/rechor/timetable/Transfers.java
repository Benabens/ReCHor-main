package ch.epfl.rechor.timetable;

import java.util.NoSuchElementException;

/**
 * Représente un ensemble de changements.
 * Un changement n'existe qu'entre des gares (pas entre voies/quais).
 * Toutes les méthodes lèvent une {@link IndexOutOfBoundsException} si l'index fourni est hors de l'intervalle [0, size()-1],
 * et la méthode {@code minutesBetween} peut lever une {@link NoSuchElementException} si aucun changement n'existe entre les deux gares.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface Transfers extends Indexed {

    /**
     * Retourne l'index de la gare de départ du changement à l'index donné.
     *
     * @param id l'index du changement
     * @return l'index de la gare de départ
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int depStationId(int id);

    /**
     * Retourne la durée, en minutes, du changement à l'index donné.
     *
     * @param id l'index du changement
     * @return la durée en minutes du changement
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int minutes(int id);

    /**
     * Retourne l'intervalle empaqueté des index des changements qui arrivent à la gare dont l'index est donné.
     *
     * @param stationId l'index de la gare d'arrivée
     * @return l'intervalle empaqueté des changements arrivant à cette gare
     * @throws IndexOutOfBoundsException si stationId est invalide
     */
    int arrivingAt(int stationId);

    /**
     * Retourne la durée (en minutes) du changement entre deux gares.
     *
     * @param depStationId l'index de la gare de départ
     * @param arrStationId l'index de la gare d'arrivée
     * @return la durée en minutes du changement entre les deux gares
     * @throws IndexOutOfBoundsException si l'un des deux index est invalide
     * @throws NoSuchElementException si aucun changement n'existe entre ces deux gares
     */
    int minutesBetween(int depStationId, int arrStationId);
}
