package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Stations;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * BufferedStations permet d'accéder à une table de gares représentée de manière aplatie.
 * Les données des gares sont stockées dans un ByteBuffer selon la structure suivante :
 * - Champ NAME_FIELD (U16) : index de chaîne du nom de la gare dans la table des chaînes.
 * - Champ LON_FIELD (S32)    : longitude brute.
 * - Champ LAT_FIELD (S32)    : latitude brute.
 * Chaque gare occupe 10 octets (2 + 4 + 4).
 * La conversion en degrés se fait par la formule : degrees = raw * (360.0 / 2^32).
 * Pour le calcul du facteur de conversion, on utilise Math.scalb(360.0, -32).
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class BufferedStations implements Stations {

    private static final int NAME_FIELD_INDEX = 0;
    private static final int LON_FIELD_INDEX = 1;
    private static final int LAT_FIELD_INDEX = 2;

    private static final double DEGREE_CONVERSION_FACTOR = Math.scalb(360.0, -32);

    private static final Structure STATION_STRUCTURE = new Structure(
            Structure.field(NAME_FIELD_INDEX, Structure.FieldType.U16),
            Structure.field(LON_FIELD_INDEX, Structure.FieldType.S32),
            Structure.field(LAT_FIELD_INDEX, Structure.FieldType.S32)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Construit une instance de BufferedStations à partir d'une table de chaînes et d'un ByteBuffer.
     * Le ByteBuffer doit avoir une capacité multiple de structure.totalSize().
     *
     * @param stringTable la table des chaînes
     * @param buffer le ByteBuffer contenant les données aplaties des gares
     * @throws NullPointerException si stringTable ou buffer est null
     * @throws IllegalArgumentException si la capacité du buffer n'est pas un multiple de la taille de la structure
     */
    public BufferedStations(List<String> stringTable, ByteBuffer buffer) {
        Objects.requireNonNull(stringTable);
        Objects.requireNonNull(buffer);
        this.stringTable = stringTable;
        this.buffer = new StructuredBuffer(STATION_STRUCTURE, buffer);
    }

    /**
     * Retourne le nombre de gares stockées.
     *
     * @return le nombre d'éléments
     */
    @Override
    public int size() {
        return buffer.size();
    }

    /**
     * Retourne le nom de la gare à l'index donné.
     * Le nom est obtenu en lisant l'index de chaîne du champ NAME_FIELD (U16)
     * et en le recherchant dans la table des chaînes.
     *
     * @param index l'index de la gare (0 .. size()-1)
     * @return le nom de la gare
     * @throws IndexOutOfBoundsException si index est invalide
     */
    @Override
    public String name(int index) {
        int nameId = buffer.getU16(NAME_FIELD_INDEX, index);
        return stringTable.get(nameId);
    }

    /**
     * Retourne la longitude de la gare à l'index donné, en degrés.
     * La valeur brute du champ LON_FIELD (S32) est convertie en degrés
     * par multiplication avec le facteur DEGREE_CONVERSION_FACTOR.
     *
     * @param index l'index de la gare (0 .. size()-1)
     * @return la longitude en degrés
     * @throws IndexOutOfBoundsException si index est invalide
     */
    @Override
    public double longitude(int index) {
        int rawLongitude = buffer.getS32(LON_FIELD_INDEX, index);
        return rawLongitude * DEGREE_CONVERSION_FACTOR;
    }

    /**
     * Retourne la latitude de la gare à l'index donné, en degrés.
     * La valeur brute du champ LAT_FIELD (S32) est convertie en degrés
     * par multiplication avec le facteur DEGREE_CONVERSION_FACTOR.
     *
     * @param index l'index de la gare (0 .. size()-1)
     * @return la latitude en degrés
     * @throws IndexOutOfBoundsException si index est invalide
     */
    @Override
    public double latitude(int index) {
        int rawLatitude = buffer.getS32(LAT_FIELD_INDEX, index);
        return rawLatitude * DEGREE_CONVERSION_FACTOR;
    }
}
