package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Trips;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * BufferedTrips permet d’accéder à une table aplatie de courses.
 * La table est constituée de deux champs :
 *  • À l’index 0, ROUTE_ID (U16) qui représente l’index de la ligne associée à la course.
 *  • À l’index 1, DESTINATION_ID (U16) qui représente l’index de chaîne du nom de la destination finale.
 *
 * Auteurs : Benjamin (392901) et Jeremy (397366)
 */
public final class BufferedTrips implements Trips {

    private static final int ROUTE_ID_FIELD = 0;
    private static final int DESTINATION_FIELD = 1;

    private static final Structure TRIPS_STRUCTURE = new Structure(
            Structure.field(ROUTE_ID_FIELD, Structure.FieldType.U16),
            Structure.field(DESTINATION_FIELD, Structure.FieldType.U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Construit une instance de BufferedTrips à partir d’une table de chaînes et d’un ByteBuffer.
     * La capacité du ByteBuffer doit être un multiple de TRIPS_STRUCTURE.totalSize().
     *
     * @param stringTable la table des chaînes
     * @param buffer le ByteBuffer contenant les données aplaties des courses
     * @throws NullPointerException si stringTable ou buffer est null
     * @throws IllegalArgumentException si la capacité du buffer n'est pas un multiple de la taille de la structure
     */
    public BufferedTrips(List<String> stringTable, ByteBuffer buffer) {
        Objects.requireNonNull(stringTable);
        Objects.requireNonNull(buffer);
        this.stringTable = stringTable;
        this.buffer = new StructuredBuffer(TRIPS_STRUCTURE, buffer);
    }

    @Override
    public int size() {
        return buffer.size();
    }

    @Override
    public int routeId(int id) {
        return buffer.getU16(ROUTE_ID_FIELD, id);
    }

    @Override
    public String destination(int id) {
        int destId = buffer.getU16(DESTINATION_FIELD, id);
        return stringTable.get(destId);
    }
}
