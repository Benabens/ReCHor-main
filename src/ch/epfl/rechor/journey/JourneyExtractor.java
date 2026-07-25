package ch.epfl.rechor.journey;

import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fournit une méthode statique permettant d'extraire tous les voyages optimaux
 * pour un profil donné et une gare de départ spécifiée.
 *
 * Les voyages sont reconstruits en parcourant la frontière de Pareto de la gare de départ.
 * Chaque critère empaqueté donne lieu à une séquence d'étapes (legs),
 * potentiellement constituée d'un segment de marche et d'un segment en transport.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class JourneyExtractor {

    /**
     * Constructeur privé pour empêcher toute instanciation de la classe.
     */
    private JourneyExtractor() {
        throw new UnsupportedOperationException();
    }

    /**
     * Retourne la liste des voyages optimaux pour le profil et la gare de départ donnés.
     * Les voyages sont triés par heure de départ, puis par heure d'arrivée.
     *
     * @param profile      le profil (horaire, date, fronts de Pareto)
     * @param depStationId l'identifiant de la gare de départ
     * @return la liste des voyages optimaux correspondant au profil et à la gare de départ
     */
    public static List<Journey> journeys(Profile profile, int depStationId) {
        List<Journey> journeyList = new ArrayList<>();
        profile.forStation(depStationId).forEach(initCriteria -> {
            journeyList.add(new Journey(
                    buildLegSequence(profile, depStationId, initCriteria)
            ));
        });
        journeyList.sort(Comparator
                .comparing(Journey::depTime)
                .thenComparing(Journey::arrTime));
        return journeyList;
    }

    /**
     * Construit la séquence d'étapes d'un voyage à partir d'un critère empaqueté.
     *
     * @param profile      le profil contenant l'horaire et la date
     * @param depStationId l'identifiant de la gare de départ
     * @param initCriteria le critère empaqueté décrivant l'heure d'arrivée finale et le nombre de changements
     * @return la séquence des étapes du voyage
     */
    private static List<Journey.Leg> buildLegSequence(
            Profile profile, int depStationId, long initCriteria) {
        List<Journey.Leg> legs = new ArrayList<>();
        TimeTable tt       = profile.timeTable();
        Connections conn   = profile.connections();
        Trips trips        = profile.trips();
        Routes routes      = tt.routes();
        LocalDate date     = profile.date();

        int targetArr   = PackedCriteria.arrMins(initCriteria);
        int payload     = PackedCriteria.payload(initCriteria);
        int currConn    = payload >>> 8;
        int changes     = PackedCriteria.changes(initCriteria);
        int currStopId  = depStationId;
        int currArrMins = 0;

        // marche initiale si nécessaire
        int firstDepStop = conn.depStopId(currConn);
        if (tt.stationId(firstDepStop) != currStopId) {
            legs.add(createFootLeg(
                    date, conn.depMins(currConn), false,
                    currStopId, firstDepStop, tt
            ));
        }

        // boucle sur les changements restants
        for (int rem = changes; rem >= 0; rem--) {
            ParetoFront pf = profile.forStation(tt.stationId(currStopId));
            long crit      = pf.get(targetArr, rem);
            payload        = PackedCriteria.payload(crit);
            currConn       = payload >>> 8;
            int skipped    = payload & 0xFF;
            int depStopId  = conn.depStopId(currConn);

            // marche avant de prendre le train si nécessaire
            if (!legs.isEmpty() &&
                    legs.get(legs.size() - 1) instanceof Journey.Leg.Transport) {
                legs.add(createFootLeg(
                        date, currArrMins, true,
                        currStopId, depStopId, tt
                ));
            }

            // arrêts intermédiaires
            List<Journey.Leg.IntermediateStop> inters = new ArrayList<>();
            int initDepMins = conn.depMins(currConn);
            int c = currConn;
            for (int i = 0; i < skipped; i++) {
                int arrMin = conn.arrMins(c);
                c = conn.nextConnectionId(c);
                inters.add(createIntermediateStop(
                        date, tt, conn, c, arrMin
                ));
            }

            // segment de transport
            legs.add(createTransportLeg(
                    date, tt, conn, trips, routes,
                    currConn, c, initDepMins, inters
            ));

            currConn    = c;
            currStopId  = conn.arrStopId(c);
            currArrMins = conn.arrMins(c);
        }

        // marche finale si la destination n'est pas atteinte
        if (tt.stationId(currStopId) != profile.arrStationId()) {
            legs.add(createFootLeg(
                    date, currArrMins, true,
                    currStopId, profile.arrStationId(), tt
            ));
        }

        return legs;
    }

    // ---- méthodes auxiliaires ----

    /**
     * Convertit un nombre de minutes depuis minuit en LocalDateTime,
     * en gérant les dépassements de 24 heures.
     *
     * @param date la date de référence
     * @param mins minutes depuis minuit (>= 0, peut dépasser 1440)
     * @return l'instant correspondant
     */
    private static LocalDateTime toDateTime(LocalDate date, int mins) {
        return date.atStartOfDay().plusMinutes(mins);
    }

    /**
     * Crée un Stop à partir d'un arrêt (gare ou quai) identifié par stopId.
     *
     * @param tt     la table des horaires
     * @param stopId identifiant d'arrêt (gare ou quai)
     * @return le Stop correspondant
     */
    private static Stop createStop(TimeTable tt, int stopId) {
        int stationIndex = tt.stationId(stopId);
        return new Stop(
                tt.stations().name(stationIndex),
                tt.platformName(stopId),
                tt.stations().longitude(stationIndex),
                tt.stations().latitude(stationIndex)
        );
    }

    /**
     * Crée un segment de marche (Foot) entre deux arrêts.
     *
     * @param date        date du voyage
     * @param refMins     minute de référence (si isDeparture départ, sinon arrivée)
     * @param isDeparture true si refMins est l'heure de départ
     * @param depStopId   arrêt de départ
     * @param arrStopId   arrêt d'arrivée
     * @param tt          la table des horaires
     * @return le leg à pied
     */
    private static Journey.Leg.Foot createFootLeg(
            LocalDate date,
            int refMins,
            boolean isDeparture,
            int depStopId,
            int arrStopId,
            TimeTable tt) {
        int depStation = tt.stationId(depStopId);
        int arrStation = tt.stationId(arrStopId);
        int range      = tt.transfers().arrivingAt(arrStation);

        for (int i = PackedRange.startInclusive(range);
             i < PackedRange.endExclusive(range); i++) {
            if (tt.transfers().depStationId(i) == depStation) {
                int walk  = tt.transfers().minutes(i);
                int start = isDeparture ? refMins : refMins - walk;
                int end   = isDeparture ? refMins + walk : refMins;
                return new Journey.Leg.Foot(
                        createStop(tt, depStopId),
                        toDateTime(date, start),
                        createStop(tt, arrStopId),
                        toDateTime(date, end)
                );
            }
        }
        throw new IllegalStateException(
                "Aucun transfert de " + depStation + " vers " + arrStation
        );
    }

    /**
     * Crée un arrêt intermédiaire pour un segment de transport.
     *
     * @param date        date du voyage
     * @param tt          la table des horaires
     * @param conn        connexions
     * @param connId      identifiant de la connexion
     * @param prevArrMins minute d'arrivée précédente
     * @return le leg intermédiaire
     */
    private static Journey.Leg.IntermediateStop createIntermediateStop(
            LocalDate date,
            TimeTable tt,
            Connections conn,
            int connId,
            int prevArrMins) {
        return new Journey.Leg.IntermediateStop(
                createStop(tt, conn.depStopId(connId)),
                toDateTime(date, prevArrMins),
                toDateTime(date, conn.depMins(connId))
        );
    }

    /**
     * Crée un segment de transport (Transport) complet entre deux connexions.
     *
     * @param date         date du voyage
     * @param tt           table des horaires
     * @param conn         connexions du jour
     * @param trips        courses du jour
     * @param routes       routes (nom + véhicule)
     * @param firstConnId  première connexion du segment
     * @param lastConnId   dernière connexion du segment
     * @param initDepMin   minute de départ initiale
     * @param interStops   arrêts intermédiaires
     * @return le leg de transport
     */
    private static Journey.Leg.Transport createTransportLeg(
            LocalDate date,
            TimeTable tt,
            Connections conn,
            Trips trips,
            Routes routes,
            int firstConnId,
            int lastConnId,
            int initDepMin,
            List<Journey.Leg.IntermediateStop> interStops) {
        int tripIdx = conn.tripId(firstConnId);
        int depStop = conn.depStopId(firstConnId);
        int arrStop = conn.arrStopId(lastConnId);
        int routeId = trips.routeId(tripIdx);

        return new Journey.Leg.Transport(
                createStop(tt, depStop),
                toDateTime(date, initDepMin),
                createStop(tt, arrStop),
                toDateTime(date, conn.arrMins(lastConnId)),
                List.copyOf(interStops),
                routes.vehicle(routeId),
                routes.name(routeId),
                trips.destination(tripIdx)
        );
    }
}
