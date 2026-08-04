package ch.epfl.rechor.timetable;

/**
 * Représente un ensemble de données indexées, c'est-à-dire gérées comme un tableau.
 * Chaque élément est identifié par un index.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface Indexed {

    /**
     * Retourne le nombre d'éléments dans cet ensemble de données indexées.
     *
     * @return la taille (nombre d'éléments)
     */
    int size();
}
