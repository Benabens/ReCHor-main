package ch.epfl.rechor.timetable;

/**
 * Représente un ensemble de courses effectuées sur une ligne de transport public.
 * Chaque course possède une destination finale.
 * Toutes les méthodes lèvent une {@link IndexOutOfBoundsException} si l'index fourni est hors de l'intervalle [0, size() - 1].
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface Trips extends Indexed {

    /**
     * Retourne l'index de la ligne (route) à laquelle appartient la course à l'index donné.
     *
     * @param id l'index de la course
     * @return l'index de la ligne correspondante
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int routeId(int id);

    /**
     * Retourne le nom de la destination finale pour la course à l'index donné.
     *
     * @param id l'index de la course
     * @return la destination finale de la course
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    String destination(int id);
}
