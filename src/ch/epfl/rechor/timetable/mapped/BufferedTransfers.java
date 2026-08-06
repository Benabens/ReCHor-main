package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.Preconditions;
import ch.epfl.rechor.timetable.Transfers;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * BufferedTransfers permet d’accéder à une table aplatie de changements et à une table auxiliaire
 * contenant l’index de la liaison suivante pour chaque changement.
 *
 * La table principale contient trois champs :
 *  • À l’index 0, DEP_STATION_ID (U16) : l’index de la gare de départ.
 *  • À l’index 1, ARR_STATION_ID (U16) : l’index de la gare d’arrivée.
 *  • À l’index 2, TRANSFER_MINUTES (U8) : la durée du changement, en minutes.
 *
 * Auteurs : Benjamin (392901) et Jeremy (397366)
 */
public final class BufferedTransfers implements Transfers {

    private static final int DEP_STATION_FIELD       = 0;
    private static final int ARR_STATION_FIELD       = 1;
    private static final int TRANSFER_MINUTES_FIELD = 2;

    private static final Structure TRANSFERS_STRUCTURE = new Structure(
            Structure.field(DEP_STATION_FIELD,       Structure.FieldType.U16),
            Structure.field(ARR_STATION_FIELD,       Structure.FieldType.U16),
            Structure.field(TRANSFER_MINUTES_FIELD,  Structure.FieldType.U8)
    );

    private final StructuredBuffer buffer;
    private final int[] arrivalRanges;

    /**
     * Construit une instance de BufferedTransfers à partir d’un ByteBuffer.
     * La capacité du ByteBuffer doit être un multiple de TRANSFERS_STRUCTURE.totalSize().
     *
     * @param buffer le ByteBuffer contenant les données aplaties des changements
     * @throws NullPointerException if buffer is null
     * @throws IllegalArgumentException if buffer capacity is not a multiple of the structure size
     */
    public BufferedTransfers(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        this.buffer = new StructuredBuffer(TRANSFERS_STRUCTURE, buffer);
        int n = this.buffer.size();

        // Calcule les intervalles d'arrivée en une seule passe
        List<Integer> rangesList = new ArrayList<>();
        int currentArr = -1;
        int start      = 0;

        for (int i = 0; i < n; i++) {
            int arrId = this.buffer.getU16(ARR_STATION_FIELD, i);
            if (arrId != currentArr) {
                if (currentArr != -1) {
                    rangesList.set(currentArr, PackedRange.pack(start, i));
                }
                while (rangesList.size() <= arrId) {
                    rangesList.add(-1);
                }
                currentArr = arrId;
                start      = i;
            }
        }
        if (currentArr != -1) {
            rangesList.set(currentArr, PackedRange.pack(start, n));
        }
        arrivalRanges = rangesList.stream().mapToInt(x -> x).toArray();
    }

    @Override public int size()                   { return buffer.size(); }
    @Override public int depStationId(int id)     { return buffer.getU16(DEP_STATION_FIELD, id); }
    @Override public int minutes(int id)          { return buffer.getU8(TRANSFER_MINUTES_FIELD, id); }

    @Override
    public int arrivingAt(int stationId) {
        if (stationId < 0 || stationId >= arrivalRanges.length || arrivalRanges[stationId] == -1) {
            throw new IndexOutOfBoundsException();
        }
        return arrivalRanges[stationId];
    }

    /**
     * Retourne la durée du changement entre les deux gares spécifiées.
     * Utilise l'intervalle pré-calculé via {@link #arrivingAt(int)}.
     *
     * @param depStationId index de la gare de départ
     * @param arrStationId index de la gare d'arrivée
     * @return la durée du changement en minutes
     * @throws NoSuchElementException si aucun changement n'existe entre ces deux gares
     */
    @Override
    public int minutesBetween(int depStationId, int arrStationId) {
        int range = arrivingAt(arrStationId);
        int start = PackedRange.startInclusive(range);
        int end   = PackedRange.endExclusive(range);
        for (int i = start; i < end; i++) {
            if (buffer.getU16(DEP_STATION_FIELD, i) == depStationId) {
                return buffer.getU8(TRANSFER_MINUTES_FIELD, i);
            }
        }
        throw new NoSuchElementException();
    }
}
