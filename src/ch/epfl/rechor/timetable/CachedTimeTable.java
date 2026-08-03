package ch.epfl.rechor.timetable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Horaire stockant en cache les courses et liaisons d'une seule date.
 * Les autres informations (gares, voies, lignes, changements) sont
 * déléguées directement à l'horaire sous-jacent.
 *
 * @author Benjamin (392901)
 * @author Jeremy   (397366)
 */
public final class CachedTimeTable implements TimeTable {
    /**
     * Horaire sous-jacent vers lequel sont déléguées toutes les requêtes
     * hors cache de date.
     */
    private final TimeTable underlying;

    /**
     * Date pour laquelle {@link #cachedTrips} et {@link #cachedConnections}
     * sont à jour. Null si aucun cache n’a encore été initialisé.
     */
    private LocalDate cachedDate;

    /**
     * Cache des liaisons actives pour la date {@link #cachedDate}.
     */
    private Connections cachedConnections;

    /**
     * Cache des courses actives pour la date {@link #cachedDate}.
     */
    private Trips cachedTrips;

    /**
     * Construit un horaire mis en cache à partir de l'horaire donné.
     *
     * @param timeTable l'horaire sous-jacent (non null)
     * @throws NullPointerException si {@code timeTable} est {@code null}
     */
    public CachedTimeTable(TimeTable timeTable) {
        this.underlying = Objects.requireNonNull(timeTable);
    }

    @Override
    public Stations stations() {
        return underlying.stations();
    }

    @Override
    public StationAliases stationAliases() {
        return underlying.stationAliases();
    }

    @Override
    public Platforms platforms() {
        return underlying.platforms();
    }

    @Override
    public Routes routes() {
        return underlying.routes();
    }

    @Override
    public Transfers transfers() {
        return underlying.transfers();
    }

    /**
     * Retourne les courses actives à la date donnée, en les
     * chargeant une seule fois dans le cache pour cette date.
     *
     * @param date la date du voyage (non null)
     * @return l'objet {@link Trips} pour cette date
     * @throws NullPointerException si {@code date} est {@code null}
     */
    @Override
    public Trips tripsFor(LocalDate date) {
        ensureCached(date);
        return cachedTrips;
    }

    /**
     * Retourne les liaisons actives à la date donnée, en les
     * chargeant une seule fois dans le cache pour cette date.
     *
     * @param date la date du voyage (non null)
     * @return l'objet {@link Connections} pour cette date
     * @throws NullPointerException si {@code date} est {@code null}
     */
    @Override
    public Connections connectionsFor(LocalDate date) {
        ensureCached(date);
        return cachedConnections;
    }

    /**
     * Vérifie si le cache est à jour pour la date donnée.
     * Sinon, recharge {@link #cachedTrips} et {@link #cachedConnections}
     * depuis l'horaire sous-jacent et met à jour {@link #cachedDate}.
     *
     * @param date la date à vérifier (non null)
     * @throws NullPointerException si {@code date} est {@code null}
     */
    private void ensureCached(LocalDate date) {
        Objects.requireNonNull(date);
        if (!date.equals(cachedDate)) {
            cachedTrips       = underlying.tripsFor(date);
            cachedConnections = underlying.connectionsFor(date);
            cachedDate        = date;
        }
    }
}
