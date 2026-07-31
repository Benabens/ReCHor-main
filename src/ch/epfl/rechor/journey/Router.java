package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Transfers;
import ch.epfl.rechor.timetable.Trips;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Routeur calculant, pour une date et une gare d'arrivée données,
 * le profil de tous les voyages optimaux depuis chacune des gares du réseau,
 * selon l'algorithme CSA (Connection Scan Algorithm) avec gestion de la charge utile
 * et optimisations de frontière.
 *
 * @author Benjamin (392901)
 * @author Jeremy   (397366)
 */
public record Router(TimeTable timeTable) {

    /**
     * Constructs a router using the given time table.
     *
     * @param timeTable table horaire non null
     * @throws NullPointerException si {@code timeTable} est null
     */
    public Router {
        Objects.requireNonNull(timeTable);
    }

    /**
     * Computes the profile of optimal journeys for the given date and
     * arrival station.
     *
     * @param date         jour du voyage
     * @param arrStationId identifiant de la gare d'arrivée
     * @return profil immuable pour toutes les gares de départ
     * @throws NullPointerException     si {@code date} est null
     * @throws IndexOutOfBoundsException si {@code arrStationId} est invalide
     */
    public Profile profile(LocalDate date, int arrStationId) {
        Objects.requireNonNull(date);
        final int nStations = timeTable.stations().size();
        if (arrStationId < 0 || arrStationId >= nStations) {
            throw new IndexOutOfBoundsException();
        }

        final Connections connections = timeTable.connectionsFor(date);
        final Trips       trips       = timeTable.tripsFor(date);
        final Transfers   transfers   = timeTable.transfers();


        final Profile.Builder builder = new Profile.Builder(timeTable, date, arrStationId);


        final ParetoFront.Builder[] stationB = new ParetoFront.Builder[nStations];
        for (int i = 0; i < nStations; i++) {
            stationB[i] = new ParetoFront.Builder();
            builder.setForStation(i, stationB[i]);
        }


        final ParetoFront.Builder[] tripB = new ParetoFront.Builder[trips.size()];
        for (int i = 0; i < trips.size(); i++) {
            tripB[i] = new ParetoFront.Builder();
            builder.setForTrip(i, tripB[i]);
        }


        final int[] walk = new int[nStations];
        for (int i = 0; i < nStations; i++) {
            walk[i] = -1;
        }
        final int destRange = transfers.arrivingAt(arrStationId);
        for (int c = PackedRange.startInclusive(destRange);
             c < PackedRange.endExclusive(destRange);
             c++) {
            int st = transfers.depStationId(c);
            int m  = transfers.minutes(c);
            int old = walk[st];
            walk[st] = old < 0 || m < old ? m : old;
        }

        // Builder de Pareto réutilisé d'une liaison à l'autre (évite une allocation par liaison)
        final ParetoFront.Builder f = new ParetoFront.Builder();

        for (int l = 0; l < connections.size(); l++) {
            final int connIdx = l;
            final int posL    = connections.tripPos(connIdx);

            final int depStop    = connections.depStopId(connIdx);
            final int arrStop    = connections.arrStopId(connIdx);
            final int depStation = timeTable.stationId(depStop);
            final int arrStation = timeTable.stationId(arrStop);
            final int depMins    = connections.depMins(connIdx);
            final int arrMins    = connections.arrMins(connIdx);
            final int tripId     = connections.tripId(connIdx);


            f.clear();


            final int w = walk[arrStation];
            if (w >= 0) {
                f.add(arrMins + w, 0, connIdx);
            }


            f.addAll(tripB[tripId]);


            stationB[arrStation].forEach(tuple -> {
                if (PackedCriteria.depMins(tuple) >= arrMins) {
                    f.add(
                            PackedCriteria.arrMins(tuple),
                            PackedCriteria.changes(tuple) + 1,
                            connIdx
                    );
                }
            });

            if (f.isEmpty()) {
                continue;
            }

            tripB[tripId].addAll(f);

            if (stationB[depStation].fullyDominates(f, depMins)) {
                continue;
            }

            final int depRange = transfers.arrivingAt(depStation);
            for (int c = PackedRange.startInclusive(depRange);
                 c < PackedRange.endExclusive(depRange);
                 c++) {
                final int startSt = transfers.depStationId(c);
                final int d       = depMins - transfers.minutes(c);

                f.forEach(tuple -> {
                    int arrCrit = PackedCriteria.arrMins(tuple);
                    int chg     = PackedCriteria.changes(tuple);
                    int alight  = PackedCriteria.payload(tuple);
                    int skipped = connections.tripPos(alight) - posL;
                    stationB[startSt].add(
                            PackedCriteria.pack(
                                    d,
                                    arrCrit,
                                    chg,
                                    Bits32_24_8.pack(connIdx, skipped)
                            )
                    );
                });
            }
        }

        return builder.build();
    }
}
