package ch.epfl.rechor.gui;

import ch.epfl.rechor.journey.Vehicle;
import javafx.scene.image.Image;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fournit l'accès aux icônes des différents véhicules en les chargeant
 * une seule fois et en les mettant en cache.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class VehicleIcons {
    /**
     * Cache des images des icônes, indexées par type de véhicule.
     * Permet de ne charger chaque image qu’une seule fois.
     */
    private static final Map<Vehicle, Image> CACHE = new EnumMap<>(Vehicle.class);

    /**
     * Constructeur privé pour empêcher l'instanciation de cette classe utilitaire.
     *
     * @throws AssertionError toujours levée pour empêcher la création d'instance
     */
    private VehicleIcons() {
        throw new AssertionError();
    }

    /**
     * Retourne l'icône correspondant au véhicule donné. L'image est chargée
     * depuis les ressources lors de la première invocation pour ce véhicule,
     * puis conservée dans un cache pour les appels suivants.
     *
     * @param vehicle le type de véhicule (non null)
     * @return l'image JavaFX associée à ce véhicule
     * @throws NullPointerException si {@code vehicle} est null
     */
    public static Image iconFor(Vehicle vehicle) {
        Objects.requireNonNull(vehicle);
        return CACHE.computeIfAbsent(vehicle,
                v -> new Image(v.name() + ".png")
        );
    }
}
