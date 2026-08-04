package ch.epfl.rechor.timetable;

/**
 * Représente un ensemble de liaisons, triées par heure de départ décroissante.
 * Chaque liaison est caractérisée par un arrêt et une heure de départ, un arrêt et une heure d'arrivée,
 * et appartient à une course.
 * Toutes les méthodes lèvent une {@link IndexOutOfBoundsException} si l'index fourni est hors de l'intervalle [0, size() - 1].
 * Les index d'arrêt (retournés par {@code depStopId} et {@code arrStopId}) peuvent désigner soit une gare, soit un quai/voie,
 * selon qu'ils sont inférieurs ou supérieurs au nombre total de gares.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface Connections extends Indexed {

    /**
     * Retourne l'index de l'arrêt de départ de la liaison (gare ou quai/voie).
     *
     * @param id l'index de la liaison
     * @return l'index de l'arrêt de départ
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int depStopId(int id);

    /**
     * Retourne l'heure de départ de la liaison, en minutes après minuit.
     *
     * @param id l'index de la liaison
     * @return l'heure de départ (en minutes après minuit)
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int depMins(int id);

    /**
     * Retourne l'index de l'arrêt d'arrivée (gare ou quai/voie).
     *
     * @param id l'index de la liaison
     * @return l'index de l'arrêt d'arrivée
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int arrStopId(int id);

    /**
     * Retourne l'heure d'arrivée de la liaison, en minutes après minuit.
     *
     * @param id l'index de la liaison
     * @return l'heure d'arrivée (en minutes après minuit)
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int arrMins(int id);

    /**
     * Retourne l'index de la course à laquelle appartient cette liaison.
     *
     * @param id l'index de la liaison
     * @return l'index de la course
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int tripId(int id);

    /**
     * Retourne la position de la liaison dans sa course (0 pour la première liaison).
     *
     * @param id l'index de la liaison
     * @return la position de la liaison dans sa course
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int tripPos(int id);

    /**
     * Retourne l'index de la liaison suivante dans la même course, ou l'index de la première liaison
     * de cette course si la liaison courante est la dernière.
     *
     * @param id l'index de la liaison
     * @return l'index de la liaison suivante
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    int nextConnectionId(int id);
}
