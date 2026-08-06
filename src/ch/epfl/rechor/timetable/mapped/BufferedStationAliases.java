package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.StationAliases;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * BufferedStationAliases permet d'accéder à une table de noms alternatifs des gares représentée de manière aplatie.
 * Chaque enregistrement se compose de deux champs :
 * - ALIAS_FIELD (U16) : index de chaîne du nom alternatif dans la table des chaînes
 * - STATION_NAME_FIELD (U16) : index de chaîne du nom officiel de la gare dans la table des chaînes
 * Chaque enregistrement occupe 4 octets.
 *
 * La table des noms alternatifs est utilisée pour associer, par exemple, "Losanna" à "Lausanne".
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class BufferedStationAliases implements StationAliases {

    private static final int ALIAS_FIELD = 0;
    private static final int STATION_NAME_FIELD = 1;

    private static final Structure ALIAS_STRUCTURE = new Structure(
            Structure.field(ALIAS_FIELD, Structure.FieldType.U16),
            Structure.field(STATION_NAME_FIELD, Structure.FieldType.U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Construit une instance de BufferedStationAliases à partir d'une table de chaînes et d'un ByteBuffer.
     * Le ByteBuffer doit avoir une capacité multiple de la taille de la structure (4 octets par enregistrement).
     *
     * @param stringTable la table des chaînes
     * @param buffer le ByteBuffer contenant les données aplaties des alias de gares
     * @throws NullPointerException si stringTable ou buffer est null
     * @throws IllegalArgumentException si la capacité du buffer n'est pas un multiple de structure.totalSize()
     */
    public BufferedStationAliases(List<String> stringTable, ByteBuffer buffer) {
        Objects.requireNonNull(stringTable);
        Objects.requireNonNull(buffer);
        this.stringTable = stringTable;
        this.buffer = new StructuredBuffer(ALIAS_STRUCTURE, buffer);
    }

    /**
     * Retourne le nombre d'alias stockés.
     *
     * @return le nombre d'enregistrements
     */
    @Override
    public int size() {
        return buffer.size();
    }

    /**
     * Retourne le nom alternatif de la gare pour l'index donné.
     * La valeur est obtenue en lisant le champ ALIAS_FIELD (U16) et en le recherchant dans la table des chaînes.
     *
     * @param id l'index de l'alias (0 .. size()-1)
     * @return le nom alternatif
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    @Override
    public String alias(int id) {
        int aliasIndex = buffer.getU16(ALIAS_FIELD, id);
        return stringTable.get(aliasIndex);
    }

    /**
     * Retourne le nom officiel de la gare correspondant à cet alias.
     * La valeur est obtenue en lisant le champ STATION_NAME_FIELD (U16) et en le recherchant dans la table des chaînes.
     *
     * @param id l'index de l'alias (0 .. size()-1)
     * @return le nom officiel de la gare
     * @throws IndexOutOfBoundsException si l'index est invalide
     */
    @Override
    public String stationName(int id) {
        int stationNameIndex = buffer.getU16(STATION_NAME_FIELD, id);
        return stringTable.get(stationNameIndex);
    }
}
