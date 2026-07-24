package ch.epfl.rechor.journey;

import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.Duration;
import ch.epfl.rechor.Preconditions;

/**
 * Représente un voyage constitué d'une séquence d'étapes (legs).
 * Le voyage doit comporter au moins une étape et les étapes consécutives doivent être cohérentes :
 * l'arrêt d'arrivée d'une étape doit être égal à l'arrêt de départ de l'étape suivante.
 * De plus, deux étapes consécutives ne peuvent pas être toutes deux à pied ni toutes deux en transport.
 * La liste des étapes est rendue immuable.
 *
 * @param legs la liste des étapes du voyage
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public record Journey(List<Leg> legs) {

    /**
     * Vérifie que la liste n'est pas vide et que les étapes sont cohérentes,
     * puis en crée une copie immuable.
     *
     * @throws IllegalArgumentException si la liste est vide ou incohérente
     */
    public Journey {
        Preconditions.checkArgument(!legs.isEmpty());
        checkCoherence(legs);
        legs = List.copyOf(legs);
    }

    /**
     * @return l'arrêt de départ du voyage (début de la première étape)
     */
    public Stop depStop() {
        return legs.get(0).depStop();
    }

    /**
     * @return l'heure de départ du voyage (début de la première étape)
     */
    public LocalDateTime depTime() {
        return legs.get(0).depTime();
    }

    /**
     * @return l'arrêt d'arrivée du voyage (fin de la dernière étape)
     */
    public Stop arrStop() {
        return legs.get(legs.size() - 1).arrStop();
    }

    /**
     * @return l'heure d'arrivée du voyage (fin de la dernière étape)
     */
    public LocalDateTime arrTime() {
        return legs.get(legs.size() - 1).arrTime();
    }

    /**
     * @return la durée totale du voyage
     */
    public Duration duration() {
        return Duration.between(depTime(), arrTime());
    }

    /**
     * Étape d'un itinéraire — à pied ou en transport public.
     * Cette interface est scellée pour n'autoriser que ces deux types d'étapes.
     *
     * @author Benjamin (392901)
     * @author Jeremy (397366)
     */
    public sealed interface Leg
            permits Leg.Foot, Leg.Transport {

        Stop depStop();
        LocalDateTime depTime();
        Stop arrStop();
        LocalDateTime arrTime();
        List<IntermediateStop> intermediateStops();

        /**
         * @return la durée de cette étape
         */
        default Duration duration() {
            return Duration.between(depTime(), arrTime());
        }

        /**
         * Arrêt intermédiaire d'un segment de transport.
         */
        public record IntermediateStop(
                Stop stop,
                LocalDateTime arrTime,
                LocalDateTime depTime
        ) {
            public IntermediateStop {
                Objects.requireNonNull(stop);
                Objects.requireNonNull(arrTime);
                Objects.requireNonNull(depTime);
                Preconditions.checkArgument(!arrTime.isAfter(depTime));
            }
        }

        /**
         * Trajet en transport public, avec arrêts intermédiaires possibles.
         */
        public record Transport(
                Stop depStop,
                LocalDateTime depTime,
                Stop arrStop,
                LocalDateTime arrTime,
                List<IntermediateStop> intermediateStops,
                Vehicle vehicle,
                String route,
                String destination
        ) implements Leg {
            public Transport {
                Objects.requireNonNull(depStop);
                Objects.requireNonNull(depTime);
                Objects.requireNonNull(arrStop);
                Objects.requireNonNull(arrTime);
                Objects.requireNonNull(intermediateStops);
                Objects.requireNonNull(vehicle);
                Objects.requireNonNull(route);
                Objects.requireNonNull(destination);
                Preconditions.checkArgument(!depTime.isAfter(arrTime));
                intermediateStops = List.copyOf(intermediateStops);
            }
        }

        /**
         * Étape à pied entre deux arrêts.
         */
        public record Foot(
                Stop depStop,
                LocalDateTime depTime,
                Stop arrStop,
                LocalDateTime arrTime
        ) implements Leg {
            public Foot {
                Objects.requireNonNull(depStop);
                Objects.requireNonNull(depTime);
                Objects.requireNonNull(arrStop);
                Objects.requireNonNull(arrTime);
                Preconditions.checkArgument(!depTime.isAfter(arrTime));
            }
            @Override public List<IntermediateStop> intermediateStops() { return List.of(); }

            /**
             * @return true si cet épisode de marche est un transfert
             */
            public boolean isTransfer() {
                return depStop.name().equals(arrStop.name())
                        && depStop.longitude() == arrStop.longitude()
                        && depStop.latitude()  == arrStop.latitude();
            }
        }
    }

    /**
     * Vérifie la cohérence des étapes :
     * - l'arrêt d'arrivée doit correspondre à l'arrêt de départ suivant,
     * - les types Foot / Transport alternent.
     */
    private static void checkCoherence(List<Leg> legs) {
        for (int i = 0; i < legs.size() - 1; i++) {
            Leg current = legs.get(i);
            Leg next    = legs.get(i + 1);
            Preconditions.checkArgument(current.arrStop().equals(next.depStop()));
            Preconditions.checkArgument(!(current instanceof Leg.Foot && next instanceof Leg.Foot));
            Preconditions.checkArgument(!(current instanceof Leg.Transport && next instanceof Leg.Transport));
        }
    }
}
