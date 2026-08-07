package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Représente un horaire de transport public dont les données aplaties
 * sont stockées dans des fichiers. Cette classe implémente l'interface {@link TimeTable}
 * et permet de charger les différentes tables (gares, alias, voies/quais, lignes, changements)
 * ainsi que les données spécifiques à une date (courses, liaisons).
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public record FileTimeTable(
        Path directory,
        List<String> stringTable,
        Stations stations,
        StationAliases stationAliases,
        Platforms platforms,
        Routes routes,
        Transfers transfers
) implements TimeTable {

    /**
     * Constructeur compact : effectue une copie défensive immuable de la table des
     * chaînes afin de garantir l'immuabilité du record, quelle que soit la liste fournie.
     * ({@link List#copyOf} renvoie la même instance si elle est déjà immuable : coût nul
     * pour la fabrique {@link #in(Path)} qui passe déjà une liste immuable.)
     *
     * @throws NullPointerException si {@code stringTable} est null
     */
    public FileTimeTable {
        stringTable = List.copyOf(stringTable);
    }

    /**
     * Lit les différents fichiers dans le dossier donné, notamment strings.txt, pour construire
     * une table des chaînes immuable, ainsi que les binaires contenant les informations aplaties
     * (gares, aliases, voies/quais, lignes et changements).
     *
     * @param directory le chemin vers le répertoire contenant les fichiers horaires
     * @return une instance de {@code FileTimeTable} correspondant aux données chargées
     * @throws IOException en cas de problème de lecture ou d'accès aux fichiers
     */
    public static TimeTable in(Path directory) throws IOException {
        List<String> rawStrings = Files.readAllLines(directory.resolve("strings.txt"), StandardCharsets.ISO_8859_1);
        List<String> stringTable = List.copyOf(rawStrings);

        ByteBuffer stationsBuffer       = mapFile(directory.resolve("stations.bin"));
        ByteBuffer aliasesBuffer        = mapFile(directory.resolve("station-aliases.bin"));
        ByteBuffer platformsBuffer      = mapFile(directory.resolve("platforms.bin"));
        ByteBuffer routesBuffer         = mapFile(directory.resolve("routes.bin"));
        ByteBuffer transfersBuffer      = mapFile(directory.resolve("transfers.bin"));

        Stations stations               = new BufferedStations(stringTable, stationsBuffer);
        StationAliases stationAliases   = new BufferedStationAliases(stringTable, aliasesBuffer);
        Platforms platforms             = new BufferedPlatforms(stringTable, platformsBuffer);
        Routes routes                   = new BufferedRoutes(stringTable, routesBuffer);
        Transfers transfers             = new BufferedTransfers(transfersBuffer);

        return new FileTimeTable(directory, stringTable, stations, stationAliases, platforms, routes, transfers);
    }

    /**
     * Retourne les liaisons correspondant à la date spécifiée. Les fichiers connections.bin
     * et connections-succ.bin sont chargés, puis mappés en mémoire. Les exceptions de type
     * {@link IOException} sont converties en {@link UncheckedIOException}.
     *
     * @param date la date pour laquelle on désire obtenir les liaisons
     * @return un objet {@link Connections} permettant d'accéder aux liaisons du jour
     * @throws UncheckedIOException en cas de problème d'E/S lors du chargement des fichiers
     */
    @Override
    public Connections connectionsFor(LocalDate date) {
        try {
            Path dateDir = directory.resolve(date.toString());
            ByteBuffer connBuffer = mapFile(dateDir.resolve("connections.bin"));
            ByteBuffer succBuffer = mapFile(dateDir.resolve("connections-succ.bin"));
            return new BufferedConnections(connBuffer, succBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Retourne les courses (trips) actives pour la date spécifiée. Le fichier trips.bin
     * est chargé et mappé en mémoire. Les exceptions de type {@link IOException} sont
     * converties en {@link UncheckedIOException}.
     *
     * @param date la date pour laquelle on désire obtenir les courses
     * @return un objet {@link Trips} permettant d'accéder aux courses du jour
     * @throws UncheckedIOException en cas de problème d'E/S lors du chargement du fichier
     */
    @Override
    public Trips tripsFor(LocalDate date) {
        try {
            Path dateDir = directory.resolve(date.toString());
            ByteBuffer tripsBuffer = mapFile(dateDir.resolve("trips.bin"));
            return new BufferedTrips(stringTable, tripsBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Mappe un fichier binaire en mémoire pour lecture uniquement.
     * Ce mappage permet de lire efficacement le contenu sans le charger entièrement.
     *
     * @param filePath le chemin vers le fichier à mapper
     * @return le contenu mappé en mémoire comme {@link ByteBuffer}
     * @throws IOException si une erreur survient lors de l'ouverture ou du mappage
     */
    private static ByteBuffer mapFile(Path filePath) throws IOException {
        try (FileChannel channel = FileChannel.open(filePath)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }
}
