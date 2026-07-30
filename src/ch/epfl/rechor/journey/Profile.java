package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Trips;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Représente un profil qui combine un horaire, une date, l'identifiant d'une gare d'arrivée
 * et la liste des fronts de Pareto associés à toutes les gares.
 * Ce profil est utilisé pour extraire les voyages optimaux vers une destination donnée.
 *
 * Ce record est immutable. Pour construire une instance, utilisez le bâtisseur {@code Profile.Builder}.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public record Profile(TimeTable timeTable,
                      LocalDate date,
                      int arrStationId,
                      List<ParetoFront> stationFront) {

    /**
     * Constructeur compact de {@code Profile}.
     * La liste des fronts de Pareto est copiée pour garantir l'immuabilité.
     *
     * @param timeTable    l'horaire utilisé pour ce profil
     * @param date         la date à laquelle le profil s'applique
     * @param arrStationId l'identifiant de la gare d'arrivée
     * @param stationFront la liste des fronts de Pareto, indexée par identifiant de gare
     * @throws NullPointerException si {@code timeTable}, {@code date} ou {@code stationFront} est null
     */
    public Profile {
        Objects.requireNonNull(timeTable);
        Objects.requireNonNull(date);
        Objects.requireNonNull(stationFront);
        stationFront = List.copyOf(stationFront);
    }

    /**
     * Retourne les connexions pour la date du profil.
     *
     * @return les connexions du jour pour l'horaire associé à ce profil
     */
    public Connections connections() {
        return timeTable.connectionsFor(date);
    }

    /**
     * Retourne les courses pour la date du profil.
     *
     * @return les courses du jour pour l'horaire associé à ce profil
     */
    public Trips trips() {
        return timeTable.tripsFor(date);
    }

    /**
     * Retourne le front de Pareto associé à la gare spécifiée.
     *
     * @param stationId l'identifiant de la gare
     * @return le front de Pareto pour la gare indiquée
     * @throws IndexOutOfBoundsException si {@code stationId} est hors limites
     */
    public ParetoFront forStation(int stationId) {
        return stationFront.get(stationId);
    }

    /**
     * Bâtisseur permettant de construire progressivement une instance de {@code Profile}.
     * Ce bâtisseur permet d'assembler les fronts de Pareto pour chaque gare, ainsi que
     * pour les courses si nécessaire. Une fois toutes les informations rassemblées,
     * l'appel à {@link #build()} retourne un profil immutable.
     *
     * (Les bâtisseurs pour les courses sont stockés dans {@code tripBuilders} pour une éventuelle utilisation ultérieure.)
     *
     * @author Benjamin (392901)
     * @author Jeremy (397366)
     */
    public static final class Builder {

        private final TimeTable timeTable;
        private final LocalDate date;
        private final int arrStationId;
        private final ParetoFront.Builder[] stationBuilders;
        private final ParetoFront.Builder[] tripBuilders;

        /**
         * Construit un bâtisseur de {@code Profile} pour l'horaire, la date
         * et la gare d'arrivée spécifiés.
         *
         * @param timeTable    l'horaire à utiliser pour ce bâtisseur
         * @param date         la date considérée
         * @param arrStationId l'identifiant de la gare d'arrivée
         * @throws NullPointerException si {@code timeTable} ou {@code date} est null
         */
        public Builder(TimeTable timeTable, LocalDate date, int arrStationId) {
            this.timeTable = Objects.requireNonNull(timeTable);
            this.date = Objects.requireNonNull(date);
            this.arrStationId = arrStationId;
            int nbStations = timeTable.stations().size();
            stationBuilders = new ParetoFront.Builder[nbStations];
            int nbTrips = timeTable.tripsFor(date).size();
            tripBuilders = new ParetoFront.Builder[nbTrips];
        }

        /**
         * Retourne le bâtisseur de front de Pareto associé à la gare spécifiée.
         *
         * @param stationId l'identifiant de la gare
         * @return le bâtisseur de front de Pareto pour la gare donnée
         * @throws IndexOutOfBoundsException si {@code stationId} est hors limites
         */
        public ParetoFront.Builder forStation(int stationId) {
            if (stationId < 0 || stationId >= stationBuilders.length) {
                throw new IndexOutOfBoundsException();
            }
            return stationBuilders[stationId];
        }

        /**
         * Associe un bâtisseur de front de Pareto à une gare.
         *
         * @param stationId l'identifiant de la gare
         * @param builder   le bâtisseur de front de Pareto à associer
         * @throws IndexOutOfBoundsException si {@code stationId} est hors limites
         */
        public void setForStation(int stationId, ParetoFront.Builder builder) {
            if (stationId < 0 || stationId >= stationBuilders.length) {
                throw new IndexOutOfBoundsException();
            }
            stationBuilders[stationId] = builder;
        }

        /**
         * Retourne le bâtisseur de front de Pareto associé à un trajet.
         *
         * @param tripId l'identifiant du trajet
         * @return le bâtisseur de front de Pareto pour le trajet spécifié
         * @throws IndexOutOfBoundsException si {@code tripId} est hors limites
         */
        public ParetoFront.Builder forTrip(int tripId) {
            if (tripId < 0 || tripId >= tripBuilders.length) {
                throw new IndexOutOfBoundsException();
            }
            return tripBuilders[tripId];
        }

        /**
         * Associe un bâtisseur de front de Pareto à un trajet.
         *
         * @param tripId  l'identifiant du trajet
         * @param builder le bâtisseur de front de Pareto à associer
         * @throws IndexOutOfBoundsException si {@code tripId} est hors limites
         */
        public void setForTrip(int tripId, ParetoFront.Builder builder) {
            if (tripId < 0 || tripId >= tripBuilders.length) {
                throw new IndexOutOfBoundsException();
            }
            tripBuilders[tripId] = builder;
        }

        /**
         * Construit l'instance finale de {@code Profile} à partir des informations accumulées.
         * Pour chaque gare, si aucun bâtisseur n'a été défini, on utilise {@code ParetoFront.EMPTY}.
         *
         * @return une instance immutable de {@code Profile} construite avec les fronts de Pareto
         */
        public Profile build() {
            List<ParetoFront> stationFronts = new ArrayList<>(stationBuilders.length);
            for (int i = 0; i < stationBuilders.length; i++) {
                if (stationBuilders[i] == null) {
                    stationFronts.add(ParetoFront.EMPTY);
                } else {
                    stationFronts.add(stationBuilders[i].build());
                }
            }
            return new Profile(timeTable, date, arrStationId, stationFronts);
        }
    }
}
