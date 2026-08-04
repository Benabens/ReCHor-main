package ch.epfl.rechor.timetable;

/**
 * Représente un ensemble de noms alternatifs pour les gares.
 * Chaque alias correspond à un nom alternatif (par exemple "Losanna")
 * et à la gare officielle (par exemple "Lausanne") à laquelle il renvoie.
 *
 * Toutes les méthodes lèvent une {@link IndexOutOfBoundsException}
 * si l'index fourni est hors de l'intervalle [0, size() - 1].
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface StationAliases extends Indexed {

    /**
     * Retourne le nom alternatif de la gare à l'index donné.
     *
     * @param id l'index de l'alias
     * @return le nom alternatif
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    String alias(int id);

    /**
     * Retourne le nom officiel de la gare auquel correspond cet alias.
     *
     * @param id l'index de l'alias
     * @return le nom officiel de la gare
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    String stationName(int id);
}
