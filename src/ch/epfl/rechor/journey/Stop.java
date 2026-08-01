package ch.epfl.rechor.journey;

import ch.epfl.rechor.Preconditions;
import java.util.Objects;

/**
 * Représente un arrêt dans le réseau de transport.
 * Ce record contient le nom de l'arrêt, le nom de la plateforme, la longitude et la latitude.
 * Les coordonnées géographiques doivent être dans les plages valides :
 * - la longitude doit être comprise entre -180 et 180,
 * - la latitude doit être comprise entre -90 et 90.
 *
 * @param name le nom de l'arrêt (ne doit pas être null)
 * @param platformName le nom de la plateforme
 * @param longitude la longitude de l'arrêt (doit être entre -180 et 180)
 * @param latitude la latitude de l'arrêt (doit être entre -90 et 90)
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public record Stop(String name, String platformName, double longitude, double latitude) {

    /**
     * Constructeur compact du record Stop.
     * Vérifie que le nom n'est pas null et que les coordonnées géographiques sont valides.
     *
     * @param name le nom de l'arrêt, ne doit pas être null
     * @param platformName le nom de la plateforme
     * @param longitude la longitude de l'arrêt, doit être entre -180 et 180
     * @param latitude la latitude de l'arrêt, doit être entre -90 et 90
     * @throws NullPointerException si le nom est null
     * @throws IllegalArgumentException si la longitude ou la latitude ne sont pas dans les plages valides
     */
    public Stop {
        Objects.requireNonNull(name);
        Preconditions.checkArgument(((-180 <= longitude) && (longitude <= 180))
                && ((-90 <= latitude) && (latitude <= 90)));
    }
}
