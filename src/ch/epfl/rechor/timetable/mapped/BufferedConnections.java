package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.timetable.Connections;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import ch.epfl.rechor.Preconditions;

/**
 * BufferedConnections permet d’accéder à une table aplatie de liaisons et à une table auxiliaire
 * contenant l’index de la liaison suivante pour chaque liaison.
 *
 * La table principale contient cinq champs :
 *  • À l’index 0, DEP_STOP_ID (U16) : l’index de l’arrêt de départ.
 *  • À l’index 1, DEP_MINUTES (U16) : l’heure de départ, en minutes après minuit.
 *  • À l’index 2, ARR_STOP_ID (U16) : l’index de l’arrêt d’arrivée.
 *  • À l’index 3, ARR_MINUTES (U16) : l’heure d’arrivée, en minutes après minuit.
 *  • À l’index 4, TRIP_POS_ID (S32) : une valeur empaquetée dont les 24 bits de poids fort représentent
 *     l’index de la course et les 8 bits de poids faible la position de la liaison dans la course.
 *
 * La table auxiliaire (suivante) est un tableau d’entiers (IntBuffer) qui contient, pour chaque liaison,
 * l’index de la liaison suivante dans la même course.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class BufferedConnections implements Connections {

    private static final int DEP_STOP_ID_FIELD = 0;
    private static final int DEP_MINUTES_FIELD = 1;
    private static final int ARR_STOP_ID_FIELD = 2;
    private static final int ARR_MINUTES_FIELD = 3;
    private static final int TRIP_POS_ID_FIELD = 4;

    private static final Structure CONNECTIONS_STRUCTURE = new Structure(
            Structure.field(DEP_STOP_ID_FIELD, Structure.FieldType.U16),
            Structure.field(DEP_MINUTES_FIELD, Structure.FieldType.U16),
            Structure.field(ARR_STOP_ID_FIELD, Structure.FieldType.U16),
            Structure.field(ARR_MINUTES_FIELD, Structure.FieldType.U16),
            Structure.field(TRIP_POS_ID_FIELD, Structure.FieldType.S32)
    );

    private final StructuredBuffer buffer;
    private final IntBuffer nextConnections;

    /**
     * Construit une instance de BufferedConnections à partir d’un ByteBuffer principal
     * et d’un ByteBuffer contenant les indices des liaisons suivantes.
     *
     * @param buffer le ByteBuffer contenant les données aplaties des liaisons
     * @param succBuffer le ByteBuffer contenant les indices des liaisons suivantes
     * @throws NullPointerException si buffer ou succBuffer est null
     * @throws IllegalArgumentException si la capacité du buffer n'est pas un multiple de la taille de la structure
     *         ou si succBuffer ne contient pas exactement le bon nombre d’entiers
     */
    public BufferedConnections(ByteBuffer buffer, ByteBuffer succBuffer) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(succBuffer);

        this.buffer = new StructuredBuffer(CONNECTIONS_STRUCTURE, buffer);
        int n = this.buffer.size();
        Preconditions.checkArgument(succBuffer.remaining() == Integer.BYTES * n);
        this.nextConnections = succBuffer.asIntBuffer();
    }

    @Override
    public int size() {
        return buffer.size();
    }

    @Override
    public int depStopId(int id) {
        return buffer.getU16(DEP_STOP_ID_FIELD, id);
    }

    @Override
    public int depMins(int id) {
        return buffer.getU16(DEP_MINUTES_FIELD, id);
    }

    @Override
    public int arrStopId(int id) {
        return buffer.getU16(ARR_STOP_ID_FIELD, id);
    }

    @Override
    public int arrMins(int id) {
        return buffer.getU16(ARR_MINUTES_FIELD, id);
    }

    @Override
    public int tripId(int id) {
        int packed = buffer.getS32(TRIP_POS_ID_FIELD, id);
        return Bits32_24_8.unpack24(packed);
    }

    @Override
    public int tripPos(int id) {
        int packed = buffer.getS32(TRIP_POS_ID_FIELD, id);
        return Bits32_24_8.unpack8(packed);
    }

    @Override
    public int nextConnectionId(int id) {
        return nextConnections.get(id);
    }
}
