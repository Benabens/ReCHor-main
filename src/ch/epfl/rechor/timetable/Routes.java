package ch.epfl.rechor.timetable;

import ch.epfl.rechor.journey.Vehicle;

/**
 * Représente un ensemble de lignes de transport public indexées.
 * Chaque ligne est desservie par un type de véhicule et possède un nom.
 * Toutes les méthodes lèvent une {@link IndexOutOfBoundsException} si l'index fourni est hors de l'intervalle [0, size()-1].
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public interface Routes extends Indexed {

    /**
     * Retourne le type de véhicule desservant la ligne à l'index donné.
     *
     * @param id l'index de la ligne (0 .. size()-1)
     * @return le type de véhicule
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    Vehicle vehicle(int id);

    /**
     * Retourne le nom de la ligne à l'index donné.
     *
     * @param id l'index de la ligne (0 .. size()-1)
     * @return le nom de la ligne
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    String name(int id);
}
