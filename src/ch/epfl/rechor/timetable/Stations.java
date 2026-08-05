package ch.epfl.rechor.timetable;

/**
 * Représente un ensemble de gares, permettant d'obtenir leur nom et leur position géographique
 * (longitude et latitude) à partir d'un index.
 * Tout index invalide (plus petit que 0 ou supérieur ou égal à la taille) doit provoquer une
 * {@link IndexOutOfBoundsException}.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface Stations extends Indexed {

    /**
     * Retourne le nom de la gare à l'index donné.
     *
     * @param id l'index de la gare
     * @return le nom de la gare
     * @throws IndexOutOfBoundsException si l'index est hors de [0..size()-1]
     */
    String name(int id);

    /**
     * Retourne la longitude, en degrés, de la gare à l'index donné.
     *
     * @param id l'index de la gare
     * @return la longitude de la gare, en degrés
     * @throws IndexOutOfBoundsException si l'index est hors de [0..size()-1]
     */
    double longitude(int id);

    /**
     * Retourne la latitude, en degrés, de la gare à l'index donné.
     *
     * @param id l'index de la gare
     * @return la latitude de la gare, en degrés
     * @throws IndexOutOfBoundsException si l'index est hors de [0..size()-1]
     */
    double latitude(int id);
}
