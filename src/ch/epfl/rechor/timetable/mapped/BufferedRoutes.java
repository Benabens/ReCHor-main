package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.journey.Vehicle;
import ch.epfl.rechor.timetable.Routes;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * BufferedRoutes permet d’accéder à une table aplatie de lignes de transport public.
 * La table est constituée de deux champs :
 * - À l’index 0, NAME_ID (U16) qui représente l’index de la chaîne correspondant au nom de la ligne.
 * - À l’index 1, KIND (U8) qui représente le type de véhicule desservant la ligne (valeur comprise entre 0 et 6).
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class BufferedRoutes implements Routes {

    private static final int NAME_FIELD = 0;
    private static final int KIND_FIELD = 1;

    private static final Structure ROUTES_STRUCTURE = new Structure(
            Structure.field(NAME_FIELD, Structure.FieldType.U16),
            Structure.field(KIND_FIELD, Structure.FieldType.U8)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Construit une instance de BufferedRoutes à partir d’une table de chaînes et d’un ByteBuffer.
     * La capacité du ByteBuffer doit être un multiple de ROUTES_STRUCTURE.totalSize().
     *
     * @param stringTable la table des chaînes
     * @param buffer le ByteBuffer contenant les données aplaties des lignes
     * @throws NullPointerException si stringTable ou buffer est null
     * @throws IllegalArgumentException si la capacité du buffer n'est pas un multiple de la taille de la structure
     */
    public BufferedRoutes(List<String> stringTable, ByteBuffer buffer) {
        Objects.requireNonNull(stringTable);
        Objects.requireNonNull(buffer);
        this.stringTable = stringTable;
        this.buffer = new StructuredBuffer(ROUTES_STRUCTURE, buffer);
    }

    @Override
    public int size() {
        return buffer.size();
    }

    @Override
    public String name(int id) {
        int nameId = buffer.getU16(NAME_FIELD, id);
        return stringTable.get(nameId);
    }

    @Override
    public Vehicle vehicle(int id) {
        int kind = buffer.getU8(KIND_FIELD, id);
        return Vehicle.ALL.get(kind);
    }
}
