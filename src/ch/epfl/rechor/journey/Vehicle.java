package ch.epfl.rechor.journey;

import java.util.List;

/**
 * Représente les différents types de véhicules utilisés dans le projet.
 * Les véhicules disponibles sont : TRAM, METRO, TRAIN, BUS, FERRY, AERIAL_LIFT et FUNICULAR.
 * La constante ALL fournit une liste immuable contenant toutes les valeurs de l'énumération.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public enum Vehicle {
    TRAM,
    METRO,
    TRAIN,
    BUS,
    FERRY,
    AERIAL_LIFT,
    FUNICULAR;

    /**
     * Liste immuable contenant toutes les valeurs de l'énumération Vehicle.
     */
    public static final List<Vehicle> ALL = List.of(values());
}
