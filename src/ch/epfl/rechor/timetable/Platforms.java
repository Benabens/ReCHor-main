package ch.epfl.rechor.timetable;

/**
 * Représente un ensemble de voies/quais indexés.
 * Chaque voie est associée à une gare (station) et possède un nom qui peut être vide.
 * Toutes les méthodes lèvent une {@link IndexOutOfBoundsException} si l'index fourni est hors de l'intervalle [0, size() - 1].
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface Platforms extends Indexed {

    /**
     * Retourne le nom de la voie/quai à l'index donné, ou une chaîne vide.
     *
     * @param id l'index de la voie/quai (0..size()-1)
     * @return le nom de la voie/quai
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    String name(int id);

    /**
     * Retourne l'index de la gare associée à cette voie/quai.
     *
     * @param id l'index de la voie/quai (0..size()-1)
     * @return l'index de la gare associée
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int stationId(int id);
}
